package com.kunano.wavesynch.data.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiController(private val context: Context) {
    private var wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnectedToWifiFlow = MutableStateFlow(false)
    val isConnectedToWifiFlow: StateFlow<Boolean> = _isConnectedToWifiFlow.asStateFlow()

    private val _hostIpFlow = MutableStateFlow<String?>(null)
    val hostIpFlow: StateFlow<String?> = _hostIpFlow.asStateFlow()

    init {
        registerWifiCallback()
    }

    private fun registerWifiCallback(): ConnectivityManager.NetworkCallback {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _hostIpFlow.tryEmit(getWifiIpAddress())
                _isConnectedToWifiFlow.tryEmit(true)
            }

            override fun onLost(network: Network) {
                _isConnectedToWifiFlow.tryEmit(false)
                _hostIpFlow.tryEmit(null)
            }

            override fun onUnavailable() {
                _isConnectedToWifiFlow.tryEmit(false)
                _hostIpFlow.tryEmit(null)
            }
        }

        connectivityManager.registerNetworkCallback(request, callback)
        // Also check current state
        val currentNetwork = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (currentNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            _isConnectedToWifiFlow.tryEmit(true)
            _hostIpFlow.tryEmit(getWifiIpAddress())
        }
        
        return callback
    }

    fun unregisterCallback(callback: ConnectivityManager.NetworkCallback) {
        connectivityManager.unregisterNetworkCallback(callback)
    }

    @SuppressLint("DefaultLocale")
    private fun getWifiIpAddress(): String? {
        val ip = wifiManager.connectionInfo.ipAddress
        if (ip == 0) return null

        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }
}
