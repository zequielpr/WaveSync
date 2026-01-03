package com.kunano.wavesynch.domain.usecase.host

import com.android.identity.flow.annotation.FlowState
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
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class HostUseCases @Inject constructor(
    private val hostRepository: HostRepository,
    private val soundRoomRepository: SoundRoomRepository,
) {
    val hotspotInfoFlow: Flow<HotspotInfo?> = hostRepository.hotspotInfoFlow
    val hotSpotStateFlow: Flow<HotspotState> = hostRepository.hotSpotStateFlow
    val serverStateFlow: Flow<ServerState> = hostRepository.serverStateFlow
    val logFlow : Flow<String> = hostRepository.logFlow
    val handShakeResultFlow: Flow<HandShakeResult> = hostRepository.handShakeResultFlow
    val connectedGuest: Flow<ArrayList<Guest>?> = hostRepository.connectedGuest

    fun addGuestToHostStreamer(guestId: String) = hostRepository.addGuestToHostStreamer(guestId)


    //Manage hotspot
    fun startHotspot(
        onStarted: (HotspotInfo) -> Unit, onError: (Int) -> Unit,
    ) = hostRepository.startHotspot(onStarted, onError)

    fun stopHotspot() = hostRepository.stopHotspot()
    fun isHotspotRunning() = hostRepository.isHotspotRunning()
    fun finishSessionAsHost() = hostRepository.finishSessionAsHost()
    fun isHostStreaming() = hostRepository.isHostStreaming()



    //Manage rooms
    suspend fun createRoom(roomName: String): Long = soundRoomRepository.createRoom(roomName)
    fun observerRooms(): Flow<List<Room>> = soundRoomRepository.observerRooms()
    suspend fun deleteRoom(roomId: Long): Int = soundRoomRepository.deleteRoom(roomId)
    suspend fun editRoomName(roomId: Long, newName: String): Int =
        soundRoomRepository.editRoomName(roomId, newName)


    //Host room

    fun playGuest(guestId: String) = hostRepository.playGuest(guestId)
    fun pauseGuest(guestId: String) = hostRepository.pauseGuest(guestId)
    fun expelGuest(guestId: String) = hostRepository.expelGuest(guestId)
    suspend fun sendAnswerToGuest(guestId: String, roomName: String? = null, answer: HandShakeResult) = hostRepository.sendAnswerToGuest(guestId, roomName, answer)
    suspend fun acceptUserConnection(guest: Guest) = hostRepository.acceptUserConnection(guest)

    //Manage streaming
    suspend fun startServer(room: Room) = hostRepository.startServer(room)
    fun stopServer() = hostRepository.stopServer()
    fun stopStreaming() = hostRepository.stopStreaming()
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



    fun observerRoomGuests(roomId: Long): Flow<List<TrustedGuest>> =
        soundRoomRepository.observerRoomGuests(roomId)

    fun emptyRoom()  = hostRepository.emptyRoom()





}