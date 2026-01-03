package com.kunano.wavesynch.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.stream.HostAudioCapturer
import com.kunano.wavesynch.data.stream.HostStreamer
import com.kunano.wavesynch.data.wifi.hotspot.HotspotInfo
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
import com.kunano.wavesynch.data.wifi.hotspot.LocalHotspotController
import com.kunano.wavesynch.data.wifi.server.ServerManager
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.repositories.HostRepository
import com.kunano.wavesynch.services.StartHotspotService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import javax.inject.Inject

// data/stream/AudioStreamRepositoryImpl.kt
// data/stream/AudioStreamRepositoryImpl.kt
class HostRepositoryImpl @Inject constructor(
    private val serverManager: ServerManager,
    private val hostStreamer: HostStreamer,
    private  val localHotspotController: LocalHotspotController,
    @ApplicationContext private val context: Context


    ) : HostRepository {

    override val hotspotInfoFlow: Flow<HotspotInfo?> = localHotspotController.hotspotInfoFLow
    override val hotSpotStateFlow: Flow<HotspotState> = localHotspotController.hotspotStateFlow
    override val connectedGuest: Flow<ArrayList<Guest>?> = serverManager.connectedGuests

    private val _serverStateFlow = MutableStateFlow<ServerState>(ServerState.Stopped)

    override val serverStateFlow: Flow<ServerState> =  _serverStateFlow.asStateFlow()

    override val logFlow: Flow<String> = serverManager.logFlow

    private val _handShakeResult = MutableSharedFlow<HandShakeResult>(extraBufferCapacity = 20)
    override val handShakeResultFlow: Flow<HandShakeResult> = _handShakeResult.asSharedFlow()

    override fun isHostStreaming(): Boolean {
        return hostStreamer.isHostStreaming()
    }

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
        val intent = Intent(context, StartHotspotService::class.java)
        context.stopService(intent)
    }

    override fun isHotspotRunning(): Boolean {
        return localHotspotController.isHotspotRunning()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun getHotspotInfo(): HotspotInfo? {
        return localHotspotController.getHotspotInfo()
    }

    override fun finishSessionAsHost() {
        hostStreamer.stopStreaming()
        stopHotspot()
        serverManager.closeServerSocket()
        emptyRoom()


    }



    override suspend fun startServer(room: Room) {
        _serverStateFlow.tryEmit(ServerState.Starting)
        serverManager.startServerSocket(room, inComingHandShakeResult = {
            _handShakeResult.tryEmit(it)
        })
        _serverStateFlow.tryEmit(ServerState.Running)
    }

    override fun stopServer() {
        serverManager.closeServerSocket()
        _serverStateFlow.tryEmit(ServerState.Stopped)
    }



    override  fun expelGuest(guestId: String) {
        hostStreamer.removeGuest(guestId)
        serverManager.closeGuestSocket(guestId)
    }

    override fun sendAnswerToGuest(
        guestId: String,
        roomName: String?,
        answer: HandShakeResult,
    ) {
        serverManager.sendAnswerToGuest(guestId, roomName, answer)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)


  override fun addGuestToHostStreamer(guestId: String){
        val guestSocket = serverManager.socketList[guestId]
        val inetSocketAddress: InetSocketAddress = InetSocketAddress(guestSocket?.inetAddress, AudioStreamConstants.UDP_PORT)
        hostStreamer.addGuest(guestId,inetSocketAddress )
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun acceptUserConnection(guest: Guest) {
        serverManager.acceptUserConnection(guest)
        val guestSocket = serverManager.socketList[guest.userId]
        if (guestSocket != null) {
            guestSocket.tcpNoDelay = true
        } else {
            // Handle the case where the guestSocket is null
        }
    }

    override fun closeUserSocket(userId: String) {
        serverManager.closeGuestSocket(userId)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startStreamingAsHost(hostAudioCapturer: HostAudioCapturer) {
        hostStreamer.startStreaming(hostAudioCapturer)
        _serverStateFlow.tryEmit(ServerState.Streaming)
    }

    override fun playGuest(guestId: String){

        serverManager.setGuestPlayingState(guestId, true)
        hostStreamer.resumeGuest(guestId)
    }

    override fun pauseGuest(guestId: String){
        hostStreamer.pauseGuest(guestId)
        serverManager.setGuestPlayingState(guestId, false)
    }

    override fun stopStreaming(){
        hostStreamer.pauseStreaming()
        _serverStateFlow.tryEmit(ServerState.Idle)

    }

    override fun emptyRoom() {
        hostStreamer.removeAllGuests()
        serverManager.clearSockets()
        serverManager.clearConnectedGuests()

    }

}