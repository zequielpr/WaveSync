package com.kunano.wavesynch.data.data_source.local.entity

import androidx.room.Entity

@Entity(primaryKeys = ["roomId", "userId"], tableName = "room_trusted_guest_cross_ref")
data class RoomTrustedGuestCrossRefEntity(
    val roomId: Long,
    var userId: String,
    var isConnected: Boolean = false,
)