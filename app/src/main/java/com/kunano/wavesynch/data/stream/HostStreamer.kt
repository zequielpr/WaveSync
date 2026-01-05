package com.kunano.wavesynch.data.stream

import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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

    init {
        //It load opus library
        System.loadLibrary("wavesynch")
        udpSocket = DatagramSocket().apply {
            reuseAddress = true
        }

    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun startStreaming(capturer: HostAudioCapturer) {

        val audioStreamerThread = Thread{
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            isHostStreaming = true
            audioCapturer = capturer
            // Reuse DatagramPacket object to reduce allocations
            val dp = DatagramPacket(ByteArray(0), 0)



            var seq = 0
            //It receive the already encoded audio frames from the audio capturer and send them to the guests
            capturer.start { chunk ->

                dp.data = chunk
                dp.length = chunk.size
                guests.values.filter { it.isPlaying }.forEach { guest ->
                    dp.socketAddress = guest.inetSocketAddress
                    try {
                        // Send the packet to the guest
                        udpSocket?.send(dp)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        audioStreamerThread.start()
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
