package com.kunano.wavesynch.data.wifi.hotspot

sealed class HotspotState{
    data object Idle : HotspotState()
    data object Starting : HotspotState()
    data object Running : HotspotState()
    data object Stopping : HotspotState()
    data object Stopped : HotspotState()

}