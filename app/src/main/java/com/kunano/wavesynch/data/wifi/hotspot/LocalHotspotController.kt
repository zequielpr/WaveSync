package com.kunano.wavesynch.data.wifi.hotspot

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class LocalHotspotController @Inject constructor(
    private val wifiManager: WifiManager,
    private val context: Context, // Ideally inject Context or ConnectivityManager directly
) {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _hotspotStateFlow = MutableStateFlow<HotspotState>(HotspotState.Idle)
    val hotspotStateFlow = _hotspotStateFlow.asStateFlow()

    private val _hotspotInfoFLow = MutableStateFlow<HotspotInfo?>(null)
    val hotspotInfoFLow = _hotspotInfoFLow.asStateFlow()

    private val _connectionStateFlow = MutableSharedFlow<HotSpotConnectionState>(extraBufferCapacity = 1)
    val connectionStateFlow = _connectionStateFlow.asSharedFlow()



    var isConnectedToHotspotAsGuest: Boolean = false


    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null


    //I am gonna adda a generate password and the mobile device name as the ssid
    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun startHotspot(
        onStarted: (hotspotInfo: HotspotInfo) -> Unit,
        onError: (Int) -> Unit,
    ) {

        if (isHotspotRunning()) {
            val hotspotInfo = getHotspotInfo()
            if (hotspotInfo != null) {
                onStarted(hotspotInfo)
                _hotspotInfoFLow.tryEmit(hotspotInfo)
            }
            _hotspotStateFlow.tryEmit(HotspotState.Running)
            return
        }


        _hotspotStateFlow.tryEmit(HotspotState.Starting)
        wifiManager.startLocalOnlyHotspot(
            object : WifiManager.LocalOnlyHotspotCallback() {

                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                    reservation = res
                    val hotspotInfo = getHotspotInfo()
                    if (hotspotInfo != null) {
                        _hotspotStateFlow.tryEmit(HotspotState.Running)
                        _hotspotInfoFLow.tryEmit(hotspotInfo)
                        onStarted(hotspotInfo)
                    } else {
                        onError(ERROR_GENERIC)
                        _hotspotStateFlow.tryEmit(HotspotState.Stopped)
                    }
                }

                override fun onStopped() {
                    reservation = null
                    _hotspotStateFlow.tryEmit(HotspotState.Stopped)
                }

                override fun onFailed(reason: Int) {
                    onError(reason)
                }
            },
            Handler(Looper.getMainLooper())
        )
    }

    fun stopHotspot() {
        reservation?.close()
        reservation = null
        _hotspotStateFlow.tryEmit(HotspotState.Stopped)
        _hotspotInfoFLow.tryEmit(null)
    }

    fun isHotspotRunning(): Boolean {
        return reservation != null
    }


    fun getHotspotInfo(): HotspotInfo? {
        if (reservation == null) return null
        var ssid: String
        val pass: String

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val config = reservation!!.softApConfiguration
            ssid = config.wifiSsid.toString().replace("\"", "")
            pass = config.passphrase ?: ""
        } else {
            @Suppress("DEPRECATION")
            val config = reservation!!.wifiConfiguration
            if (config == null) {
                return null
            }
            ssid = config.SSID
            pass = config.preSharedKey
        }

        if (ssid.isNullOrEmpty() || pass.isNullOrEmpty()) {
            return null
        }
        ssid = "/$ssid"

        return HotspotInfo(ssid, pass)
    }

    var hotspotNetwork: Network? = null



    fun getGatewayInfo(): String? {
        val linkProperties = connectivityManager.getLinkProperties(hotspotNetwork)

        return linkProperties
            ?.routes
            ?.firstOrNull { it.hasGateway() }
            ?.gateway
            ?.hostAddress
    }

    var guestRequestCallback: ConnectivityManager.NetworkCallback? = null


    fun connectToHotspot(
        ssid: String,
        password: String,
        onConnected: () -> Unit,
        onFailed: () -> Unit,
    ) {
        _connectionStateFlow.tryEmit(HotSpotConnectionState.Connecting )
        val wifiSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiSpecifier)
            .build()

        guestRequestCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                hotspotNetwork = network
                // VERY IMPORTANT
                //It force the app onto the hotspot network
                connectivityManager.bindProcessToNetwork(network)
                isConnectedToHotspotAsGuest = true
                _connectionStateFlow.tryEmit(HotSpotConnectionState.Connected )
                onConnected()
            }

            override fun onUnavailable() {
                _connectionStateFlow.tryEmit(HotSpotConnectionState.ConnectionUnavailable )
                onFailed()
            }

            override fun onLost(network: Network) {
                hotspotNetwork = null
                isConnectedToHotspotAsGuest = false
                _connectionStateFlow.tryEmit(HotSpotConnectionState.ConnectionLost )
                onFailed()
                // Handle disconnect if you want
            }
        }



        guestRequestCallback?.let {
            connectivityManager.requestNetwork(networkRequest, it)
        }
    }

    fun setIsConnectedToHotspotAsGuest(state: Boolean) {
        isConnectedToHotspotAsGuest = state
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun disconnectFromHotspot(): Boolean {
        val target = hotspotNetwork ?: return true

        // 0) VERY IMPORTANT: release the network request that is holding Wi-Fi
        guestRequestCallback?.let { cb ->
            runCatching { connectivityManager.unregisterNetworkCallback(cb) }
            guestRequestCallback = null
        }

        // 1) unbind your process (good)
        connectivityManager.bindProcessToNetwork(null)

        // 2) wait until the OS reports the target network is lost
        val lost = withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val lossCb = object : ConnectivityManager.NetworkCallback() {
                    override fun onLost(network: Network) {
                        if (network == target && cont.isActive) cont.resume(true) {
                            _connectionStateFlow.tryEmit(HotSpotConnectionState.Disconnected)
                        }
                    }

                    override fun onUnavailable() {
                        if (cont.isActive) cont.resume(true) {}
                    }
                }

                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()

                connectivityManager.registerNetworkCallback(request, lossCb)

                fun finish(value: Boolean) {
                    runCatching { connectivityManager.unregisterNetworkCallback(lossCb) }
                    if (cont.isActive) cont.resume(value) {}
                }

                cont.invokeOnCancellation {
                    runCatching { connectivityManager.unregisterNetworkCallback(lossCb) }
                }

                // already gone?
                if (connectivityManager.activeNetwork != target) {
                    finish(true)
                }
            }
        } ?: false

        hotspotNetwork = null
        return lost
    }


}
