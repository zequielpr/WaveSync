package com.kunano.wavesynch.data.stream.host

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Process
import androidx.annotation.RequiresPermission
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kunano.wavesynch.data.stream.AudioStreamConstants

class HostAudioCapturer(
    private val mediaProjection: MediaProjection,
) {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var isCapturing = false

    private val frameShorts: Int =
        AudioStreamConstants.SAMPLES_PER_CHANNEL * AudioStreamConstants.CHANNELS

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(
        pool: PcmFramePool,
        queue: PcmFrameQueue,
    ) {
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

            var seq = 0
            try {
                recorder.startRecording()

                while (isCapturing && !Thread.currentThread().isInterrupted) {
                    val readCount = recorder.read(readBuf, 0, readBuf.size)
                    if (!isCapturing) break
                    if (readCount <= 0) continue

                    assembler.push(readBuf, readCount) { frameView ->
                        if (!isCapturing) return@push

                        val f = pool.acquire() ?: return@push // pool exhausted -> drop

                        // Copy into pooled frame storage
                        // IMPORTANT: use f.shorts.size (source of truth)
                        System.arraycopy(frameView, 0, f.shorts, 0, f.shorts.size)

                        f.seq = seq
                        f.tsMs = (System.nanoTime() / 1_000_000L).toInt()
                        seq++

                        // Enqueue; if full, drop oldest and return it to pool
                        val dropped = queue.offerDropOldest(f)
                        if (dropped != null) {
                            pool.release(dropped)
                        }
                    }
                }
            } catch (t: Throwable) {
                crashlytics.setCustomKey("captureThread", "HostAudioCapturer")
                crashlytics.log("Audio capture thread error")
                crashlytics.recordException(t)
            } finally {
                try { recorder.stop() } catch (_: Throwable) {}
            }
        }.apply { start() }
    }

    fun stop() {
        isCapturing = false

        // Unblock AudioRecord.read()
        try { audioRecord?.stop() } catch (_: Throwable) {}

        captureThread?.interrupt()
        try { captureThread?.join(500) } catch (_: Throwable) {}
        captureThread = null

        try { audioRecord?.release() } catch (_: Throwable) {}
        audioRecord = null

        // No queue.wakeAll() in ABQ design; sender unblocks via interrupt()

        try { mediaProjection.stop() } catch (_: Throwable) {}
    }
}

/**
 * Assembles arbitrary short chunks into fixed frames.
 * It passes an internal buffer to onFrame; caller must copy.
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
