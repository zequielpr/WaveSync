package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.domain.model.Room
import kotlinx.coroutines.flow.Flow

interface SoundRoomRepository {
    suspend fun createRoom(roomName: String):Long
    suspend fun joinRoom(roomId: String)
    fun observerRooms(): Flow<List<Room>>
    suspend fun deleteRoom(roomId: Long): Int
    suspend fun editRoomName(roomId: Long, newName: String): Int
}

