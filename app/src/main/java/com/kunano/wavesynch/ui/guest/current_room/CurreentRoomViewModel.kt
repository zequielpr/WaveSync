package com.kunano.wavesynch.ui.guest.current_room

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.domain.repositories.SessionRepository
import com.kunano.wavesynch.domain.usecase.GuestUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentRoomViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val guestUseCases: GuestUseCases,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CurrentRoomUIState())
    val uiState: StateFlow<CurrentRoomUIState> = _uiState.asStateFlow()


    init {
        guestUseCases.startReceivingAudioStream()
        populateCurrentRoom()
    }

    private fun populateCurrentRoom() {
        viewModelScope.launch {
            sessionRepository.session.collect {
                _uiState.value = _uiState.value.copy(roomName = it.roomName, hostName = it.hostName)
            }
        }

    }


    fun leaveRoom() {
        viewModelScope.launch {
            val result = guestUseCases.leaveRoom()
            Log.d("CurrentRoomViewModel", "leaveRoom: $result")

        }
    }


}


data class CurrentRoomUIState(
    val hostName: String? = null,
    val roomName: String? = null,
    val playingTrackName: String? = "Giving it all",
)

