package com.kunano.wavesynch.domain.usecase.host

import com.kunano.wavesynch.data.stream.HostAudioCapturer
import com.kunano.wavesynch.data.wifi.hotspot.HotspotInfo
import com.kunano.wavesynch.data.wifi.hotspot.HotspotState
import com.kunano.wavesynch.data.wifi.server.HandShakeResult
import com.kunano.wavesynch.data.wifi.server.ServerState
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.domain.model.Room
import com.kunano.wavesynch.domain.model.RoomWithTrustedGuests
import com.kunano.wavesynch.domain.model.TrustedGuest
import com.kunano.wavesynch.domain.repositories.HostRepository
import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HostUseCases @Inject constructor(
    private val hostRepository: HostRepository,
    private val soundRoomRepository: SoundRoomRepository,
) {
    val hotSpotStateFlow: Flow<HotspotState> = hostRepository.hotSpotStateFlow
    val serverStateFlow: Flow<ServerState> = hostRepository.serverStateFlow
    val logFlow : Flow<String> = hostRepository.logFlow
    val handShakeResultFlow: Flow<HandShakeResult> = hostRepository.handShakeResultFlow
    val connectedGuest: Flow<HashSet<Guest>?> = hostRepository.connectedGuest


    //Manage hotspot
    fun startHotspot(
        onStarted: (HotspotInfo) -> Unit, onError: (Int) -> Unit,
    ) = hostRepository.startHotspot(onStarted, onError)

    fun stopHotspot() = hostRepository.stopHotspot()
    fun isHotspotRunning() = hostRepository.isHotspotRunning()
    fun getHotspotInfo() = hostRepository.getHotspotInfo()




    //Manage rooms
    suspend fun createRoom(roomName: String): Long = soundRoomRepository.createRoom(roomName)
    fun observerRooms(): Flow<List<Room>> = soundRoomRepository.observerRooms()
    suspend fun deleteRoom(roomId: Long): Int = soundRoomRepository.deleteRoom(roomId)
    suspend fun editRoomName(roomId: Long, newName: String): Int =
        soundRoomRepository.editRoomName(roomId, newName)


    //Host room

    suspend fun expelGuest(guestId: String) = hostRepository.expelGuest(guestId)
    suspend fun sendAnswerToGuest(guestId: String, answer: HandShakeResult) = hostRepository.sendAnswerToGuest(guestId, answer)
    suspend fun acceptUserConnection(guest: Guest) = hostRepository.acceptUserConnection(guest)

    //Manage streaming
    suspend fun startServer(roomId: Long? = null) = hostRepository.startServer(roomId)
    fun stopServer() = hostRepository.stopServer()
    fun startStreamingAsHost(capturer: HostAudioCapturer) = hostRepository.startStreamingAsHost(capturer)
    fun stopStreaming() = hostRepository.stopStreaming()
    fun startStreamingToGuest(guestId: String) = hostRepository.startStreamingToGuest(guestId)
    fun stopStreamingToGuest(guestId: String) = hostRepository.stopStreamingToGuest(guestId)
    fun closeUserSocket(userId: String) = hostRepository.closeUserSocket(userId)




    //Manage trusted guests
    suspend fun getRoomTrustedGuests(roomId: Long): List<String> =
        soundRoomRepository.getRoomTrustedGuests(roomId)

    suspend fun getTrustedGuestById(guestId: String): TrustedGuest? =
        soundRoomRepository.getTrustedGuestById(guestId)

    suspend fun addTrustedGuest(roomWithTrustedGuests: RoomWithTrustedGuests): Long =
        soundRoomRepository.addTrustedGuest(roomWithTrustedGuests)

    suspend fun removeTrustedGuest(roomWithTrustedGuests: RoomWithTrustedGuests) =
        soundRoomRepository.removeTrustedGuest(roomWithTrustedGuests)

    suspend fun createTrustedGuest(trustedGuest: TrustedGuest): Long =
        soundRoomRepository.createTrustedGuest(trustedGuest)

    suspend fun deleteTrustedGuest(trustedGuest: TrustedGuest): Int =
        soundRoomRepository.deleteTrustedGuest(trustedGuest)

    suspend fun updateTrustedGuest(trustedGuest: TrustedGuest): Int =
        soundRoomRepository.updateTrustedGuest(trustedGuest)

    suspend fun updateConnectionStatus(roomWithTrustedGuests: RoomWithTrustedGuests): Int =
        soundRoomRepository.updateConnectionStatus(roomWithTrustedGuests)

    fun observerRoomGuests(roomId: Long): Flow<List<TrustedGuest>> =
        soundRoomRepository.observerRoomGuests(roomId)


}