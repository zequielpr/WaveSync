package com.kunano.wavesynch.data.stream

import android.util.Log

object OpusNative {
    init {
        Log.e("OpusJNI", "Opus init: " + OpusNative::class.java.name)
        Log.e("OpusJNI", "✅ wavesynch loaded")
    }

    // =========================
    // Encoder
    // =========================
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

        fun setBitrate(bitrate: Int) {
            setBitrate(pointer, bitrate)
        }

        fun setComplexity(complexity: Int) {
            setComplexity(pointer, complexity)
        }

        fun destroy() {
            if (pointer != 0L) {
                destroyEncoder(pointer)
                pointer = 0L
            }
        }

        private external fun createEncoder(sampleRate: Int, channels: Int): Long
        private external fun encodePcm16(
            pointer: Long,
            pcm: ShortArray,
            frameSize: Int,
            channels: Int,
        ): ByteArray?

        private external fun destroyEncoder(pointer: Long)

        // FEC controls + tuning
        private external fun setInbandFecEnabled(ptr: Long, enabled: Boolean)
        private external fun setExpectedPacketLossPercent(ptr: Long, lossPercent: Int)
        private external fun setSignalMusic(ptr: Long)
        private external fun setBitrate(ptr: Long, bitrate: Int)
        private external fun setComplexity(ptr: Long, complexity: Int)
    }

    // =========================
    // Decoder
    // =========================
    class Decoder(sampleRate: Int, private val channels: Int) {
        private var pointer: Long = createDecoder(sampleRate, channels).also {
            require(it != 0L) { "Failed to create Opus decoder" }
        }

        // -------- OLD API (allocates every call) --------

        fun decode(packet: ByteArray, frameSize: Int): ShortArray =
            decodePcm16(pointer, packet, frameSize)
                ?: error("Opus decode returned null")

        fun decodeFecFromNextPcm16(nextPacket: ByteArray, frameSize: Int): ShortArray =
            decodeFecFromNextPcm16(pointer, nextPacket, frameSize)
                ?: error("Opus FEC decode returned null")

        fun decodeWithPLC(frameSize: Int): ShortArray =
            decodePlcPcm16(pointer, frameSize)
                ?: error("Opus PLC decode returned null")

        // -------- NEW API (ZERO-ALLOCATION) --------
        // Writes PCM into `out` (interleaved), returns number of shorts written.
        // You reuse the same out ShortArray forever -> smooth music.

        fun decodeInto(packet: ByteArray, frameSize: Int, out: ShortArray): Int {
            check(out.size >= frameSize * channels) {
                "out too small: need >= ${frameSize * channels}, got ${out.size}"
            }
            val n = decodePcm16Into(pointer, packet, frameSize, out)
            if (n < 0) error("Opus decodeInto failed (rc=$n)")
            return n
        }

        fun decodeFecInto(nextPacket: ByteArray, frameSize: Int, out: ShortArray): Int {
            check(out.size >= frameSize * channels) {
                "out too small: need >= ${frameSize * channels}, got ${out.size}"
            }
            val n = decodeFecFromNextPcm16Into(pointer, nextPacket, frameSize, out)
            if (n < 0) error("Opus decodeFecInto failed (rc=$n)")
            return n
        }

        fun decodePlcInto(frameSize: Int, out: ShortArray): Int {
            check(out.size >= frameSize * channels) {
                "out too small: need >= ${frameSize * channels}, got ${out.size}"
            }
            val n = decodePlcPcm16Into(pointer, frameSize, out)
            if (n < 0) error("Opus decodePlcInto failed (rc=$n)")
            return n
        }

        // Optional: very useful after resync jumps
        fun reset() {
            val rc = resetDecoderState(pointer)
            if (rc != 0) {
                Log.w("OpusJNI", "OPUS_RESET_STATE rc=$rc")
            }
        }

        fun destroy() {
            if (pointer != 0L) {
                destroyDecoder(pointer)
                pointer = 0L
            }
        }

        private external fun createDecoder(sampleRate: Int, channels: Int): Long
        private external fun destroyDecoder(pointer: Long)

        // OLD natives (allocating)
        private external fun decodePcm16(pointer: Long, packet: ByteArray, frameSize: Int): ShortArray?
        private external fun decodePlcPcm16(pointer: Long, frameSize: Int): ShortArray?
        private external fun decodeFecFromNextPcm16(pointer: Long, nextPacket: ByteArray, frameSize: Int): ShortArray?

        // NEW natives (no allocations)
        private external fun decodePcm16Into(pointer: Long, packet: ByteArray, frameSize: Int, out: ShortArray): Int
        private external fun decodePlcPcm16Into(pointer: Long, frameSize: Int, out: ShortArray): Int
        private external fun decodeFecFromNextPcm16Into(pointer: Long, nextPacket: ByteArray, frameSize: Int, out: ShortArray): Int

        // Reset decoder state (recommended)
        private external fun resetDecoderState(pointer: Long): Int
    }
}
