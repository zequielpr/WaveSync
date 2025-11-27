package com.kunano.wavesynch.ui.host.active_room

data class ActiveRoomUIState(
    val roomId: String? = null,
    val overFlowMenuExpanded: Boolean = false,
    val QRCode: String? = null,
    val roomName: String? = "room 1",
    val playingInHost: Boolean = true,
    val isQRCodeExpanded: Boolean = true,
    val guests: List<GuestInfo> = listOf(GuestInfo("Guest 1", "1"), GuestInfo("Guest 2", "2"), GuestInfo("Guest 3", "3"))

)

data class GuestInfo(
    var name: String,
    var id: String,
)
