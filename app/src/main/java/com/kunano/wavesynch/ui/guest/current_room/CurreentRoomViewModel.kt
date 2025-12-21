package com.kunano.wavesynch.ui.guest.current_room

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.domain.repositories.SessionRepository
import com.kunano.wavesynch.domain.usecase.GuestUseCases
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.utils.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentRoomViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val guestUseCases: GuestUseCases,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CurrentRoomUIState())
    val uiState: StateFlow<CurrentRoomUIState> = _uiState.asStateFlow()

    private val _uIEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uIEvents.asSharedFlow()


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

    fun navigateBack(){
        Log.d("CurrentRoomViewModel", "navigateBack: ")
        viewModelScope.launch {
            if (guestUseCases.isConnectedToHotspotAsGuest()) {
                _uIEvents.emit(UiEvent.NavigateTo(Screen.MainScreen))
            }else{
                _uIEvents.emit(UiEvent.NavigateBack(""))
            }

        }
    }


    fun leaveRoom() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = guestUseCases.leaveRoom()
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (result) {
                _uIEvents.emit(UiEvent.ShowSnackBar(context.getString(R.string.room_left_successfully  )))
                _uIEvents.emit(UiEvent.NavigateTo(Screen.MainScreen))
            } else {
                _uIEvents.emit(UiEvent.ShowSnackBar(context.getString(R.string.error_leaving_room)))
            }
            Log.d("CurrentRoomViewModel", "leaveRoom: $result")

        }
    }


}


data class CurrentRoomUIState(
    val hostName: String? = null,
    val roomName: String? = null,
    val playingTrackName: String? = "Giving it all",
    val isLoading: Boolean = false,
)

