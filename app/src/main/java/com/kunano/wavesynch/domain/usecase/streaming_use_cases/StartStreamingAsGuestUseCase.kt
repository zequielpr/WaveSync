package com.kunano.wavesynch.domain.usecase.streaming_use_cases

import com.kunano.wavesynch.domain.repositories.sound_streaming_repository.AudioStreamRepository
import javax.inject.Inject

class StartStreamingAsGuestUseCase @Inject constructor(
    private val repo: AudioStreamRepository
) {
    suspend operator fun invoke() = repo.startStreamingAsGuest()
}