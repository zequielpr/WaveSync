package com.kunano.wavesynch.data.stream.guest

import android.util.Log
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.OpusNative

class OpusGuestDecoder(
    sampleRate: Int = AudioStreamConstants.SAMPLE_RATE,
    channels: Int = AudioStreamConstants.CHANNELS,
) {
    private val decoder = OpusNative.Decoder(sampleRate, channels)



    fun decode(p: ByteArray): ShortArray{
        return decoder.decode(p, AudioStreamConstants.SAMPLES_PER_CHANNEL)
    }

    fun decodeWithFEC(p: ByteArray): ShortArray {
        return decoder.decodeFecFromNextPcm16( p, AudioStreamConstants.SAMPLES_PER_CHANNEL)
    }

    fun decodeWithPLC(): ShortArray {
        return decoder.decodeWithPLC(AudioStreamConstants.SAMPLES_PER_CHANNEL)
    }


    //Manage PLC and FEC automatically
    fun decodeForPlayout(
        expectedSeq: Int,
        packetBuffer: MutableMap<Int, ByteArray> // seq -> opus payload bytes (NOT including header)
    ): ShortArray {

        val p0 = packetBuffer[expectedSeq]
        if (p0 != null) {
            packetBuffer.remove(expectedSeq)
            return decoder.decode(p0, AudioStreamConstants.SAMPLES_PER_PACKET)
        }

        // Missing expected packet: try FEC from next packet if available
        val p1 = packetBuffer[expectedSeq + 1]
        if (p1 != null) {
            Log.d("tag", "decodeForPlayout with FEC: $p1")
            // IMPORTANT: do NOT remove p1 here.
            // We use p1 to reconstruct expectedSeq (previous frame),
            // then next tick we still need p1 for normal decode.
            return decoder.decodeFecFromNextPcm16( p1, AudioStreamConstants.SAMPLES_PER_PACKET)
        }

        // Neither packet nor next packet is available -> PLC
        return decoder.decodeWithPLC(AudioStreamConstants.SAMPLES_PER_PACKET)
    }

    fun close() {
        decoder.destroy()
    }
}
