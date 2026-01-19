package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.data.wifi.client.ServerConnectionState
import com.kunano.wavesynch.data.wifi.client.SessionData
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GuestRepository{
    val isPlayingState: StateFlow<Boolean>
    val hanShakeResponse: Flow<HandShakeResult>
    val serverConnectionsStateFLow: Flow<ServerConnectionState>




    fun getSessionInfo(): SessionData?
    fun startReceivingAudioStream()
    fun connectToServer(hostIp: String)
    fun isConnectedToServer(): Boolean
    suspend fun leaveRoom(): Boolean
    fun pauseAudio()
    fun resumeAudio()
    fun isConnectedToAudioServer(): Boolean
    suspend fun cancelJoinRoomRequest(): Boolean
    fun connectToHostOverLocalWifi(ip: String)

}