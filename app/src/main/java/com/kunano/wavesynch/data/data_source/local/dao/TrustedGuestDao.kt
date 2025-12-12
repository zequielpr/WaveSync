package com.kunano.wavesynch.data.data_source.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kunano.wavesynch.data.data_source.local.entity.TrustedGuestEntity

@Dao
interface TrustedGuestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upSert(trustedGuestEntity: TrustedGuestEntity): Long

    @Update
    suspend fun update(trustedGuestEntity: TrustedGuestEntity): Int

    @Delete
    suspend fun delete(trustedGuestEntity: TrustedGuestEntity): Int

    @Query("SELECT * FROM trusted_guest WHERE userId = :guestId")
    suspend fun getTrustedGuestById(guestId: String): TrustedGuestEntity?

    @Query("SELECT * FROM trusted_guest")
    suspend fun getAllTrustedGuests(): List<TrustedGuestEntity>

}