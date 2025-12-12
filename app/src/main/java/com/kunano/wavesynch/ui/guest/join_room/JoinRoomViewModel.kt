package com.kunano.wavesynch.ui.guest.join_room

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.wifi.GuestConnectionEvent
import com.kunano.wavesynch.data.wifi.HandShakeResult
import com.kunano.wavesynch.domain.repositories.GuestRepository
import com.kunano.wavesynch.domain.usecase.guest_host_connection_use_cases.ConnectToHostServerUseCase
import com.kunano.wavesynch.domain.usecase.guest_host_connection_use_cases.JoinRoomUseCase
import com.kunano.wavesynch.domain.usecase.guest_host_connection_use_cases.StartPeerDiscoveryUseCase
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
    private val startPeerDiscoveryUseCase: StartPeerDiscoveryUseCase,
    private val joinRoomUseCase: JoinRoomUseCase,
    private val guestRepository: GuestRepository,
    private val connectToHostServerUseCase: ConnectToHostServerUseCase,
    @ApplicationContext private val app: Context
) : ViewModel() {

    private val _UiState = MutableStateFlow(JoinRoomUiState())
    val uiState: StateFlow<JoinRoomUiState> = _UiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()


    init {
        startPeerDiscovery()
        collectHandShakeResults()
        collectConnectionEvents()
    }

    private fun collectHandShakeResults() {
        viewModelScope.launch {
            guestRepository.hanShakeResponse.collect {
                Log.d("JoinRoomViewModel", "collectHandShakeResults: $it")
                when (it) {
                    is HandShakeResult.Success -> {
                        _uiEvent.trySend(UiEvent.NavigateTo(Screen.CurrentRoomScreen))
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


    private fun startPeerDiscovery() {
        viewModelScope.launch {
            startPeerDiscoveryUseCase(onStarted = { started ->
                _UiState.value = _UiState.value.copy(discoveringPeers = started)
                if (started) {

                    collectAvailablePeersList()
                } else {
                    Log.d("JoinRoomViewModel", "startPeerDiscovery: failed")
                }
            })
        }
    }

    private fun collectAvailablePeersList() {
        viewModelScope.launch {
            startPeerDiscoveryUseCase.currentPeers.collect { peers ->
                _UiState.value = _UiState.value.copy(availableHostsList = peers)
                peers.forEach {
                    Log.d("JoinRoomViewModel", "collectPeersList: ${it.deviceName}")
                }
            }
        }
    }


    fun joinRoom(host: WifiP2pDevice) {
        viewModelScope.launch {
            val result = joinRoomUseCase(host)

            if (result.isSuccess) {
                Log.d("JoinRoomViewModel", "joinRoom: success")
                connectToHostServerUseCase(host)

            } else {

            }
        }
    }

    fun collectConnectionEvents() {
        viewModelScope.launch {
            connectToHostServerUseCase.connectionEvents.collect { event ->
                when (event) {
                    is GuestConnectionEvent.Connected -> {
                        Log.d("JoinRoomViewModel", "collectConnectionEvents: connected")

                    }

                    is GuestConnectionEvent.HostHandshakeReceived -> {
                        Log.d(
                            "JoinRoomViewModel",
                            "collectConnectionEvents: host handshake received"
                        )
                    }

                    is GuestConnectionEvent.SocketReady -> {
                        Log.d("JoinRoomViewModel", "collectConnectionEvents: socket ready")
                    }
                }

            }
        }
    }


    fun stopPeerDiscovery() {
        viewModelScope.launch {

        }
    }


    fun leaveRoom() {
        viewModelScope.launch {

        }
    }


}





