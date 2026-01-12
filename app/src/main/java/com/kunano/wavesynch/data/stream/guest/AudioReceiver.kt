package com.kunano.wavesynch.data.stream.guest

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.kunano.wavesynch.data.stream.AudioPacket
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.PacketCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class AudioReceiver(
    private val context: Context
) {

    init { System.loadLibrary("wavesynch") }

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState = _isPlayingState.asStateFlow()

    private val running = AtomicBoolean(false)

    @Volatile private var isPaused = false
    private var rxThread: Thread? = null
    private var playoutThread: Thread? = null

    @Volatile private var udpSocket: DatagramSocket? = null

    private val buffer = JitterBuffer(maxFrames = 2048)
    private lateinit var decoder: OpusGuestDecoder

    private val padBuf = ShortArray(AudioStreamConstants.SAMPLES_PER_PACKET)

    // -------- BASE TUNING (speaker/wired) --------
    private var targetFrames = 20
    private val minFrames = 12
    private val maxFrames = 30

    private val reorderWaitMs = 12L
    private val lateToleranceFrames = 48

    private val jumpForwardThreshold = 250
    private val bufferBehindDropThreshold = 450

    @Volatile private var lastRxNs: Long = 0L
    private val connectionTimeoutNs = 2_500_000_000L // 2.5s

    // -------- BLUETOOTH MODE (gentle hysteresis drain) --------
    @Volatile private var btMode: Boolean = false

    // Keep app-added latency small, but don't over-correct.
    private val btTargetFrames = 30          // slightly higher than 8 = fewer corrections
    private val btHighWater = 40            // start draining only above this
    private val btLowWater = 18              // stop draining once below this (hysteresis)
    private val btMaxDropPerSecond = 2       // safety cap

    private val btDropStepFrames = 1         // drop 1 frame at a time (gentle)
    private val btDropCooldownNs = 500_000_000L // 500ms between drops

    private var btDropsThisSecond = 0
    private var btSecondMarkNs = 0L
    private var btLastDropNs = 0L

    fun start(socket: DatagramSocket) {
        if (running.getAndSet(true)) return

        udpSocket = socket
        decoder = OpusGuestDecoder(AudioStreamConstants.SAMPLE_RATE, AudioStreamConstants.CHANNELS)
        _isPlayingState.tryEmit(true)

        btMode = isBluetoothOutputActive()
        Log.w("AudioReceiver", "Output route: btMode=$btMode")

        socket.soTimeout = 50
        val track = buildAudioTrack()

        // ---------------- RX THREAD ----------------
        rxThread = Thread {
            val recvBuf = ByteArray(4096)
            val dp = DatagramPacket(recvBuf, recvBuf.size)

            while (running.get() && !socket.isClosed) {
                try {
                    socket.receive(dp)
                    if (!running.get() || socket.isClosed) break
                    if (isPaused) continue

                    val pkt = PacketCodec.decode(dp.data, dp.length) ?: continue
                    lastRxNs = System.nanoTime()
                    buffer.put(pkt)

                } catch (_: SocketTimeoutException) {
                    // normal
                } catch (e: Exception) {
                    if (!running.get() || socket.isClosed) break
                    Log.e("AudioReceiver", "RX error", e)
                }
            }
        }.apply { start() }

        // ---------------- PLAYOUT THREAD ----------------
        playoutThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)

            try { track.play() } catch (_: Exception) {}

            // If BT is active, start with BT target (avoid waiting for large initial buffer)
            if (btMode) targetFrames = btTargetFrames

            // Wait for initial fill + first seq
            var seq: Int? = null
            while (running.get()) {
                val first = buffer.firstSeq()
                val size = buffer.size()
                if (first != null && size >= targetFrames) {
                    seq = first
                    break
                }
                if (lastRxNs != 0L && System.nanoTime() - lastRxNs > connectionTimeoutNs) break
                buffer.waitForData(10)
            }

            var expectedSeq = seq ?: run {
                safeStopTrack(track)
                return@Thread
            }

            // Stats + controllers
            var lateWindow = 0
            var okWindow = 0
            var fecWindow = 0

            var lastStatsNs = System.nanoTime()
            var lastDriftNs = System.nanoTime()
            var driftScore = 0
            val driftThreshold = 40

            // hysteresis state
            var btDraining = false

            while (running.get()) {

                // Route can change mid-stream
                val btNow = isBluetoothOutputActive()
                if (btNow != btMode) {
                    btMode = btNow
                    btDropsThisSecond = 0
                    btSecondMarkNs = 0L
                    btLastDropNs = 0L
                    btDraining = false
                    Log.w("AudioReceiver", "Route change detected. btMode=$btMode")
                    if (btMode) targetFrames = btTargetFrames
                }

                // Connection-loss fail-safe
                if (lastRxNs != 0L && System.nanoTime() - lastRxNs > connectionTimeoutNs) {
                    Log.w("AudioReceiver", "No packets for too long → stopping playout")
                    break
                }

                // Drop packets too late to ever be used
                buffer.dropOlderThan(expectedSeq - lateToleranceFrames)

                // Jump forward on restart / huge forward gap
                val first = buffer.firstSeq()
                if (first != null) {
                    val gapForward = first - expectedSeq
                    val gapBackward = expectedSeq - first

                    if (gapForward > jumpForwardThreshold) {
                        Log.w("AudioReceiver", "Jump forward: first=$first expected=$expectedSeq gapF=$gapForward")
                        expectedSeq = first
                        // If you exposed OPUS_RESET_STATE, call it here:
                        // decoder.reset()
                        continue
                    }

                    if (gapBackward > bufferBehindDropThreshold) {
                        buffer.dropOlderThan(expectedSeq - lateToleranceFrames)
                    }
                }

                // 1) Try exact packet
                var pkt = buffer.pop(expectedSeq)

                // 2) If missing, wait briefly for reorder
                if (pkt == null) {
                    buffer.waitForSeq(expectedSeq, reorderWaitMs)
                    pkt = buffer.pop(expectedSeq)
                }

                val pcm: ShortArray = if (pkt != null) {
                    okWindow++
                    decoder.decode(pkt.payload)
                } else {
                    // 3) Try Opus FEC using packet N+1
                    val next = buffer.peek(expectedSeq + 1)
                    if (next != null) {
                        fecWindow++
                        decoder.decodeWithFEC(next.payload)
                    } else {
                        lateWindow++
                        decoder.decodeWithPLC()
                    }
                }

                writeFixed(track, pcm)
                expectedSeq++

                val now = System.nanoTime()

                // -------- Drift correction / latency control --------
                if (!btMode) {
                    // Speaker/wired: speed nudges are good
                    if (now - lastDriftNs > 250_000_000L) {
                        applyDriftCorrection(track, buffer.size(), targetFrames)
                        lastDriftNs = now
                    }
                } else {
                    // Bluetooth: use hysteresis drain (gentle, capped)
                    targetFrames = btTargetFrames

                    val bufNow = buffer.size()

                    // hysteresis: enter draining above high-water, exit below low-water
                    if (!btDraining && bufNow > btHighWater) btDraining = true
                    if (btDraining && bufNow < btLowWater) btDraining = false

                    // per-second limiter reset
                    if (btSecondMarkNs == 0L) btSecondMarkNs = now
                    if (now - btSecondMarkNs >= 1_000_000_000L) {
                        btDropsThisSecond = 0
                        btSecondMarkNs = now
                    }

                    val canDrain =
                        (now - btLastDropNs) >= btDropCooldownNs &&
                                btDropsThisSecond < btMaxDropPerSecond

                    if (btDraining && canDrain) {
                        expectedSeq += btDropStepFrames
                        buffer.dropOlderThan(expectedSeq - lateToleranceFrames)

                        btLastDropNs = now
                        btDropsThisSecond++

                        Log.w("AudioReceiver", "BT drain: dropped $btDropStepFrames frame(s). buf=$bufNow")
                    }
                }

                // -------- Buffer target update (every 1s) --------
                if (now - lastStatsNs > 1_000_000_000L) {
                    val total = okWindow + fecWindow + lateWindow
                    val lateRate = if (total == 0) 0.0 else lateWindow.toDouble() / total.toDouble()
                    val bufSize = buffer.size()

                    if (!btMode) {
                        // Grow faster if PLC often
                        if (lateRate > 0.02) targetFrames = (targetFrames + 1).coerceAtMost(maxFrames)

                        // Shrink slowly only if stable AND buffer is healthy
                        if (lateRate < 0.003 && bufSize > targetFrames + 3) {
                            targetFrames = (targetFrames - 1).coerceAtLeast(minFrames)
                        }

                        // Gentle drift controller around target
                        val e = bufSize - targetFrames
                        driftScore += e
                        if (abs(driftScore) > driftThreshold) {
                            targetFrames = (targetFrames + if (driftScore < 0) 1 else -1)
                                .coerceIn(minFrames, maxFrames)
                            driftScore = 0
                        }
                    } else {
                        // Bluetooth mode: fixed target; draining logic handles growth
                        targetFrames = btTargetFrames
                        driftScore = 0
                    }

                    Log.d(
                        "AudioReceiver",
                        "buf=$bufSize target=$targetFrames lateRate=${"%.3f".format(lateRate)} ok=$okWindow fec=$fecWindow plc=$lateWindow bt=$btMode draining=$btDraining drops1s=$btDropsThisSecond"
                    )

                    okWindow = 0
                    fecWindow = 0
                    lateWindow = 0
                    lastStatsNs = now
                }
            }

            safeStopTrack(track)
        }.also { it.start() }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        _isPlayingState.tryEmit(false)

        try { udpSocket?.close() } catch (_: Exception) {}
        try { rxThread?.interrupt() } catch (_: Exception) {}
        try { playoutThread?.interrupt() } catch (_: Exception) {}

        try { rxThread?.join(500) } catch (_: Exception) {}
        try { playoutThread?.join(500) } catch (_: Exception) {}

        rxThread = null
        playoutThread = null
        udpSocket = null

        buffer.clear()

        try { if (::decoder.isInitialized) decoder.close() } catch (_: Exception) {}
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

        val target = AudioStreamConstants.PCM_FRAME_BYTES * 10
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
        frameShorts: Int = AudioStreamConstants.SAMPLES_PER_PACKET,
    ) {
        val out = if (data.size >= frameShorts) data else {
            java.util.Arrays.fill(padBuf, 0.toShort())
            System.arraycopy(data, 0, padBuf, 0, data.size)
            padBuf
        }

        if (isPaused) {
            track.write(padBuf, 0, frameShorts, AudioTrack.WRITE_BLOCKING)
            return
        }

        track.write(out, 0, frameShorts, AudioTrack.WRITE_BLOCKING)
    }

    /**
     * Speaker/Wired drift correction:
     * Bluetooth often ignores PlaybackParams or behaves poorly, so we don't use it in btMode.
     */
    private fun applyDriftCorrection(track: AudioTrack, bufSize: Int, target: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val error = (bufSize - target).coerceIn(-6, 6)
        val speed = (1.0f + error * 0.0025f).coerceIn(0.985f, 1.015f)

        try {
            val pp = track.playbackParams ?: PlaybackParams()
            if (abs(pp.speed - speed) > 0.001f) {
                track.playbackParams = pp.setSpeed(speed).setPitch(1.0f)
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun isBluetoothOutputActive(): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val outs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return outs.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
        }
    }

    private fun safeStopTrack(track: AudioTrack) {
        try { track.pause() } catch (_: Exception) {}
        try { track.flush() } catch (_: Exception) {}
        try { track.stop() } catch (_: Exception) {}
        try { track.release() } catch (_: Exception) {}
    }
}

