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
import java.net.DatagramPacket

class HostAudioCapturer(
    private val mediaProjection: MediaProjection,
) {

    companion object {

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
            .setEncoding(AudioStreamConstants.AUDIO_FORMAT)
            .setSampleRate(AudioStreamConstants.SAMPLE_RATE)
            .setChannelMask(AudioStreamConstants.CHANNEL_MASK)
            .build()

        val minBuffer = AudioRecord.getMinBufferSize(
            AudioStreamConstants.SAMPLE_RATE,
            AudioStreamConstants.CHANNEL_MASK,
            AudioStreamConstants.AUDIO_FORMAT
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


            // Reuse DatagramPacket object to reduce allocations
            val dp = DatagramPacket(ByteArray(0), 0)
            // 20ms PCM @ 48k mono 16-bit
            val pcm = ByteArray(1920)

            var seq = 0
            while (isCapturing) {
                val read =audioRecord?.read(pcm, 0, pcm.size)
                if (read != null) {
                    if (read <= 0) continue

                    val tsMs = (System.nanoTime() / 1_000_000L).toInt()

                    // Encode ONCE per frame
                    val chunk= PacketCodec.encode(seq, tsMs, pcm, read)

                    onChunk(chunk)  // → AudioSender
                }

                seq++
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
