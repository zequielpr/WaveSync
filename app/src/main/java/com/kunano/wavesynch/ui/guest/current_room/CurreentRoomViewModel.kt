package com.kunano.wavesynch.ui.guest.current_room

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.wifi.client.ClientConnectionsState
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentRoomViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val guestUseCases: GuestUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrentRoomUIState())
    val uiState: StateFlow<CurrentRoomUIState> = _uiState.asStateFlow()

    private val _uIEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uIEvents.asSharedFlow()


    init {
        guestUseCases.startReceivingAudioStream()
        populateCurrentRoom()
        collectConnectionEvents()
        collectIsPlayingState()

    }

    private fun collectIsPlayingState() {
        viewModelScope.launch {
            guestUseCases.isPlayingState.collect {

                setReceivingAudio(it)
            }

        }
    }


    private fun populateCurrentRoom() {
        val sessionInfo = guestUseCases.getSessionInfo()
        _uiState.update {
            it.copy(
                hostName = sessionInfo?.hostName,
                roomName = sessionInfo?.roomName
            )
        }

    }

    fun pauseAudio() {
        guestUseCases.pauseAudio()

    }

    fun resumeAudio() {
        guestUseCases.resumeAudio()
    }

    fun setReceivingAudio(status: Boolean) {
        _uiState.update { it.copy(isReceivingAudio = status) }
    }


    fun leaveRoom() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = guestUseCases.leaveRoom()
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (result) {
                _uIEvents.emit(UiEvent.ShowSnackBar(context.getString(R.string.room_left_successfully)))
                _uIEvents.emit(UiEvent.NavigateTo(Screen.MainScreen))
            } else {
                _uIEvents.emit(UiEvent.ShowSnackBar(context.getString(R.string.error_leaving_room)))
            }
            Log.d("CurrentRoomViewModel", "leaveRoom: $result")

        }
    }

    private fun collectConnectionEvents() {
        viewModelScope.launch {
            guestUseCases.connectionEvents.collect {
                when (it) {
                    ClientConnectionsState.ConnectedToHotspot -> {}
                    ClientConnectionsState.ConnectedToServer -> {}
                    ClientConnectionsState.ConnectingToHotspot -> {}
                    ClientConnectionsState.ConnectingToServer -> {}
                    ClientConnectionsState.Disconnected -> {}
                    ClientConnectionsState.Idle -> {}
                    ClientConnectionsState.ReceivingAudioStream -> {}
                }
            }
        }
    }


}


data class CurrentRoomUIState(
    val hostName: String? = null,
    val roomName: String? = null,
    val isLoading: Boolean = false,
    var isReceivingAudio: Boolean = false,
)

