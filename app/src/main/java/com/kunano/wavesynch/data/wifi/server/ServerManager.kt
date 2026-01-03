package com.kunano.wavesynch.data.wifi.server

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

// data/wifi/WifiDirectManager.kt
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


    var socketList: HashMap<String, Socket> = HashMap()

    private val _connectedGuests = MutableStateFlow< ArrayList<Guest>?>(arrayListOf())
    val connectedGuests: Flow< ArrayList<Guest>?> = _connectedGuests.asStateFlow()




    var isServerRunning = false


    fun startServerSocket(
        room: Room,
        inComingHandShakeResult: (handShakeResult: HandShakeResult) -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (serverSocket == null) {
                serverSocket = ServerSocket(AudioStreamConstants.TCP_PORT)
                _logFlow.tryEmit("socket open on port ${serverSocket?.localPort}")
                isServerRunning = true
                //this block is an infinite loop
                while (isServerRunning) {
                    if (serverSocket?.isClosed != true) {
                        // blocks until a Guest connects
                        try {
                            val clientSocket = serverSocket?.accept()
                            handleClient(clientSocket!!, room, inComingHandShakeResult)
                        } catch (e: Exception) {
                            Log.d("ServerManager", "startServerSocket: ${e.message}")
                        }


                    }

                }
            }
        }
    }

    private fun handleClient(
        clientSocket: Socket,
        room: Room,
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



                    val result = verifyHandshake(guestHandshake, room)
                    inComingHandShakeResult(result)
                }
            } catch (e: IOException) {
                // client disconnected abruptly
                Log.d("Server", "Client disconnected: ${clientSocket.remoteSocketAddress}")
            }catch (e: Exception){
                Log.d("Server", "Client disconnected: ${clientSocket.remoteSocketAddress}")
            } finally {
                // cleanup ALWAYS
                closeGuestSocket(guestId = guestId)
                try { clientSocket.close() } catch (_: Exception) {}
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
        // 1) Receive handshake from guest
        val guestHandshakeJson = input.readLine()
        val guestHandShake = parseHandshake(guestHandshakeJson)
        Log.d("", "Received handshake: $guestHandShake")


        return guestHandShake
    }

    fun sendAnswerToGuest(guestId: String, roomName: String?, answer: HandShakeResult) {

        CoroutineScope(Dispatchers.IO).launch {
            val socket = socketList[guestId]

            val output = BufferedWriter(OutputStreamWriter(socket?.getOutputStream()))

            // 2) Send handshake from host
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
            if (answer == HandShakeResult.DeclinedByHost()) {
                closeGuestSocket(guestId)
            }
        }
    }


    suspend fun verifyHandshake(handshake: HandShake, room: Room): HandShakeResult {

        if (room.id == null) {
            return HandShakeResult.InvalidHandshake(handshake)
        }
        if (handshake.appIdentifier != AppIdProvider.APP_ID) {
            return HandShakeResult.InvalidAppId(handshake)
        }
        if (handshake.protocolVersion != PROTOCOL_VERSION) {
            return HandShakeResult.InvalidProtocol(handshake)
        }

        //If the handshake if indicating that UPD socket of the guest is open
        if (handshake.response == HandShakeResult.UdpSocketOpen().intValue) {
            return HandShakeResult.UdpSocketOpen(handshake)
        }

        /*Both app are the same and share the same protocol so
            it proceeds to check if the guest is trusted
             */
        return checkIfUserIsTrusted(handShake = handshake, room)

    }

    private suspend fun checkIfUserIsTrusted(handShake: HandShake, room: Room): HandShakeResult {
        val trustedGuestsList = getRoomTrustedGuestsUseCase(roomId = room.id!!)

        val isGuestTrusted = trustedGuestsList.contains(handShake.userId)
        Log.d("ServerManager", "checkIfUserIsTrusted: $isGuestTrusted")

        return if (isGuestTrusted) {
            HandShakeResult.Success(handShake)
            //Proceed to streaming audio

        } else {
            //Ask is the user wants to trust this guest
            HandShakeResult.HostApprovalRequired(handShake)
        }
    }


    fun acceptUserConnection(guest: Guest) {

        _connectedGuests.update { current ->
            (current?.plus(guest.copy(isPlaying = true))) as ArrayList<Guest>?   // returns a new List
        }
        val guestSocket = socketList[guest.userId]
        if (guestSocket != null) {
            Log.d("HostRepositoryImpl", "acceptUserConnection: Socket accepted${socketList.size}")
            Log.d("HostRepositoryImpl", "acceptUserConnection: Socket accepted")
            // Start streaming for the guest
        } else {
            // Handle the case where the guestSocket is null
        }
    }

    fun setGuestPlayingState(guestId: String, state: Boolean) {
        _connectedGuests.update { current -> current?.map { guest ->
            if (guest.userId == guestId)
                guest.copy(isPlaying = state)
            else guest
        } as ArrayList<Guest> }
    }


    fun closeGuestSocket(guestId: String) {

        val guestSocket = socketList[guestId]
        if (guestSocket != null) {
            try {
                _connectedGuests.update { current-> current?.filter { it.userId != guestId } as ArrayList<Guest>? }
                guestSocket.close()
                _logFlow.tryEmit("Guest socket closed")
            } catch (e: Exception) {
                _logFlow.tryEmit("Error closing guest socket: ${e.message}")
            }
            // Close the guestSocket
        } else {
            _logFlow.tryEmit("Guest socket not found")
        }
    }

    fun clearConnectedGuests() {

        _connectedGuests.update { arrayListOf() }
    }

    fun clearSockets() {
        socketList.clear()

    }
}