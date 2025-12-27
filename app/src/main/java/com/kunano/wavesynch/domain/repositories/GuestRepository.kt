package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.data.wifi.client.ClientConnectionsState
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface GuestRepository{
    val isPlayingState: StateFlow<Boolean>
    val hanShakeResponse: Flow<HandShakeResult>
    val clientConnectionsStateFLow: Flow<ClientConnectionsState>




    fun connectToHotspot(password: String, ssid: String)
    fun startReceivingAudioStream()
    fun connectToServer()
    suspend fun leaveRoom(): Boolean
    fun pauseAudio()
    fun resumeAudio()
    fun isConnectedToHotspotAsGuest(): Boolean
    fun isConnectedToAudioServer(): Boolean

}