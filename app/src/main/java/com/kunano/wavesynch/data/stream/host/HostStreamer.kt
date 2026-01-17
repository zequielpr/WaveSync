package com.kunano.wavesynch.data.stream.host

import android.Manifest
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.PacketCodec
import com.kunano.wavesynch.data.stream.guest.GuestStreamingData
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class HostStreamer {

    private val lock = Any()
    private val guests = HashMap<String, GuestStreamingData>()

    private var udpSocket: DatagramSocket? = null
    private var encoder: OpusHostEncoder? = null

    private val running = AtomicBoolean(false)

    private val mtuBytes = 1500
    private val outPkt = ByteArray(mtuBytes)

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
            if (encoder == null) encoder = OpusHostEncoder()
        }

        var sock: DatagramSocket
        var enc: OpusHostEncoder
        var targets: Array<InetSocketAddress>
        // capturer pushes frames -> we encode+send inline (same capture thread)
        capturer.start { pcm, seq, tsMs ->
            if (!running.get()) return@start



            synchronized(lock) {
                sock = udpSocket ?: return@start
                if (sock.isClosed) return@start
                enc = encoder ?: return@start

                targets = guests.values
                    .asSequence()
                    .filter { it.isPlaying }
                    .map { it.inetSocketAddress }
                    .toList()
                    .toTypedArray()
            }

            if (targets.isEmpty()) return@start

            val opus = enc.encodeInto(pcm)

            val packetLen = PacketCodec.writeInto(
                out = outPkt,
                seq = seq,
                tsMs = tsMs,
                payload = opus.buf,
                payloadLen = opus.len
            )
            if (packetLen <= 0) return@start

            for (addr in targets) {
                try {
                    sock.send(DatagramPacket(outPkt, packetLen, addr))
                } catch (_: Exception) { }
            }
        }
    }

    fun stopStreaming(capturer: HostAudioCapturer) {
        if (!running.getAndSet(false)) return

        // stop capture first so callbacks stop firing
        try { capturer.stop() } catch (_: Throwable) {}

        synchronized(lock) {
            try { encoder?.close() } catch (_: Throwable) {}
            encoder = null

            try { udpSocket?.close() } catch (_: Throwable) {}
            udpSocket = null
        }
    }
}
