package com.kunano.wavesynch.data.data_source.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kunano.wavesynch.data.data_source.local.entity.RoomTrustedGuestCrossRefEntity
import com.kunano.wavesynch.data.data_source.local.entity.TrustedGuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomTrustedGuestCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(roomTrustedGuestCrossRef: RoomTrustedGuestCrossRefEntity): Long

    @Delete
    suspend fun delete(roomTrustedGuestCrossRef: RoomTrustedGuestCrossRefEntity): Int

    @Query("SELECT userId FROM room_trusted_guest_cross_ref WHERE roomId = :roomId")
    suspend fun getTrustedGuestsForRoom(roomId: Long): List<String>

    @Update
    suspend fun updateConnectionStatus(roomTrustedGuestCrossRef: RoomTrustedGuestCrossRefEntity): Int

    @Query("SELECT trusted_guest.userId, trusted_guest.userName , trusted_guest.deviceName, room_trusted_guest_cross_ref.isConnected as isConnected FROM trusted_guest LEFT JOIN room_trusted_guest_cross_ref ON" +
            " trusted_guest.userId = room_trusted_guest_cross_ref.userId" +
            " WHERE roomId = :roomId"
    )
    fun observerRoomGuests(roomId: Long): Flow<List<TrustedGuestEntity>>
}




