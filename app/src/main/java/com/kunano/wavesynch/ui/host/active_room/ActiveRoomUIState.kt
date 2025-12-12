package com.kunano.wavesynch.ui.host.active_room

import com.kunano.wavesynch.data.stream.AudioStreamConstants
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.model.TrustedGuest

data class ActiveRoomUIState(
    val showJoinRoomRequest: Boolean = false,
    val room: Room? = null,
    val hostIpAddress: String? = null,
    val port: Int = AudioStreamConstants.PORT,
    val overFlowMenuExpanded: Boolean = false,
    val playingInHost: Boolean = true,
    val isQRCodeExpanded: Boolean = true,
    val guests: List<Guest> = emptyList<Guest>(),

    )

data class GuestInfo(
    var name: String,
    var id: String,
)
