package com.kunano.wavesynch.data.wifi.client

sealed class ServerConnectionState {
    object Idle : ServerConnectionState()
    object ConnectingToServer : ServerConnectionState()
    object ConnectedToServer : ServerConnectionState()
    object ReceivingAudioStream : ServerConnectionState()
    object ConnectionLost : ServerConnectionState()
    object Disconnected : ServerConnectionState()

}