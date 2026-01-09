package com.kunano.wavesynch.data.stream.host

import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.OpusNative

class OpusHostEncoder(
    sampleRate: Int = AudioStreamConstants.SAMPLE_RATE,
    channels: Int = AudioStreamConstants.CHANNELS,
) {
    private val encoder = OpusNative.Encoder(sampleRate, channels)


    init {
        encoder.setInbandFecEnabled(true)
        encoder.setExpectedPacketLossPercent(10)
        encoder.setSignalMusic()
        encoder.setBitrate(AudioStreamConstants.STEREO_BITRATE)
        encoder.setComplexity(AudioStreamConstants.HOST_SPOT_COMPLEXITY)
    }

    fun encode(framePcm: ShortArray): ByteArray {
        val out = encoder.encode(framePcm, AudioStreamConstants.SAMPLES_PER_CHANNEL)
        return out
    }

    fun close() {
        encoder.destroy()
    }
}
