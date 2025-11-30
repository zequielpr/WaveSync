package com.kunano.wavesynch.data.repository

import android.util.Log
import com.kunano.wavesynch.data.RoomMapper.toDomain
import com.kunano.wavesynch.data.data_source.local.dao.RoomDao
import com.kunano.wavesynch.data.data_source.local.entity.RoomEntity
import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SoundRoomRepositoryImpl @Inject constructor(val roomDao: RoomDao) : SoundRoomRepository {
    override suspend fun createRoom(roomName: String): Long {
        return roomDao.upsertRoom(RoomEntity(name = roomName))

    }

    override suspend fun joinRoom(roomId: String) {

    }

    override fun observerRooms(): Flow<List<Room>> {
        return roomDao.observerRooms().map { rooms ->
            rooms.map {it.toDomain()} }

    }

    override suspend fun deleteRoom(roomId: Long): Int {
        return roomDao.deleteRoom(roomId)
    }

    override suspend fun editRoomName(roomId: Long, newName: String): Int {
        return roomDao.updateRoom(roomId, newName)


    }
}