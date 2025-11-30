package com.kunano.wavesynch.domain.usecase

import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import javax.inject.Inject

class EditRoomNameUseCase @Inject constructor(private val soundRoomRepository: SoundRoomRepository) {
    suspend operator fun invoke(roomId: Long, newName: String): Int {
        return soundRoomRepository.editRoomName(roomId, newName)
    }
}