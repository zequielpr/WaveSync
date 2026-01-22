package com.kunano.wavesynch.ui.guest.join_room

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.wifi.client.ServerConnectionState
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.domain.usecase.GuestUseCases
import com.kunano.wavesynch.domain.usecase.host.HostUseCases
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.utils.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    private val guestUseCases: GuestUseCases,
    @ApplicationContext private val app: Context,
    private val hostUseCases: HostUseCases,
) : ViewModel() {

    private val _UiState = MutableStateFlow(JoinRoomUiState())
    val uiState: StateFlow<JoinRoomUiState> = _UiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()


    init {
        collectHandShakeResults()
        collectServerConnectionEvents()

    }

    fun cancelJoinRoomRequest() {
        viewModelScope.launch {
            val result = guestUseCases.cancelJoinRoomRequest()
            updateWaitingState(!result)
            setShowCancelRequestDialog(false)
        }
    }

    fun setShowCancelRequestDialog(show: Boolean) {
        _UiState.update { currentState ->
            currentState.copy(showCancelRequestDialog = show)
        }
    }


    private fun updateIsConnectingToHostState(state: Boolean) {
        _UiState.update { currentState ->
            currentState.copy(
                isConnectingToHotspot = state
            )
        }
    }

    private fun updateWaitingState(waiting: Boolean) {
        _UiState.update { currentState ->
            currentState.copy(
                waitingForHostAnswer = waiting
            )
        }
    }

    private fun collectHandShakeResults() {
        viewModelScope.launch {
            guestUseCases.hanShakeResponse.collect {
                Log.d("JoinRoomViewModel", "collectHandShakeResults: $it")
                when (it) {
                    is HandShakeResult.Success -> {
                        updateWaitingState(false)
                        Log.d("JoinRoomViewModel", "Handshake success ${it.handShake?.roomName}")
                        _uiEvent.send(UiEvent.NavigateTo(Screen.CurrentRoomScreen))
                        showSnackBar(app.getString(R.string.success_joining_room))

                    }

                    is HandShakeResult.DeclinedByHost -> {
                        updateWaitingState(false)
                        showSnackBar(app.getString(R.string.request_declined))
                        Log.d("JoinRoomViewModel", "collectHandShakeResults: declined by host")
                    }

                    is HandShakeResult.RoomFull -> {
                        updateWaitingState(false)
                        showSnackBar(app.getString(R.string.room_full))

                    }

                    is HandShakeResult.Error -> {}

                    else -> {}
                }
            }

        }
    }

    fun showSnackBar(message: String) {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.ShowSnackBar(message))

        }
    }


    fun finishSessionAsHost() {
        hostUseCases.finishSessionAsHost()
    }

    fun connectToHostOverLocalWifi(ip: String) {
        updateWaitingState(true)
        guestUseCases.connectToHostOverLocalWifi(ip)

    }


    fun collectServerConnectionEvents() {
        viewModelScope.launch {
            guestUseCases.serverConnectionEvents.collect {
                Log.d("JoinRoomViewModel", "collectConnectionEvents: $it")
                when (it) {
                    is ServerConnectionState.ConnectedToServer -> {
                        Log.d("JoinRoomViewModel", "collectConnectionEvents: connected to server")
                    }

                    is ServerConnectionState.ConnectingToServer -> {
                        Log.d("JoinRoomViewModel", "collectConnectionEvents: connecting to server")
                    }

                    is ServerConnectionState.Disconnected -> {
                        Log.d("JoinRoomViewModel", "collectConnectionEvents: disconnected")
                    }

                    is ServerConnectionState.Idle -> {
                        Log.d("JoinRoomViewModel", "collectConnectionEvents: idle")
                    }

                    is ServerConnectionState.ReceivingAudioStream -> {
                        Log.d(
                            "JoinRoomViewModel",
                            "collectConnectionEvents: receiving audio stream"
                        )
                    }

                    ServerConnectionState.ConnectionLost -> {
                        Log.d("JoinRoomViewModel", "collectConnectionEvents: connection lost")
                    }
                }

            }
        }
    }


}






