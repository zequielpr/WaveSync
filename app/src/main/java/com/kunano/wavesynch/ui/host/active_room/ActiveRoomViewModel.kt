package com.kunano.wavesynch.ui.host.active_room

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ActiveRoomViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveRoomUIState())
    val uiState: StateFlow<ActiveRoomUIState> = _uiState.asStateFlow()


    fun emptyRoom(roomId: String) {

    }

    fun deleteRoom(roomId: String) {

    }

    fun editRoomName(roomId: String) {

    }
    fun editRoomName(roomId: String, newName: String) {

    }

    fun expelGuest( guestId: String) {

    }




    fun setIsPlayingInHostState(state: Boolean) {
        _uiState.value = _uiState.value.copy(playingInHost = state)
    }

    fun setOverFlowMenuExpandedState(state: Boolean) {
        _uiState.value = _uiState.value.copy(overFlowMenuExpanded = state)
    }


    fun setIsQRCodeExpandedState(state: Boolean) {
        _uiState.value = _uiState.value.copy(isQRCodeExpanded = state)
    }



    fun setRoomName(): String {
        return "Room 1"
    }


    fun setQRCode() {

    }

    fun setGuestsList(): List<String> {
        return listOf("Guest 1", "Guest 2", "Guest 3")
    }


}