package com.kunano.wavesynch.data.stream.host

import android.Manifest
import android.os.Process
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.PacketCodec
import com.kunano.wavesynch.data.stream.guest.GuestStreamingData
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ArrayBlockingQueue

class HostStreamer {
    private val lock = Any()
    private val guests = HashMap<String, GuestStreamingData>()

    private var udpSocket: DatagramSocket? = null

    private val running = AtomicBoolean(false)
    private var senderThread: Thread? = null

    // buffering targets
    private val queueCapacity = 128          // frames buffered
    private val pcmPoolSize = 192            // MUST be >= queueCapacity + headroom
    private val pktPoolSize = 192
    private val mtuBytes = 1500

    private var pcmQueue: PcmFrameQueue? = null
    private var pcmPool: PcmBufferPool? = null
    private var pktPool: PacketBufferPool? = null

    fun addGuest(id: String, inetSocketAddress: InetSocketAddress) = synchronized(lock) {
        guests[id] = GuestStreamingData(id, inetSocketAddress, isPlaying = true)
    }

    fun pauseGuest(id: String) = synchronized(lock) { guests[id]?.isPlaying = false }
    fun resumeGuest(id: String) = synchronized(lock) { guests[id]?.isPlaying = true }
    fun removeGuest(id: String) = synchronized(lock) { guests.remove(id) }
    fun removeAllGuests() = synchronized(lock) { guests.clear() }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startStreaming(capturer: HostAudioCapturer) {
        if (running.getAndSet(true)) return

        synchronized(lock) {
            if (udpSocket == null || udpSocket?.isClosed == true) {
                udpSocket = DatagramSocket().apply { reuseAddress = true }
            }
        }

        val q = PcmFrameQueue(queueCapacity)
        val pcmPoolLocal = PcmBufferPool(capturer.frameShorts, pcmPoolSize)
        val pktPoolLocal = PacketBufferPool(mtuBytes, pktPoolSize)

        pcmQueue = q
        pcmPool = pcmPoolLocal
        pktPool = pktPoolLocal

        // capture -> PCM queue (no encoding here)
        capturer.start(q, pcmPoolLocal)

        senderThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            val encoder = OpusHostEncoder()

            var cachedTargets: Array<InetSocketAddress> = emptyArray()
            var lastTargetsRefreshNs = 0L

            while (running.get()) {
                val frame = q.take(timeoutMs = 50) ?: continue

                val sock = synchronized(lock) { udpSocket }
                if (sock == null || sock.isClosed) {
                    pcmPoolLocal.release(frame.pcm)
                    continue
                }

                // refresh targets ~5x/sec
                val now = System.nanoTime()
                if (now - lastTargetsRefreshNs > 200_000_000L) {
                    cachedTargets = synchronized(lock) {
                        guests.values
                            .asSequence()
                            .filter { it.isPlaying }
                            .map { it.inetSocketAddress }
                            .toList()
                            .toTypedArray()
                    }
                    lastTargetsRefreshNs = now
                }

                if (cachedTargets.isEmpty()) {
                    pcmPoolLocal.release(frame.pcm)
                    continue
                }

                // ---- encode PCM -> Opus (ADAPT THIS LINE if your signature differs)
                val opus = encoder.encodeInto(frame.pcm) // expected: opus.buf (ByteArray), opus.len (Int)

                // release PCM ASAP
                pcmPoolLocal.release(frame.pcm)

                val outPkt = pktPoolLocal.acquire()
                if (outPkt == null) continue

                val packetLen = PacketCodec.writeInto(
                    out = outPkt,
                    seq = frame.seq,
                    tsMs = frame.tsMs,
                    payload = opus.buf,
                    payloadLen = opus.len
                )

                if (packetLen <= 0) {
                    pktPoolLocal.release(outPkt)
                    continue
                }

                for (addr in cachedTargets) {
                    try {
                        sock.send(DatagramPacket(outPkt, packetLen, addr))
                    } catch (_: Exception) { }
                }

                pktPoolLocal.release(outPkt)
            }

            try { encoder.close() } catch (_: Throwable) {}
        }.apply { start() }
    }

    fun stopStreaming(capturer: HostAudioCapturer) {
        running.set(false)
        pcmQueue?.clear()

        try { senderThread?.join(500) } catch (_: Throwable) {}
        senderThread = null

        try { capturer.stop() } catch (_: Throwable) {}

        synchronized(lock) {
            try { udpSocket?.close() } catch (_: Throwable) {}
            udpSocket = null
        }

        pcmQueue = null
        pcmPool = null
        pktPool = null
    }
}





class PacketBufferPool(
    mtuBytes: Int,
    poolSize: Int
) {
    private val free = ArrayBlockingQueue<ByteArray>(poolSize)

    init {
        repeat(poolSize) { free.offer(ByteArray(mtuBytes)) }
    }

    fun acquire(): ByteArray? = free.poll()

    fun release(buf: ByteArray) {
        free.offer(buf)
    }
}

