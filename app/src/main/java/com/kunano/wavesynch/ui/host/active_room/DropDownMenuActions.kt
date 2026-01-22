package com.kunano.wavesynch.ui.host.active_room

sealed interface DropDownMenuActions {
    data object EmptyRoom : DropDownMenuActions
    data object EditRoomName : DropDownMenuActions
    data object DeleteRoom : DropDownMenuActions
    data object GoToTrustedUsers : DropDownMenuActions
}


