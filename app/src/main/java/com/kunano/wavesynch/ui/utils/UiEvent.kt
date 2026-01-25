package com.kunano.wavesynch.ui.utils

import com.kunano.wavesynch.ui.nav.Screen
import kotlinx.coroutines.CompletableDeferred

sealed class UiEvent {
    data class ShowSnackBar(val message: String) : UiEvent()
    data class ShowDialog(val title: String, val message: String) : UiEvent()
    data class NavigateBack(val route: String?) : UiEvent()
    data class NavigateTo(val screen: Screen) : UiEvent()
}





