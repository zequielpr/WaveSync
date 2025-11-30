package com.kunano.wavesynch.ui.host.active_room

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.domain.usecase.DeleteRoomUseCase
import com.kunano.wavesynch.domain.usecase.EditRoomNameUseCase
import com.kunano.wavesynch.domain.usecase.ObserverRoomsUseCase
import com.kunano.wavesynch.ui.utils.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveRoomUIState())
    val uiState: StateFlow<ActiveRoomUIState> = _uiState.asStateFlow()


    //UiEvents
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()


    init {
        viewModelScope.launch {
            observeRoomsUseCase().catch {
                it.printStackTrace()
            }.catch { throwable ->
                throwable.printStackTrace()
            }.collect {
                if (it.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(room = it[0])
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




        if (newName.isNotEmpty()){
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

        }else
        {
            _uiEvent.trySend(UiEvent.ShowSnackBar(appContext.getString(R.string.enter_new_name)))
        }


    }

    fun expelGuest(guestId: String) {

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