package com.kunano.wavesynch.ui.guest.join_room

import android.net.wifi.p2p.WifiP2pDevice

data class JoinRoomUiState (
    val isThisDeviceHost: Boolean = false,
    val isConnectingToHotspot: Boolean = false,
    val waitingForHostAnswer: Boolean = false,
    val showCancelRequestDialog: Boolean = false

)