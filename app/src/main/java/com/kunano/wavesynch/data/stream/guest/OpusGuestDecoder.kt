package com.kunano.wavesynch.data.stream.guest

import android.util.Log
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.OpusNative

class OpusGuestDecoder(
    sampleRate: Int = AudioStreamConstants.SAMPLE_RATE,
    channels: Int = AudioStreamConstants.CHANNELS,
) {
    private val decoder = OpusNative.Decoder(sampleRate, channels)

    // Reused output buffer: ONE allocation total.
    private val outPcm = ShortArray(AudioStreamConstants.SAMPLES_PER_PACKET)

    /**
     * Decode normal frame into reused buffer.
     * Returns the same outPcm reference every time.
     */
    fun decode(p: ByteArray): ShortArray {
        decoder.decodeInto(p, AudioStreamConstants.SAMPLES_PER_CHANNEL, outPcm)
        return outPcm
    }

    /**
     * Decode missing frame using FEC from next packet (packet N+1 contains redundancy for N).
     * Returns the same outPcm reference every time.
     */
    fun decodeWithFEC(nextPacket: ByteArray): ShortArray {
        decoder.decodeFecInto(nextPacket, AudioStreamConstants.SAMPLES_PER_CHANNEL, outPcm)
        return outPcm
    }

    /**
     * PLC (concealment) into reused buffer.
     * Returns the same outPcm reference every time.
     */
    fun decodeWithPLC(): ShortArray {
        decoder.decodePlcInto(AudioStreamConstants.SAMPLES_PER_CHANNEL, outPcm)
        return outPcm
    }

    fun close() {
        decoder.destroy()
    }
}
