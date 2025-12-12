package com.kunano.wavesynch.data.stream

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import java.net.Socket

data class GuestStreamingData (
    val id: String,
    val socket: Socket,
    val channel: Channel<ByteArray>,   // audio chunks to send
    val job: Job                       // coroutine that writes to socket
)