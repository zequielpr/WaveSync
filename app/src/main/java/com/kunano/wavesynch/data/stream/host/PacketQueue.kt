package com.kunano.wavesynch.data.stream.host

class PacketQueue(capacity: Int) {
    data class PacketRef(val buf: ByteArray, val len: Int)

    private val lock = Object()
    private val q = ArrayDeque<PacketRef>(capacity)
    private val cap = capacity

    fun offer(p: PacketRef) {
        synchronized(lock) {
            // drop oldest when full (prefer fresh audio)
            if (q.size >= cap) q.removeFirst()
            q.addLast(p)
            lock.notifyAll()
        }
    }

    fun take(timeoutMs: Long): PacketRef? {
        val end = System.currentTimeMillis() + timeoutMs
        synchronized(lock) {
            while (q.isEmpty()) {
                val rem = end - System.currentTimeMillis()
                if (rem <= 0) return null
                lock.wait(rem)
            }
            return q.removeFirst()
        }
    }

    fun clear() = synchronized(lock) { q.clear() }
}
