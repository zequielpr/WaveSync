package com.kunano.wavesynch.domain.usecase

import com.kunano.wavesynch.data.wifi.client.ServerConnectionState
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
    val serverConnectionEvents: Flow<ServerConnectionState> = guestRepository.serverConnectionsStateFLow
    val hanShakeResponse: Flow<HandShakeResult> = guestRepository.hanShakeResponse





    fun startReceivingAudioStream() = guestRepository.startReceivingAudioStream()

    fun isConnectedToServer(): Boolean = guestRepository.isConnectedToServer()



    fun getSessionInfo(): SessionData? = guestRepository.getSessionInfo()
    fun isConnectedToAudioServer(): Boolean = guestRepository.isConnectedToAudioServer()

    suspend fun leaveRoom(): Boolean = guestRepository.leaveRoom()
    fun pauseAudio() = guestRepository.pauseAudio()
    fun resumeAudio() = guestRepository.resumeAudio()
    suspend fun cancelJoinRoomRequest(): Boolean = guestRepository.cancelJoinRoomRequest()
    fun connectToHostOverLocalWifi(ip: String) = guestRepository.connectToHostOverLocalWifi(ip)

}