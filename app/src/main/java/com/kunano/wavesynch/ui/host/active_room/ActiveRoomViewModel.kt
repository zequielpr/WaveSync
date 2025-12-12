package com.kunano.wavesynch.ui.host.active_room

import android.content.Context
import android.media.projection.MediaProjection
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.stream.HostAudioCapturer
import com.kunano.wavesynch.data.wifi.HandShake
import com.kunano.wavesynch.data.wifi.HandShakeResult
import com.kunano.wavesynch.data.wifi.ServerState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.RoomWithTrustedGuests
import com.kunano.wavesynch.domain.model.TrustedGuest
import com.kunano.wavesynch.domain.repositories.HostRepository
import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import com.kunano.wavesynch.domain.usecase.room_use_cases.DeleteRoomUseCase
import com.kunano.wavesynch.domain.usecase.room_use_cases.EditRoomNameUseCase
import com.kunano.wavesynch.domain.usecase.room_use_cases.ObserverRoomsUseCase
import com.kunano.wavesynch.domain.usecase.streaming_use_cases.HostRoomUseCase
import com.kunano.wavesynch.domain.usecase.trusted_guest_use_cases.AddTrustedGuestUseCase
import com.kunano.wavesynch.domain.usecase.trusted_guest_use_cases.CreateTrustedGuestUseCase
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
    private val observeRoomsUseCase: ObserverRoomsUseCase,
    private val deleteRoomUseCase: DeleteRoomUseCase,
    private val editRoomNameUseCase: EditRoomNameUseCase,
    private val hostRoomUseCase: HostRoomUseCase,
    private val hostRepository: HostRepository,
    private val createTrustedGuestUseCase: CreateTrustedGuestUseCase,
    private val addTrustedGuestUseCase: AddTrustedGuestUseCase,
    private val soundRoomRepository: SoundRoomRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveRoomUIState())
    val uiState: StateFlow<ActiveRoomUIState> = _uiState.asStateFlow()

    val _serverState = hostRepository.serverStateFlow

    var askToTrustGuestEvent: ActiveRoomUiEvent.AskToAcceptGuestRequest? = null

    companion object {
        const val REQ_CAPTURE_AUDIO = 100
    }

    private var hostAudioCapturer: HostAudioCapturer? = null


    //UiEvents
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()


    init {
        retrieveRoom()
        collectServerStates()
        collectLogs()
        collectHandShakeResults()


    }




    private fun collectHandShakeResults() {
        viewModelScope.launch {
            hostRepository.handShakeResultFlow.collect { answer ->
                when (answer) {
                    is HandShakeResult.Success -> {
                        answer.handShake?.let {
                            val guest = Guest(it.deviceName, it.userId, it.deviceName)
                            hostRepository.acceptUserConnection(guest)
                        }
                        //startStreamingAsHostUseCase()
                        answer.handShake?.let {
                            hostRepository.sendAnswerToGuest(it.userId, answer)
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

    fun hostRoom(roomId: Long? = null) {
        viewModelScope.launch {
            hostRoomUseCase(roomId)

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
                hostRepository.acceptUserConnection(guest)
                hostRepository.sendAnswerToGuest(
                    handShake.userId, HandShakeResult.Success()
                )
                uiState.value.room?.id?.let {
                    updateGuestConnectionStatus(it, handShake.userId, true)

                }
            } else {
                handShake.response = HandShakeResult.DeclinedByHost().intValue
                hostRepository.sendAnswerToGuest(handShake.userId, HandShakeResult.DeclinedByHost())
            }
        }
    }

    private fun updateGuestConnectionStatus(roomId: Long, guestId: String, status: Boolean) {
        viewModelScope.launch {
            val roomWithTrustedGuest = RoomWithTrustedGuests(roomId, guestId, status)
            val result = soundRoomRepository.updateConnectionStatus(roomWithTrustedGuest)
            if (result >= 1) {
                Log.d("ActiveRoomViewModel", "updateGuestConnectionStatus: $result")
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
            val result = createTrustedGuestUseCase(trustedGuest)


            if (result >= 1) {
                currentRoomId?.let {
                    val roomWithTrustedGuest =
                        RoomWithTrustedGuests(it, handShake.userId, isConnected = true)
                    val result = addTrustedGuestUseCase(roomWithTrustedGuest)

                    if (result >= 1) {
                        Log.d("ActiveRoomViewModel", "addTrustedGuest: Trusted guest added")
                    }

                }
            }
        }

    }


    fun launchAudioCapture() {
        _uiEvent.trySend(ActiveRoomUiEvent.StartAudioCapturer)
    }
    fun startStreaming(mediaProjection: MediaProjection?) {
        mediaProjection?.let {mp ->
            hostAudioCapturer = HostAudioCapturer(mp)
            hostRepository.startStreamingAsHost(hostAudioCapturer!!)
        }



    }



    


    private fun collectLogs() {
        viewModelScope.launch {
            hostRepository.logFlow.collect {
                Log.d("ActiveRoomViewModel", "WifiDirectManager Logs: $it")

            }
        }
    }

    private fun collectServerStates() {
        viewModelScope.launch {
            _serverState.collect {
                when (it) {
                    ServerState.Starting -> {}
                    ServerState.Running -> {
                        Log.d("ActiveRoomViewModel", "collectStreamStates: Running")

                    }

                    is ServerState.Error -> {
                        Log.d("ActiveRoomViewModel", "collectStreamStates: ${it.message}")
                    }

                    ServerState.Idle -> {}
                    ServerState.Streaming -> {
                        Log.d("ActiveRoomViewModel", "collectStreamStates: Streaming")
                    }
                }
            }
        }
    }


    private fun retrieveRoom() {
        viewModelScope.launch {
            observeRoomsUseCase().catch {
                it.printStackTrace()
            }.catch { throwable ->
                throwable.printStackTrace()
            }.collect {
                if (it.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(room = it[0])
                    //Only host room if not already running/hosted to avoid re-hosting on config changes/updates
                    //Or you can rely on the repository to handle idempotency
                    hostRoom(it[0].id)
                    collectRoomGuests(it[0].id)
                    launchAudioCapture()
                }


            }
        }
    }


    private fun collectRoomGuests(romId: Long?) {
        viewModelScope.launch {
            romId?.let {
                hostRepository.connectedGuest.collect {
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
            val result = deleteRoomUseCase(roomId)
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
                val result = editRoomNameUseCase(
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


}