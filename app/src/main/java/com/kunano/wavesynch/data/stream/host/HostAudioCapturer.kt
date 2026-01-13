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

    /** Shorts per PCM frame (fixed) */
    val frameShorts: Int =
        AudioStreamConstants.SAMPLES_PER_CHANNEL * AudioStreamConstants.CHANNELS

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(queue: PcmFrameQueue, pool: PcmBufferPool) {
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

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBufferBytes)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        val readBuf = ShortArray(maxOf(frameShorts * 2, 2048))
        val assembler = ShortFrameAssembler(frameShorts)

        isCapturing = true

        captureThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

            val recorder = audioRecord ?: return@Thread
            recorder.startRecording()

            var seq = 0
            while (isCapturing) {
                val readCount = recorder.read(readBuf, 0, readBuf.size)
                if (readCount <= 0) continue

                assembler.push(readBuf, readCount) { frameView ->
                    val pcm = pool.acquire()
                    if (pcm == null) {
                        // Streamer is lagging; drop frame (do NOT block audio thread)
                        return@push
                    }

                    System.arraycopy(frameView, 0, pcm, 0, frameShorts)
                    val tsMs = (System.nanoTime() / 1_000_000L).toInt()

                    val ok = queue.offer(PcmFrameQueue.Frame(pcm, frameShorts, seq, tsMs))
                    if (ok) {
                        seq++
                    } else {
                        // Queue full; drop + release buffer back to pool
                        pool.release(pcm)
                    }
                }
            }

            try { recorder.stop() } catch (_: Throwable) {}
        }.apply { start() }
    }

    fun stop() {
        isCapturing = false
        try { captureThread?.join(500) } catch (_: Throwable) {}
        captureThread = null

        audioRecord?.let { try { it.release() } catch (_: Throwable) {} }
        audioRecord = null

        try { mediaProjection.stop() } catch (_: Throwable) {}
    }
}


/**
 * Assembles arbitrary short chunks into fixed frames of frameShorts.
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

