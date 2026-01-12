package com.kunano.wavesynch.data.stream.host

import android.Manifest
import android.os.Process
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.guest.GuestStreamingData
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class HostStreamer {
    private val lock = Any()
    private val guests = HashMap<String, GuestStreamingData>()

    private var udpSocket: DatagramSocket? = null

    private val running = AtomicBoolean(false)
    private var senderThread: Thread? = null

    private val queue = PacketQueue(capacity = 128) // ~0.6s @ 20ms, tune as you want

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

        // start capture -> queue
        capturer.start(queue)

        // start sender
        senderThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            var cachedTargets: Array<InetSocketAddress> = emptyArray()
            var lastTargetsRefreshNs = 0L

            while (running.get()) {
                val pkt = queue.take(timeoutMs = 50) ?: continue

                val sock = synchronized(lock) { udpSocket } ?: continue
                if (sock.isClosed) continue

                val now = System.nanoTime()
                if (now - lastTargetsRefreshNs > 200_000_000L) { // refresh ~5x/sec
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

                if (cachedTargets.isEmpty()) continue

                for (addr in cachedTargets) {
                    try {
                        sock.send(DatagramPacket(pkt.buf, pkt.len, addr))
                    } catch (_: Exception) { }
                }
            }
        }.apply { start() }
    }

    fun stopStreaming(capturer: HostAudioCapturer) {
        running.set(false)
        queue.clear()

        try { senderThread?.join(500) } catch (_: Throwable) {}
        senderThread = null

        try { capturer.stop() } catch (_: Throwable) {}

        synchronized(lock) {
            try { udpSocket?.close() } catch (_: Throwable) {}
            udpSocket = null
        }
    }
}
