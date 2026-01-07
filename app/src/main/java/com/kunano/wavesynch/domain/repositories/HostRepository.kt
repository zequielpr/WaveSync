package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.data.stream.host.HostAudioCapturer
import com.kunano.wavesynch.data.wifi.hotspot.HotspotInfo
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import kotlinx.coroutines.flow.Flow

interface HostRepository {
    val hotSpotStateFlow: Flow<HotspotState>
    val hotspotInfoFlow: Flow<HotspotInfo?>
    val serverStateFlow: Flow<ServerState>
    val logFlow : Flow<String>
    val handShakeResultFlow: Flow<HandShakeResult>
    val connectedGuest: Flow<LinkedHashSet<Guest>?>

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

    fun acceptUserConnection(guest: Guest)
    fun startStreamingAsHost(hostAudioCapturer: HostAudioCapturer)
    fun playGuest(guestId: String)
    fun pauseGuest(guestId: String)
    fun stopStreaming()
    fun emptyRoom()


}