class JitterBuffer(private val maxFrames: Int = 2048) {
    private val map = java.util.TreeMap<Int, AudioPacket>()
    private val lock = Object()

    fun put(pkt: AudioPacket) {
        synchronized(lock) {
            if (map.size >= maxFrames) map.pollFirstEntry()
            map[pkt.seq] = pkt
            lock.notifyAll()
        }
    }

    fun pop(seq: Int): AudioPacket? = synchronized(lock) { map.remove(seq) }

    fun peek(seq: Int): AudioPacket? = synchronized(lock) { map[seq] }

    fun firstSeq(): Int? = synchronized(lock) { map.firstKeyOrNull() }

    fun size(): Int = synchronized(lock) { map.size }

    fun clear() = synchronized(lock) { map.clear() }

    fun dropOlderThan(seqInclusive: Int) {
        synchronized(lock) {
            while (true) {
                val first = map.firstKeyOrNull() ?: break
                if (first < seqInclusive) map.pollFirstEntry() else break
            }
        }
    }

    fun waitForSeq(seq: Int, waitMs: Long) {
        val end = System.currentTimeMillis() + waitMs
        synchronized(lock) {
            while (!map.containsKey(seq)) {
                val remaining = end - System.currentTimeMillis()
                if (remaining <= 0) break
                lock.wait(remaining)
            }
        }
    }

    fun waitForData(waitMs: Long) {
        synchronized(lock) {
            if (map.isEmpty()) lock.wait(waitMs)
        }
    }

    private fun <K, V> java.util.TreeMap<K, V>.firstKeyOrNull(): K? =
        if (this.isEmpty()) null else this.firstKey()
}
