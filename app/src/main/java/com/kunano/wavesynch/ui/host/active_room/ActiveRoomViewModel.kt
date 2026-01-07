package com.kunano.wavesynch.ui.host.active_room

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.wifi.hotspot.HotspotInfo
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
import com.kunano.wavesynch.data.wifi.server.HandShake
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.RoomWithTrustedGuests
import com.kunano.wavesynch.domain.model.TrustedGuest
import com.kunano.wavesynch.domain.usecase.GuestUseCases
import com.kunano.wavesynch.domain.usecase.host.HostUseCases
import com.kunano.wavesynch.services.AudioCaptureService
import com.kunano.wavesynch.services.StartHotspotService
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveRoomViewModel @Inject constructor(
    private val hostUseCases: HostUseCases,
    private val guestUseCases: GuestUseCases,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveRoomUIState())
    val uiState: StateFlow<ActiveRoomUIState> = _uiState.asStateFlow()

    val _serverState = hostUseCases.serverStateFlow

    //UiEvents
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()


    init {
        Log.d("ActiveRoomViewModel", "ActiveRoomViewModel init")
        retrieveRoom()
        collectServerStates()
        collectLogs()
        collectHandShakeResults()
        if (!checkIfDeviceIsGuest()) {
            startLocalHotSpot()
        }
        collectHotSpotInfo()
        collectHotSpotState()


    }


    fun checkIfDeviceIsGuest(): Boolean {
        return guestUseCases.isConnectedToHotspotAsGuest()
    }


    fun stopBeingAGuest() {
        viewModelScope.launch {
            val roomLeftSuccessfully = guestUseCases.leaveRoom()
            Log.d("ActiveRoomViewModel", "stopBeingAGuest: $roomLeftSuccessfully")
            if (roomLeftSuccessfully) {
                startLocalHotSpot()
            }
        }
    }


    //It start the hotspot service in the background
    fun startLocalHotSpot() {
        val intent = Intent(appContext, StartHotspotService::class.java)
        appContext.startForegroundService(intent)
    }


    private fun collectHotSpotInfo() {
        viewModelScope.launch {
            hostUseCases.hotspotInfoFlow.collect {

                if (it != null) {
                    Log.d("ActiveRoomViewModel", "collectHotSpotInfo: ${it.password}")
                    setHotSpotSsidAndPassword(it)
                }
            }

        }
    }

    private fun setHotSpotSsidAndPassword(hotspotInfo: HotspotInfo?) {
        _uiState.value = _uiState.value.copy(hotspotInfo = hotspotInfo)
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
                        answer.handShake?.let {
                            val roomName = uiState.value.room?.name
                            hostUseCases.sendAnswerToGuest(it.userId, roomName, answer)
                        }

                    }

                    is HandShakeResult.HostApprovalRequired -> {
                        answer.handShake?.copy(roomName = uiState.value.room?.name)

                        answer.handShake?.let {
                            onRequestHostApproval(handShake = answer.handShake)
                            Log.d(
                                "ActiveRoomViewModel",
                                "collectHandShakeResults: Host approval required"
                            )
                        }
                    }

                    is HandShakeResult.UdpSocketOpen -> {
                        Log.d("ActiveRoomViewModel", "collectHandShakeResults: Udp socket open")
                        //It will ad the client to the udp broadcasting
                        answer.handShake?.userId?.let {
                            hostUseCases.addGuestToHostStreamer(it)
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
                val roomName = uiState.value.room?.name
                hostUseCases.sendAnswerToGuest(
                    handShake.userId, roomName, HandShakeResult.Success()
                )

            } else {
                handShake.response = HandShakeResult.DeclinedByHost().intValue
                hostUseCases.sendAnswerToGuest(
                    guestId = handShake.userId,
                    answer = HandShakeResult.DeclinedByHost()
                )
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
            val result = hostUseCases.createTrustedGuest(trustedGuest)


            if (result >= 1) {
                currentRoomId?.let {
                    val roomWithTrustedGuest =
                        RoomWithTrustedGuests(it, handShake.userId)
                    val result = hostUseCases.addTrustedGuest(roomWithTrustedGuest)

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
                    collectRoomGuests(it[0].id)
                }


            }
        }
    }


    private fun collectRoomGuests(romId: Long?) {
        viewModelScope.launch {
            romId?.let {
                hostUseCases.connectedGuest.collect { it ->
                    Log.d("ActiveRoomViewModel", "collectRoomGuests: $it")
                    if (it != null) {
                        _uiState.update { uIState ->
                            uIState.copy(guests = it.toList())
                        }
                    }
                }

            }
        }

    }
    fun setShowAskToEmptyRoom(show: Boolean){
        _uiState.update { uIState ->
            uIState.copy(showAskToEmptyRoom = show)
        }
    }

    fun emptyRoom() {
        viewModelScope.launch {
            hostUseCases.emptyRoom()
            _uiEvent.send(UiEvent.ShowSnackBar(appContext.getString(R.string.room_emptied)))
        }
    }

    fun deleteRoom(roomId: Long) {
        viewModelScope.launch {

            val result = hostUseCases.deleteRoom(roomId)
            if (result >= 1) {
                hostUseCases.finishSessionAsHost()
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


    fun setShowAskToExpelGuestState(state: Boolean, guest: Guest?) {
        _uiState.update { uIState ->
            uIState.copy(showAskToExpelGuest = state)
        }

        _uiState.update { uIState ->
            uIState.copy(guestToBeExpelled = guest)
        }
    }

    fun expelGuest() {
        val guestToExpel = _uiState.value.guestToBeExpelled
        guestToExpel?.let {
            hostUseCases.expelGuest(it.userId)
        }

        setShowAskToExpelGuestState(false, null)
        _uiEvent.trySend(UiEvent.ShowSnackBar(appContext.getString(R.string.guest_expelled)))
    }

    fun playGuest(guestId: String) {
        hostUseCases.playGuest(guestId)
    }

    fun pauseGuest(guestId: String) {
        hostUseCases.pauseGuest(guestId)
    }





    fun setShowAskTrustGuestState(state: Boolean) {
        _uiState.update { uIState ->
            uIState.copy(showJoinRoomRequest = state)
        }
    }

    fun setOverFlowMenuExpandedState(state: Boolean) {
        _uiState.update { uIState ->
            uIState.copy(overFlowMenuExpanded = state)
        }
    }


    fun setIsQRCodeExpandedState(state: Boolean) {
        _uiState.update { uIState ->
            uIState.copy(isQRCodeExpanded = state)
        }
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
            hostUseCases.hotSpotStateFlow.collect { it ->
                when (it) {
                    HotspotState.Idle -> hostUseCases.stopServer()

                    HotspotState.Running -> {
                        _uiState.value.room?.let { romId ->
                            hostUseCases.startServer(romId)
                        }

                        Log.d("ActiveRoomViewModel", "collectHotSpotState: Running")
                    }

                    HotspotState.Starting -> {
                        Log.d("ActiveRoomViewModel", "collectHotSpotState: Starting")
                    }

                    HotspotState.Stopped -> {}
                    HotspotState.Stopping -> {}
                }
            }


        }
    }


    private fun collectServerStates() {
        viewModelScope.launch {
            _serverState.collect {
                when (it) {
                    ServerState.Starting -> {
                        Log.d("ActiveRoomViewModel", "server state: Starting")
                    }

                    ServerState.Running -> {
                        Log.d("ActiveRoomViewModel", "server state: Running")

                    }

                    is ServerState.Error -> {
                        Log.d("ActiveRoomViewModel", "server state: ${it.message}")
                    }

                    ServerState.Idle -> setHostStreamingState(false)

                    ServerState.Streaming -> setHostStreamingState(true)

                    ServerState.Stopped -> setHostStreamingState(false)

                    ServerState.Stopping -> {}
                }
            }
        }
    }

    fun setHostStreamingState(state: Boolean) {
        _uiState.update { uIState ->
            uIState.copy(isHostStreaming = state)
        }
    }

    fun setShowAskStopStreaming(show: Boolean){
        _uiState.update { uIState ->
            uIState.copy(showAskToStopStreaming = show)
        }
    }

    fun stopStreaming() {
        val intent = Intent(appContext, AudioCaptureService::class.java)
        appContext.stopService(intent)
        _uiEvent.trySend(UiEvent.ShowSnackBar(appContext.getString(R.string.you_have_stopped_streaming)))
    }



}