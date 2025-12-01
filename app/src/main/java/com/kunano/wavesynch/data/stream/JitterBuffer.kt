package com.kunano.wavesynch.data.stream

// data/stream/JitterBuffer.kt
class JitterBuffer(
    private val capacityInPackets: Int
) {
    private val queue: ArrayDeque<ByteArray> = ArrayDeque()

    @Synchronized
    fun push(packet: ByteArray) {
        if (queue.size >= capacityInPackets) {
            queue.removeFirst() // drop oldest
        }
        queue.addLast(packet)
    }

    @Synchronized
    fun pop(): ByteArray? = if (queue.isEmpty()) null else queue.removeFirst()
}
