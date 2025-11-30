package com.kunano.wavesynch.domain.usecase

import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import javax.inject.Inject

class CreateRoomUseCase @Inject constructor(private val soundRoomRepository: SoundRoomRepository) {
    suspend operator fun invoke(roomName: String): Long {
        return soundRoomRepository.createRoom(roomName)
    }
}
