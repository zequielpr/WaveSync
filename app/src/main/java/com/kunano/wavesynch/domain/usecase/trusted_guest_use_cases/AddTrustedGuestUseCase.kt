package com.kunano.wavesynch.domain.usecase.trusted_guest_use_cases

import com.kunano.wavesynch.domain.model.RoomWithTrustedGuests
import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import javax.inject.Inject

class AddTrustedGuestUseCase @Inject constructor(
    private val repository: SoundRoomRepository
) {
    suspend operator fun invoke(roomWithTrustedGuests: RoomWithTrustedGuests): Long {
        return repository.addTrustedGuest(roomWithTrustedGuests)
    }
}