package com.kunano.wavesynch.data.wifi.client

import android.content.Context
import android.os.Build
import android.util.Log
import com.kunano.wavesynch.AppIdProvider
import com.kunano.wavesynch.CrashReporter
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.data.wifi.ConnectionProtocol
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
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class ClientManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    val TAG = "Server manager"

    private val _handShakeResponseFlow =
        MutableSharedFlow<HandShakeResult>(extraBufferCapacity = 20)
    val handShakeResponse: SharedFlow<HandShakeResult> = _handShakeResponseFlow.asSharedFlow()

    private val _serverConnectionsStateFlow =
        MutableStateFlow<ServerConnectionState>(ServerConnectionState.Idle)
    val serverConnectionsStateFlow = _serverConnectionsStateFlow.asStateFlow()

    var handShakeFromHost: HandShake? = null
    var socket: Socket? = null
    var isConnectedToHostServer: Boolean = false
    private var sessionData: SessionData? = null

    val sessionInfo: SessionData?
        get() = sessionData

    fun connectToServer(hostIp: String) {
        scope.launch(Dispatchers.IO) {
            try {
                _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectingToServer)
                socket = Socket()
                socket?.connect(InetSocketAddress(hostIp, AudioStreamConstants.TCP_PORT), 5000)
                _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectedToServer)
                isConnectedToHostServer = true

                val connectionRequestHandShake = HandShake(
                    appIdentifier = AppIdProvider.APP_ID,
                    userId = AppIdProvider.getUserId(context),
                    deviceName = Build.MODEL,
                    protocolVersion = ConnectionProtocol.Protocol.PROTOCOL_VERSION
                )
                sendHandShake(connectionRequestHandShake)

                while (socket?.isConnected == true && isConnectedToHostServer) {
                    receiveHandShakeResponse()
                }
            } catch (e: SocketTimeoutException) {
                _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectionLost)
                CrashReporter.set("operation_tag", "connect_to_server_timeout")
                CrashReporter.record(e)
            } catch (e: IOException) {
                _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectionLost)
                CrashReporter.set("operation_tag", "connect_to_server_io")
                CrashReporter.record(e)
            } catch (e: SecurityException) {
                CrashReporter.set("operation_tag", "connect_to_server_security")
                CrashReporter.record(e)
            } catch (e: IllegalArgumentException) {
                CrashReporter.set("operation_tag", "connect_to_server_illegal_argument")
                CrashReporter.record(e)
            }
        }
    }

    fun sendLivingRoomHandShake() {
        val livingRoomHandShake = HandShake(
            appIdentifier = AppIdProvider.APP_ID,
            userId = AppIdProvider.getUserId(context),
            deviceName = Build.MODEL,
            protocolVersion = ConnectionProtocol.Protocol.PROTOCOL_VERSION,
            response = HandShakeResult.GuestLeftRoom().intValue
        )
        sendHandShake(livingRoomHandShake)
    }

    private fun sendHandShake(handShake: HandShake) {
        scope.launch {
            try {
                val output = BufferedWriter(OutputStreamWriter(socket?.getOutputStream()))
                val myJson = serializeHandshake(handShake)
                output.write(myJson)
                output.newLine()
                output.flush()
                Log.d(TAG, "Sent handshake: $myJson")
            } catch (e: IOException) {
                CrashReporter.set("operation_tag", "send_handshake_io")
                CrashReporter.record(e)
            } catch (e: NullPointerException) {
                CrashReporter.set("operation_tag", "send_handshake_npe")
                CrashReporter.record(e)
            } catch (e: SecurityException) {
                CrashReporter.set("operation_tag", "send_handshake_security")
                CrashReporter.record(e)
            }catch (e: Exception) {
                CrashReporter.set("operation_tag", "Exception send handShake")
                CrashReporter.record(e)
            }
        }
    }

    private fun receiveHandShakeResponse() {
        try {
            val input = BufferedReader(InputStreamReader(socket?.getInputStream()))
            val hostJson = input.readLine()
            if (hostJson == null) {
                _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectionLost)
                return
            }
            Log.d(TAG, "Received host handshake: $hostJson")
            handShakeFromHost = parseHandshake(hostJson)
            handShakeFromHost?.let {
                processHandshakeResponse(it)
            }
        } catch (e: IOException) {
            _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectionLost)
            CrashReporter.set("operation_tag", "receive_handshake_io")
            CrashReporter.record(e)
        } catch (e: NullPointerException) {
            _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectionLost)
            CrashReporter.set("operation_tag", "receive_handshake_npe")
            CrashReporter.record(e)
        }
    }

    var udpSocket: DatagramSocket? = null

    fun openUdpSocket(): DatagramSocket? {
        try {
            udpSocket?.let {
                sendUdpSocketStatus(true)
                return it
            }
            Log.d(TAG, "Opening UDP socket")

            udpSocket = DatagramSocket(null).apply {
                reuseAddress = true
                soTimeout = 0
                bind(InetSocketAddress(AudioStreamConstants.UDP_PORT))
            }

            sendUdpSocketStatus(true)
            return udpSocket
        } catch (e: SocketException) {
            CrashReporter.set("operation_tag", "open_udp_socket")
            CrashReporter.record(e)
            return null
        }
    }

    fun sendUdpSocketStatus(isOpen: Boolean) {
        val response =
            if (isOpen) HandShakeResult.UdpSocketOpen().intValue else HandShakeResult.UdpSocketClosed().intValue

        val udpSocketStatusHandShake = HandShake(
            appIdentifier = AppIdProvider.APP_ID,
            userId = AppIdProvider.getUserId(context),
            deviceName = Build.MODEL,
            protocolVersion = ConnectionProtocol.Protocol.PROTOCOL_VERSION,
            response = response
        )
        sendHandShake(udpSocketStatusHandShake)
    }

    private fun processHandshakeResponse(handShake: HandShake) {
        handShake.response?.let {
            Log.d("tag", "processHandshakeResponse: $it")
            when (handShake.response) {
                HandShakeResult.DeclinedByHost().intValue -> {
                    isConnectedToHostServer = false
                    _handShakeResponseFlow.tryEmit(HandShakeResult.DeclinedByHost(handShake))
                }

                HandShakeResult.Success().intValue -> {
                    _serverConnectionsStateFlow.tryEmit(ServerConnectionState.ConnectedToServer)
                    isConnectedToHostServer = true
                    sessionData = SessionData(handShake.roomName, handShake.deviceName)
                    _handShakeResponseFlow.tryEmit(HandShakeResult.Success(handShake))
                }

                HandShakeResult.HostApprovalRequired().intValue -> {
                    _handShakeResponseFlow.tryEmit(HandShakeResult.HostApprovalRequired(handShake))
                }

                HandShakeResult.ExpelledByHost().intValue -> {
                    isConnectedToHostServer = false
                    _handShakeResponseFlow.tryEmit(HandShakeResult.ExpelledByHost(handShake))
                }
            }
        }
    }

    fun disconnectFromServer() {
        isConnectedToHostServer = false
        try {
            socket?.close()
            udpSocket?.close()
        } catch (e: IOException) {
            CrashReporter.set("operation_tag", "disconnect_from_server")
            CrashReporter.record(e)
        } finally {
            socket = null
            sessionData = null
            udpSocket = null
            _serverConnectionsStateFlow.tryEmit(ServerConnectionState.Disconnected)
        }
    }

}