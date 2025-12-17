package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.data.wifi.client.ClientConnectionsState
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface GuestRepository{
    val hanShakeResponse: Flow<HandShakeResult>
    val clientConnectionsStateFLow: Flow<ClientConnectionsState>




    fun connectToHotspot(password: String, ssid: String)
    fun startReceivingAudioStream()
    fun connectToServer()
    fun leaveRoom()
    fun mute()
    fun unmute()

}