package com.kunano.wavesynch.data.stream

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.net.DatagramPacket

class HostAudioCapturer(
    private val mediaProjection: MediaProjection,
) {
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var isCapturing = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun start(onPcmFrame: (ByteArray) -> Unit) {
        if (isCapturing) return

        val compressor = OpusNative.Encoder(AudioStreamConstants.SAMPLE_RATE, 1)

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        // IMPORTANT: Opus wants 48k. If your constant isn't 48000, change it.
        val sampleRate = AudioStreamConstants.SAMPLE_RATE
        val channelMask = AudioStreamConstants.CHANNEL_MASK_IN
        val encoding = AudioFormat.ENCODING_PCM_16BIT

        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .build()

        val channels = when (channelMask) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> 2 // fallback
        }

        // Buffer size in bytes (AudioRecord API requires bytes)
        val minBufferBytes = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding)
            .coerceAtLeast(4096)

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBufferBytes)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        // Choose an Opus-friendly frame size:
        // 20ms @ 48k = 960 samples per channel
        // Total shorts per frame = frameSizePerCh * channels
        val frameSizePerChannel = 960
        val frameShorts = frameSizePerChannel * channels

        isCapturing = true

        captureThread = Thread {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            val recorder = audioRecord ?: return@Thread
            recorder.startRecording()

            // Read in short samples
            val readShorts = maxOf(frameShorts, minBufferBytes / 2 /*bytes->shorts*/)
            val readBuf = ShortArray(readShorts)

            val assembler = ShortFrameAssembler(frameShorts)

            var seq = 0
            while (isCapturing) {
                val readCount = recorder.read(readBuf, 0, readBuf.size)
                if (readCount <= 0) continue

                assembler.push(readBuf, readCount) { frame ->
                    val tsMs = (System.nanoTime() / 1_000_000L).toInt()
                    val compressedFrame = compressor.encode(frame, frameShorts)
                    val packet = PacketCodec.encode(seq, tsMs, compressedFrame, compressedFrame.size)
                    onPcmFrame(packet) // frame is ShortArray sized exactly frameShorts
                    seq++
                }
            }

            try { recorder.stop() } catch (_: Throwable) {}
        }.apply { start() }
    }

    fun stop() {
        isCapturing = false
        captureThread?.join(300)
        captureThread = null

        audioRecord?.let {
            try { it.release() } catch (_: Throwable) {}
        }
        audioRecord = null

        try { mediaProjection.stop() } catch (_: Throwable) {}
    }
}

private class ShortFrameAssembler(private val frameShorts: Int) {
    private val buf = ShortArray(frameShorts)
    private var filled = 0

    fun push(input: ShortArray, length: Int, onFrame: (ShortArray) -> Unit) {
        var offset = 0
        while (offset < length) {
            val toCopy = minOf(frameShorts - filled, length - offset)
            java.lang.System.arraycopy(input, offset, buf, filled, toCopy)
            filled += toCopy
            offset += toCopy

            if (filled == frameShorts) {
                onFrame(buf.clone())
                filled = 0
            }
        }
    }
}