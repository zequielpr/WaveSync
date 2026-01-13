package com.kunano.wavesynch.ui.guest.join_room

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.wifi.client.ServerConnectionState
import com.kunano.wavesynch.data.wifi.hotspot.HotSpotConnectionState
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
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
        collectHotspotState()
        collectHotspotConnectionState()

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

    private fun collectHotspotState() {
        viewModelScope.launch {
            hostUseCases.hotSpotStateFlow.collect {
                when (it) {

                    HotspotState.Running -> updateDeviceStatus(true)
                    HotspotState.Stopped -> updateDeviceStatus(false)
                    else -> {}
                }
            }
        }
    }

    private fun updateDeviceStatus(isHost: Boolean) {
        _UiState.update { currentState ->
            currentState.copy(
                isThisDeviceHost = isHost
            )
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


    fun connectToHotspot(password: String, ssid: String) {
        viewModelScope.launch {
            guestUseCases.connectToHotspot(password, ssid)
        }
    }

    private fun connectToServer() {
        updateWaitingState(true)
        guestUseCases.connectToServer()
    }


    private fun collectHotspotConnectionState() {
        viewModelScope.launch {
            guestUseCases.hotspotConnectionStates.collect {
                when (it) {
                    HotSpotConnectionState.Connected -> {
                        updateIsConnectingToHostState(false)
                        connectToServer()
                        Log.d("JoinRoomViewModel", "collectHotspotConnectionState: connected")
                    }

                    HotSpotConnectionState.Connecting -> {
                        updateIsConnectingToHostState(true)
                        Log.d("JoinRoomViewModel", "collectHotspotConnectionState: connecting")

                    }

                    HotSpotConnectionState.ConnectionLost -> {
                        updateIsConnectingToHostState(false)
                    }

                    HotSpotConnectionState.ConnectionUnavailable -> {
                        updateIsConnectingToHostState(false)
                        _uiEvent.send(UiEvent.ShowSnackBar(app.getString(R.string.connection_unavailable)))

                    }

                    HotSpotConnectionState.Disconnected -> {
                        updateIsConnectingToHostState(false)
                        Log.d("JoinRoomViewModel", "collectHotspotConnectionState: disconnected")
                    }
                }
            }

        }
    }


    fun collectServerConnectionEvents() {
        viewModelScope.launch {
            guestUseCases.serverConnectionEvents.collect {
                Log.d("JoinRoomViewModel", "collectConnectionEvents: $it")
                when (it) {
                    is ServerConnectionState.ConnectedToServer -> {
                        guestUseCases.startReceivingAudioStream()
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
                }

            }
        }
    }


}






