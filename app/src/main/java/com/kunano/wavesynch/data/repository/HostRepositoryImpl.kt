package com.kunano.wavesynch.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.host.HostAudioCapturer
import com.kunano.wavesynch.data.stream.host.HostStreamer
import com.kunano.wavesynch.data.wifi.WifiLocalPortInfo
import com.kunano.wavesynch.data.wifi.getWifiIpAddress
import com.kunano.wavesynch.data.wifi.hotspot.HotspotInfo
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
import com.kunano.wavesynch.data.wifi.hotspot.LocalHotspotController
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerManager
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.repositories.HostRepository
import com.kunano.wavesynch.services.AudioCaptureService
import com.kunano.wavesynch.services.StartHotspotService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import javax.inject.Inject

// data/stream/AudioStreamRepositoryImpl.kt
// data/stream/AudioStreamRepositoryImpl.kt
class HostRepositoryImpl @Inject constructor(
    private val serverManager: ServerManager,
    private val hostStreamer: HostStreamer,
    @ApplicationContext private val context: Context,


    ) : HostRepository {

    override val connectedGuest: Flow<LinkedHashSet<Guest>?> = serverManager.connectedGuests

    private val _serverStateFlow = MutableStateFlow<ServerState>(ServerState.Stopped)

    override val serverStateFlow: Flow<ServerState> = _serverStateFlow.asStateFlow()

    override val logFlow: Flow<String> = serverManager.logFlow

    private val _handShakeResult = MutableSharedFlow<HandShakeResult>(extraBufferCapacity = 20)
    override val handShakeResultFlow: Flow<HandShakeResult> = _handShakeResult.asSharedFlow()




    override fun finishSessionAsHost() {
        stopStreamingService()
        emptyRoom()
        serverManager.closeServerSocket()
    }


    override fun startServer(room: Room, hostIp: String) {
        _serverStateFlow.tryEmit(ServerState.Starting)
        serverManager.startServerSocket(hostIp, room, inComingHandShakeResult = {
            _handShakeResult.tryEmit(it)
        })
        _serverStateFlow.tryEmit(ServerState.Running)
    }

    override fun stopServer() {
        serverManager.closeServerSocket()
        _serverStateFlow.tryEmit(ServerState.Stopped)
    }


    override fun expelGuest(guestId: String) {
        hostStreamer.removeGuest(guestId)
        serverManager.sendAnswerToGuest(
            guestId = guestId,
            answer = HandShakeResult.ExpelledByHost()
        )
    }

    override fun sendAnswerToGuest(
        guestId: String,
        roomName: String?,
        answer: HandShakeResult,
    ) {
        serverManager.sendAnswerToGuest(guestId, roomName, answer)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)


    override fun addGuestToHostStreamer(guestId: String) {
        val guestSocket = serverManager.socketList[guestId]
        val inetSocketAddress: InetSocketAddress =
            InetSocketAddress(guestSocket?.inetAddress, AudioStreamConstants.UDP_PORT)
        hostStreamer.addGuest(guestId, inetSocketAddress)
    }


    override fun acceptUserConnection(guest: Guest) {
        serverManager.acceptUserConnection(guest)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startStreamingAsHost(hostAudioCapturer: HostAudioCapturer) {
        hostStreamer.startStreaming(hostAudioCapturer)
        _serverStateFlow.tryEmit(ServerState.Streaming)
    }

    override fun playGuest(guestId: String) {

        serverManager.setGuestPlayingState(guestId, true)
        hostStreamer.resumeGuest(guestId)
    }

    override fun pauseGuest(guestId: String) {
        hostStreamer.pauseGuest(guestId)
        serverManager.setGuestPlayingState(guestId, false)
    }

    fun stopStreamingService() {
        val intent = Intent(context, AudioCaptureService::class.java)
        context.stopService(intent)
    }

    override fun stopStreaming(capturer: HostAudioCapturer) {
        hostStreamer.stopStreaming(
            capturer = capturer
        )
        _serverStateFlow.tryEmit(ServerState.Idle)

    }

    override fun emptyRoom() {
        hostStreamer.removeAllGuests()
        serverManager.closeAndClearSockets()
        serverManager.clearConnectedGuests()

    }

    override fun openPortOverLocalWifi(room: Room): WifiLocalPortInfo? {
        val hostIp = getWifiIpAddress(context)
        if (hostIp == null) return null
        _serverStateFlow.tryEmit(ServerState.Starting)
        val serverSocket = serverManager.startServerSocket(hostIp, room, inComingHandShakeResult = {
            _handShakeResult.tryEmit(it)
        })
        _serverStateFlow.tryEmit(ServerState.Running)

        val ipAddress = "/" + serverSocket?.inetAddress?.hostAddress
        Log.d("HostRepositoryImpl", "openPortOverLocalWifi: $ipAddress")

        val wifiLocalPortInfo =
            WifiLocalPortInfo(ipAddress = ipAddress, port = AudioStreamConstants.TCP_PORT)

        return wifiLocalPortInfo
    }

}