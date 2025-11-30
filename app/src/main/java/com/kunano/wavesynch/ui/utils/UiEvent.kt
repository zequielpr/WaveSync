package com.kunano.wavesynch.ui.utils

interface SnackBarListener {
    fun showSnackBar(message: String)
}

sealed class UiEvent {
    data class ShowSnackBar(val message: String) : UiEvent()
}