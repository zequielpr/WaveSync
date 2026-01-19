package com.kunano.wavesynch.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent

import android.util.Log

import com.kunano.wavesynch.services.AudioPlayerService
import com.kunano.wavesynch.data.stream.guest.AudioReceiver
import com.kunano.wavesynch.data.wifi.client.ServerConnectionState
import com.kunano.wavesynch.data.wifi.client.ClientManager
import com.kunano.wavesynch.data.wifi.client.SessionData
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.domain.repositories.GuestRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class GuestRepositoryImpl @Inject constructor(
    private val clientManager: ClientManager,
    private val audioReceiver: AudioReceiver,
    @ApplicationContext private val context: Context,


    ) : GuestRepository {

    override val isPlayingState: StateFlow<Boolean> = audioReceiver.isPlayingState

    override val serverConnectionsStateFLow: Flow<ServerConnectionState> = clientManager.serverConnectionsStateFlow

    override val hanShakeResponse: Flow<HandShakeResult> = clientManager.handShakeResponse




    override fun getSessionInfo(): SessionData? {
        return clientManager.sessionInfo
    }

    
    override fun startReceivingAudioStream() {
        // Start the foreground service instead of directly starting the receiver
        val intent = Intent(context, AudioPlayerService::class.java)
        context.startForegroundService(intent)
    }


    override fun connectToServer(hostIp: String) {
        clientManager.connectToServer(hostIp)
    }

    override fun isConnectedToServer(): Boolean {
        return clientManager.isConnectedToHostServer
    }



    fun discConnectFromServer(){
        val intent = Intent(context, AudioPlayerService::class.java)
        context.stopService(intent)
    }


    override suspend fun leaveRoom(): Boolean{
        clientManager.sendLivingRoomHandShake()
        discConnectFromServer()
        return true

    }

    override fun pauseAudio() {
        audioReceiver.pause()
    }

    override fun resumeAudio() {
        audioReceiver.resume()
    }


    override fun isConnectedToAudioServer(): Boolean {
        TODO()
    }

    override suspend fun cancelJoinRoomRequest(): Boolean {
        clientManager.disconnectFromServer()
        return true
    }

    override fun connectToHostOverLocalWifi(ip: String) {
        connectToServer(ip)
    }


}
