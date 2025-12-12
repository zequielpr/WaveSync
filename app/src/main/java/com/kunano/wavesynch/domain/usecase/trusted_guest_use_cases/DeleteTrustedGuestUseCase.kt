package com.kunano.wavesynch.domain.usecase.trusted_guest_use_cases

import com.kunano.wavesynch.domain.model.TrustedGuest
import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import javax.inject.Inject

class DeleteTrustedGuestUseCase @Inject constructor(
    private val repository: SoundRoomRepository
) {
    suspend operator fun invoke(trustedGuest: TrustedGuest): Int {
        return repository.deleteTrustedGuest(trustedGuest)
    }
}