package com.kunano.wavesynch.data.data_source.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(primaryKeys = ["roomId", "userId"], tableName = "room_trusted_guest_cross_ref",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrustedGuestEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("roomId"),
        Index("userId")
    ])
data class RoomTrustedGuestCrossRefEntity(
    val roomId: Long,
    var userId: String
)