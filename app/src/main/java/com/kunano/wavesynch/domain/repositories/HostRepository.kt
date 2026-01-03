package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.data.stream.HostAudioCapturer
import com.kunano.wavesynch.data.wifi.hotspot.HotspotInfo
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface HostRepository {
    val hotSpotStateFlow: Flow<HotspotState>
    val hotspotInfoFlow: Flow<HotspotInfo?>
    val serverStateFlow: Flow<ServerState>
    val logFlow : Flow<String>
    val handShakeResultFlow: Flow<HandShakeResult>
    val connectedGuest: Flow<ArrayList<Guest>?>

    fun addGuestToHostStreamer(guestId: String)


    //New hotspot implementation
    fun startHotspot(
        onStarted: (HotspotInfo) -> Unit,
        onError: (Int) -> Unit,)
    fun stopHotspot()
    fun isHotspotRunning(): Boolean
    fun getHotspotInfo(): HotspotInfo?
    fun finishSessionAsHost()
    fun isHostStreaming(): Boolean

    //New server implementation


    suspend fun startServer(room: Room)
    fun stopServer()

    fun expelGuest(guestId: String)
    fun sendAnswerToGuest(guestId: String, roomName: String?, answer: HandShakeResult)

    suspend fun acceptUserConnection(guest: Guest)
    fun closeUserSocket(userId: String)
    fun startStreamingAsHost(hostAudioCapturer: HostAudioCapturer)
    fun playGuest(guestId: String)
    fun pauseGuest(guestId: String)
    fun stopStreaming()
    fun emptyRoom()


}