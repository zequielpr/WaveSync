package com.kunano.wavesynch.ui.host.active_room

import com.kunano.wavesynch.data.wifi.hotspot.HotspotInfo
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room

data class ActiveRoomUIState(
    val showJoinRoomRequest: Boolean = false,
    val room: Room? = null,
    val hotspotInfo: HotspotInfo? = null,
    val overFlowMenuExpanded: Boolean = false,
    val isQRCodeExpanded: Boolean = true,
    val guests: List<Guest> = emptyList(),
    val guestToBeExpelled: Guest? = null,
    val showAskToExpelGuest: Boolean = false,
    val isHostStreaming: Boolean = false,
    val showAskToStopStreaming: Boolean = false,
    val showAskToEmptyRoom: Boolean = false,
    val hostIp: String? = null,
    )
