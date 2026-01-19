package com.kunano.wavesynch.data.wifi.server

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.datastore.core.IOException
import com.kunano.wavesynch.AppIdProvider
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.usecase.host.GetRoomTrustedGuestsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

@SuppressLint("MissingPermission")
class ServerManager(
    private val context: Context,
    private val getRoomTrustedGuestsUseCase: GetRoomTrustedGuestsUseCase,
) {
    val PROTOCOL_VERSION = 1
    private val scope = CoroutineScope(Dispatchers.IO)
    var serverSocket: ServerSocket? = null
    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 20)
    val logFlow: SharedFlow<String> = _logFlow
    var socketList: ArrayMap<String, Socket> = arrayMapOf<String, Socket>()
    private val _connectedGuests = MutableStateFlow<LinkedHashSet<Guest>?>(linkedSetOf())
    val connectedGuests: Flow<LinkedHashSet<Guest>?> = _connectedGuests.asStateFlow()
    var isServerRunning = false
    private var currentRoom: Room? = null

    fun setCurrentRoomOnServer(room: Room) {
        currentRoom = room
    }

    fun startServerSocket(
        ipAddress: String,
        onRunning: () -> Unit = {},
        inComingHandShakeResult: (handShakeResult: HandShakeResult) -> Unit,
    ): ServerSocket? {
        val inetAddress = InetAddress.getByName(ipAddress)

        if (isServerRunning && serverSocket?.inetAddress == inetAddress) {
            return serverSocket
        } else if (serverSocket == null) {
            _logFlow.tryEmit("Server started")
        } else {
            _logFlow.tryEmit("Server restarted")
        }
        serverSocket = ServerSocket(
            AudioStreamConstants.TCP_PORT,
            50,
            inetAddress
        )
        _logFlow.tryEmit("socket open on port ${serverSocket?.localPort}")

        isServerRunning = true
        onRunning()
        CoroutineScope(Dispatchers.IO).launch {
            while (isServerRunning) {
                if (serverSocket?.isClosed != true) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        handleClient(clientSocket!!,  inComingHandShakeResult)
                    } catch (e: Exception) {
                        Log.d("ServerManager", "startServerSocket: ${e.message}")
                        if (serverSocket?.isClosed == true){
                            _logFlow.tryEmit("Server closed")
                            serverSocket = null
                        }
                        break
                    }
                }
            }
        }
        return serverSocket
    }

    private fun handleClient(
        clientSocket: Socket,
        inComingHandShakeResult: (handShakeResult: HandShakeResult) -> Unit,
    ) {
        scope.launch {
            var guestId: String = ""
            val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

            try {
                while (isServerRunning) {
                    val guestHandshake = readIncomingHandShake(input)
                    guestId = guestHandshake.userId
                    socketList[guestId] = clientSocket
                    val result = verifyHandshake(guestHandshake)
                    inComingHandShakeResult(result)
                }
            } catch (e: IOException) {
                Log.d("Server", "Client disconnected: ${clientSocket.remoteSocketAddress}")
            } catch (e: Exception) {
                Log.d("Server", "Client disconnected: ${clientSocket.remoteSocketAddress}")
            } finally {
                //closeGuestSocket(guestId = guestId)
                try {
                    clientSocket.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun closeServerSocket() {
        try {
            isServerRunning = false
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun removeGuestFromWifiGroup() {
        /*manager.removeClient(channel, guestId, object : WifiP2pManager.ActionListener {

        })*/
    }

    fun readIncomingHandShake(input: BufferedReader): HandShake {
        val guestHandshakeJson = input.readLine()
        val guestHandShake = parseHandshake(guestHandshakeJson)
        Log.d("", "Received handshake: $guestHandShake")
        return guestHandShake
    }

    fun sendAnswerToGuest(guestId: String, roomName: String? = null, answer: HandShakeResult) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = socketList[guestId]
                val output = BufferedWriter(OutputStreamWriter(socket?.getOutputStream()))
                val hostHandshake = HandShake(
                    appIdentifier = AppIdProvider.APP_ID,
                    userId = AppIdProvider.getUserId(context),
                    deviceName = Build.MODEL,
                    roomName = roomName,
                    protocolVersion = 1,
                    response = answer.intValue
                )
                output.write(serializeHandshake(handshake = hostHandshake))
                Log.d("", "Sent handshake: $hostHandshake")
                output.newLine()
                output.flush()
            } catch (e: Exception) {
                Log.d("ServerManager", "sendAnswerToGuest: ${e.message}")
            }

            if (answer == HandShakeResult.DeclinedByHost() || answer == HandShakeResult.ExpelledByHost()) {
                closeGuestSocket(guestId)
            }
        }
    }

    suspend fun verifyHandshake(handshake: HandShake): HandShakeResult {
        if (currentRoom?.id == null) {
            return HandShakeResult.InvalidHandshake(handshake)
        }
        if (handshake.appIdentifier != AppIdProvider.APP_ID) {
            return HandShakeResult.InvalidAppId(handshake)
        }
        if (handshake.protocolVersion != PROTOCOL_VERSION) {
            return HandShakeResult.InvalidProtocol(handshake)
        }
        if (handshake.response == HandShakeResult.UdpSocketOpen().intValue) {
            return HandShakeResult.UdpSocketOpen(handshake)
        }
        return checkIfUserIsTrusted(handShake = handshake)
    }

    private suspend fun checkIfUserIsTrusted(handShake: HandShake): HandShakeResult {
        val trustedGuestsList = getRoomTrustedGuestsUseCase(roomId = currentRoom?.id!!)
        val isGuestTrusted = trustedGuestsList.contains(handShake.userId)
        Log.d("ServerManager", "checkIfUserIsTrusted: $isGuestTrusted")

        return if (isGuestTrusted) {
            HandShakeResult.Success(handShake)
        } else {
            HandShakeResult.HostApprovalRequired(handShake)
        }
    }

    fun acceptUserConnection(guest: Guest) {
        _connectedGuests.update { current ->
            val newSet = LinkedHashSet(current ?: emptySet())
            newSet.add(guest.copy(isPlaying = true))
            newSet
        }
    }

    fun setGuestPlayingState(guestId: String, state: Boolean) {
        _connectedGuests.update { currentGuestList ->
            val old = currentGuestList ?: linkedSetOf()
            val updated = old.map { g ->
                if (g.userId == guestId) g.copy(isPlaying = state) else g
            }
            LinkedHashSet(updated)
        }
    }

    fun closeGuestSocket(guestId: String) {
        val guestSocket = socketList[guestId]
        if (guestSocket != null) {
            try {
                _connectedGuests.update {
                    val newSet = LinkedHashSet(it ?: emptySet())
                    newSet.removeIf { g -> g.userId == guestId }
                    newSet
                }
                guestSocket.close()
                _logFlow.tryEmit("Guest socket closed")
            } catch (e: Exception) {
                _logFlow.tryEmit("Error closing guest socket: ${e.message}")
            }
        } else {
            _logFlow.tryEmit("Guest socket not found")
        }
    }

    fun clearConnectedGuests() {
        _connectedGuests.update {
            linkedSetOf()
        }
    }

    fun closeAndClearSockets() {
        socketList.forEach {
            it.value.close()
        }
        socketList.clear()
    }
}
