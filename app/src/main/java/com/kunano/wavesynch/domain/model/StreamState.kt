package com.kunano.wavesynch.domain.model

sealed class StreamState {
    data object Idle : StreamState()
    data object Connecting : StreamState()
    data object Connected : StreamState()
    data object Streaming : StreamState()
    data class Error(val message: String) : StreamState()
}
