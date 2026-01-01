package com.kunano.wavesynch.data.stream

import java.net.InetSocketAddress

data class GuestStreamingData (
    val id: String,
    val inetSocketAddress: InetSocketAddress,
    var isPlaying: Boolean = false
)