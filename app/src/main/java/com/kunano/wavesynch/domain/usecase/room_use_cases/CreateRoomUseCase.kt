package com.kunano.wavesynch.domain.usecase.room_use_cases

import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import javax.inject.Inject

class CreateRoomUseCase @Inject constructor(private val soundRoomRepository: SoundRoomRepository) {
    suspend operator fun invoke(roomName: String): Long {
        return soundRoomRepository.createRoom(roomName)
    }
}
