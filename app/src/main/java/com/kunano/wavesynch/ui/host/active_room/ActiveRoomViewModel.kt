package com.kunano.wavesynch.ui.host.active_room

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.stream.HostAudioCapturer
import com.kunano.wavesynch.data.wifi.server.HandShake
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.RoomWithTrustedGuests
import com.kunano.wavesynch.domain.model.TrustedGuest
import com.kunano.wavesynch.domain.usecase.host.HostUseCases
import com.kunano.wavesynch.ui.utils.ActiveRoomUiEvent
import com.kunano.wavesynch.ui.utils.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveRoomViewModel @Inject constructor(
    private val hostUseCases: HostUseCases,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveRoomUIState())
    val uiState: StateFlow<ActiveRoomUIState> = _uiState.asStateFlow()

    val _serverState =  hostUseCases.serverStateFlow

    var askToTrustGuestEvent: ActiveRoomUiEvent.AskToAcceptGuestRequest? = null


    //UiEvents
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()


    init {
        retrieveRoom()
        collectServerStates()
        collectLogs()
        collectHandShakeResults()
        collectHotSpotState()


    }

    //HotSpotImplementation
    fun startLocalHotSpot() {
       if(!hostUseCases.isHotspotRunning()){
           hostUseCases.startHotspot(onStarted = { ssid, password ->
               Log.d("ActiveRoomViewModel", "startLocalHotSpot: $ssid $password")
               setHotSpotSsidAndPassword(ssid, password)
           }, onError = {})
       }
    }

    private fun setHotSpotSsidAndPassword(ssid: String, password: String) {
        _uiState.value = _uiState.value.copy(ssid = ssid, password = password)
    }



    private fun collectHandShakeResults() {
        viewModelScope.launch {
            hostUseCases.handShakeResultFlow.collect { answer ->
                when (answer) {
                    is HandShakeResult.Success -> {
                        answer.handShake?.let {
                            val guest = Guest(it.deviceName, it.userId, it.deviceName)
                            hostUseCases.acceptUserConnection(guest)
                        }
                        //startStreamingAsHostUseCase()
                        answer.handShake?.let {
                            hostUseCases.sendAnswerToGuest(it.userId, answer)
                        }

                    }

                    is HandShakeResult.HostApprovalRequired -> {
                        Log.d(
                            "ActiveRoomViewModel", "collectHandShakeResults: Host approval required"
                        )
                        answer.handShake?.let {
                            onRequestHostApproval(handShake = answer.handShake)
                        }
                    }

                    else -> {
                        Log.d("ActiveRoomViewModel", "collectHandShakeResults: $answer")
                        // Handle other cases like errors or invalid handshakes
                        // Maybe log them or show a message if needed
                    }
                }

            }
        }
    }



    fun onRequestHostApproval(handShake: HandShake) {
        viewModelScope.launch {
            val decision = CompletableDeferred<Boolean>()
            val guestTrusted = CompletableDeferred<Boolean>()
            _uiEvent.send(
                ActiveRoomUiEvent.AskToAcceptGuestRequest(
                    guestName = handShake.deviceName,
                    deviceName = handShake.deviceName,
                    decision = decision,
                    guestTrusted = guestTrusted
                )
            )


            val approved = decision.await()
            if (approved) {
                val guestTrusted = guestTrusted.await()
                if (guestTrusted) {
                    addTrustedGuest(handShake)
                }


                val guest = Guest(handShake.deviceName, handShake.userId, handShake.deviceName)
                hostUseCases.acceptUserConnection(guest)
                hostUseCases.sendAnswerToGuest(
                    handShake.userId, HandShakeResult.Success()
                )

            } else {
                handShake.response = HandShakeResult.DeclinedByHost().intValue
                hostUseCases.sendAnswerToGuest(handShake.userId, HandShakeResult.DeclinedByHost())
            }
        }
    }




    private fun addTrustedGuest(handShake: HandShake) {
        viewModelScope.launch {
            val currentRoomId = _uiState.value.room?.id
            val trustedGuest = TrustedGuest(
                userId = handShake.userId,
                userName = handShake.deviceName,
                deviceName = handShake.deviceName
            )
            val result =  hostUseCases.createTrustedGuest(trustedGuest)


            if (result >= 1) {
                currentRoomId?.let {
                    val roomWithTrustedGuest =
                        RoomWithTrustedGuests(it, handShake.userId, isConnected = true)
                    val result =  hostUseCases.addTrustedGuest(roomWithTrustedGuest)

                    if (result >= 1) {
                        Log.d("ActiveRoomViewModel", "addTrustedGuest: Trusted guest added")
                    }

                }
            }
        }

    }




    private fun collectLogs() {
        viewModelScope.launch {
            hostUseCases.logFlow.collect {
                Log.d("ActiveRoomViewModel", "WifiDirectManager Logs: $it")

            }
        }
    }




    private fun retrieveRoom() {
        viewModelScope.launch {
            hostUseCases.observerRooms().catch {
                it.printStackTrace()
            }.catch { throwable ->
                throwable.printStackTrace()
            }.collect {
                if (it.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(room = it[0])
                    //Only host room if not already running/hosted to avoid re-hosting on config changes/updates
                    //Or you can rely on the repository to handle idempotency
                    startLocalHotSpot()
                    collectRoomGuests(it[0].id)
                }


            }
        }
    }


    private fun collectRoomGuests(romId: Long?) {
        viewModelScope.launch {
            romId?.let {
                hostUseCases.connectedGuest.collect {
                    Log.d("ActiveRoomViewModel", "collectRoomGuests: $it")
                    if (it != null) {
                        Log.d("ActiveRoomViewModel", "collectRoomGuests: $it")
                        _uiState.value = _uiState.value.copy(guests = it.toList())
                    }
                }
            }

        }
    }

    fun emptyRoom(roomId: Long) {

    }

    fun deleteRoom(roomId: Long) {
        viewModelScope.launch {
            val result = hostUseCases.deleteRoom(roomId)
            if (result >= 1) {
                _uiEvent.send(UiEvent.ShowSnackBar(appContext.getString(R.string.room_deleted)))
                _uiEvent.send(UiEvent.NavigateBack(null)) //Navigate back to main screen
            } else {
                _uiEvent.send(UiEvent.ShowSnackBar(appContext.getString(R.string.error_deleting_room)))
            }
        }
    }


    fun editRoomName(roomId: Long, newName: String) {


        if (newName.isNotEmpty()) {
            viewModelScope.launch {
                val result = hostUseCases.editRoomName(
                    roomId = roomId, newName = newName
                ).runCatching {

                }


                if (result.isSuccess) {
                    _uiEvent.send(
                        UiEvent.ShowSnackBar(
                            appContext.getString(R.string.room_name_updated)
                        )
                    )
                }
            }

        } else {
            _uiEvent.trySend(UiEvent.ShowSnackBar(appContext.getString(R.string.enter_new_name)))
        }


    }

    fun expelGuest(guestId: String) {

    }

    fun setShowAskTrustGuestState(state: Boolean) {
        _uiState.value = _uiState.value.copy(showJoinRoomRequest = state)
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


    //When the hotspot is activated, the server is started, otherwise the server is shutdown
    fun collectHotSpotState() {
        viewModelScope.launch {
            hostUseCases.hotSpotStateFlow.collect {
                when (it) {
                    HotspotState.Idle -> hostUseCases.stopServer()

                    HotspotState.Running -> {
                        hostUseCases.startServer(_uiState.value.room?.id)
                        Log.d("ActiveRoomViewModel", "collectHotSpotState: Running")
                    }

                    HotspotState.Starting -> {Log.d("ActiveRoomViewModel", "collectHotSpotState: Starting")}}
                }


        }
    }



    private fun collectServerStates() {
        viewModelScope.launch {
            _serverState.collect {
                when (it) {
                    ServerState.Starting -> {Log.d("ActiveRoomViewModel", "server state: Starting")}
                    ServerState.Running -> {
                        Log.d("ActiveRoomViewModel", "server state: Running")

                    }

                    is ServerState.Error -> {
                        Log.d("ActiveRoomViewModel", "server state: ${it.message}")
                    }

                    ServerState.Idle -> {Log.d("ActiveRoomViewModel", "server state: Idle")}
                    ServerState.Streaming -> {
                        Log.d("ActiveRoomViewModel", "server state: Streaming")
                    }
                }
            }
        }
    }


}