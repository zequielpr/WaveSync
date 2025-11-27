package com.kunano.wavesynch.ui.main_screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.domain.usecase.SoundRoomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val soundRoomUseCase: SoundRoomUseCase
) : ViewModel() {

    fun shareSound() {
        viewModelScope.launch {
             soundRoomUseCase.createRoom("Test Room")
        }
    }

    fun joinRoom() {
        viewModelScope.launch {
            soundRoomUseCase.joinRoom("Test Room")
        }
    }
}
