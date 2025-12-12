package com.kunano.wavesynch.domain.usecase.guest_host_connection_use_cases

import android.net.wifi.p2p.WifiP2pDevice
import com.kunano.wavesynch.domain.repositories.GuestRepository
import javax.inject.Inject

class JoinRoomUseCase @Inject constructor(
    private val repository: GuestRepository,
) {
    suspend  operator fun invoke(device: WifiP2pDevice): Result<Unit>{
        return repository.connectTo(device)

    }
}