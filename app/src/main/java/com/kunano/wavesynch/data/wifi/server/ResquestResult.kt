package com.kunano.wavesynch.data.wifi.server

sealed class RequestResult {
    data object Declined : RequestResult()
    data object Cancelled : RequestResult()
    data class Error(val message: String) : RequestResult()
    data object Unknown : RequestResult()
    data object Timeout : RequestResult()
}