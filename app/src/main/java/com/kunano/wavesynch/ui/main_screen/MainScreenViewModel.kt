package com.kunano.wavesynch.ui.main_screen

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.domain.usecase.GuestUseCases
import com.kunano.wavesynch.domain.usecase.host.HostUseCases
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.utils.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val hostUseCases: HostUseCases,
    private val guestUseCases: GuestUseCases,
    @ApplicationContext val appContext: Context,
) : ViewModel() {
    private val _UIState = MutableStateFlow(MainScreenUIState())
    val UIState = _UIState.asStateFlow()

    private val _uiEven: MutableSharedFlow<UiEvent> = MutableSharedFlow()
    val uiEvent = _uiEven.asSharedFlow()



    init {
        viewModelScope.launch {
            hostUseCases.observerRooms().catch {
                _UIState.value = _UIState.value.copy(false)
                it.printStackTrace()
            }.collect { rooms ->
                _UIState.value = _UIState.value.copy(!rooms.isEmpty())

            }
        }

    }


    fun shareSound(navigateToShareSound: () -> Unit) {
        viewModelScope.launch {
            if (UIState.value.isDefaultRoomCreated) {
                navigateToShareSound()
            } else {
                val roomDefaultName = appContext.getString(R.string.default_room_name)
                val result = hostUseCases.createRoom("$roomDefaultName 1")

                if (result >= 1L) {
                    navigateToShareSound()
                }
            }

        }
    }


    fun joinRoom() {
        viewModelScope.launch {

            val result = guestUseCases.isConnectedToHotspotAsGuest()
            Log.d("MainScreen", "already joined: $result")
            if (result) {
                _uiEven.emit(UiEvent.NavigateTo(Screen.CurrentRoomScreen))
            }else{
                _uiEven.emit(UiEvent.NavigateTo(Screen.JoinRoomScreen))
            }


        }
    }
}
