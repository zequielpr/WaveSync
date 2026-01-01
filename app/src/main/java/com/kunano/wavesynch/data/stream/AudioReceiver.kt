package com.kunano.wavesynch.data.stream

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.TreeMap

class AudioReceiver(
    private val jitterBuffer: JitterBuffer = JitterBuffer(),
    private val scope: CoroutineScope,
) {
    var _isPlayingState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPlayingState = _isPlayingState.asStateFlow()


    @Volatile
    private var running = false
    private var isOnPause = false


    //Up implementation
    fun start(socket: DatagramSocket) {
        if (running) return
        running = true
        _isPlayingState.tryEmit(true)

        scope.launch(Dispatchers.IO) {
            val minOut = AudioTrack.getMinBufferSize(
                AudioStreamConstants.SAMPLE_RATE,
                AudioStreamConstants.CHANNEL_OUT,
                AudioStreamConstants.AUDIO_FORMAT
            )
            val track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(AudioStreamConstants.SAMPLE_RATE)
                    .setEncoding(AudioStreamConstants.AUDIO_FORMAT)
                    .setChannelMask(AudioStreamConstants.CHANNEL_OUT)
                    .build(),
                minOut * 4,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )


            val recvBuf = ByteArray(4096)
            val dp = DatagramPacket(recvBuf, recvBuf.size)

            val buffer = TreeMap<Int, ByteArray>() // seq -> payload
            var expectedSeq: Int? = null
            val targetPrebufferFrames = 3
            val maxBufferFrames = 50

            track.play()

            try {
                while (running) {
                    socket.receive(dp)

                    val decoded = PacketCodec.decode(dp.data, dp.length) ?: continue

                    // init expected seq from first packet
                    if (expectedSeq == null) expectedSeq = decoded.seq

                    // store payload
                    buffer[decoded.seq] = decoded.payload

                    // prevent unbounded growth
                    while (buffer.size > maxBufferFrames) {
                        buffer.pollFirstEntry()
                    }

                    // prebuffer before starting strict playback
                    if (buffer.size < targetPrebufferFrames) continue


                    Log.d("AudioReceiver", "seq: ${decoded.seq}")

                    // play as many contiguous frames as we have
                    val payload = buffer.remove(expectedSeq) ?: continue
                    Log.d("AudioReceiver", "data length: ${dp.length}")
                    if (!isOnPause) {
                        track.write(payload, 0, payload.size)
                    }
                    expectedSeq++

                    // if we’re missing too much, resync to the next available packet
                    if (buffer.isNotEmpty()) {
                        val lowest = buffer.firstKey()
                        if (lowest - expectedSeq > 20) {
                            expectedSeq = lowest
                        }
                    }
                }
            } finally {
                socket.close()
                try {
                    track.stop()
                } catch (_: Exception) {
                }
                track.release()
            }
        }
    }


    fun stop() {
        running = false
        _isPlayingState.tryEmit(false)
    }

    fun pause() {
        isOnPause = true
        _isPlayingState.tryEmit(false)
    }

    fun resume() {
        isOnPause = false
        _isPlayingState.tryEmit(true)

    }
}
