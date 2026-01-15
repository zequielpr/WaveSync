package com.kunano.wavesynch.data.wifi

import android.content.Context
import android.net.wifi.WifiManager

fun getWifiIpAddress(context: Context): String? {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

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