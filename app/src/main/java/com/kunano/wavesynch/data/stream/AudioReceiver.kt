package com.kunano.wavesynch.data.stream

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioManager.*
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicBoolean

class AudioReceiver(
    private val scope: CoroutineScope,
) {
    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState = _isPlayingState.asStateFlow()

    private val running = AtomicBoolean(false)
    @Volatile private var isPaused = false

    private var rxThread: Thread? = null
    private var playoutThread: Thread? = null

    // jitter buffer
    private val buffer = TreeMap<Int, ByteArray>()
    private val bufferLock = Any()

    @Volatile private var expectedSeq: Int? = null

    fun start(socket: DatagramSocket) {
        if (running.getAndSet(true)) return
        _isPlayingState.tryEmit(true)

        // Make receive loop interruptible (so stop() works)
        socket.soTimeout = 200

        val track = buildAudioTrack().also { it.play() }

        val silence = ByteArray(AudioStreamConstants.PAYLOAD_BYTES)

        // Receiver: read UDP, decode, store
        rxThread = Thread {
            val recvBuf = ByteArray(4096)
            val dp = DatagramPacket(recvBuf, recvBuf.size)

            try {
                while (running.get() && !socket.isClosed) {
                    try {
                        socket.receive(dp)
                        val decoded = PacketCodec.decode(dp.data, dp.length) ?: continue

                        // init expected seq from first packet
                        if (expectedSeq == null) expectedSeq = decoded.seq

                        synchronized(bufferLock) {
                            buffer[decoded.seq] = decoded.payload
                            // Keep buffer bounded
                            while (buffer.size > AudioStreamConstants.MAX_JITTER_FRAMES) {
                                buffer.pollFirstEntry()
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        // normal: allows loop to check running flag
                    } catch (e: Exception) {
                        // If socket closed, exit quietly
                        if (!running.get() || socket.isClosed) break
                        Log.e("AudioReceiver", "RX error", e)
                    }
                }
            } finally {
                // Receiver exits; playout thread will stop via running flag
            }
        }.apply { start() }

        // Playout: fixed cadence, never wait for UDP
        playoutThread = Thread {
            val frameNs = AudioStreamConstants.FRAME_NS
            var nextTick = System.nanoTime()

            // Prebuffer before playing “in time”
            val prebufferTarget = AudioStreamConstants.PREBUFFER_FRAMES

            try {
                while (running.get()) {

                    // Wait until we have enough buffered frames (startup or after resync)
                    if (expectedSeq != null) {
                        val buffered = synchronized(bufferLock) { buffer.size }
                        if (buffered < prebufferTarget) {
                            // Don’t write yet; let buffer build
                            Thread.sleep(2)
                            continue
                        }
                    } else {
                        Thread.sleep(2)
                        continue
                    }

                    // Clock-driven tick
                    val now = System.nanoTime()
                    if (now < nextTick) {
                        val sleepMs = ((nextTick - now) / 1_000_000L).coerceAtMost(5)
                        if (sleepMs > 0) Thread.sleep(sleepMs)
                        continue
                    }
                    nextTick += frameNs

                    val seq = expectedSeq ?: continue

                    val payload: ByteArray? = synchronized(bufferLock) {
                        buffer.remove(seq)
                    }

                    // Resync if we are falling behind badly (burst loss)
                    if (payload == null) {
                        val lowest = synchronized(bufferLock) { buffer.firstKeyOrNull() }
                        if (lowest != null) {
                            val gap = lowest - seq
                            if (gap > AudioStreamConstants.RESYNC_THRESHOLD_FRAMES) {
                                Log.d("AudioReceiver", "Resync: gap=$gap oldExp=$seq newExp=$lowest")
                                expectedSeq = lowest
                                continue
                            }
                        }
                    }

                    if (!isPaused) {
                        Log.d("AudioReceiver", "Playout: seq=$seq")
                        val data = payload ?: silence
                        Log.d("AudioReceiver", "Playout: seq=${data.size}")
                        writeFixed(track, data, AudioStreamConstants.PAYLOAD_BYTES)
                    }

                    expectedSeq = seq + 1
                }
            } catch (e: Exception) {
                Log.e("AudioReceiver", "Playout error", e)
            } finally {
                try { socket.close() } catch (_: Exception) {}
                try { track.stop() } catch (_: Exception) {}
                track.release()
                _isPlayingState.tryEmit(false)
                running.set(false)
            }
        }.apply { start() }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        _isPlayingState.tryEmit(false)

        // Unblock receive() quickly
        try { rxThread?.interrupt() } catch (_: Exception) {}
        try { playoutThread?.interrupt() } catch (_: Exception) {}

        rxThread = null
        playoutThread = null

        synchronized(bufferLock) { buffer.clear() }
        expectedSeq = null
    }

    fun pause() {
        isPaused = true
        _isPlayingState.tryEmit(false)
    }

    fun resume() {
        isPaused = false
        _isPlayingState.tryEmit(true)
    }

    private fun buildAudioTrack(): AudioTrack {
        val minOut = AudioTrack.getMinBufferSize(
            AudioStreamConstants.SAMPLE_RATE,
            AudioStreamConstants.CHANNEL_MASK_OUT,
            AudioStreamConstants.AUDIO_FORMAT
        )

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AudioStreamConstants.SAMPLE_RATE)
                    .setEncoding(AudioStreamConstants.AUDIO_FORMAT)
                    .setChannelMask(AudioStreamConstants.CHANNEL_MASK_OUT)
                    .build()
            )
            // Don’t go too small; let track breathe
            .setBufferSizeInBytes(minOut * 3)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun writeFixed(track: AudioTrack, data: ByteArray, frameBytes: Int) {
        when {
            data.size == frameBytes -> track.write(data, 0, frameBytes)
            data.size > frameBytes -> track.write(data, 0, frameBytes)
            else -> {
                val tmp = ByteArray(frameBytes)
                System.arraycopy(data, 0, tmp, 0, data.size)
                track.write(tmp, 0, frameBytes)
            }
        }
    }

    private fun TreeMap<Int, ByteArray>.firstKeyOrNull(): Int? =
        if (isEmpty()) null else firstKey()
}