package com.kunano.wavesynch.data.stream

import android.Manifest
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import java.io.OutputStream

// data/stream/AudioSender.kt
class AudioSender {

    @Volatile
    private var running = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(output: OutputStream) {
        if (running) return
        running = true

        val minBuffer = AudioRecord.getMinBufferSize(
            AudioStreamConstants.SAMPLE_RATE,
            AudioStreamConstants.CHANNEL_CONFIG,
            AudioStreamConstants.AUDIO_FORMAT,
        )

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            AudioStreamConstants.SAMPLE_RATE,
            AudioStreamConstants.CHANNEL_CONFIG,
            AudioStreamConstants.AUDIO_FORMAT,
            minBuffer,
        )

        val buffer = ByteArray(minBuffer)

        Thread {
            try {
                audioRecord.startRecording()
                while (running) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        output.write(buffer, 0, read)
                        output.flush()
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioSender", "Error", e)
            } finally {
                try { audioRecord.stop() } catch (_: Exception) {}
                audioRecord.release()
                try { output.close() } catch (_: Exception) {}
            }
        }.start()
    }

    fun stop() {
        running = false
    }
}
