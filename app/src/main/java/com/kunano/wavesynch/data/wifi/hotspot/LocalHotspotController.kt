package com.kunano.wavesynch.data.wifi.hotspot

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
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
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class LocalHotspotController @Inject constructor(
    private val wifiManager: WifiManager,
    private val context: Context // Ideally inject Context or ConnectivityManager directly
) {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _hotspotStateFlow = MutableStateFlow<HotspotState>(HotspotState.Idle)
    val hotspotStateFlow = _hotspotStateFlow.asStateFlow()

    private val _hotspotInfoFLow = MutableStateFlow<HotspotInfo?>(null)
    val hotspotInfoFLow = _hotspotInfoFLow.asStateFlow()




    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null


    //I am gonna adda a generate password and the mobile device name as the ssid
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun startHotspot(
        onStarted: (hotspotInfo: HotspotInfo) -> Unit,
        onError: (Int) -> Unit,
    ) {

        if(isHotspotRunning()){
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


                @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                    reservation = res

                    val config = res.softApConfiguration
                    val ssid = config.wifiSsid.toString()
                    val pass = config.passphrase ?: ""
                    _hotspotInfoFLow.tryEmit(HotspotInfo(ssid, pass))

                    _hotspotStateFlow.tryEmit(HotspotState.Running)
                    onStarted(HotspotInfo(ssid, pass))
                }

                override fun onStopped() {
                    reservation = null
                    _hotspotStateFlow.tryEmit(HotspotState.Idle)
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
    }

    fun isHotspotRunning(): Boolean {
        return reservation != null
    }



    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getHotspotInfo(): HotspotInfo? {
        if (reservation == null) return null
        val config = reservation!!.softApConfiguration
        val ssid = config.wifiSsid.toString()
        val pass = config.passphrase ?: ""

        return HotspotInfo(ssid, pass)
    }

     var hotspotNetwork: Network? = null

    
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getGatewayInfo(): String? {
        val linkProperties = connectivityManager.getLinkProperties(hotspotNetwork)

        return linkProperties
            ?.routes
            ?.firstOrNull { it.hasGateway() }
            ?.gateway
            ?.hostAddress
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToHotspot(
        ssid: String,
        password: String,
        onConnected: () -> Unit,
        onFailed: () -> Unit
    ) {
        val wifiSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiSpecifier)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                hotspotNetwork = network
                // VERY IMPORTANT
                //It force the app onto the hotspot network
                connectivityManager.bindProcessToNetwork(network)
                onConnected()
            }

            override fun onUnavailable() {
                onFailed()
            }

            override fun onLost(network: Network) {
                // Handle disconnect if you want
            }
        }

        
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

}
