package com.kunano.wavesynch.domain.usecase

import android.util.Log
import javax.inject.Inject

public class SoundRoomUseCase @Inject constructor() {
    
    suspend fun createRoom(name: String) {
        Log.d("room created ", name)


    }

    suspend fun joinRoom(roomId: String) {
    }

    suspend fun leaveRoom(roomId: String) {
    }

    fun observeRoomState(roomId: String) {
    }

    suspend fun startStreaming() {

    }

    suspend fun stopStreaming() {
    }
}