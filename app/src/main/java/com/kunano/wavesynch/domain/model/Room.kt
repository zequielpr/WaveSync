package com.kunano.wavesynch.domain.model

data class Room(
    val QRCode: String? = null,
    val id: Long? = null,
    val name: String,
)

 data class TrustedGuest(
    var userName: String?,
    var userId: String,
    var deviceName: String?,
    var isConnected: Boolean = false,
)

data class Guest(
    var userName: String,
    var userId: String,
    var deviceName: String,
){
    // Force equality only by userId
    override fun equals(other: Any?): Boolean {
        return other is Guest && other.userId == this.userId
    }

    override fun hashCode(): Int {
        return userId.hashCode()
    }
}


data class RoomWithTrustedGuests(
    val roomId: Long,
    var userId: String,
    var isConnected: Boolean = false,
)