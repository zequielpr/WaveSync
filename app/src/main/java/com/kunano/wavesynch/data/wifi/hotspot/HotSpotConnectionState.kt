package com.kunano.wavesynch.data.wifi.hotspot

sealed class HotSpotConnectionState {
    data object Connecting : HotSpotConnectionState()
    data object Connected : HotSpotConnectionState()
    data object Disconnected : HotSpotConnectionState()
    data object ConnectionUnavailable : HotSpotConnectionState()
    data object ConnectionLost : HotSpotConnectionState()
}
