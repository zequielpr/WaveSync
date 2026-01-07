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

        fun setInbandFecEnabled(enabled: Boolean) {
            setInbandFecEnabled(pointer, enabled)
        }

        fun setExpectedPacketLossPercent(lossPercent: Int) {
            setExpectedPacketLossPercent(pointer, lossPercent)
        }

        fun setSignalMusic() {
            setSignalMusic(pointer)
        }

        /**
         * @param bitrate Recommended values:
         * - Mono: 32_000–64_000 bps
         * - Stereo: 64_000–128_000 bps (depends on quality target)
         */
        fun setBitrate(bitrate: Int) {
            setBitrate(pointer, bitrate)
        }

        /**
         * @param complexity Recommended values:
         * - Host phone: 5–7
         */
        fun setComplexity(complexity: Int) {
            setComplexity(pointer, complexity)
        }



        fun destroy() {
            if (pointer != 0L) {
                destroyEncoder(pointer)
                pointer = 0L
            }
        }

        // NEW (FEC controls)
        private external fun setInbandFecEnabled(ptr: Long, enabled: Boolean)
        private external fun setExpectedPacketLossPercent(ptr: Long, lossPercent: Int)
        private external fun createEncoder(sampleRate: Int, channels: Int): Long
        private external fun encodePcm16(
            pointer: Long,
            pcm: ShortArray,
            frameSize: Int,
            channels: Int,
        ): ByteArray?

        private external fun setSignalMusic(ptr: Long)
        private external fun setBitrate(ptr: Long, bitrate: Int)
        private external fun setComplexity(ptr: Long, complexity: Int)

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

        fun decodeFecFromNextPcm16(nextPacket: ByteArray, frameSize: Int): ShortArray =
            decodeFecFromNextPcm16(pointer, nextPacket, frameSize)
                ?: error("Opus decode returned null")


        fun decodeWithPLC(frameSize: Int): ShortArray =
            decodePlcPcm16(pointer, frameSize)
                ?: error("Opus decode returned null")


        fun destroy() {
            if (pointer != 0L) {
                destroyDecoder(pointer)
                pointer = 0L
            }
        }

        private external fun createDecoder(sampleRate: Int, channels: Int): Long
        private external fun decodePcm16(
            pointer: Long,
            packet: ByteArray,
            frameSize: Int,
        ): ShortArray?

        private external fun decodePlcPcm16(pointer: Long, frameSize: Int): ShortArray?
        private external fun destroyDecoder(pointer: Long)
        private external fun decodeFecFromNextPcm16(
            pointer: Long,
            nextPacket: ByteArray,
            frameSize: Int,
        ): ShortArray?
    }
}
