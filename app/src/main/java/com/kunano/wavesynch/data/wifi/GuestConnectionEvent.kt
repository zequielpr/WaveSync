package com.kunano.wavesynch.data.wifi

import java.net.Socket

sealed class GuestConnectionEvent {
    data class Connected(val handshake: HandShake) : GuestConnectionEvent()
    data class HostHandshakeReceived(val handshake: HandShake) : GuestConnectionEvent()
    data class SocketReady(val socket: Socket) : GuestConnectionEvent()
}