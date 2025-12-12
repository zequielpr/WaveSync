package com.kunano.wavesynch.data.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.AppIdProvider
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.resume

class WifiP2pGuestManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    val socket = Socket()
    private val _handShakeResponseFlow = MutableSharedFlow<HandShakeResult>(extraBufferCapacity = 20)
    val handShakeResponse: SharedFlow<HandShakeResult> = _handShakeResponseFlow.asSharedFlow()


    companion object {
        private const val TAG = "WifiP2pGuest"
    }

    private val manager: WifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager

    private val channel: WifiP2pManager.Channel =
        manager.initialize(context, context.mainLooper, null)

    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peers: StateFlow<List<WifiP2pDevice>> = _peers.asStateFlow()

    private val _connectionEvents = MutableSharedFlow<GuestConnectionEvent>()
    val connectionEvents: SharedFlow<GuestConnectionEvent> = _connectionEvents.asSharedFlow()


    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
    }

    private val receiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    requestPeers()
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo =
                        intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        manager.requestConnectionInfo(channel) { info ->
                            Log.d(TAG, "Connection info: ${info.groupOwnerAddress.hostAddress}")
                            requestConnectionToRoomServer(info)
                        }
                    } else if (networkInfo?.state == NetworkInfo.State.DISCONNECTED) {
                        Log.d(TAG, "Disconnected from host")
                    }
                }

                else -> Log.d(TAG, "Unknown action: ${intent.action}")
            }
        }
    }

    init {
        context.registerReceiver(receiver, intentFilter)
    }

    @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.NEARBY_WIFI_DEVICES])
    suspend fun WifiP2pManager.isAlreadyConnectedTo(
        channel: WifiP2pManager.Channel,
        target: WifiP2pDevice,
    ): Boolean = suspendCancellableCoroutine { cont ->
        requestGroupInfo(channel) { group ->
            val result = if (group == null) {
                false // no group -> no connection
            } else {
                if (group.isGroupOwner) {
                    // We are the host. Check if target is one of our clients.
                    group.clientList.any {
                        it.deviceAddress.equals(
                            target.deviceAddress,
                            ignoreCase = true
                        )
                    }
                } else {
                    // We are a client. Check if our host is the target device.
                    group.owner?.deviceAddress?.equals(
                        target.deviceAddress,
                        ignoreCase = true
                    ) == true
                }
            }
            if (cont.isActive) cont.resume(result)
        }
    }


    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // already unregistered
        }
    }

    /** Expose peers to UI */
    var currentPeers: List<WifiP2pDevice> = emptyList()
        private set

    /** Call this when user taps "Join session" */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun startDiscovery(onResult: (Boolean) -> Unit = {}) {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Peer discovery started")
                onResult(true)
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Peer discovery failed: $reason")
                onResult(false)
            }
        })
    }

    /** Called from BroadcastReceiver when WIFI_P2P_PEERS_CHANGED_ACTION fires */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private fun requestPeers() {
        manager.requestPeers(channel) { list: WifiP2pDeviceList ->
            val peerList = list.deviceList.toList()
            currentPeers = peerList
            _peers.value = peerList
            Log.d(TAG, "Peers updated: ${peerList.size}")
        }
    }

    /** UI chooses which device to connect to */
    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    suspend fun connectTo(device: WifiP2pDevice): Result<Unit> =
        suspendCancellableCoroutine { cont ->


            CoroutineScope(Dispatchers.IO).launch {
                val alreadyConnected = manager.isAlreadyConnectedTo(channel, device)
                if (alreadyConnected) {
                    Log.d(TAG, "Already connected to ${device.deviceName}")
                    if (cont.isActive) cont.resume(Result.success(Unit), null)
                    deviceAlreadyConnected()
                    return@launch
                }
            }


            val config = WifiP2pConfig().apply {
                deviceAddress = device.deviceAddress
                wps.setup = WpsInfo.PBC
            }


            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
                override fun onSuccess() {
                    Log.d(TAG, "Connected to ${device.deviceName}")
                    if (cont.isActive) {
                        cont.resume(Result.success(Unit))
                    }
                }

                override fun onFailure(reason: Int) {
                    Log.e(TAG, "Connect failed: $reason")
                    if (cont.isActive) {
                        val reasonText = when (reason) {
                            WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
                            WifiP2pManager.BUSY -> "BUSY"
                            WifiP2pManager.ERROR -> "ERROR"
                            else -> "UNKNOWN($reason)"
                        }
                        cont.resume(Result.failure(Exception("Connect failed: $reasonText")))
                    }
                }
            })
        }

    //Send request to join room
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun deviceAlreadyConnected() {
        manager.requestConnectionInfo(channel, object : WifiP2pManager.ConnectionInfoListener {
            override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
                requestConnectionToRoomServer(info)
            }

        })
    }

    /**
     * Called from BroadcastReceiver when WIFI_P2P_CONNECTION_CHANGED_ACTION fires.
     * If group is formed & this device is not owner → we are the guest.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun requestConnectionToRoomServer(info: WifiP2pInfo) {

        if (info.groupFormed && !info.isGroupOwner) {
            val hostAddress = info.groupOwnerAddress.hostAddress
            Log.d(TAG, "We are GUEST. Host IP: $hostAddress")
            hostAddress?.let {
                connectToServer(it)
            }
        } else {
            Log.d(TAG, "Not a guest or group not formed yet")
        }
    }

    /** Open TCP socket to host and do handshake */
    private fun connectToServer(hostIp: String) {
        scope.launch(Dispatchers.IO) {
            runCatching {

                socket.connect(InetSocketAddress(hostIp, AudioStreamConstants.PORT), 5000)

                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))


                // 1) Send handshake to host
                val myHandshake = HandShake(
                    appIdentifier = AppIdProvider.APP_ID,
                    userId = AppIdProvider.getUserId(context),
                    deviceName = android.os.Build.MODEL,
                    protocolVersion = 1
                )
                val myJson = serializeHandshake(myHandshake)
                output.write(myJson)
                output.newLine()
                output.flush()
                Log.d(TAG, "Sent handshake: $myJson")

                _connectionEvents.emit(GuestConnectionEvent.Connected(myHandshake))

                // 2) Receive host handshake
                val hostJson = input.readLine()
                Log.d(TAG, "Received host handshake: $hostJson")
                val hostHandshake = parseHandshake(hostJson)
                receiveResponseFromHost(hostHandshake)

            }.onFailure { e ->
                Log.e(TAG, "Error connecting to server", e)
            }
        }
    }


    private fun receiveResponseFromHost(handShake: HandShake) {

        handShake.response?.let {

            when (handShake.response) {
                HandShakeResult.DeclinedByHost().intValue -> _handShakeResponseFlow.tryEmit(HandShakeResult.DeclinedByHost())
                HandShakeResult.Success().intValue -> _handShakeResponseFlow.tryEmit(HandShakeResult.Success())
                HandShakeResult.HostApprovalRequired().intValue -> _handShakeResponseFlow.tryEmit(HandShakeResult.HostApprovalRequired())

            }


        }
    }

}
