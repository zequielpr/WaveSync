package com.kunano.wavesynch.data.wifi.client

import android.content.Context
import android.os.Build
import android.util.Log
import com.kunano.wavesynch.AppIdProvider
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.wifi.server.HandShake
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.parseHandshake
import com.kunano.wavesynch.data.wifi.server.serializeHandshake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket

class ClientManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    val TAG = "Server manager"

    // Removed class-level socket property to avoid reuse issues
    private val _handShakeResponseFlow =
        MutableSharedFlow<HandShakeResult>(extraBufferCapacity = 20)
    val handShakeResponse: SharedFlow<HandShakeResult> = _handShakeResponseFlow.asSharedFlow()

    private val _serverConnectionsStateFlow = MutableStateFlow<ServerConnectionState>(ServerConnectionState.Idle)
    val serverConnectionsStateFlow = _serverConnectionsStateFlow.asStateFlow()


    var handShakeFromHost: HandShake? = null

    var socket: Socket? = null
    var isConnectedToHostServer: Boolean = false

    private var sessionData: SessionData? = null

    val sessionInfo: SessionData?
        get() = sessionData


    /** Open TCP socket to host and do handshake */
    fun connectToServer(hostIp: String) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectingToServer)
                // Create a new socket for each connection attempt
                socket = Socket()
                socket?.connect(InetSocketAddress(hostIp, AudioStreamConstants.TCP_PORT), 5000)


                val connectionRequestHandShake = HandShake(
                    appIdentifier = AppIdProvider.APP_ID,
                    userId = AppIdProvider.getUserId(context),
                    deviceName = Build.MODEL,
                    protocolVersion = 1
                )
                sendHandShake(connectionRequestHandShake)
                while (socket != null && socket!!.isConnected){
                    try {
                        receiveHandShakeResponse()
                    }catch (e: Exception){
                        if(e.javaClass == NullPointerException::class.java){
                            _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectionLost)
                        }
                        Log.d(TAG, "connectToServer: ${e.javaClass}")

                        Log.d(TAG, "connectToServer: ${e.message}")
                        break
                    }
                }


            }.onFailure { e ->
                Log.e(TAG, "Error connecting to server", e)
            }
        }
    }

    private fun sendHandShake(handShake: HandShake) {
        // 1) Send handshake to host
        scope.launch {
            try {
                val output = BufferedWriter(OutputStreamWriter(socket?.getOutputStream()))
                val myJson = serializeHandshake(handShake)
                output.write(myJson)
                output.newLine()
                output.flush()
                Log.d(TAG, "Sent handshake: $myJson")
            }catch (e: Exception){
                Log.d(TAG, "sendHandShake: ${e.message}")
            }
        }
    }

    private fun receiveHandShakeResponse() {
        // 2) Receive host handshake
        val input = BufferedReader(InputStreamReader(socket?.getInputStream()))
        val hostJson = input.readLine()
        Log.d(TAG, "Received host handshake: $hostJson")
        handShakeFromHost = parseHandshake(hostJson)
        handShakeFromHost?.let {
            processHandshakeResponse(it)
        }

    }

    var udpSocket: DatagramSocket? = null

    fun openUdpSocket(): DatagramSocket {

        udpSocket?.let { return it }
        Log.d(TAG, "Opening UDP socket")

        udpSocket = DatagramSocket(null).apply {
            reuseAddress = true
            soTimeout = 0
            bind(InetSocketAddress(AudioStreamConstants.UDP_PORT))
        }

        sendUdpSocketStatus(true)
        return udpSocket!!
    }

    fun sendUdpSocketStatus(isOpen: Boolean) {

        val response =
            if (isOpen) HandShakeResult.UdpSocketOpen().intValue else HandShakeResult.UdpSocketClosed().intValue

        val udpSocketStatusHandShake = HandShake(
            appIdentifier = AppIdProvider.APP_ID,
            userId = AppIdProvider.getUserId(context),
            deviceName = Build.MODEL,
            protocolVersion = 1,
            response = response
        )
        sendHandShake(udpSocketStatusHandShake)

    }


    private fun processHandshakeResponse(handShake: HandShake) {

        handShake.response?.let {

            Log.d("tag", "processHandshakeResponse: $it")
            when (handShake.response) {
                HandShakeResult.DeclinedByHost().intValue -> {
                    _handShakeResponseFlow.tryEmit(
                        HandShakeResult.DeclinedByHost(handShake)
                    )
                }

                HandShakeResult.Success().intValue -> {
                    _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectedToServer)
                    isConnectedToHostServer = true
                    sessionData = SessionData(handShake.roomName, handShake.deviceName)
                    _handShakeResponseFlow.tryEmit(HandShakeResult.Success(handShake))
                }

                HandShakeResult.HostApprovalRequired().intValue -> {
                    _handShakeResponseFlow.tryEmit(
                        HandShakeResult.HostApprovalRequired(handShake)
                    )
                }

                HandShakeResult.ExpelledByHost().intValue -> {
                    isConnectedToHostServer = false
                    _handShakeResponseFlow.tryEmit(
                        HandShakeResult.ExpelledByHost(handShake)
                    )
                }


            }


        }
    }

    fun disconnectFromServer() {
        socket?.close()
        isConnectedToHostServer = false
        socket = null
        sessionData = null
        udpSocket?.close()
        udpSocket = null
        _serverConnectionsStateFlow.tryEmit(ServerConnectionState.Disconnected)
    }

    fun isConnectedToServer(): Boolean {
        return isConnectedToHostServer
    }


}
