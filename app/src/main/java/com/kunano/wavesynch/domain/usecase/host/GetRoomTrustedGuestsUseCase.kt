package com.kunano.wavesynch.domain.usecase.host

import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import javax.inject.Inject

class GetRoomTrustedGuestsUseCase @Inject constructor(
    private val soundRoomRepository: SoundRoomRepository,
) {
    suspend operator fun invoke(roomId: Long): List<String> {
        return soundRoomRepository.getRoomTrustedGuests(roomId)
    }
}