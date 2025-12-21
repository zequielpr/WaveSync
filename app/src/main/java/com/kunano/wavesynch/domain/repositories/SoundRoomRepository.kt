package com.kunano.wavesynch.domain.repositories

import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.model.RoomWithTrustedGuests
import com.kunano.wavesynch.domain.model.TrustedGuest
import kotlinx.coroutines.flow.Flow

interface SoundRoomRepository {
    suspend fun createRoom(roomName: String):Long
    fun observerRooms(): Flow<List<Room>>
    suspend fun deleteRoom(roomId: Long): Int
    suspend fun editRoomName(roomId: Long, newName: String): Int

    suspend fun getRoomTrustedGuests(roomId: Long): List<String>
    suspend fun getTrustedGuestById(guestId: String): TrustedGuest?
    suspend fun addTrustedGuest(roomWithTrustedGuests: RoomWithTrustedGuests): Long
    suspend fun removeTrustedGuest(roomWithTrustedGuests: RoomWithTrustedGuests)
    suspend fun createTrustedGuest(trustedGuest: TrustedGuest): Long
    suspend fun deleteTrustedGuest(trustedGuest: TrustedGuest): Int
    suspend fun updateTrustedGuest(trustedGuest: TrustedGuest): Int
    fun observerRoomGuests(roomId: Long): Flow<List<TrustedGuest>>
}

