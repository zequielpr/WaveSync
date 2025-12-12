package com.kunano.wavesynch.data.stream



import java.util.concurrent.ArrayBlockingQueue

class JitterBuffer(
    private val capacityInPackets: Int = AudioLatencyConfig.JITTER_CAPACITY
) {
    private val queue = java.util.concurrent.ArrayBlockingQueue<ByteArray>(capacityInPackets)

    fun push(packet: ByteArray) {
        // drop oldest if full to avoid unbounded latency
        if (!queue.offer(packet)) {
            queue.poll()
            queue.offer(packet)
        }
    }

    fun pop(): ByteArray? = queue.poll()

    fun size(): Int = queue.size
}

