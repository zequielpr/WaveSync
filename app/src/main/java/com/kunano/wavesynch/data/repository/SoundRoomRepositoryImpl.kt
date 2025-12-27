package com.kunano.wavesynch.data.repository

import com.kunano.wavesynch.data.RoomMapper.toDomain
import com.kunano.wavesynch.data.RoomMapper.toEntity
import com.kunano.wavesynch.data.data_source.local.dao.RoomDao
import com.kunano.wavesynch.data.data_source.local.dao.RoomTrustedGuestCrossRefDao
import com.kunano.wavesynch.data.data_source.local.dao.TrustedGuestDao
import com.kunano.wavesynch.data.data_source.local.entity.RoomEntity
import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.model.RoomWithTrustedGuests
import com.kunano.wavesynch.domain.model.TrustedGuest
import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SoundRoomRepositoryImpl @Inject constructor(
    val roomDao: RoomDao,
    private val trustedGuestDao: TrustedGuestDao,
    private val roomTrustedGuestCrossRefDao: RoomTrustedGuestCrossRefDao,
) : SoundRoomRepository {
    override suspend fun createRoom(roomName: String): Long {
        return roomDao.upsertRoom(RoomEntity(name = roomName))

    }

    override fun observerRooms(): Flow<List<Room>> {
        return roomDao.observerRooms().map { rooms ->
            rooms.map { it.toDomain() }
        }

    }

    override suspend fun deleteRoom(roomId: Long): Int {
        return roomDao.deleteRoom(roomId)
    }

    override suspend fun editRoomName(roomId: Long, newName: String): Int {
        return roomDao.updateRoom(roomId, newName)


    }

    override suspend fun getRoomTrustedGuests(roomId: Long): List<String> {
        return roomTrustedGuestCrossRefDao.getTrustedGuestsForRoom(roomId)
    }

    override suspend fun getTrustedGuestById(guestId: String): TrustedGuest? {
        return trustedGuestDao.getTrustedGuestById(guestId)?.toDomain()
    }

    override suspend fun addTrustedGuest(roomWithTrustedGuests: RoomWithTrustedGuests): Long {
        return roomTrustedGuestCrossRefDao.insert(roomWithTrustedGuests.toEntity())
    }

    override suspend fun removeTrustedGuest(roomWithTrustedGuests: RoomWithTrustedGuests) {
        roomTrustedGuestCrossRefDao.delete(roomWithTrustedGuests.toEntity())
    }

    override suspend fun createTrustedGuest(trustedGuest: TrustedGuest): Long {
        return trustedGuestDao.upSert(trustedGuest.toEntity())
    }

    override suspend fun deleteTrustedGuest(trustedGuest: TrustedGuest): Int {
        return trustedGuestDao.delete(trustedGuest.toEntity())

    }


    override suspend fun updateTrustedGuest(trustedGuest: TrustedGuest): Int {
       return trustedGuestDao.update(trustedGuest.toEntity())
    }




    override fun observerRoomGuests(roomId: Long): Flow<List<TrustedGuest>> {
        return roomTrustedGuestCrossRefDao.observerRoomGuests(roomId).map { guests ->
            guests.map { it.toDomain() }
        }


    }

}