package com.kunano.wavesynch.data.RoomMapper

import com.kunano.wavesynch.data.data_source.local.entity.RoomEntity
import com.kunano.wavesynch.data.data_source.local.entity.RoomTrustedGuestCrossRefEntity
import com.kunano.wavesynch.data.data_source.local.entity.TrustedGuestEntity
import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.model.RoomWithTrustedGuests
import com.kunano.wavesynch.domain.model.TrustedGuest

//Room mapper
fun RoomEntity.toDomain() = Room(
    id = id,
    name = name
)

fun Room.toEntity() = RoomEntity(
    id = id,
    name = name,
)


//Trusted guest mapper
fun TrustedGuestEntity.toDomain() = TrustedGuest(
    userName = userName,
    userId = userId,
    deviceName = deviceName
)

fun TrustedGuest.toEntity() = TrustedGuestEntity(
    userName = userName,
    userId = userId,
    deviceName = deviceName
)


//Room with trusted guests mapper
fun RoomTrustedGuestCrossRefEntity.toDomain() = RoomWithTrustedGuests(
    roomId = roomId,
    userId = userId
)

fun RoomWithTrustedGuests.toEntity() = RoomTrustedGuestCrossRefEntity(
    roomId = roomId,
    userId = userId
)