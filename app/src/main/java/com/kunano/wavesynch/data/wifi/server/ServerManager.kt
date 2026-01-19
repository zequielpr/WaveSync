package com.kunano.wavesynch.data.wifi.server

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.kunano.wavesynch.AppIdProvider
import com.kunano.wavesynch.CrashReporter
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.wifi.ConnectionProtocol
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.usecase.host.GetRoomTrustedGuestsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@SuppressLint("MissingPermission")
class ServerManager(
    private val context: Context,
    private val getRoomTrustedGuestsUseCase: GetRoomTrustedGuestsUseCase,
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    var serverSocket: ServerSocket? = null
    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 20, replay = 1)
    val logFlow: SharedFlow<String> = _logFlow.asSharedFlow()
    var socketList: ArrayMap<String, Socket> = arrayMapOf<String, Socket>()
    private val _connectedGuests = MutableStateFlow<LinkedHashSet<Guest>?>(linkedSetOf())
    val connectedGuests: Flow<LinkedHashSet<Guest>?> = _connectedGuests.asStateFlow()
    var isServerRunning = false
    private var currentRoom: Room? = null

    private val _handShakeResult = MutableSharedFlow<HandShakeResult>(extraBufferCapacity = 20)
    val handShakeResultFlow: Flow<HandShakeResult> = _handShakeResult.asSharedFlow()

    private val _serverStateFlow = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverStateFlow: Flow<ServerState> = _serverStateFlow.asStateFlow()

    fun setCurrentRoomOnServer(room: Room) {
        currentRoom = room
    }

    fun startServerSocket(
        ipAddress: String,
    ): ServerSocket? {
        try {
            _serverStateFlow.tryEmit(ServerState.Starting)
            val inetAddress = InetAddress.getByName(ipAddress)

            if (isServerRunning && serverSocket?.inetAddress?.hostAddress == ipAddress) {
                _serverStateFlow.tryEmit(ServerState.Running)
                _logFlow.tryEmit("socket open on port ${serverSocket?.localPort}")
                return serverSocket
            } else {
                serverSocket?.close()
                serverSocket = ServerSocket(AudioStreamConstants.TCP_PORT, 50, inetAddress)
                _logFlow.tryEmit("Server restarted")
            }
            _logFlow.tryEmit("socket open on port ${serverSocket?.localPort}")
            isServerRunning = true
            _serverStateFlow.tryEmit(ServerState.Running)

            CoroutineScope(Dispatchers.IO).launch {
                while (isServerRunning) {
                    if (serverSocket?.isClosed == false) {
                        try {
                            val clientSocket = serverSocket?.accept()
                            clientSocket?.let { handleClient(it) }
                        } catch (e: IOException) {
                            if (serverSocket?.isClosed == true) {
                                _logFlow.tryEmit("Server closed")
                            } else {
                                CrashReporter.set("operation_tag", "accept_client_connection")
                                CrashReporter.record(e)
                            }
                            break
                        }
                    }
                }
            }
        } catch (e: UnknownHostException) {
            CrashReporter.set("operation_tag", "start_server_socket_unknown_host")
            CrashReporter.record(e)
        } catch (e: IOException) {
            CrashReporter.set("operation_tag", "start_server_socket_io")
            CrashReporter.record(e)
        } catch (e: SecurityException) {
            CrashReporter.set("operation_tag", "start_server_socket_security")
            CrashReporter.record(e)
        } catch (e: IllegalArgumentException) {
            CrashReporter.set("operation_tag", "start_server_socket_illegal_argument")
            CrashReporter.record(e)
        }
        return serverSocket
    }

    private fun handleClient(clientSocket: Socket) {
        scope.launch {
            var guestId: String = ""
            try {
                val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                while (isServerRunning) {
                    val guestHandshake = readIncomingHandShake(input)
                    guestId = guestHandshake!!.userId
                    socketList[guestId] = clientSocket
                    val result = verifyHandshake(guestHandshake)
                    _handShakeResult.emit(result)
                }
            } catch (e: IOException) {
                Log.d("Server", "Client disconnected: ${clientSocket.remoteSocketAddress}")
            } finally {
                try {
                    clientSocket.close()
                } catch (e: IOException) {
                    CrashReporter.set("operation_tag", "handle_client_close_socket")
                    CrashReporter.record(e)
                }
            }
        }
    }

    fun closeServerSocket() {
        try {
            isServerRunning = false
            serverSocket?.close()
            serverSocket = null
        } catch (e: IOException) {
            CrashReporter.set("operation_tag", "close_server_socket")
            CrashReporter.record(e)
        }
    }

    fun readIncomingHandShake(input: BufferedReader): HandShake? {
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
                    protocolVersion = ConnectionProtocol.Protocol.PROTOCOL_VERSION,
                    response = answer.intValue
                )
                output.write(serializeHandshake(handshake = hostHandshake))
                Log.d("", "Sent handshake: $hostHandshake")
                output.newLine()
                output.flush()
            } catch (e: IOException) {
                CrashReporter.set("operation_tag", "send_answer_to_guest")
                CrashReporter.record(e)
            }catch (e: NullPointerException) {
                CrashReporter.set("operation_tag", "send_answer_to_guest_npe")
                CrashReporter.record(e)
            }catch (e: SecurityException) {
                CrashReporter.set("operation_tag", "send_answer_to_guest_security")
                CrashReporter.record(e)
            }catch (e: Exception) {
                CrashReporter.set("operation_tag", "send_answer_to_guest")
                CrashReporter.record(e)
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
        if (handshake.protocolVersion != ConnectionProtocol.Protocol.PROTOCOL_VERSION) {
            return HandShakeResult.InvalidProtocol(handshake)
        }
        if (handshake.response == HandShakeResult.UdpSocketOpen().intValue) {
            return HandShakeResult.UdpSocketOpen(handshake)
        }
        if (handshake.response == HandShakeResult.GuestLeftRoom().intValue) {
            return HandShakeResult.GuestLeftRoom(handshake)
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
            } catch (e: IOException) {
                _logFlow.tryEmit("Error closing guest socket: ${e.message}")
                CrashReporter.set("operation_tag", "close_guest_socket")
                CrashReporter.record(e)
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
            try {
                it.value.close()
            } catch (e: IOException) {
                CrashReporter.set("operation_tag", "close_and_clear_sockets")
                CrashReporter.record(e)
            }
        }
        socketList.clear()
    }
}