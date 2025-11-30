package com.kunano.wavesynch.ui.utils

sealed class UiEvent {
    data class ShowSnackBar(val message: String) : UiEvent()
    data class NavigateBack(val route: String?) : UiEvent()
    data class NavigateTo(val route: String): UiEvent()
}


