package com.kunano.wavesynch.data.stream

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class HostAudioCapturer(
    private val mediaProjection: MediaProjection,
) {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_STEREO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var isCapturing = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun start(onChunk: (ByteArray) -> Unit) {
        if (isCapturing) return

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(ENCODING)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL_MASK)
            .build()

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_MASK,
            ENCODING
        )

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuffer * 2)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        isCapturing = true

        captureThread = Thread {
            val buffer = ByteArray(minBuffer)
            audioRecord?.startRecording()

            while (isCapturing) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    // slice to actual size
                    val chunk = buffer.copyOf(read)
                    onChunk(chunk)  // → AudioSender
                }
            }

            audioRecord?.stop()
        }.apply { start() }
    }

    fun stop() {
        isCapturing = false
        captureThread?.join(200)
        captureThread = null
        audioRecord?.release()
        audioRecord = null
        mediaProjection.stop()
    }
}
