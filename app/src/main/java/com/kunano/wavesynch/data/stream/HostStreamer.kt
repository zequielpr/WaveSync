package com.kunano.wavesynch.data.stream

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class HostStreamer(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val guests: HashMap<String, GuestStreamingData> = hashMapOf()
    private var isHostStreaming = false

    private var job: Job? = null

    fun addGuest(id: String, inetSocketAddress: InetSocketAddress) {
        guests[id] = GuestStreamingData(id, inetSocketAddress, isPlaying = true)
    }

    private var audioCapturer: HostAudioCapturer? = null
    private var udpSocket: DatagramSocket? = null


    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun startStreaming(capturer: HostAudioCapturer) {
        scope.launch {
            isHostStreaming = true
            audioCapturer = capturer
            udpSocket = DatagramSocket().apply {
                reuseAddress = true
            }
            // Reuse DatagramPacket object to reduce allocations
            val dp = DatagramPacket(ByteArray(0), 0)

            var seq = 0

            capturer.start { chunk ->
                Log.d("HostStreamer", "chunk size: ${chunk.size}")
                dp.data = chunk
                dp.length = chunk.size

                val snapshot = guests.values.toList()

                snapshot.forEach { guest ->
                    dp.socketAddress = guest.inetSocketAddress
                    try {
                        Log.d("HostStreamer", "startStreaming: ${guest.inetSocketAddress}")
                        // Send the packet to the guest
                        udpSocket?.send(dp)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                seq++
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

    }

    fun pauseStreaming(){
        stopCapturingAudio()
    }

    fun stopStreaming() {
        isHostStreaming = false
        stopCapturingAudio()
        udpSocket?.close()
        udpSocket = null
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
