package com.kunano.wavesynch.domain.usecase.host

import com.kunano.wavesynch.data.wifi.WifiLocalPortInfo
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

    val serverStateFlow: Flow<ServerState> = hostRepository.serverStateFlow
    val logFlow : Flow<String> = hostRepository.logFlow
    val handShakeResultFlow: Flow<HandShakeResult> = hostRepository.handShakeResultFlow
    val connectedGuest: Flow<LinkedHashSet<Guest>?> = hostRepository.connectedGuest

    fun addGuestToHostStreamer(guestId: String) = hostRepository.addGuestToHostStreamer(guestId)



    fun finishSessionAsHost() = hostRepository.finishSessionAsHost()



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
    fun sendAnswerToGuest(guestId: String, roomName: String? = null, answer: HandShakeResult) = hostRepository.sendAnswerToGuest(guestId, roomName, answer)
    fun acceptUserConnection(guest: Guest) = hostRepository.acceptUserConnection(guest)

    //Manage streaming
    fun startServer(room: Room, hostIp: String) = hostRepository.startServer(hostIp = hostIp, room = room)
    fun stopServer() = hostRepository.stopServer()




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
    fun openPortOverLocalWifi(room: Room): WifiLocalPortInfo?  = hostRepository.openPortOverLocalWifi(room)



}