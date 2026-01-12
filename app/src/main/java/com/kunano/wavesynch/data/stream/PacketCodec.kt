package com.kunano.wavesynch.data.stream

data class AudioPacket(
    val seq: Int,
    val tsMs: Int,
    val payload: ByteArray
)

/**
 * Packet format (big endian):
 * [0]  'W'
 * [1]  'S'
 * [2]  version
 * [3]  flags
 * [4..7]  seq (int)
 * [8..11] tsMs (int)
 * [12..]  payload bytes
 */
object PacketCodec {
    private const val MAGIC_0: Byte = 'W'.code.toByte()
    private const val MAGIC_1: Byte = 'S'.code.toByte()
    private const val VERSION: Byte = 1
    const val HEADER_SIZE = 12

    // ----------------------------
    // Zero-allocation ENCODE
    // ----------------------------

    /**
     * Writes packet into [out] and returns total packet length.
     *
     * @return total bytes written, or -1 if out is too small.
     */
    fun writeInto(
        out: ByteArray,
        seq: Int,
        tsMs: Int,
        payload: ByteArray,
        payloadLen: Int,
        flags: Int = 0
    ): Int {
        if (payloadLen < 0) return -1
        val total = HEADER_SIZE + payloadLen
        if (out.size < total) return -1

        out[0] = MAGIC_0
        out[1] = MAGIC_1
        out[2] = VERSION
        out[3] = (flags and 0xFF).toByte()

        putIntBE(out, 4, seq)
        putIntBE(out, 8, tsMs)

        // copy payload
        System.arraycopy(payload, 0, out, HEADER_SIZE, payloadLen)
        return total
    }

    // ----------------------------
    // Zero-allocation DECODE (header only + payload view)
    // ----------------------------

    data class DecodedHeader(
        val seq: Int,
        val tsMs: Int,
        val flags: Int,
        val payloadOffset: Int,
        val payloadLen: Int
    )

    /**
     * Parses header and returns offsets (no payload allocation).
     */
    fun decodeHeader(packetBytes: ByteArray, length: Int): DecodedHeader? {
        if (length < HEADER_SIZE) return null
        if (packetBytes[0] != MAGIC_0 || packetBytes[1] != MAGIC_1 || packetBytes[2] != VERSION) return null

        val flags = packetBytes[3].toInt() and 0xFF
        val seq = getIntBE(packetBytes, 4)
        val ts = getIntBE(packetBytes, 8)

        val payloadLen = length - HEADER_SIZE
        return DecodedHeader(
            seq = seq,
            tsMs = ts,
            flags = flags,
            payloadOffset = HEADER_SIZE,
            payloadLen = payloadLen
        )
    }

    // ----------------------------
    // Compatibility API (allocating)
    // ----------------------------

    /**
     * Old encode API (allocates).
     */
    fun encode(seq: Int, tsMs: Int, pcm: ByteArray, length: Int): ByteArray {
        val out = ByteArray(HEADER_SIZE + length)
        val n = writeInto(out, seq, tsMs, pcm, length)
        return if (n > 0) out else ByteArray(0)
    }

    /**
     * Old decode API (allocates payload).
     */
    fun decode(packetBytes: ByteArray, length: Int): AudioPacket? {
        val h = decodeHeader(packetBytes, length) ?: return null
        val payload = ByteArray(h.payloadLen)
        System.arraycopy(packetBytes, h.payloadOffset, payload, 0, h.payloadLen)
        return AudioPacket(h.seq, h.tsMs, payload)
    }

    // ----------------------------
    // Big-endian helpers
    // ----------------------------

    private fun putIntBE(a: ByteArray, off: Int, v: Int) {
        a[off] = (v ushr 24).toByte()
        a[off + 1] = (v ushr 16).toByte()
        a[off + 2] = (v ushr 8).toByte()
        a[off + 3] = (v).toByte()
    }

    private fun getIntBE(a: ByteArray, off: Int): Int {
        return ((a[off].toInt() and 0xFF) shl 24) or
                ((a[off + 1].toInt() and 0xFF) shl 16) or
                ((a[off + 2].toInt() and 0xFF) shl 8) or
                ((a[off + 3].toInt() and 0xFF))
    }
}
