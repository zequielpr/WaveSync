package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.data.wifi.client.ServerConnectionState
import com.kunano.wavesynch.data.wifi.client.SessionData
import com.kunano.wavesynch.data.wifi.hotspot.HotSpotConnectionState
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GuestRepository{
    val isPlayingState: StateFlow<Boolean>
    val hanShakeResponse: Flow<HandShakeResult>
    val serverConnectionsStateFLow: Flow<ServerConnectionState>
    val hotspotConnectionStates: Flow<HotSpotConnectionState>




    fun getSessionInfo(): SessionData?
    fun connectToHotspot(password: String, ssid: String)
    suspend fun disconnectFromHotspot(): Boolean
    fun startReceivingAudioStream()
    fun connectToServer()
    fun isConnectedToServer(): Boolean
    suspend fun leaveRoom(): Boolean
    fun pauseAudio()
    fun resumeAudio()
    fun isConnectedToHotspotAsGuest(): Boolean
    fun isConnectedToAudioServer(): Boolean
    suspend fun cancelJoinRoomRequest(): Boolean

}