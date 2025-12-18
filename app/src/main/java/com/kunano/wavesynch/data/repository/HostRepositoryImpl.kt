package com.kunano.wavesynch.data.repository

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.HostAudioCapturer
import com.kunano.wavesynch.data.stream.HostStreamer
import com.kunano.wavesynch.data.wifi.hotspot.HotspotInfo
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
import com.kunano.wavesynch.data.wifi.hotspot.LocalHotspotController
import com.kunano.wavesynch.data.wifi.server.ServerManager
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.repositories.HostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Socket
import javax.inject.Inject

// data/stream/AudioStreamRepositoryImpl.kt
// data/stream/AudioStreamRepositoryImpl.kt
class HostRepositoryImpl @Inject constructor(
    private val severManager: ServerManager,
    private val hostStreamer: HostStreamer,
    private  val localHotspotController: LocalHotspotController,


    ) : HostRepository {

    override val hotspotInfoFlow: Flow<HotspotInfo?> = localHotspotController.hotspotInfoFLow
    override val hotSpotStateFlow: Flow<HotspotState> = localHotspotController.hotspotStateFlow
    override val connectedGuest: Flow<HashSet<Guest>?> = severManager.connectedGuests


    //Hotspot implementation
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun startHotspot(
        onStarted: (HotspotInfo) -> Unit,
        onError: (Int) -> Unit,
    ) {

        localHotspotController.startHotspot(onStarted, onError)
    }

    override fun stopHotspot() {
        localHotspotController.stopHotspot()
    }

    override fun isHotspotRunning(): Boolean {
        return localHotspotController.isHotspotRunning()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun getHotspotInfo(): HotspotInfo? {
        return localHotspotController.getHotspotInfo()
    }


    override val serverStateFlow: Flow<ServerState> = severManager.serverStateFlow

    override val logFlow: Flow<String> = severManager.logFlow

    private val _handShakeResult = MutableSharedFlow<HandShakeResult>(extraBufferCapacity = 20)
    override val handShakeResultFlow: Flow<HandShakeResult> = _handShakeResult.asSharedFlow()








    override suspend fun startServer(roomId: Long?) {
        severManager.startServerSocket(inComingHandShake = {
            CoroutineScope(Dispatchers.IO).launch {
                performHandShake(it, roomId)
            }
        })
    }

    override fun stopServer() {
        severManager.closeServerSocket()
    }

    suspend fun performHandShake(socket: Socket?, roomId: Long?) {
        val guestHandShake = severManager.readIncomingHandShake(socket)
        val result = severManager.verifyHandshake(guestHandShake, roomId)
        _handShakeResult.tryEmit(result)
    }





    override  fun expelGuest(guestId: String) {
    }

    override fun sendAnswerToGuest(
        guestId: String,
        answer: HandShakeResult,
    ) {
        severManager.sendAnswerToGuest(guestId, answer)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)


    private fun addGuestToHostStreamer(guestSocket: Socket, guestId: String){
        hostStreamer.addGuest(guestId, guestSocket)
    }

    override fun stopStreaming() {

    }

    override fun stopStreamingToGuest(guestId: String) {
        hostStreamer.removeGuest(guestId)
    }

    override fun startStreamingToGuest(guestId: String) {
        TODO("Not yet implemented")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun acceptUserConnection(guest: Guest) {
        severManager.acceptUserConnection(guest)
        val guestSocket = severManager.socketList[guest.userId]
        if (guestSocket != null) {
            guestSocket.tcpNoDelay = true
            addGuestToHostStreamer(guestSocket, guest.userId)
        } else {
            // Handle the case where the guestSocket is null
        }
    }

    override fun closeUserSocket(userId: String) {
        severManager.closeGuestSocket(userId)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startStreamingAsHost(hostAudioCapturer: HostAudioCapturer) {
        hostStreamer.startStreaming(hostAudioCapturer)
    }

}