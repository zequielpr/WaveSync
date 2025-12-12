package com.kunano.wavesynch.ui.guest.current_room

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.domain.repositories.GuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentRoomViewModel @Inject constructor(private val guestRepository: GuestRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CurrentRoomUIState())
    val uiState: StateFlow<CurrentRoomUIState> = _uiState.asStateFlow()


    init {
        guestRepository.startReceivingAudioStream()
    }


    fun leaveRoom() {
        viewModelScope.launch {

        }
    }


}


data class CurrentRoomUIState(
    val hostName: String? = "Pixel 7 xxx",
    val roomName: String? = "Room 1",
    val roomId: String? = "123456789",
    val playingTrackName: String? = "Giving it all",
)

