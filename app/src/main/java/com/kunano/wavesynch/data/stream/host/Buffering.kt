package com.kunano.wavesynch.data.stream.host

import java.util.concurrent.ArrayBlockingQueue

/**
 * Fixed-size PCM frame container.
 */
class PcmFrame(frameShorts: Int) {
    val shorts: ShortArray = ShortArray(frameShorts)
    var seq: Int = 0
    var tsMs: Int = 0

    fun reset() {
        seq = 0
        tsMs = 0
    }
}

/**
 * Thread-safe frame pool (preallocates frames; acquire/release are non-blocking).
 */
class PcmFramePool(
    frameShorts: Int,
    poolSize: Int
) {
    private val q = ArrayBlockingQueue<PcmFrame>(poolSize)

    init {
        repeat(poolSize) {
            q.offer(PcmFrame(frameShorts))
        }
    }

    /** Returns null if pool empty (caller should drop frame). */
    fun acquire(): PcmFrame? = q.poll()

    /** Returns frame to pool (silently drops if pool is full). */
    fun release(frame: PcmFrame) {
        frame.reset()
        q.offer(frame) // if full, offer returns false; we ignore
    }

    fun available(): Int = q.size
}

/**
 * Thread-safe bounded queue (producer: capturer, consumer: sender).
 * If full, drops oldest frame (poll) and enqueues newest frame.
 */
class PcmFrameQueue(capacity: Int) {
    private val q = ArrayBlockingQueue<PcmFrame>(capacity)

    /**
     * Offers frame. If full, removes and returns the dropped (oldest) frame.
     * Caller should return dropped to pool.
     */
    fun offerDropOldest(frame: PcmFrame): PcmFrame? {
        // fast path
        if (q.offer(frame)) return null

        // full: drop oldest then try again
        val dropped = q.poll()
        // try to enqueue; if it still fails (rare contention), drop newest
        if (!q.offer(frame)) {
            // couldn't enqueue newest; give it back to caller as "droppedNewest"
            // (caller can pool.release(frame))
            return frame
        }
        return dropped
    }

    /**
     * Blocking take with interrupt support.
     * Returns null if interrupted.
     */
    fun takeBlocking(): PcmFrame? {
        return try {
            q.take()
        } catch (_: InterruptedException) {
            null
        }
    }

    fun size(): Int = q.size

    /**
     * Drain all frames currently queued (caller returns to pool).
     */
    fun drainAll(): List<PcmFrame> {
        val out = ArrayList<PcmFrame>(q.size)
        q.drainTo(out)
        return out
    }
}
