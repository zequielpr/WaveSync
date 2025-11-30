package com.kunano.wavesynch.data.data_source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kunano.wavesynch.data.data_source.local.entity.RoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoom(room: RoomEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRooms(rooms: List<RoomEntity>)

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getRoomById(id: String): RoomEntity?

    @Query("SELECT * FROM rooms")
    suspend fun getAllRooms(): List<RoomEntity>

    @Query("SELECT * FROM rooms")
    fun observerRooms(): Flow<List<RoomEntity>>

    @Query("DELETE FROM rooms")
    suspend fun clearRooms(): Int

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteRoom(id: Long): Int

    @Query("UPDATE rooms SET name = :newName WHERE id = :roomId")
    suspend fun updateRoom(roomId: Long, newName: String): Int





}