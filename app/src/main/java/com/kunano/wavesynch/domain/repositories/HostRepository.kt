package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.data.stream.host.HostAudioCapturer
import com.kunano.wavesynch.data.wifi.WifiLocalPortInfo
import com.kunano.wavesynch.data.wifi.hotspot.HotspotInfo
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import kotlinx.coroutines.flow.Flow

interface HostRepository {

    val serverStateFlow: Flow<ServerState>
    val logFlow : Flow<String>
    val handShakeResultFlow: Flow<HandShakeResult>
    val connectedGuest: Flow<LinkedHashSet<Guest>?>

    fun addGuestToHostStreamer(guestId: String)

    fun finishSessionAsHost()

    //New server implementation


    fun startServer(room: Room, hostIp: String)
    fun stopServer()

    fun expelGuest(guestId: String)
    fun sendAnswerToGuest(guestId: String, roomName: String?, answer: HandShakeResult)

    fun acceptUserConnection(guest: Guest)
    fun startStreamingAsHost(hostAudioCapturer: HostAudioCapturer)
    fun playGuest(guestId: String)
    fun pauseGuest(guestId: String)
    fun stopStreaming(capturer: HostAudioCapturer)
    fun emptyRoom()
    fun openPortOverLocalWifi(room: Room): WifiLocalPortInfo?


}