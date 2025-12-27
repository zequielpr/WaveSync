package com.kunano.wavesynch.data.stream

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HostStreamer(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val guests: HashMap<String, GuestStreamingData> = hashMapOf()
    private var isHostStreaming = false


    fun addGuest(id: String, socket: Socket) {
        val channel = Channel<ByteArray>(capacity = 16)

        val job = scope.launch(Dispatchers.IO) {
            val out = socket.getOutputStream()
            try {
                for (chunk in channel) {

                    // ---- FRAME ----
                    val header = ByteBuffer
                        .allocate(4)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putInt(chunk.size)
                        .array()

                    out.write(header)
                    out.write(chunk)
                    //out.flush()
                }
            } catch (_: IOException) {
            } finally {
                socket.close()
            }
        }

        //Add guest
        guests[id] = GuestStreamingData(id, socket, channel, job, isPlaying = true)
    }

    private var audioCapturer: HostAudioCapturer? = null

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun startStreaming(capturer: HostAudioCapturer) {
        scope.launch {
            isHostStreaming = true
            audioCapturer = capturer
            capturer.start { chunk ->
                val copy = chunk.copyOf()
                guests.values.forEach { guest ->
                    if (guest.isPlaying){
                        guest.channel.trySend(copy)
                    }

                }
            }
        }
    }

    fun pauseGuest(id: String) {
        guests[id]?.isPlaying = false
    }

    fun resumeGuest(id: String) {
        guests[id]?.isPlaying = true
    }

    fun removeGuest(id: String) {
        val g = guests[id] ?: return
        guests.remove(id)
        g.channel.close()
        g.job.cancel()
        g.socket.close()
    }

    fun pauseStreaming(){
        stopCapturingAudio()
    }

    fun stopStreaming() {
        isHostStreaming = false
        stopCapturingAudio()
        guests.values.forEach {
            it.channel.close()
            it.job.cancel()
            it.socket.close()
        }
    }

    fun stopCapturingAudio(){
        audioCapturer?.stop()
    }

    fun removeAllGuests(){
        guests.clear()
    }

    fun isHostStreaming(): Boolean {
        return isHostStreaming
    }



}
