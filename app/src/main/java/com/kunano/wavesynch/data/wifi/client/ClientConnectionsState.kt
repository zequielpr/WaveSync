package com.kunano.wavesynch.data.wifi.client

sealed class ClientConnectionsState {
    object Idle : ClientConnectionsState()
    object ConnectingToHotspot : ClientConnectionsState()
    object ConnectedToHotspot : ClientConnectionsState()
    object ConnectingToServer : ClientConnectionsState()
    object ConnectedToServer : ClientConnectionsState()
    object ReceivingAudioStream : ClientConnectionsState()
    object Disconnected : ClientConnectionsState()

}