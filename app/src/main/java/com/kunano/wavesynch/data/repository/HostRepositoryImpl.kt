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
import com.kunano.wavesynch.data.wifi.WifiController

import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerManager
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.repositories.HostRepository
import com.kunano.wavesynch.services.AudioCaptureService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import javax.inject.Inject


class HostRepositoryImpl @Inject constructor(
    private val serverManager: ServerManager,
    private val hostStreamer: HostStreamer,
    private val wifiController: WifiController,
    @ApplicationContext private val context: Context,


    ) : HostRepository {
    override val isConnectedToWifi: Flow<Boolean> = wifiController.isConnectedToWifiFlow
    override val hostIpAddress: Flow<String?> = wifiController.hostIpFlow
    override val connectedGuest: Flow<LinkedHashSet<Guest>?> = serverManager.connectedGuests
    override val isHostStreamingFlow: StateFlow<Boolean> = hostStreamer.isHostStreamingFlow

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
    }

    override fun emptyRoom() {
        hostStreamer.removeAllGuests()
        serverManager.closeAndClearSockets()
        serverManager.clearConnectedGuests()

    }

    override fun setCurrentRoom(room: Room) {
        serverManager.setCurrentRoomOnServer(room)
    }

    override fun openPortOverLocalWifi(hostIp: String) {
        _serverStateFlow.tryEmit(ServerState.Starting)

        serverManager.startServerSocket(
            hostIp,
            onRunning = { _serverStateFlow.tryEmit(ServerState.Running) },
            inComingHandShakeResult = {
                _handShakeResult.tryEmit(it)
            })
    }
}
