package com.kunano.wavesynch.data.stream.host

import android.Manifest
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kunano.wavesynch.CrashReporter
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.PacketCodec
import com.kunano.wavesynch.data.stream.guest.GuestStreamingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class HostStreamer {

    private val lock = Any()
    private val guests = HashMap<String, GuestStreamingData>()

    private var udpSocket: DatagramSocket? = null
    private var encoder: OpusHostEncoder? = null

    private var sendThread: Thread? = null
    private val running = AtomicBoolean(false)

    private val mtuBytes = 1500
    private val outPkt = ByteArray(mtuBytes)
    val pool: PcmFramePool = PcmFramePool(
    frameShorts = AudioStreamConstants.SAMPLES_PER_PACKET,
    poolSize = 32
    )
    val queue: PcmFrameQueue = PcmFrameQueue(capacity = 16)

    private val _isHostStreamingFlow = MutableStateFlow(false)
    val isHostStreamingFlow: StateFlow<Boolean> = _isHostStreamingFlow

    fun addGuest(id: String, inetSocketAddress: InetSocketAddress) = synchronized(lock) {
        guests[id] = GuestStreamingData(id, inetSocketAddress, isPlaying = true)
    }

    fun pauseGuest(id: String) = synchronized(lock) { guests[id]?.isPlaying = false }
    fun resumeGuest(id: String) = synchronized(lock) { guests[id]?.isPlaying = true }
    fun removeGuest(id: String) = synchronized(lock) { guests.remove(id) }
    fun removeAllGuests() = synchronized(lock) { guests.clear() }



    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startStreaming(
        capturer: HostAudioCapturer,
    ) {
        if (running.getAndSet(true)) return
        _isHostStreamingFlow.tryEmit(true)

        synchronized(lock) {
            if (udpSocket == null || udpSocket?.isClosed == true) {
                udpSocket = DatagramSocket().apply { reuseAddress = true }
            }
            if (encoder == null) encoder = OpusHostEncoder()
        }

        // Producer: capture thread writes into queue (your capturer must use pool+queue now)
        capturer.start(pool, queue)

        // Consumer: send thread reads from queue
        sendThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            try {
                while (running.get() && !Thread.currentThread().isInterrupted) {
                    // Blocking take; returns null if interrupted
                    val frame = queue.takeBlocking() ?: break

                    val sock: DatagramSocket?
                    val enc: OpusHostEncoder?
                    val targets: Array<InetSocketAddress>

                    synchronized(lock) {
                        sock = udpSocket
                        enc = encoder
                        targets = guests.values
                            .asSequence()
                            .filter { it.isPlaying }
                            .map { it.inetSocketAddress }
                            .toList()
                            .toTypedArray()
                    }

                    if (sock == null || sock.isClosed || enc == null) {
                        pool.release(frame)
                        continue
                    }

                    if (targets.isEmpty()) {
                        pool.release(frame)
                        continue
                    }

                    val opus = enc.encodeInto(frame.shorts)

                    val packetLen = PacketCodec.writeInto(
                        out = outPkt,
                        seq = frame.seq,
                        tsMs = frame.tsMs,
                        payload = opus.buf,
                        payloadLen = opus.len
                    )

                    // Return frame to pool ASAP
                    pool.release(frame)

                    if (packetLen <= 0) continue

                    for (addr in targets) {
                        try {
                            sock.send(DatagramPacket(outPkt, packetLen, addr))
                        }catch (io: IOException){
                            CrashReporter.log(io.message.toString())
                            CrashReporter.set("hostStreamer", io.message.toString())
                            CrashReporter.record(io)
                        }
                        catch (ex: Exception) {
                            CrashReporter.log(ex.message.toString())
                            CrashReporter.set("hostStreamer", ex.message.toString())
                            CrashReporter.record(ex)
                            Log.e("HostStreamer", "UDP send error", ex)
                        }
                    }
                }
            } catch (t: Throwable) {
                CrashReporter.log(t.message.toString())
                CrashReporter.set("hostStreamer", t.message.toString())
                CrashReporter.record(t)
                Log.e("HostStreamer", "sendThread error", t)
            }
        }.apply { start() }
    }

    fun stopStreaming(
        capturer: HostAudioCapturer,
    ) {
        if (!running.getAndSet(false)) return

        // Stop producer first
        try { capturer.stop() } catch (_: Throwable) {}

        // Stop consumer: interrupt unblocks take()
        sendThread?.interrupt()
        try { sendThread?.join(500) } catch (_: Throwable) {}
        sendThread = null

        // Drain leftover frames and return them to pool
        try {
            queue.drainAll().forEach { pool.release(it) }
        } catch (_: Throwable) {}

        synchronized(lock) {
            try { encoder?.close() } catch (_: Throwable) {}
            encoder = null

            try { udpSocket?.close() } catch (_: Throwable) {}
            udpSocket = null
        }

        _isHostStreamingFlow.tryEmit(false)
    }
}
