package com.kunano.wavesynch.data.stream

import com.kunano.wavesynch.data.wifi.WifiDirectManager
import com.kunano.wavesynch.domain.model.StreamState
import com.kunano.wavesynch.domain.repositories.sound_streaming_repository.AudioStreamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject

// data/stream/AudioStreamRepositoryImpl.kt
// data/stream/AudioStreamRepositoryImpl.kt
class AudioStreamRepositoryImpl @Inject constructor(
    private val wifi: WifiDirectManager
) : AudioStreamRepository {

    private val sender = AudioSender()
    private val receiver = AudioReceiver()

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    val streamState: Flow<StreamState> = _state.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null

    override suspend fun hostRoom(){
        _state.value = StreamState.Connecting
        val res = wifi.createGroupAsHost()
        if (res.isFailure) {
            _state.value = StreamState.Error(res.exceptionOrNull()?.message ?: "Host failed")
            return
        }
        _state.value = StreamState.Connected
    }

    override suspend fun joinRoom(hostIp: String) {
        _state.value = StreamState.Connecting
        socket = wifi.connectToHost(hostIp)
        _state.value = StreamState.Connected
    }

    override suspend fun startStreamingAsHost() {
        _state.value = StreamState.Streaming
        withContext(Dispatchers.IO) {
            serverSocket = wifi.openServerSocket()
            socket = wifi.waitForClient(serverSocket!!)
            receiver.start(socket!!.getInputStream())
        }
    }

    override suspend fun startStreamingAsGuest() {

    }

    override suspend fun stopStreaming() {
        _state.value = StreamState.Idle
        sender.stop()
        receiver.stop()
        withContext(Dispatchers.IO) {
            try { socket?.close() } catch (_: Exception) {}
            try { serverSocket?.close() } catch (_: Exception) {}
            wifi.removeGroup()
        }
    }
}

