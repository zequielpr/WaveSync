package com.kunano.wavesynch.data.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kunano.wavesynch.CrashReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiController(private val context: Context) {
    private var wifiManager: WifiManager? = null
    private var connectivityManager: ConnectivityManager? = null


    private val _isConnectedToWifiFlow = MutableStateFlow(false)
    val isConnectedToWifiFlow: StateFlow<Boolean> = _isConnectedToWifiFlow.asStateFlow()

    private val _hostIpFlow = MutableStateFlow<String?>(null)
    val hostIpFlow: StateFlow<String?> = _hostIpFlow.asStateFlow()

    init {
        try {
            wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            registerWifiCallback()
        } catch (e: IllegalStateException) {
            Log.e("WifiController", "Failed to get system service", e)
            FirebaseCrashlytics.getInstance().setCustomKey("operation_tag", "get_system_service")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun registerWifiCallback(): ConnectivityManager.NetworkCallback? {
        try {
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

            connectivityManager?.registerNetworkCallback(request, callback)
            // Also check current state
            val currentNetwork = connectivityManager?.getNetworkCapabilities(connectivityManager?.activeNetwork)
            if (currentNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                _isConnectedToWifiFlow.tryEmit(true)
                _hostIpFlow.tryEmit(getWifiIpAddress())
            }

            return callback
        } catch (e: SecurityException) {
            Log.e("WifiController", "Permission missing for network callback", e)
            CrashReporter.set("operation_tag", "register_wifi_callback")
            CrashReporter.log("Permission missing for network callback")
            CrashReporter.record(e)
            return null
        }
    }

    fun unregisterCallback(callback: ConnectivityManager.NetworkCallback) {
        try {
            connectivityManager?.unregisterNetworkCallback(callback)
        } catch (e: IllegalArgumentException) {
            Log.e("WifiController", "Callback not registered", e)
            CrashReporter.set("operation_tag", "register_wifi_callback")
            CrashReporter.log("Permission missing for network callback")
            CrashReporter.record(e)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getWifiIpAddress(): String? {
        try {
            val ip = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ip == 0) return null

            return String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )
        } catch (e: SecurityException) {
            Log.e("WifiController", "Permission missing for connection info", e)
            CrashReporter.set("operation_tag", "get_connection_info")
            CrashReporter.log("Permission missing for connection info")
            CrashReporter.record(e)
            return null
        }
    }
}