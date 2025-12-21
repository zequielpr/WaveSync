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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
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

    var inputStream: InputStream? = null


    var socket: Socket? = null


    /** Open TCP socket to host and do handshake */
    fun connectToServer(hostIp: String, onConnected: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                // Create a new socket for each connection attempt
                socket = Socket()
                socket?.connect(InetSocketAddress(hostIp, AudioStreamConstants.PORT), 5000)

                inputStream = socket?.getInputStream()

                socket?.let {
                    sendHandShake(it)
                    receiveHandShakeResponse(it)
                }

                onConnected
            }.onFailure { e ->
                Log.e(TAG, "Error connecting to server", e)
            }
        }
    }

    private fun sendHandShake(socket: Socket) {
        // 1) Send handshake to host
        val output = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
        val myHandshake = HandShake(
            appIdentifier = AppIdProvider.APP_ID,
            userId = AppIdProvider.getUserId(context),
            deviceName = Build.MODEL,
            protocolVersion = 1
        )
        val myJson = serializeHandshake(myHandshake)
        output.write(myJson)
        output.newLine()
        output.flush()
        Log.d(TAG, "Sent handshake: $myJson")
    }

    private fun receiveHandShakeResponse(socket: Socket) {
        // 2) Receive host handshake
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val hostJson = input.readLine()
        Log.d(TAG, "Received host handshake: $hostJson")
        val hostHandshake = parseHandshake(hostJson)
        processHandshakeResponse(hostHandshake)
    }


    private fun processHandshakeResponse(handShake: HandShake) {

        handShake.response?.let {

            when (handShake.response) {
                HandShakeResult.DeclinedByHost().intValue -> _handShakeResponseFlow.tryEmit(
                    HandShakeResult.DeclinedByHost(handShake)
                )

                HandShakeResult.Success().intValue -> _handShakeResponseFlow.tryEmit(HandShakeResult.Success(handShake))
                HandShakeResult.HostApprovalRequired().intValue -> _handShakeResponseFlow.tryEmit(
                    HandShakeResult.HostApprovalRequired(handShake)
                )

            }


        }
    }

    fun disconnectFromServer() {
        socket?.close()
    }


}
