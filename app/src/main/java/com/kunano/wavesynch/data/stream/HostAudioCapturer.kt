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
    fun start(onChunk: (ByteArray) -> Unit) {
        if (isCapturing) return

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioStreamConstants.AUDIO_FORMAT)
            .setSampleRate(AudioStreamConstants.SAMPLE_RATE)
            .setChannelMask(AudioStreamConstants.CHANNEL_MASK_IN)
            .build()

        val minBuffer = AudioRecord.getMinBufferSize(
            AudioStreamConstants.SAMPLE_RATE,
            AudioStreamConstants.CHANNEL_MASK_IN,
            AudioStreamConstants.AUDIO_FORMAT
        ).coerceAtLeast(AudioStreamConstants.PAYLOAD_BYTES * 2)

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuffer)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        isCapturing = true

        captureThread = Thread {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val recorder = audioRecord ?: return@Thread
            val readBuf = ByteArray(minBuffer / 2) // read in chunks
            val assembler = FrameAssembler(AudioStreamConstants.PAYLOAD_BYTES)

            var seq = 0
            recorder.startRecording()

            while (isCapturing) {
                val read = recorder.read(readBuf, 0, readBuf.size)
                if (read <= 0) continue

                assembler.push(readBuf, read) { payload ->
                    val tsMs = (System.nanoTime() / 1_000_000L).toInt()
                    val packet = PacketCodec.encode(seq, tsMs, payload, payload.size)
                    Log.d("HostAudioCapturer", "chunk size: ${packet.size}")
                    onChunk(packet)
                    seq++ // increment ONLY when a packet is emitted
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

private class FrameAssembler(private val frameSize: Int) {
    private val buf = ByteArray(frameSize)
    private var filled = 0

    /**
     * Push bytes in; whenever a full frame is ready, calls onFrame(frameBytes).
     * Note: onFrame receives a copy-safe frame buffer.
     */
    fun push(input: ByteArray, length: Int, onFrame: (ByteArray) -> Unit) {
        var offset = 0
        while (offset < length) {
            val toCopy = minOf(frameSize - filled, length - offset)
            System.arraycopy(input, offset, buf, filled, toCopy)
            filled += toCopy
            offset += toCopy

            if (filled == frameSize) {
                // Emit a frame. Copy to avoid mutation issues.
                onFrame(buf.clone())
                filled = 0
            }
        }
    }
}

