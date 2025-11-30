package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.domain.model.Room

interface SoundRoomRepository {
    suspend fun createRoom(roomName: String): Room
    suspend fun joinRoom(roomId: String): Room
}