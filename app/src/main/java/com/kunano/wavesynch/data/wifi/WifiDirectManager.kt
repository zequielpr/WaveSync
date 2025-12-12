package com.kunano.wavesynch.data.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.kunano.wavesynch.AppIdProvider
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.usecase.trusted_guest_use_cases.GetRoomTrustedGuestsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.coroutines.resume

// data/wifi/WifiDirectManager.kt
@SuppressLint("MissingPermission")
class WifiDirectManager(
    private val context: Context,
    private val getRoomTrustedGuestsUseCase: GetRoomTrustedGuestsUseCase,
) {
    val PROTOCOL_VERSION = 1

    private val manager: WifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: WifiP2pManager.Channel =
        manager.initialize(context, context.mainLooper, null)

    private var currentInfo: WifiP2pInfo? = null
    private var groupOwnerAddress: InetAddress? = null

    var serverSocket: ServerSocket? = null

    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 20)
    val logFlow: SharedFlow<String> = _logFlow

    private val _isServerRunning = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val isServerRunning: Flow<Boolean> = _isServerRunning.asSharedFlow()


    var socketList: HashMap<String, Socket> = HashMap()

    private val _connectedGuests = MutableSharedFlow<HashSet<Guest>?>(replay = 1)
    val connectedGuests: Flow<HashSet<Guest>?> = _connectedGuests.asSharedFlow()







    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) {



                manager.requestGroupInfo(channel) { group ->
                    group?.let {
                        if (group.isGroupOwner) {

                        }


                    }
                }
            }

        }


    }

    init {
        context.registerReceiver(receiver, intentFilter)
    }


    suspend fun createGroupAsHost(): Result<Unit> = suspendCancellableCoroutine { cont ->

        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Now safe to create new group
                manager.createGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        _logFlow.tryEmit("Group created, waiting for client…")
                        cont.resume(Result.success(Unit))
                    }

                    override fun onFailure(reason: Int) {
                        val reasonText = when (reason) {
                            WifiP2pManager.BUSY -> "BUSY"
                            WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
                            WifiP2pManager.ERROR -> "ERROR"
                            else -> "UNKNOWN($reason)"
                        }
                        _logFlow.tryEmit("createGroupAsHost: failed: $reasonText")
                        cont.resume(Result.failure(IllegalStateException("createGroup failed: $reasonText")))
                    }
                })
            }

            override fun onFailure(reason: Int) {
                // even if removeGroup fails, we can still try to create
                manager.createGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        _logFlow.tryEmit("Group created, waiting for client…")
                        cont.resume(Result.success(Unit))
                    }

                    override fun onFailure(reason: Int) {
                        val reasonText = when (reason) {
                            WifiP2pManager.BUSY -> "BUSY"
                            WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
                            WifiP2pManager.ERROR -> "ERROR"
                            else -> "UNKNOWN($reason)"
                        }
                        _logFlow.tryEmit("createGroupAsHost: failed: $reasonText")
                        cont.resume(Result.failure(IllegalStateException("createGroup failed: $reasonText")))
                    }
                })
            }
        })
    }


    fun startServerSocket( inComingHandShake: (socket: Socket?) -> Unit ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (serverSocket == null) {
                serverSocket = ServerSocket(AudioStreamConstants.PORT)
                _logFlow.tryEmit("socket open on port ${serverSocket?.localPort}")

                _isServerRunning.tryEmit(true)
                //this block is an infinite loop
                while (true) {
                    val clientSocket = serverSocket?.accept()  // blocks until a Guest connects
                    inComingHandShake(clientSocket)
                }
            }
        }
    }

    private suspend fun closeServerSocket() {
        withContext(Dispatchers.IO) {
            try {
                serverSocket?.close()
                _isServerRunning.tryEmit(false)
            } catch (_: Exception) {
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun removeGuestFromWifiGroup(){
        /*manager.removeClient(channel, guestId, object : WifiP2pManager.ActionListener {

        })*/
    }



    fun readIncomingHandShake(socket: Socket?): HandShake {
        val input = BufferedReader(InputStreamReader(socket?.getInputStream()))
        // 1) Receive handshake from guest
        val guestHandshakeJson = input.readLine()
        val guestHandShake = parseHandshake(guestHandshakeJson)
        Log.d("", "Received handshake: $guestHandShake")


        //Add guest socket to list
        socket?.let {
            socketList[guestHandShake.userId] = socket
        }





        return guestHandShake
    }

    fun  sendAnswerToGuest(guestId: String, answer: HandShakeResult){

        CoroutineScope(Dispatchers.IO).launch {
            val socket = socketList[guestId]

            val output = BufferedWriter(OutputStreamWriter(socket?.getOutputStream()))

            // 2) Send handshake from host
            val hostHandshake = HandShake(
                appIdentifier = AppIdProvider.APP_ID,
                userId = AppIdProvider.getUserId(context),
                deviceName = android.os.Build.MODEL,
                protocolVersion = 1,
                response = answer.intValue
            )
            output.write(serializeHandshake(handshake = hostHandshake))
            Log.d("", "Sent handshake: $hostHandshake")
            output.newLine()
            output.flush()
            if (answer == HandShakeResult.DeclinedByHost()){
                closeGuestSocket(guestId)
            }
        }
    }


    suspend fun verifyHandshake(handshake: HandShake, roomId: Long?): HandShakeResult {

        if (roomId == null){
            return HandShakeResult.InvalidHandshake(handshake)
        }
        if (handshake.appIdentifier != AppIdProvider.APP_ID){
            return HandShakeResult.InvalidAppId(handshake)
        }
        if (handshake.protocolVersion != PROTOCOL_VERSION){
            return HandShakeResult.InvalidProtocol(handshake)
        }

        /*Both app are the same and share the same protocol so
            it proceeds to check if the guest is trusted
             */
        return checkIfUserIsTrusted(handShake = handshake, roomId = roomId)

    }

    private suspend fun checkIfUserIsTrusted(handShake: HandShake, roomId: Long): HandShakeResult {
        val trustedGuestsList = getRoomTrustedGuestsUseCase(roomId = roomId)

        val isGuestTrusted = trustedGuestsList.contains(handShake.userId)

        if (isGuestTrusted) {
            return HandShakeResult.Success(handShake)
            //Proceed to streaming audio

        } else {
            //Ask is the user wants to trust this guest
            return HandShakeResult.HostApprovalRequired(handShake)
        }
    }

    fun isGroupOwner(): Boolean = currentInfo?.isGroupOwner == true

    val connectedGuestList: HashSet<Guest> = HashSet()

    fun acceptUserConnection(guest: Guest) {

        connectedGuestList.add(guest)
        _connectedGuests.tryEmit(connectedGuestList)
        val guestSocket = socketList[guest.userId]
        if (guestSocket != null) {
            Log.d("HostRepositoryImpl", "acceptUserConnection: Socket accepted${socketList.size}")
            Log.d("HostRepositoryImpl", "acceptUserConnection: Socket accepted")
            // Start streaming for the guest
        } else {
            // Handle the case where the guestSocket is null
        }
    }

    fun closeGuestSocket(guestId: String){

        val guestSocket = socketList[guestId]
        if (guestSocket != null) {
            try {
                connectedGuestList.removeIf { it.userId == guestId }
                _connectedGuests.tryEmit(connectedGuestList)
                guestSocket.close()
                Log.d("HostRepositoryImpl", "closeUserSocket: Socket closed")
            }catch (e: Exception){
                Log.d("HostRepositoryImpl", "closeUserSocket: ${e.message}")
            }
            // Close the guestSocket
        } else {

        }
    }


    suspend fun removeGroup() {
        suspendCancellableCoroutine<Unit> { cont ->
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    _logFlow.tryEmit("Group removed")
                    cont.resume(Unit)
                }

                override fun onFailure(reason: Int) {
                    cont.resume(Unit)
                }
            })
        }
    }
}
