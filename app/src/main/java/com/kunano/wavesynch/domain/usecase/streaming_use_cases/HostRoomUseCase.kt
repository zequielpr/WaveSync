package com.kunano.wavesynch.domain.usecase.streaming_use_cases

import com.kunano.wavesynch.domain.repositories.sound_streaming_repository.AudioStreamRepository
import javax.inject.Inject

class HostRoomUseCase @Inject constructor(
    private val repo: AudioStreamRepository
) {
    suspend operator fun invoke() = repo.hostRoom()
}