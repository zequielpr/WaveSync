package com.kunano.wavesynch.data.stream.guest

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.PacketCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicBoolean

class AudioReceiver {

    init {
        System.loadLibrary("wavesynch")
    }

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState = _isPlayingState.asStateFlow()

    private val running = AtomicBoolean(false)

    @Volatile private var isPaused = false
    @Volatile private var expectedSeq: Int? = null

    private var rxThread: Thread? = null
    private var playoutThread: Thread? = null

    @Volatile private var udpSocket: DatagramSocket? = null

    // jitter buffer: seq -> opus payload
    private val buffer = TreeMap<Int, ByteArray>()
    private val bufferLock = Any()

    private lateinit var decoder: OpusGuestDecoder

    // Avoid per-frame allocations when padding
    private val padBuf = ShortArray(AudioStreamConstants.SAMPLES_PER_PACKET)

    fun start(socket: DatagramSocket) {
        if (running.getAndSet(true)) return

        udpSocket = socket

        decoder = OpusGuestDecoder(AudioStreamConstants.SAMPLE_RATE, AudioStreamConstants.CHANNELS)
        _isPlayingState.tryEmit(true)

        // Make receive loop interruptible (so stop() works)
        socket.soTimeout = 200

        val track = buildAudioTrack().also { it.play() }

        // Receiver: read UDP, store payloads by seq
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
                        }
                    } catch (_: SocketTimeoutException) {
                        // normal: allows loop to check running flag
                    } catch (e: Exception) {
                        if (!running.get() || socket.isClosed) break
                        Log.e("AudioReceiver", "RX error", e)
                    }
                }
            } finally {
                // receiver exits; playout thread will stop via running flag
            }
        }.apply { start() }

        // Playout: fixed cadence, never wait for UDP
        playoutThread = Thread {
            val frameNs = AudioStreamConstants.FRAME_NS
            var nextTick = System.nanoTime()

            val prebufferTarget = AudioStreamConstants.PREBUFFER_FRAMES
            val silence = ShortArray(AudioStreamConstants.SAMPLES_PER_PACKET)

            try {
                while (running.get()) {

                    // Startup (or after resync): wait for prebuffer
                    val exp = expectedSeq
                    if (exp != null) {
                        val buffered = synchronized(bufferLock) { buffer.size }
                        if (buffered < prebufferTarget) {
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

                    // How late are we for the tick we were supposed to hit?
                    val lateFrames = ((now - nextTick) / frameNs).toInt().coerceAtLeast(0)

                    // If we’re extremely late, drop backlog + resync and skip this tick
                    if (lateFrames >= AudioStreamConstants.MAX_JITTER_FRAMES) {
                        var dropped = 0
                        synchronized(bufferLock) {
                            repeat(lateFrames.coerceAtMost(buffer.size)) {
                                buffer.pollFirstEntry()
                                dropped++
                            }
                            expectedSeq = buffer.firstKeyOrNull()
                        }

                        nextTick = now + frameNs
                        Log.d(
                            "AudioReceiver",
                            "Hard catch-up: late=$lateFrames dropped=$dropped buffer=${synchronized(bufferLock) { buffer.size }}"
                        )
                        continue
                    } else if (lateFrames > 0) {
                        // Soft catch-up: skip missed ticks (don’t try to “replay the past”)
                        nextTick += lateFrames * frameNs
                    }

                    // Schedule the next frame
                    nextTick += frameNs

                    val seq = expectedSeq ?: continue

                    // Decode one frame for this seq normal / FEC / PLC
                    val payload = takePayload(seq)

                    val pcmFrame: ShortArray = if (payload != null) {
                        decoder.decode(payload)
                    } else {
                        val nextPayload = peekPayload(seq + 1)
                        if (nextPayload != null) {
                            decoder.decodeWithFEC(nextPayload)
                        } else {
                            // True underflow: nothing usable, use PLC
                            val bufSize = synchronized(bufferLock) { buffer.size }
                            Log.d("AudioReceiver", "PLC: missing seq=$seq buffer=$bufSize")
                            decoder.decodeWithPLC()
                        }
                    }

                    // Drop packets that are already behind the playhead (never needed again)
                    synchronized(bufferLock) {
                        val playheadNext = seq + 1
                        while (true) {
                            val first = buffer.firstKeyOrNull() ?: break
                            if (first < playheadNext) buffer.pollFirstEntry() else break
                        }
                    }

                    // Latency cap: if buffer is huge but it's mostly "future" packets, resync forward.
                    synchronized(bufferLock) {
                        if (buffer.size > AudioStreamConstants.MAX_JITTER_FRAMES) {
                            while (buffer.size > prebufferTarget) {
                                buffer.pollFirstEntry()
                            }
                            expectedSeq = buffer.firstKeyOrNull()
                            Log.d(
                                "AudioReceiver",
                                "Latency cap resync: expectedSeq=$expectedSeq buffer=${buffer.size}"
                            )
                        }
                    }

                    // Resync if stream jumped far ahead or restarted
                    val lowest = synchronized(bufferLock) { buffer.firstKeyOrNull() }
                    if (lowest != null) {
                        val gap = lowest - seq
                        if (gap >= AudioStreamConstants.RESYNC_THRESHOLD_FRAMES || lowest < seq) {
                            Log.d("AudioReceiver", "Resync: gap=$gap oldExp=$seq newExp=$lowest")
                            expectedSeq = lowest
                            continue
                        }
                    }

                    if (!isPaused) {
                        // Optional: measure blocking writes (useful for headphone route debugging)
                        val t0 = System.nanoTime()
                        writeFixed(track, pcmFrame)
                        val writeMs = (System.nanoTime() - t0) / 1_000_000
                        if (writeMs > 10) Log.d("AudioReceiver", "AudioTrack.write blocked ${writeMs}ms")
                    } else {
                        writeFixed(track, silence)
                    }

                    expectedSeq = seq + 1
                }
            } catch (e: Exception) {
                Log.e("AudioReceiver", "Playout error", e)
            } finally {
                try { socket.close() } catch (_: Exception) {}
                try { track.stop() } catch (_: Exception) {}
                try { track.release() } catch (_: Exception) {}

                _isPlayingState.tryEmit(false)
                running.set(false)

                try {
                    if (::decoder.isInitialized) decoder.close()
                } catch (_: Exception) {}

                synchronized(bufferLock) { buffer.clear() }
                expectedSeq = null
                udpSocket = null
            }
        }.apply { start() }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        _isPlayingState.tryEmit(false)

        // Fastest way to unblock receive()
        try { udpSocket?.close() } catch (_: Exception) {}

        try { rxThread?.interrupt() } catch (_: Exception) {}
        try { playoutThread?.interrupt() } catch (_: Exception) {}

        try { rxThread?.join(500) } catch (_: Exception) {}
        try { playoutThread?.join(500) } catch (_: Exception) {}

        rxThread = null
        playoutThread = null
        udpSocket = null

        synchronized(bufferLock) { buffer.clear() }
        expectedSeq = null

        try {
            if (::decoder.isInitialized) decoder.close()
        } catch (_: Exception) {}
    }

    fun pause() {
        isPaused = true
        _isPlayingState.tryEmit(false)
    }

    fun resume() {
        isPaused = false
        _isPlayingState.tryEmit(true)
    }

    private fun takePayload(seq: Int): ByteArray? =
        synchronized(bufferLock) { buffer.remove(seq) }

    private fun peekPayload(seq: Int): ByteArray? =
        synchronized(bufferLock) { buffer[seq] }

    private fun buildAudioTrack(): AudioTrack {
        val minOut = AudioTrack.getMinBufferSize(
            AudioStreamConstants.SAMPLE_RATE,
            AudioStreamConstants.CHANNEL_MASK_OUT,
            AudioStreamConstants.AUDIO_FORMAT
        )

        // Target a small buffer (~40ms) but never below minOut
        val target = AudioStreamConstants.PCM_FRAME_BYTES * 4
        val bufSize = maxOf(minOut, target)

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
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufSize)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private fun writeFixed(
        track: AudioTrack,
        data: ShortArray,
        frameShorts: Int = AudioStreamConstants.SAMPLES_PER_PACKET
    ) {
        when {
            data.size >= frameShorts -> track.write(data, 0, frameShorts)
            else -> {
                java.util.Arrays.fill(padBuf, 0.toShort())
                System.arraycopy(data, 0, padBuf, 0, data.size)
                track.write(padBuf, 0, frameShorts)
            }
        }
    }

    private fun TreeMap<Int, ByteArray>.firstKeyOrNull(): Int? =
        if (isEmpty()) null else firstKey()
}
