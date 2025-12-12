package com.kunano.wavesynch.domain.usecase.streaming_use_cases

import com.kunano.wavesynch.domain.repositories.HostRepository
import javax.inject.Inject

class StreamingFromHostUseCases @Inject constructor(
    private val repo: HostRepository
) {
}