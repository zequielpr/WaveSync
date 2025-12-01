package com.kunano.wavesynch.data.stream

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import java.io.InputStream

// data/stream/AudioReceiver.kt
class AudioReceiver(
    private val jitterBuffer: JitterBuffer = JitterBuffer(capacityInPackets = 8)
) {

    @Volatile
    private var running = false

    fun start(input: InputStream) {
        if (running) return
        running = true

        val minBuffer = AudioTrack.getMinBufferSize(
            AudioStreamConstants.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioStreamConstants.AUDIO_FORMAT
        )

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioStreamConstants.AUDIO_FORMAT)
                .setSampleRate(AudioStreamConstants.SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            minBuffer,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        // Reader thread
        Thread {
            val buffer = ByteArray(minBuffer)
            try {
                while (running) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    jitterBuffer.push(buffer.copyOf(read))
                }
            } catch (e: Exception) {
                Log.e("AudioReceiver", "Reader error", e)
            } finally {
                try { input.close() } catch (_: Exception) {}
            }
        }.start()

        // Player thread
        Thread {
            try {
                audioTrack.play()
                while (running) {
                    val packet = jitterBuffer.pop()
                    if (packet != null) {
                        audioTrack.write(packet, 0, packet.size)
                    } else {
                        Thread.sleep(5) // avoid busy loop
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioReceiver", "Player error", e)
            } finally {
                try { audioTrack.stop() } catch (_: Exception) {}
                audioTrack.release()
            }
        }.start()
    }

    fun stop() {
        running = false
    }
}
