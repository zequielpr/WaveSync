package com.kunano.wavesynch.domain.usecase.guest_host_connection_use_cases

import android.net.wifi.p2p.WifiP2pDevice
import com.kunano.wavesynch.data.wifi.GuestConnectionEvent
import com.kunano.wavesynch.domain.repositories.GuestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ConnectToHostServerUseCase @Inject constructor(private val guestRepository: GuestRepository) {
    val connectionEvents: Flow<GuestConnectionEvent> = guestRepository.connectionEvents
    suspend operator fun invoke(serverHost: WifiP2pDevice){
        guestRepository.connectToHostServer(serverHost)

    }
}