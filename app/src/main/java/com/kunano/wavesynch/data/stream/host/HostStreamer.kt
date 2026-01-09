package com.kunano.wavesynch.data.stream.host

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.guest.GuestStreamingData
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class HostStreamer {

    private val lock = Any()
    private val guests = HashMap<String, GuestStreamingData>()

    @Volatile private var isHostStreaming = false

    private var audioCapturer: HostAudioCapturer? = null
    private var udpSocket: DatagramSocket? = null

    init {
        udpSocket = DatagramSocket().apply { reuseAddress = true }
    }

    fun addGuest(id: String, inetSocketAddress: InetSocketAddress) = synchronized(lock) {
        guests[id] = GuestStreamingData(id, inetSocketAddress, isPlaying = true)
    }

    fun pauseGuest(id: String) = synchronized(lock) { guests[id]?.isPlaying = false }
    fun resumeGuest(id: String) = synchronized(lock) { guests[id]?.isPlaying = true }
    fun removeGuest(id: String) = synchronized(lock) { guests.remove(id) }
    fun removeAllGuests() = synchronized(lock) { guests.clear() }

    fun isHostStreaming(): Boolean = isHostStreaming

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun startStreaming(capturer: HostAudioCapturer) {
        if (isHostStreaming) return

        synchronized(lock) {
            if (udpSocket == null || udpSocket?.isClosed == true) {
                udpSocket = DatagramSocket().apply { reuseAddress = true }
            }
            audioCapturer = capturer
            isHostStreaming = true
        }

        // IMPORTANT: capturer.start runs its own capture thread; no need to create another thread here.
        capturer.start { chunk ->
            if (!isHostStreaming) return@start

            // If there’s any chance the encoder reuses `chunk`, copy it.
            // Start with copyOf() to make it correct; optimize later with a buffer pool.
            val payload = chunk.copyOf()

            val (sock, targets) = synchronized(lock) {
                val s = udpSocket
                val t = guests.values
                    .asSequence()
                    .filter { it.isPlaying }
                    .map { it.inetSocketAddress }
                    .toList()
                s to t
            }

            val s = sock ?: return@start
            if (s.isClosed) return@start
            if (targets.isEmpty()) return@start

            for (addr in targets) {
                try {
                    val dp = DatagramPacket(payload, payload.size, addr)
                    s.send(dp)
                } catch (_: Exception) {
                    // swallow per guest
                }
            }
        }
    }

    fun pauseStreaming() {
        stopCapturingAudio()
    }

    fun stopStreaming() {
        isHostStreaming = false
        stopCapturingAudio()

        synchronized(lock) {
            try { udpSocket?.close() } catch (_: Exception) {}
            udpSocket = null
            audioCapturer = null
        }
    }

    fun stopCapturingAudio() {
        val capturer = synchronized(lock) { audioCapturer }
        try { capturer?.stop() } catch (_: Exception) {}
    }
}
