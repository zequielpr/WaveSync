package com.kunano.wavesynch.domain.usecase

import com.kunano.wavesynch.data.wifi.client.ClientConnectionsState
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.domain.repositories.GuestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GuestUseCases @Inject constructor(
    private val guestRepository: GuestRepository
) {
    val connectionEvents: Flow<ClientConnectionsState> = guestRepository.clientConnectionsStateFLow
    val hanShakeResponse: Flow<HandShakeResult> = guestRepository.hanShakeResponse


    fun startReceivingAudioStream() = guestRepository.startReceivingAudioStream()

    fun connectToHotspot(password: String, ssid: String) =
        guestRepository.connectToHotspot(password, ssid)
    fun connectToServer() = guestRepository.connectToServer()


    fun isConnectedToHotspotAsGuest(): Boolean = guestRepository.isConnectedToHotspotAsGuest()
    fun isConnectedToAudioServer(): Boolean = guestRepository.isConnectedToAudioServer()

    suspend fun leaveRoom(): Boolean = guestRepository.leaveRoom()
    fun pauseAudio() = guestRepository.pauseAudio()
    fun resumeAudio() = guestRepository.resumeAudio()


}