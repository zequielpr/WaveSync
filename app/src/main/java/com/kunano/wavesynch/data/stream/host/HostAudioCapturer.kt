package com.kunano.wavesynch.data.stream.host

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.PacketCodec

class HostAudioCapturer(
    private val mediaProjection: MediaProjection,
) {
    init { System.loadLibrary("wavesynch") }

    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var isCapturing = false

    private lateinit var opusEncoder: OpusHostEncoder

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(queue: PacketQueue) {
        if (isCapturing) return

        opusEncoder = OpusHostEncoder()

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
        ).coerceAtLeast(16 * 1024) // phones benefit from a bit more headroom

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBufferBytes)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        // IMPORTANT: frameSize for Opus is samples per channel.
        // total shorts in PCM frame = framePerChannel * channels
        val framePerChannel = AudioStreamConstants.SAMPLES_PER_CHANNEL
        val frameShorts = framePerChannel * AudioStreamConstants.CHANNELS

        val readBuf = ShortArray(maxOf(frameShorts * 2, 2048))
        val assembler = ShortFrameAssembler(frameShorts)

        // PCM frame ring (no allocations)
        val pcm0 = ShortArray(frameShorts)
        val pcm1 = ShortArray(frameShorts)
        val pcm2 = ShortArray(frameShorts)
        var pcmIx = 0

        // Packet ring (no allocations). 1500 is MTU-ish; adjust if you want safer like 1400.
        val pkt0 = ByteArray(1500)
        val pkt1 = ByteArray(1500)
        val pkt2 = ByteArray(1500)
        var pktIx = 0

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
                    // copy into pcm ring (fixed-size, no allocations)
                    val pcm = when (pcmIx) { 0 -> pcm0; 1 -> pcm1; else -> pcm2 }
                    pcmIx = (pcmIx + 1) % 3
                    System.arraycopy(frameView, 0, pcm, 0, frameShorts)

                    val tsMs = (System.nanoTime() / 1_000_000L).toInt()

                    // Opus encode into reusable buf; get actual len
                    val opus = opusEncoder.encodeInto(pcm) // (buf,len)

                    // write packet into packet ring
                    val pkt = when (pktIx) { 0 -> pkt0; 1 -> pkt1; else -> pkt2 }
                    pktIx = (pktIx + 1) % 3

                    val packetLen = PacketCodec.writeInto(
                        out = pkt,
                        seq = seq,
                        tsMs = tsMs,
                        payload = opus.buf,
                        payloadLen = opus.len
                    )

                    if (packetLen > 0) {
                        queue.offer(PacketQueue.PacketRef(pkt, packetLen))
                        seq++
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
                onFrame(buf) // no clone()
                filled = 0
            }
        }
    }
}
