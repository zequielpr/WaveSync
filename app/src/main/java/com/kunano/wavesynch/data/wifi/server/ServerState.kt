package com.kunano.wavesynch.data.wifi.server

sealed class ServerState {
    data object Idle : ServerState()
    data object Starting : ServerState()
    data object Running : ServerState()
    data object Streaming : ServerState()
    data class Error(val message: String) : ServerState()
}