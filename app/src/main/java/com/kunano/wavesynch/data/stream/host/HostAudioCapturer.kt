package com.kunano.wavesynch.data.stream.host

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Process
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.AudioStreamConstants

class HostAudioCapturer(
    private val mediaProjection: MediaProjection,
) {
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var isCapturing = false

    val frameShorts: Int =
        AudioStreamConstants.SAMPLES_PER_CHANNEL * AudioStreamConstants.CHANNELS

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(onFrame: (pcm: ShortArray, seq: Int, tsMs: Int) -> Unit) {
        if (isCapturing) return

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val encoding = AudioStreamConstants.AUDIO_FORMAT

        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(AudioStreamConstants.SAMPLE_RATE)
            .setChannelMask(AudioStreamConstants.CHANNEL_MASK_IN)
            .build()

        val minBufferBytes = AudioRecord.getMinBufferSize(
            AudioStreamConstants.SAMPLE_RATE,
            AudioStreamConstants.CHANNEL_MASK_IN,
            encoding
        ).coerceAtLeast(16 * 1024)

        val recorder = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBufferBytes)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord = recorder

        val readBuf = ShortArray(maxOf(frameShorts * 2, 2048))
        val assembler = ShortFrameAssembler(frameShorts)

        isCapturing = true

        captureThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

            try {
                recorder.startRecording()

                var seq = 0
                while (isCapturing && !Thread.currentThread().isInterrupted) {
                    val readCount = recorder.read(readBuf, 0, readBuf.size)
                    if (!isCapturing) break
                    if (readCount <= 0) continue

                    assembler.push(readBuf, readCount) { frameView ->
                        val tsMs = (System.nanoTime() / 1_000_000L).toInt()

                        // MUST copy: assembler reuses its internal buffer
                        val frameCopy = frameView.copyOf()

                        onFrame(frameCopy, seq, tsMs)
                        seq++
                    }
                }
            } catch (_: Throwable) {
            } finally {
                try { recorder.stop() } catch (_: Throwable) {}
            }
        }.apply { start() }
    }

    fun stop() {
        isCapturing = false

        // Unblock read()
        try { audioRecord?.stop() } catch (_: Throwable) {}
        captureThread?.interrupt()

        try { captureThread?.join(500) } catch (_: Throwable) {}
        captureThread = null

        try { audioRecord?.release() } catch (_: Throwable) {}
        audioRecord = null

        try { mediaProjection.stop() } catch (_: Throwable) {}
    }
}

/**
 * Assembles arbitrary short chunks into fixed frames.
 * It passes an internal buffer to onFrame; caller MUST copy immediately.
 */
class ShortFrameAssembler(private val frameShorts: Int) {
    private val buf = ShortArray(frameShorts)
    private var filled = 0

    fun push(input: ShortArray, length: Int, onFrame: (ShortArray) -> Unit) {
        var offset = 0
        while (offset < length) {
            val toCopy = minOf(frameShorts - filled, length - offset)
            System.arraycopy(input, offset, buf, filled, toCopy)
            filled += toCopy
            offset += toCopy

            if (filled == frameShorts) {
                onFrame(buf)
                filled = 0
            }
        }
    }
}
