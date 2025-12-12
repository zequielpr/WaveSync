package com.kunano.wavesynch.domain.usecase.guest_host_connection_use_cases

import android.net.wifi.p2p.WifiP2pDevice
import com.kunano.wavesynch.domain.repositories.GuestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StartPeerDiscoveryUseCase @Inject constructor(private val guestHostConnectionRepository: GuestRepository) {
    val currentPeers: Flow<List<WifiP2pDevice>>
        get() {
            return guestHostConnectionRepository.currentPeers
        }

    suspend operator fun invoke(onStarted: (Boolean) -> Unit) {
        return guestHostConnectionRepository.discoverPeers(onStarted)
    }

}