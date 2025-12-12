package com.kunano.wavesynch.domain.repositories

import android.net.wifi.p2p.WifiP2pDevice
import com.kunano.wavesynch.data.wifi.GuestConnectionEvent
import com.kunano.wavesynch.data.wifi.HandShakeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface GuestRepository{
    val currentPeers: Flow<List<WifiP2pDevice>>
    val connectionEvents: SharedFlow<GuestConnectionEvent>
    val hanShakeResponse: SharedFlow<HandShakeResult>



    fun startReceivingAudioStream()
    suspend fun discoverPeers(onStarted: (Boolean) -> Unit = {})
    suspend fun connectTo(device: WifiP2pDevice): Result<Unit>
    suspend fun connectToHostServer(device: WifiP2pDevice)
    suspend fun leaveRoom(device: WifiP2pDevice): Result<Unit>

}