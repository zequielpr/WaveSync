package com.kunano.wavesynch.ui.main_screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.R
import com.kunano.wavesynch.domain.usecase.room_use_cases.CreateRoomUseCase
import com.kunano.wavesynch.domain.usecase.room_use_cases.ObserverRoomsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val createRoomUseCase: CreateRoomUseCase,
    private val observerRoomsUseCase: ObserverRoomsUseCase,
    @ApplicationContext val appContext: Context,
) : ViewModel() {
    private val _UIState = MutableStateFlow(MainScreenUIState())
    val UIState = _UIState.asStateFlow()


    init {
        viewModelScope.launch {
            observerRoomsUseCase().catch {
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
                val result = createRoomUseCase("$roomDefaultName 1")

                if (result >= 1L) {
                    navigateToShareSound()
                }
            }

        }
    }


    fun joinRoom() {
        viewModelScope.launch {

        }
    }
}
