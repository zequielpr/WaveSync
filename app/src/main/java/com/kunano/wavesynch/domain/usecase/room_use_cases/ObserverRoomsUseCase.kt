package com.kunano.wavesynch.domain.usecase.room_use_cases

import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserverRoomsUseCase @Inject constructor(private val soundRoomRepository: SoundRoomRepository) {
    operator fun invoke(): Flow<List<Room>> {
        return soundRoomRepository.observerRooms()
    }

}