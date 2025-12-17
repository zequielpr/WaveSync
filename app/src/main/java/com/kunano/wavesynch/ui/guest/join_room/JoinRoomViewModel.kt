package com.kunano.wavesynch.ui.guest.join_room

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.wifi.client.ClientConnectionsState
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.domain.usecase.GuestUseCases
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.utils.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    private val guestUseCases: GuestUseCases,
    @ApplicationContext private val app: Context
) : ViewModel() {

    private val _UiState = MutableStateFlow(JoinRoomUiState())
    val uiState: StateFlow<JoinRoomUiState> = _UiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()


    init {
        collectHandShakeResults()
        collectConnectionEvents()
    }

    private fun collectHandShakeResults() {
        viewModelScope.launch {
            guestUseCases.hanShakeResponse.collect {
                Log.d("JoinRoomViewModel", "collectHandShakeResults: $it")
                when (it) {
                    is HandShakeResult.Success -> {
                        _uiEvent.send(UiEvent.NavigateTo(Screen.CurrentRoomScreen))
                        //_uiEvent.trySend(UiEvent.ShowSnackBar(app.getString(R.string.success_joining_room)))

                    }

                    is HandShakeResult.DeclinedByHost -> {
                        _uiEvent.trySend(UiEvent.ShowSnackBar(app.getString(R.string.request_declined)))
                        Log.d("JoinRoomViewModel", "collectHandShakeResults: declined by host")
                    }
                    is HandShakeResult.Error -> TODO()
                    is HandShakeResult.HostApprovalRequired -> TODO()
                    is HandShakeResult.InvalidAppId -> TODO()
                    is HandShakeResult.InvalidHandshake -> TODO()
                    is HandShakeResult.InvalidProtocol -> TODO()
                    is HandShakeResult.InvalidUserId -> TODO()
                    HandShakeResult.None -> TODO()
                }
            }

        }
    }


    fun connectToHotspot(password: String, ssid: String) {
        viewModelScope.launch {
            guestUseCases.connectToHotspot(password, ssid)
        }
    }
    private fun connectToServer(){
        guestUseCases.connectToServer()
    }

    fun collectConnectionEvents() {
        viewModelScope.launch {
            guestUseCases.connectionEvents.collect {
                when (it) {
                    ClientConnectionsState.ConnectedToHotspot -> {
                        connectToServer()
                        Log.d("JoinRoomViewModel", "collectConnectionEvents: connected to hotspot")}
                    ClientConnectionsState.ConnectedToServer -> {
                        guestUseCases.startReceivingAudioStream()
                        Log.d("JoinRoomViewModel", "collectConnectionEvents: connected to server")}
                    ClientConnectionsState.ConnectingToHotspot -> {
                        Log.d("JoinRoomViewModel", "collectConnectionEvents: connecting to hotspot")}
                    ClientConnectionsState.ConnectingToServer -> {Log.d("JoinRoomViewModel", "collectConnectionEvents: connecting to server")}
                    ClientConnectionsState.Disconnected -> {Log.d("JoinRoomViewModel", "collectConnectionEvents: disconnected")}
                    ClientConnectionsState.Idle -> {Log.d("JoinRoomViewModel", "collectConnectionEvents: idle")}
                    ClientConnectionsState.ReceivingAudioStream -> {Log.d("JoinRoomViewModel", "collectConnectionEvents: receiving audio stream")}
                }

            }
        }
    }



    fun leaveRoom() {
        guestUseCases.leaveRoom()
    }


}






