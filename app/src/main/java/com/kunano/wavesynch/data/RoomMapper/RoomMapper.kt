package com.kunano.wavesynch.data.model

import com.kunano.wavesynch.data.data_source.local.entity.RoomEntity
import com.kunano.wavesynch.domain.model.Room

data class RoomDto (
    val id: String,
    val name: String,
)

fun RoomDto.toDomain(): Room = Room(
    id = id,
    name = name
)


fun Room.toEntity() = RoomEntity(
    id = id,
    name = name,
)