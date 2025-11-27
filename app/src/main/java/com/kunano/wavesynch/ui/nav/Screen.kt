package com.kunano.wavesynch.ui.nav
sealed class Screen(val route: String){
    data object MainScreen: Screen("main_screen")
    data object ActiveRoomScreen: Screen("active_room_screen")
    data object CurrentRoomScreen: Screen("current_room_screen") //Room to which the guest in joined

}
