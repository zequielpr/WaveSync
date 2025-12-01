package com.kunano.wavesynch.domain.repositories.sound_streaming_repository

interface AudioStreamRepository {
    suspend fun hostRoom()      // create P2P group
    suspend fun joinRoom(hostAddress: String)

    suspend fun startStreamingAsGuest()
    suspend fun startStreamingAsHost()
    suspend fun stopStreaming()
}