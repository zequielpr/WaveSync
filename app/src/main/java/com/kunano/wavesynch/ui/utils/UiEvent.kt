package com.kunano.wavesynch.ui.utils

import com.kunano.wavesynch.ui.nav.Screen
import kotlinx.coroutines.CompletableDeferred

sealed class UiEvent {
    data class ShowSnackBar(val message: String) : UiEvent()
    data class NavigateBack(val route: String?) : UiEvent()
    data class NavigateTo(val screen: Screen) : UiEvent()
}

sealed class ActiveRoomUiEvent: UiEvent() {
    data class AskToAcceptGuestRequest(
        val guestName: String,
        val deviceName: String,
        val decision: CompletableDeferred<Boolean>,
        val guestTrusted: CompletableDeferred<Boolean>
    ) : ActiveRoomUiEvent()
    object StartAudioCapturer : ActiveRoomUiEvent()
}



sealed class JoinRoomUiEvent: UiEvent() {

}
