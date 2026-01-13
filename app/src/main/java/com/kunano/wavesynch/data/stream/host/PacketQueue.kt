package com.kunano.wavesynch.data.stream.host

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Reusable PCM buffers (ShortArray) so the capturer never overwrites frames
 * that the streamer hasn't processed yet.
 */
class PcmBufferPool(
    val frameShorts: Int,
    poolSize: Int
) {
    private val free = ArrayBlockingQueue<ShortArray>(poolSize)

    init {
        repeat(poolSize) { free.offer(ShortArray(frameShorts)) }
    }

    fun acquire(): ShortArray? = free.poll()

    fun release(buf: ShortArray) {
        free.offer(buf)
    }
}

/** Capturer -> Streamer queue of PCM frames that point to pooled buffers. */
class PcmFrameQueue(capacity: Int) {

    data class Frame(
        val pcm: ShortArray,
        val shorts: Int,
        val seq: Int,
        val tsMs: Int
    )

    private val q = ArrayBlockingQueue<Frame>(capacity)

    fun offer(frame: Frame): Boolean = q.offer(frame)

    fun take(timeoutMs: Long): Frame? = q.poll(timeoutMs, TimeUnit.MILLISECONDS)

    fun clear() = q.clear()
}
