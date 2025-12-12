package com.kunano.wavesynch.data.repository

import android.Manifest
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.kunano.wavesynch.data.stream.AudioReceiver
import com.kunano.wavesynch.data.wifi.GuestConnectionEvent
import com.kunano.wavesynch.data.wifi.HandShakeResult
import com.kunano.wavesynch.data.wifi.WifiP2pGuestManager
import com.kunano.wavesynch.domain.repositories.GuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class GuestRepositoryImpl @Inject constructor(
    private val wifiP2pGuestManager: WifiP2pGuestManager,
    private val audioReceiver: AudioReceiver

    ) : GuestRepository {
    override val currentPeers: Flow<List<WifiP2pDevice>> = wifiP2pGuestManager.peers
    override val connectionEvents: SharedFlow<GuestConnectionEvent> =
        wifiP2pGuestManager.connectionEvents

    override val hanShakeResponse: SharedFlow<HandShakeResult> = wifiP2pGuestManager.handShakeResponse
    override fun startReceivingAudioStream() {
        if(wifiP2pGuestManager.socket.isConnected){
            audioReceiver.start(wifiP2pGuestManager.socket.inputStream)
        }

    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override suspend fun discoverPeers(onStarted: (Boolean) -> Unit) {
        wifiP2pGuestManager.startDiscovery(onResult = { result ->
            onStarted(result)
        })

    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override suspend fun connectTo(device: WifiP2pDevice): Result<Unit> {
        return wifiP2pGuestManager.connectTo(device)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override suspend fun connectToHostServer(device: WifiP2pDevice) {
        //wifiP2pGuestManager.requestConnectionToRoomServer(device)
    }

    override suspend fun leaveRoom(device: WifiP2pDevice): Result<Unit>{
     TODO()
    }


}