package com.kunano.wavesynch.domain.usecase

import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import javax.inject.Inject

class DeleteRoomUseCase @Inject constructor(private val soundRoomRepository: SoundRoomRepository) {
    suspend operator fun invoke(roomId: Long): Int {
        return soundRoomRepository.deleteRoom(roomId)
    }
}