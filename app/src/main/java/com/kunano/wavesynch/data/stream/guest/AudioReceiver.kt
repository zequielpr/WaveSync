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
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

    val firebaseCrashlytics = FirebaseCrashlytics.getInstance()

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

    // Wake/sleep signal: playout REALLY blocks here, RX wakes it
    private val rxSignal = Object()

    // -------- BASE TUNING (speaker/wired) --------
    private var targetFrames = 20
    private val minFrames = 12
    private val maxFrames = 25

    private val reorderWaitMs = 12L
    private val lateToleranceFrames = 48

    // NOTE: big fixed thresholds are fragile; we use a dynamic jump check too.
    private val bufferBehindDropThreshold = 450

    @Volatile private var lastRxNs: Long = 0L
    private val connectionTimeoutNs = 2_500_000_000L // 2.5s

    // -------- BLUETOOTH MODE (gentle hysteresis drain) --------
    @Volatile private var btMode: Boolean = false

    private val btTargetFrames = 28
    private val btHighWater = 35
    private val btLowWater = 25
    private val btMaxDropPerSecond = 1

    private val btDropStepFrames = 1
    private val btDropCooldownNs = 500_000_000L // 500ms

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
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            val recvBuf = ByteArray(4096)
            val dp = DatagramPacket(recvBuf, recvBuf.size)

            while (running.get() && !socket.isClosed) {
                try {
                    socket.receive(dp)
                    if (!running.get() || socket.isClosed) break

                    val h = PacketCodec.decodeHeader(dp.data, dp.length) ?: continue

                    val payload = ByteArray(h.payloadLen)
                    System.arraycopy(dp.data, h.payloadOffset, payload, 0, h.payloadLen)

                    lastRxNs = System.nanoTime()
                    buffer.put(payload, seq = h.seq)

                    // Wake playout (even if buffer isn't empty)
                    synchronized(rxSignal) { rxSignal.notifyAll() }

                } catch (_: SocketTimeoutException) {
                    // normal
                } catch (e: Exception) {
                    firebaseCrashlytics.setCustomKey("rzThread", "Audio receiver thread")
                    firebaseCrashlytics.log("Audio receiver thread error")
                    firebaseCrashlytics.recordException(e)
                    if (!running.get() || socket.isClosed) break
                    Log.e("AudioReceiver", "RX error", e)
                }
            }
        }.apply { start() }

        // ---------------- PLAYOUT THREAD ----------------
        playoutThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)

            // Stats + controllers
            var lateWindow = 0
            var okWindow = 0
            var fecWindow = 0

            var lastStatsNs = System.nanoTime()
            var lastDriftNs = System.nanoTime()
            var driftScore = 0
            val driftThreshold = 40

            // Soft resync: if we're PLC-ing while buffer has plenty -> we're desynced
            var missStreak = 0
            val maxMissStreak = 12 // ~120ms at 10ms frames (tune 8..20)

            // Current expected sequence
            var expectedSeq: Int? = null

            // BT hysteresis state
            var btDraining = false

            fun resetControllers() {
                lateWindow = 0
                okWindow = 0
                fecWindow = 0
                lastStatsNs = System.nanoTime()
                lastDriftNs = System.nanoTime()
                driftScore = 0
                missStreak = 0
            }

            fun hardSleepUntilRxResumes() {
                // We consider stream "paused". Stop adding latency inside AudioTrack.
                try { track.pause() } catch (_: Exception) {}
                try { track.flush() } catch (_: Exception) {}

                val mark = lastRxNs
                synchronized(rxSignal) {
                    while (running.get() && lastRxNs == mark) {
                        rxSignal.wait(250)
                    }
                }

                if (!running.get()) return

                // Resync to live edge immediately (low latency)
                val first = buffer.firstSeq()
                if (first != null) {
                    buffer.dropOlderThan(first)     // drop backlog
                    expectedSeq = first             // jump to live stream
                }

                // restart audio
                try { track.play() } catch (_: Exception) {}

                // reset drift/stat logic so it doesn't overreact to the gap
                resetControllers()
            }

            try {
                track.play()

                if (btMode) targetFrames = btTargetFrames

                // ---- Initial sync: wait for enough fill, then start at buffer head
                while (running.get()) {
                    val first = buffer.firstSeq()
                    val size = buffer.size()
                    if (first != null && size >= targetFrames) {
                        expectedSeq = first
                        break
                    }

                    // if we've never received anything for too long, give up
                    if (lastRxNs != 0L && System.nanoTime() - lastRxNs > connectionTimeoutNs) break

                    // tiny wait: only for bootstrap
                    buffer.waitForData(10)
                }

                if (expectedSeq == null) return@Thread

                while (running.get()) {

                    // ---- Route can change mid-stream
                    val btNow = isBluetoothOutputActive()
                    if (btNow != btMode) {
                        btMode = btNow
                        btDropsThisSecond = 0
                        btSecondMarkNs = 0L
                        btLastDropNs = 0L
                        btDraining = false
                        Log.w("AudioPlayer", "Route change detected. btMode=$btMode")
                        if (btMode) targetFrames = btTargetFrames
                        resetControllers()
                    }

                    // ---- If no packets have arrived for too long -> SLEEP until RX resumes
                    val nowNs = System.nanoTime()
                    if (lastRxNs != 0L && nowNs - lastRxNs > connectionTimeoutNs) {
                        Log.w("AudioPlayer", "No packets for too long → playout sleeping")
                        hardSleepUntilRxResumes()
                        if (!running.get()) break
                        // after wake, expectedSeq may be updated
                        if (expectedSeq == null) break
                        continue
                    }

                    val exp = expectedSeq ?: break

                    // Drop packets too late to ever be used
                    buffer.dropOlderThan(exp - lateToleranceFrames)

                    // Dynamic jump forward on restart / big forward gap
                    val first = buffer.firstSeq()
                    if (first != null) {
                        val gapForward = first - exp
                        val gapBackward = exp - first

                        val dynamicJump = (targetFrames * 3).coerceIn(40, 90)
                        if (gapForward > dynamicJump) {
                            Log.w("AudioPlayer", "Jump forward: first=$first expected=$exp gapF=$gapForward")
                            buffer.dropOlderThan(first)
                            expectedSeq = first
                            resetControllers()
                            continue
                        }

                        if (gapBackward > bufferBehindDropThreshold) {
                            buffer.dropOlderThan(exp - lateToleranceFrames)
                        }
                    }

                    // 1) Try exact packet
                    var payload = buffer.pop(exp)

                    // 2) If missing, wait briefly for reorder
                    if (payload == null) {
                        buffer.waitForSeq(exp, reorderWaitMs)
                        payload = buffer.pop(exp)
                    }

                    val nextPayload = if (payload == null) buffer.peek(exp + 1) else null

                    val pcm: ShortArray = when {
                        payload != null -> {
                            missStreak = 0
                            okWindow++
                            decoder.decode(payload)
                        }
                        nextPayload != null -> {
                            missStreak = 0
                            fecWindow++
                            decoder.decodeWithFEC(nextPayload)
                        }
                        else -> {
                            missStreak++
                            lateWindow++
                            decoder.decodeWithPLC()
                        }
                    }

                    writeFixed(track, pcm)

                    // advance
                    expectedSeq = exp + 1

                    // ---- Soft resync: if we keep missing but buffer has data, we are desynced
                    if (missStreak >= maxMissStreak) {
                        val head = buffer.firstSeq()
                        val bufNow = buffer.size()
                        if (head != null && bufNow >= targetFrames) {
                            Log.w("AudioPlayer", "Soft resync: missStreak=$missStreak buf=$bufNow expected=${expectedSeq} -> head=$head")
                            buffer.dropOlderThan(head)
                            expectedSeq = head
                            missStreak = 0
                            // Optional: decoder.reset() if you implement OPUS_RESET_STATE
                        }
                    }

                    val now = System.nanoTime()

                    // -------- Drift correction / latency control --------
                    if (!btMode) {
                        // Speaker/wired: speed nudges are OK
                        if (now - lastDriftNs > 250_000_000L) {
                            applyDriftCorrection(track, buffer.size(), targetFrames)
                            lastDriftNs = now
                        }
                    } else {
                        // Bluetooth: hysteresis drain (gentle, capped)
                        targetFrames = btTargetFrames

                        val bufNow = buffer.size()

                        if (!btDraining && bufNow > btHighWater){
                            firebaseCrashlytics.setCustomKey("latency", "buffer size $bufNow")
                            firebaseCrashlytics.log("Buffer size $bufNow")
                            firebaseCrashlytics.recordException(Exception("Buffer size $bufNow"))

                            btDraining = true
                        }
                        if (btDraining && bufNow < btLowWater) btDraining = false

                        if (btSecondMarkNs == 0L) btSecondMarkNs = now
                        if (now - btSecondMarkNs >= 1_000_000_000L) {
                            btDropsThisSecond = 0
                            btSecondMarkNs = now
                        }

                        val canDrain =
                            (now - btLastDropNs) >= btDropCooldownNs &&
                                    btDropsThisSecond < btMaxDropPerSecond

                        if (btDraining && canDrain) {
                            val newExp = (expectedSeq ?: 0) + btDropStepFrames
                            expectedSeq = newExp
                            buffer.dropOlderThan(newExp - lateToleranceFrames)

                            btLastDropNs = now
                            btDropsThisSecond++

                            Log.w("AudioPlayer", "BT drain: dropped $btDropStepFrames frame(s). buf=$bufNow")
                        }
                    }

                    // -------- Buffer target update (every 1s) --------
                    if (now - lastStatsNs > 1_000_000_000L) {
                        val total = okWindow + fecWindow + lateWindow
                        val lateRate = if (total == 0) 0.0 else lateWindow.toDouble() / total.toDouble()
                        val bufSize = buffer.size()

                        if (!btMode) {
                            if (lateRate > 0.02) targetFrames = (targetFrames + 1).coerceAtMost(maxFrames)

                            if (lateRate < 0.003 && bufSize > targetFrames + 3) {
                                targetFrames = (targetFrames - 1).coerceAtLeast(minFrames)
                            }

                            val e = bufSize - targetFrames
                            driftScore += e
                            if (abs(driftScore) > driftThreshold) {
                                targetFrames = (targetFrames + if (driftScore < 0) 1 else -1)
                                    .coerceIn(minFrames, maxFrames)
                                driftScore = 0
                            }
                        } else {
                            targetFrames = btTargetFrames
                            driftScore = 0
                        }

                        Log.d(
                            "AudioPlayer",
                            "buf=$bufSize target=$targetFrames lateRate=${"%.3f".format(lateRate)} ok=$okWindow fec=$fecWindow plc=$lateWindow bt=$btMode draining=$btDraining drops1s=$btDropsThisSecond"
                        )

                        okWindow = 0
                        fecWindow = 0
                        lateWindow = 0
                        lastStatsNs = now
                    }
                }
            } catch (e: InterruptedException) {
                Log.d("AudioPlayer", "Playout thread interrupted, shutting down.")
            } catch (e: Exception) {
                firebaseCrashlytics.setCustomKey("rzThread", "Audio player thread")
                firebaseCrashlytics.log("Audio player thread error")
                firebaseCrashlytics.recordException(e)
                Log.e("AudioPlayer", "Playout thread error", e)
            } finally {
                safeStopTrack(track)
            }
        }.also { it.start() }

        rxThread?.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        playoutThread?.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        _isPlayingState.tryEmit(false)

        try { udpSocket?.close() } catch (_: Exception) {}
        try { rxThread?.interrupt() } catch (_: Exception) {}
        try { playoutThread?.interrupt() } catch (_: Exception) {}

        // Wake playout if it's blocked on rxSignal
        synchronized(rxSignal) { rxSignal.notifyAll() }

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
        val sampleRate = AudioStreamConstants.SAMPLE_RATE
        val channelMask = AudioStreamConstants.CHANNEL_MASK_OUT
        val encoding = AudioStreamConstants.AUDIO_FORMAT

        val minOut = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)
        require(minOut > 0) { "Invalid AudioTrack min buffer: $minOut" }

        val frameBytes = AudioStreamConstants.PCM_FRAME_BYTES

        // 2–4 frames keeps latency low. 10 frames is usually too much.
        val desiredFrames = 4
        val desiredBytes = frameBytes * desiredFrames

        var bufSize = maxOf(minOut, desiredBytes)

        // round up to frame multiple
        bufSize = ((bufSize + frameBytes - 1) / frameBytes) * frameBytes

        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(encoding)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufSize)

        // best-effort; some devices ignore/throw
        try { builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY) }
        catch (_: Throwable) {}

        return builder.build()
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

    private fun applyDriftCorrection(track: AudioTrack, bufSize: Int, target: Int) {

        val error = (bufSize - target).coerceIn(-6, 6)
        val speed = (1.0f + error * 0.0025f).coerceIn(0.985f, 1.015f)

        try {
            val pp = track.playbackParams
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
    private val map = java.util.TreeMap<Int, ByteArray>()
    private val lock = Object()

    fun put(payload: ByteArray, seq: Int) {
        synchronized(lock) {
            if (map.size >= maxFrames) map.pollFirstEntry()
            map[seq] = payload
            lock.notifyAll()
        }
    }

    fun pop(seq: Int): ByteArray? = synchronized(lock) { map.remove(seq) }

    fun peek(seq: Int): ByteArray? = synchronized(lock) { map[seq] }

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
                try { lock.wait(remaining) }
                catch (e: InterruptedException) { Thread.currentThread().interrupt(); break }
            }
        }
    }

    fun waitForData(waitMs: Long) {
        synchronized(lock) {
            if (map.isEmpty()) {
                try { lock.wait(waitMs) }
                catch (e: InterruptedException) { Thread.currentThread().interrupt() }
            }
        }
    }

    private fun <K, V> java.util.TreeMap<K, V>.firstKeyOrNull(): K? =
        if (this.isEmpty()) null else this.firstKey()
}
