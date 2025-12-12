package com.kunano.wavesynch.domain.usecase.trusted_guest_use_cases

import com.kunano.wavesynch.domain.model.RoomWithTrustedGuests
import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import javax.inject.Inject

class RemoveTrustedGuestUseCase @Inject constructor(
    private val repository: SoundRoomRepository
) {
    suspend operator fun invoke(roomWithTrustedGuests: RoomWithTrustedGuests) {
        repository.removeTrustedGuest(roomWithTrustedGuests)
    }
}