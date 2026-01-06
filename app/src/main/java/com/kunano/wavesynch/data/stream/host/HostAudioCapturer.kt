package com.kunano.wavesynch.data.stream.host

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
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.PacketCodec

class HostAudioCapturer(
    private val mediaProjection: MediaProjection,
) {
    init {
        System.loadLibrary("wavesynch")
    }

    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var isCapturing = false

    private lateinit var opusEncoder: OpusHostEncoder

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun start(onPacket: (ByteArray) -> Unit) {
        if (isCapturing) return

        opusEncoder = OpusHostEncoder() // ensure this uses SAMPLES_PER_PACKET internally (480)

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val sampleRate = AudioStreamConstants.SAMPLE_RATE
        val channelMask = AudioStreamConstants.CHANNEL_MASK_IN
        val encoding = AudioStreamConstants.AUDIO_FORMAT

        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .build()

        val channels = when (channelMask) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> 1
        }

        // AudioRecord buffer size in BYTES
        val minBufferBytes = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding)
            .coerceAtLeast(4096)

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBufferBytes)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        // ---------- Correct frame sizing ----------
        // 10ms @ 48kHz => 480 samples per channel
        val frameShorts = AudioStreamConstants.SAMPLES_PER_PACKET * channels // total shorts in one frame across channels

        // Read in small-ish chunks to avoid extra latency; assembler will pack exact frames
        val readShorts = maxOf(frameShorts * 2, 1024)
        val readBuf = ShortArray(readShorts)

        val assembler = ShortFrameAssembler(frameShorts)

        isCapturing = true

        captureThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            val recorder = audioRecord ?: return@Thread
            recorder.startRecording()

            var seq = 0

            while (isCapturing) {
                val readCount = recorder.read(readBuf, 0, readBuf.size)
                if (readCount <= 0) continue

                assembler.push(readBuf, readCount) { frame ->
                    // Timestamp can be whatever your PacketCodec expects
                    val tsMs = (System.nanoTime() / 1_000_000L).toInt()

                    Log.d("tag", "Frame size: ${frame.size}")

                    // Encode frame -> Opus payload (variable length)
                    val opusPayload = opusEncoder.encode(frame)

                    // Packetize: header + opus payload
                    val packet = PacketCodec.encode(seq, tsMs, opusPayload, opusPayload.size)

                    onPacket(packet)
                    seq++
                }
            }

            try { recorder.stop() } catch (_: Throwable) {}
        }.apply { start() }
    }

    fun stop() {
        isCapturing = false

        try { captureThread?.join(300) } catch (_: Throwable) {}
        captureThread = null

        audioRecord?.let {
            try { it.release() } catch (_: Throwable) {}
        }
        audioRecord = null

        if (::opusEncoder.isInitialized) opusEncoder.close()

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
            System.arraycopy(input, offset, buf, filled, toCopy)
            filled += toCopy
            offset += toCopy

            if (filled == frameShorts) {
                // Clone so downstream code can keep it; avoids mutation on next fill
                onFrame(buf.clone())
                filled = 0
            }
        }
    }
}
