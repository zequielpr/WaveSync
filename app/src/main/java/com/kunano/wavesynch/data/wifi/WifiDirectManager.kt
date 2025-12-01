package com.kunano.wavesynch.data.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import com.kunano.wavesynch.data.stream.AudioStreamConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.coroutines.resume

// data/wifi/WifiDirectManager.kt
@SuppressLint("MissingPermission")
class WifiDirectManager(
    private val context: Context
) {

    private val manager: WifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: WifiP2pManager.Channel =
        manager.initialize(context, context.mainLooper, null)

    private var currentInfo: WifiP2pInfo? = null
    private var groupOwnerAddress: InetAddress? = null

    private val _log = MutableSharedFlow<String>(extraBufferCapacity = 20)
    val log: SharedFlow<String> = _log

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo =
                        intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        manager.requestConnectionInfo(channel) { info ->
                            currentInfo = info
                            groupOwnerAddress = info.groupOwnerAddress
                            _log.tryEmit("Connected. GO=${info.groupOwnerAddress.hostAddress}")
                        }
                    }
                }
            }
        }
    }

    init {
        context.registerReceiver(receiver, intentFilter)
    }

    fun unregister() {
        context.unregisterReceiver(receiver)
    }

    suspend fun createGroupAsHost(): Result<Unit> = suspendCancellableCoroutine { cont ->
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _log.tryEmit("Group created, waiting for client…")
                cont.resume(Result.success(Unit))
            }

            override fun onFailure(reason: Int) {
                cont.resume(Result.failure(IllegalStateException("createGroup failed: $reason")))
            }
        })
    }

    suspend fun connectToExistingGroup(device: WifiP2pDevice): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val config = WifiP2pConfig().apply {
                deviceAddress = device.deviceAddress
                wps.setup = WpsInfo.PBC
            }
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    _log.tryEmit("Connecting to ${device.deviceName}")
                    cont.resume(Result.success(Unit))
                }

                override fun onFailure(reason: Int) {
                    cont.resume(Result.failure(IllegalStateException("connect failed: $reason")))
                }
            })
        }

    fun isGroupOwner(): Boolean = currentInfo?.isGroupOwner == true

    fun getGroupOwnerAddress(): String? = groupOwnerAddress?.hostAddress

    // Sockets
    suspend fun openServerSocket(): ServerSocket = withContext(Dispatchers.IO) {
        ServerSocket(AudioStreamConstants.PORT).also {
            _log.emit("ServerSocket listening on ${it.localPort}")
        }
    }

    suspend fun waitForClient(server: ServerSocket): Socket = withContext(Dispatchers.IO) {
        server.accept().also {
            _log.emit("Client connected: ${it.inetAddress.hostAddress}")
        }
    }

    suspend fun connectToHost(hostIp: String): Socket = withContext(Dispatchers.IO) {
        Socket().apply {
            connect(InetSocketAddress(hostIp, AudioStreamConstants.PORT), 10_000)
            _log.emit("Connected to host $hostIp")
        }
    }

    suspend fun removeGroup() {
        suspendCancellableCoroutine<Unit> { cont ->
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    _log.tryEmit("Group removed")
                    cont.resume(Unit)
                }

                override fun onFailure(reason: Int) {
                    cont.resume(Unit)
                }
            })
        }
    }
}
