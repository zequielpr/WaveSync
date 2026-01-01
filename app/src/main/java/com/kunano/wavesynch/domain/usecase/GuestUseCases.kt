package com.kunano.wavesynch.domain.usecase

import com.kunano.wavesynch.data.wifi.client.ClientConnectionsState
import com.kunano.wavesynch.data.wifi.client.SessionData
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.domain.repositories.GuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GuestUseCases @Inject constructor(
    private val guestRepository: GuestRepository
) {
    val isPlayingState: StateFlow<Boolean> = guestRepository.isPlayingState
    val connectionEvents: Flow<ClientConnectionsState> = guestRepository.clientConnectionsStateFLow
    val hanShakeResponse: Flow<HandShakeResult> = guestRepository.hanShakeResponse


    fun startReceivingAudioStream() = guestRepository.startReceivingAudioStream()

    fun connectToHotspot(password: String, ssid: String) =
        guestRepository.connectToHotspot(password, ssid)
    suspend fun disconnectFromHotspot() = guestRepository.disconnectFromHotspot()
    fun connectToServer() = guestRepository.connectToServer()
    fun isConnectedToServer(): Boolean = guestRepository.isConnectedToServer()



    fun getSessionInfo(): SessionData? = guestRepository.getSessionInfo()
    fun isConnectedToHotspotAsGuest(): Boolean = guestRepository.isConnectedToHotspotAsGuest()
    fun isConnectedToAudioServer(): Boolean = guestRepository.isConnectedToAudioServer()

    suspend fun leaveRoom(): Boolean = guestRepository.leaveRoom()
    fun pauseAudio() = guestRepository.pauseAudio()
    fun resumeAudio() = guestRepository.resumeAudio()
    suspend fun cancelJoinRoomRequest(): Boolean = guestRepository.cancelJoinRoomRequest()



}