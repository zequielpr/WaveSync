package com.kunano.wavesynch.data.stream.host

import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.OpusNative

class OpusHostEncoder(
    sampleRate: Int = AudioStreamConstants.SAMPLE_RATE,
    channels: Int = AudioStreamConstants.CHANNELS,
) {
    private val encoder = OpusNative.Encoder(sampleRate, channels)

    // MTU-ish, reused
    private val outBuf = ByteArray(1500)

    data class Encoded(val buf: ByteArray, val len: Int)

    init {
        encoder.setInbandFecEnabled(true)
        encoder.setExpectedPacketLossPercent(10)
        encoder.setSignalMusic()
        encoder.setBitrate(AudioStreamConstants.STEREO_BITRATE)
        encoder.setComplexity(AudioStreamConstants.HOST_SPOT_COMPLEXITY)
    }

    fun encodeInto(framePcm: ShortArray): Encoded {
        val n = encoder.encodeInto(framePcm, AudioStreamConstants.SAMPLES_PER_CHANNEL, outBuf)
        require(n > 0) { "Opus encodeInto failed: $n" }
        return Encoded(outBuf, n)
    }

    fun close() = encoder.destroy()
}
