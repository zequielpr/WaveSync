package com.kunano.wavesynch.ui.main_screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.CrashReporter
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


    private fun navigateTo(screen: Screen){
        viewModelScope.launch {
            _uiEven.emit(UiEvent.NavigateTo(screen))
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
        val isConnectedToHostServer = guestUseCases.isConnectedToServer()
        if (isConnectedToHostServer) {
            navigateTo(Screen.CurrentRoomScreen)
        } else {
            navigateTo(Screen.JoinRoomScreen)
        }
    }
}
