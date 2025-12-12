package com.kunano.wavesynch.data.repository

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.HostAudioCapturer
import com.kunano.wavesynch.data.stream.HostStreamer
import com.kunano.wavesynch.data.wifi.HandShakeResult
import com.kunano.wavesynch.data.wifi.WifiDirectManager
import com.kunano.wavesynch.data.wifi.ServerState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.repositories.HostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Socket
import javax.inject.Inject

// data/stream/AudioStreamRepositoryImpl.kt
// data/stream/AudioStreamRepositoryImpl.kt
class HostRepositoryImpl @Inject constructor(
    private val wifi: WifiDirectManager,
    private val hostStreamer: HostStreamer,
    ) : HostRepository {

    override val connectedGuest: Flow<HashSet<Guest>?> = wifi.connectedGuests



    private val _state = MutableStateFlow<ServerState>(ServerState.Idle)
    override val serverStateFlow: Flow<ServerState> = _state.asStateFlow()

    override val logFlow: Flow<String> = wifi.logFlow

    private val _handShakeResult = MutableSharedFlow<HandShakeResult>(extraBufferCapacity = 20)
    override val handShakeResultFlow: Flow<HandShakeResult> = _handShakeResult.asSharedFlow()








    override suspend fun hostRoom(roomId: Long?) {
        collectServerState()
        _state.value = ServerState.Starting
        val res = wifi.createGroupAsHost()
        if (res.isFailure) {
            _state.value = ServerState.Error(res.exceptionOrNull()?.message ?: "Host failed")
            return
        }else{
            wifi.startServerSocket(inComingHandShake = {
                CoroutineScope(Dispatchers.IO).launch {
                    performHandShake(it, roomId)
                }
            })
        }
    }

    suspend fun performHandShake(socket: Socket?, roomId: Long?) {
        val guestHandShake = wifi.readIncomingHandShake(socket)
        val result = wifi.verifyHandshake(guestHandShake, roomId)
        _handShakeResult.tryEmit(result)
    }




    private fun collectServerState() {
        CoroutineScope(Dispatchers.IO).launch {
            wifi.isServerRunning.collect {
                if (it) {
                    _state.value = ServerState.Running
                } else {
                    _state.value = ServerState.Idle
                }
            }
        }

    }

    override suspend fun expelGuest(guestId: String) {
    }

    override fun sendAnswerToGuest(
        guestId: String,
        answer: HandShakeResult,
    ) {
        wifi.sendAnswerToGuest(guestId, answer)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startStreamingAsHost(capturer: HostAudioCapturer) {
        hostStreamer.startStreaming(capturer)

    }

    private fun addGuestToHostStreamer(guestSocket: Socket, guestId: String){
        hostStreamer.addGuest(guestId, guestSocket)
    }

    override suspend fun stopStreaming() {
        _state.tryEmit(ServerState.Idle)

    }

    override fun stopStreamingToGuest(guestId: String) {
        hostStreamer.removeGuest(guestId)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun acceptUserConnection(guest: Guest) {
        wifi.acceptUserConnection(guest)
        val guestSocket = wifi.socketList[guest.userId]
        if (guestSocket != null) {
            guestSocket.tcpNoDelay = true
            addGuestToHostStreamer(guestSocket, guest.userId)
        } else {
            // Handle the case where the guestSocket is null
        }
    }

    override fun closeUserSocket(userId: String) {
        wifi.closeGuestSocket(userId)
    }

}