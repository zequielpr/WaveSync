package com.kunano.wavesynch.data.stream

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioReceiver(
    private val jitterBuffer: JitterBuffer = JitterBuffer()
) {

    @Volatile
    private var running = false

    private fun InputStream.readFully(buf: ByteArray, len: Int): Boolean {
        var total = 0
        while (total < len) {
            val r = this.read(buf, total, len - total)
            if (r <= 0) return false
            total += r
        }
        return true
    }

    fun start(input: InputStream) {
        if (running) return
        running = true

        val minTrackBuffer = AudioTrack.getMinBufferSize(
            AudioStreamConstants.SAMPLE_RATE,
            AudioStreamConstants.CHANNEL_OUT,
            AudioStreamConstants.AUDIO_FORMAT
        )

        val trackBuffer = minTrackBuffer * AudioLatencyConfig.TRACK_BUFFER_FACTOR

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioStreamConstants.AUDIO_FORMAT)
                .setSampleRate(AudioStreamConstants.SAMPLE_RATE)
                .setChannelMask(AudioStreamConstants.CHANNEL_OUT)
                .build(),
            trackBuffer,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        // ---------- Reader ----------
        Thread {
            val header = ByteArray(4)
            try {
                while (running) {
                    if (!input.readFully(header, 4)) break
                    val size = java.nio.ByteBuffer.wrap(header)
                        .order(java.nio.ByteOrder.BIG_ENDIAN)
                        .int
                    if (size <= 0 || size > 200_000) break

                    val packet = ByteArray(size)
                     if (!input.readFully(packet, size)) break

                    jitterBuffer.push(packet)
                }
            } catch (e: Exception) {
                Log.e("AudioReceiver", "Reader error", e)
            } finally {
                try { input.close() } catch (_: Exception) {}
            }
        }.start()

        // ---------- Player ----------
        Thread {
            try {
                // warm up less aggressively
                while (running && jitterBuffer.size() < AudioLatencyConfig.WARMUP_PACKETS) {
                    Thread.sleep(3)
                }

                audioTrack.play()

                while (running) {
                    val packet = jitterBuffer.pop()
                    if (packet != null) {
                        audioTrack.write(packet, 0, packet.size)
                    } else {
                        // no data: wait a tiny bit, don't spin
                        Thread.sleep(2)
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioReceiver", "Player error", e)
            } finally {
                try { audioTrack.stop() } catch (_: Throwable) {}
                audioTrack.release()
            }
        }.start()
    }

    fun stop() {
        running = false
    }
}
