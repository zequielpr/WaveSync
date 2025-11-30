package com.kunano.wavesynch.data.RoomMapper

import com.kunano.wavesynch.data.data_source.local.entity.RoomEntity
import com.kunano.wavesynch.domain.model.Room

fun RoomEntity.toDomain() = Room(
    id = id,
    name = name
)

fun Room.toEntity() = RoomEntity(
    id = id,
    name = name,
)