package com.kunano.wavesynch.data.data_source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_guest")
data class TrustedGuestEntity(
    @PrimaryKey
    var userId: String,

    var userName: String?,
    var deviceName: String?,
)

