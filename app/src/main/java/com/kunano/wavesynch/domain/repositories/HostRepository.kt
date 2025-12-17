package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.data.stream.HostAudioCapturer
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
import com.kunano.wavesynch.domain.model.Guest
import kotlinx.coroutines.flow.Flow

interface HostRepository {
    val hotSpotStateFlow: Flow<HotspotState>
    val serverStateFlow: Flow<ServerState>
    val logFlow : Flow<String>
    val handShakeResultFlow: Flow<HandShakeResult>
    val connectedGuest: Flow<HashSet<Guest>?>



    //New hotspot implementation
    fun startHotspot(
        onStarted: (ssid: String, password: String) -> Unit,
        onError: (Int) -> Unit,)
    fun stopHotspot()
    fun isHotspotRunning(): Boolean







    suspend fun startServer(roomId: Long? = null)
    fun stopServer()

    fun expelGuest(guestId: String)
    fun sendAnswerToGuest(guestId: String, answer: HandShakeResult)

    fun stopStreaming()
    fun stopStreamingToGuest(guestId: String)
    fun startStreamingToGuest(guestId: String)
    suspend fun acceptUserConnection(guest: Guest)
    fun closeUserSocket(userId: String)
    fun startStreamingAsHost(capturer: HostAudioCapturer)
}