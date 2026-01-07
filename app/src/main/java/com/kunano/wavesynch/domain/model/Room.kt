package com.kunano.wavesynch.domain.model

data class Room(
    val QRCode: String? = null,
    val id: Long? = null,
    val name: String,
)

data class TrustedGuest(
    val userName: String?,
    val userId: String,
    val deviceName: String?,
    val isConnected: Boolean = false,
)

data class Guest(
    val userName: String,
    val userId: String,
    val deviceName: String,
    var isPlaying: Boolean = false,
)


data class RoomWithTrustedGuests(
    val roomId: Long,
    var userId: String,
)