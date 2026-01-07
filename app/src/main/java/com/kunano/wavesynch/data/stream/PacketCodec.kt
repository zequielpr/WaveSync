package com.kunano.wavesynch.data.stream

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AudioPacket(
    val seq: Int,
    val tsMs: Int,
    val payload: ByteArray
)

object PacketCodec {
    private const val MAGIC_0: Byte = 'W'.code.toByte()
    private const val MAGIC_1: Byte = 'S'.code.toByte()
    private const val VERSION: Byte = 1
    private const val HEADER_SIZE = 12

    fun encode(seq: Int, tsMs: Int, pcm: ByteArray, length: Int): ByteArray {
        val out = ByteArray(HEADER_SIZE + length)
        val bb = ByteBuffer.wrap(out).order(ByteOrder.BIG_ENDIAN)
        bb.put(MAGIC_0)
        bb.put(MAGIC_1)
        bb.put(VERSION)
        bb.put(0) // flags
        bb.putInt(seq)
        bb.putInt(tsMs)
        bb.put(pcm, 0, length)
        return out
    }

    fun decode(packetBytes: ByteArray, length: Int): AudioPacket? {
        if (length < HEADER_SIZE) return null
        val bb = ByteBuffer.wrap(packetBytes, 0, length).order(ByteOrder.BIG_ENDIAN)
        val m0 = bb.get()
        val m1 = bb.get()
        val v = bb.get()
        bb.get() // flags
        if (m0 != MAGIC_0 || m1 != MAGIC_1 || v != VERSION) return null
        val seq = bb.int
        val ts = bb.int
        val payloadLen = length - HEADER_SIZE
        val payload = ByteArray(payloadLen)
        bb.get(payload)
        return AudioPacket(seq, ts, payload)
    }
}
