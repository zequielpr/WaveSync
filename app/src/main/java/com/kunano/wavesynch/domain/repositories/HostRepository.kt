package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.data.stream.HostAudioCapturer
import com.kunano.wavesynch.data.wifi.HandShakeResult
import com.kunano.wavesynch.data.wifi.ServerState
import com.kunano.wavesynch.domain.model.Guest
import kotlinx.coroutines.flow.Flow
import java.net.Socket

interface HostRepository {
    val serverStateFlow: Flow<ServerState>
    val logFlow : Flow<String>
    val handShakeResultFlow: Flow<HandShakeResult>
    val connectedGuest: Flow<HashSet<Guest>?>



    // create P2P group
    suspend fun hostRoom(roomId: Long? = null)

    suspend fun expelGuest(guestId: String)
    fun sendAnswerToGuest(guestId: String, answer: HandShakeResult)

    suspend fun stopStreaming()
    fun stopStreamingToGuest(guestId: String)
    suspend fun acceptUserConnection(guest: Guest)
    fun closeUserSocket(userId: String)
    fun startStreamingAsHost(capturer: HostAudioCapturer)
}