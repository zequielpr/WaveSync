package com.kunano.wavesynch.ui.guest.join_room

import android.net.wifi.p2p.WifiP2pDevice

data class JoinRoomUiState (
    val discoveringPeers: Boolean = false,
    val availableHostsList: List<WifiP2pDevice>? = null
)