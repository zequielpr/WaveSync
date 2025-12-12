package com.kunano.wavesynch.domain.usecase.trusted_guest_use_cases

import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import javax.inject.Inject

class GetRoomTrustedGuestsUseCase @Inject constructor(
    private val repository: SoundRoomRepository
) {
    suspend operator fun invoke(roomId: Long): List<String> {
        return repository.getRoomTrustedGuests(roomId)
    }
}