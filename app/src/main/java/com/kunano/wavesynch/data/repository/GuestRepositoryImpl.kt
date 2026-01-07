package com.kunano.wavesynch.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.services.AudioPlayerService
import com.kunano.wavesynch.data.stream.guest.AudioReceiver
import com.kunano.wavesynch.data.wifi.client.ServerConnectionState
import com.kunano.wavesynch.data.wifi.client.ClientManager
import com.kunano.wavesynch.data.wifi.client.SessionData
import com.kunano.wavesynch.data.wifi.hotspot.HotSpotConnectionState
import com.kunano.wavesynch.data.wifi.hotspot.LocalHotspotController
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.domain.repositories.GuestRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GuestRepositoryImpl @Inject constructor(
    private val clientManager: ClientManager,
    private val audioReceiver: AudioReceiver,
    private val localHotspotController: LocalHotspotController,
    @ApplicationContext private val context: Context,


    ) : GuestRepository {

    override val hotspotConnectionStates: Flow<HotSpotConnectionState> = localHotspotController.connectionStateFlow
    override val isPlayingState: StateFlow<Boolean> = audioReceiver.isPlayingState
    private val _serverConnectionsStateFlow = MutableStateFlow<ServerConnectionState>(ServerConnectionState.Idle)
    override val serverConnectionsStateFLow: Flow<ServerConnectionState> =
        _serverConnectionsStateFlow

    override fun getSessionInfo(): SessionData? {
        return clientManager.sessionInfo
    }

    override val hanShakeResponse: Flow<HandShakeResult> = clientManager.handShakeResponse
    
    override fun startReceivingAudioStream() {
        // Start the foreground service instead of directly starting the receiver
        val intent = Intent(context, AudioPlayerService::class.java)
        context.startForegroundService(intent)
        _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ReceivingAudioStream)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun connectToServer() {

        val hotIp: String? = localHotspotController.getGatewayInfo()
        hotIp?.let {
            clientManager.connectToServer(it, onConnecting = {
                _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectingToServer)
            })
        }

    }

    override fun isConnectedToServer(): Boolean {
        return clientManager.isConnectedToHostServer
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun connectToHotspot(password: String, ssid: String) {
        localHotspotController.connectToHotspot(
            ssid = ssid,
            password = password,
            onConnected = {},
            onFailed = {})
    }

    override suspend fun disconnectFromHotspot(): Boolean{
        val result = localHotspotController.disconnectFromHotspot()
        localHotspotController.setIsConnectedToHotspotAsGuest(!result)
        return result
    }


    fun discConnectFromServer(){
        val intent = Intent(context, AudioPlayerService::class.java)
        context.stopService(intent)
        clientManager.disconnectFromServer()
        _serverConnectionsStateFlow.tryEmit(ServerConnectionState.Disconnected)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun leaveRoom(): Boolean{
        discConnectFromServer()
        val result = disconnectFromHotspot()

        return result

    }

    override fun pauseAudio() {
        audioReceiver.pause()
        _serverConnectionsStateFlow.tryEmit(ServerConnectionState.Idle)
    }

    override fun resumeAudio() {
        audioReceiver.resume()
        _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ReceivingAudioStream)
    }

    override fun isConnectedToHotspotAsGuest(): Boolean {
        return localHotspotController.isConnectedToHotspotAsGuest
    }

    override fun isConnectedToAudioServer(): Boolean {
        TODO()
    }

    override suspend fun cancelJoinRoomRequest(): Boolean {
        discConnectFromServer()
        return disconnectFromHotspot()
    }


}
