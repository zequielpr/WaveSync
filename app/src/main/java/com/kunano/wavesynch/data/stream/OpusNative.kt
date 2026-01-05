
package com.kunano.wavesynch.data.stream

import android.util.Log

object OpusNative {
    init {
        Log.e("OpusJNI", "Opus init: " + OpusNative::class.java.name)

        Log.e("OpusJNI", "✅ wavesynch loaded")
    }


    class Encoder(sampleRate: Int, private val channels: Int) {
        private var pointer: Long = createEncoder(sampleRate, channels).also {
            require(it != 0L) { "Failed to create Opus encoder" }
        }

        fun encode(pcm: ShortArray, frameSize: Int): ByteArray =
            encodePcm16(pointer, pcm, frameSize, channels)
                ?: error("Opus encode returned null")

        fun destroy() {
            if (pointer != 0L) {
                destroyEncoder(pointer)
                pointer = 0L
            }
        }

        private external fun createEncoder(sampleRate: Int, channels: Int): Long
        private external fun encodePcm16(pointer: Long, pcm: ShortArray, frameSize: Int, channels: Int): ByteArray?
        private external fun destroyEncoder(pointer: Long)
    }

    class Decoder(sampleRate: Int, private val channels: Int) {
        private var pointer: Long = createDecoder(sampleRate, channels).also {
            require(it != 0L) { "Failed to create Opus decoder" }
        }

        /**
         * @param packet Opus compressed bytes
         * @param frameSize samples PER CHANNEL you want to decode (e.g. 960 for 20ms at 48kHz)
         * @return PCM16 interleaved ShortArray (size = decodedSamples * channels)
         */
        fun decode(packet: ByteArray, frameSize: Int): ShortArray =
            decodePcm16(pointer, packet, frameSize)
                ?: error("Opus decode returned null")

        fun destroy() {
            if (pointer != 0L) {
                destroyDecoder(pointer)
                pointer = 0L
            }
        }

        private external fun createDecoder(sampleRate: Int, channels: Int): Long
        private external fun decodePcm16(pointer: Long, packet: ByteArray, frameSize: Int): ShortArray?
        private external fun destroyDecoder(pointer: Long)
    }
}
