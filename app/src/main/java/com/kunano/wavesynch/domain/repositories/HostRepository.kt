package com.kunano.wavesynch.domain.repositories

import android.os.Build
import androidx.annotation.RequiresApi
import com.kunano.wavesynch.data.stream.host.HostAudioCapturer
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import kotlinx.coroutines.flow.Flow

interface HostRepository {
    val isConnectedToWifi: Flow<Boolean>
    val hostIpAddress: Flow<String?>
    val connectedGuest: Flow<LinkedHashSet<Guest>?>
    val serverStateFlow: Flow<ServerState>
    val logFlow: Flow<String>
    val handShakeResultFlow: Flow<HandShakeResult>
    fun finishSessionAsHost()
    fun expelGuest(guestId: String)
    fun sendAnswerToGuest(guestId: String, roomName: String? = null, answer: HandShakeResult)
    fun addGuestToHostStreamer(guestId: String)
    fun acceptUserConnection(guest: Guest)
    fun startStreamingAsHost(hostAudioCapturer: HostAudioCapturer)
    fun playGuest(guestId: String)
    fun pauseGuest(guestId: String)
    fun stopStreaming(capturer: HostAudioCapturer)
    fun emptyRoom()
    fun setCurrentRoom(room: Room)
    fun openPortOverLocalWifi(hostIp: String)
}
