package com.kunano.wavesynch.di

import android.content.Context
import android.net.wifi.WifiManager
import com.kunano.wavesynch.data.repository.HostRepositoryImpl
import com.kunano.wavesynch.data.stream.HostStreamer
import com.kunano.wavesynch.data.wifi.hotspot.LocalHotspotController
import com.kunano.wavesynch.data.wifi.server.ServerManager
import com.kunano.wavesynch.domain.repositories.HostRepository
import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import com.kunano.wavesynch.domain.usecase.host.HostUseCases
import com.kunano.wavesynch.domain.usecase.host.GetRoomTrustedGuestsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// di/StreamModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class HostRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHostRepository(
        impl: HostRepositoryImpl,
    ): HostRepository

    companion object {
        @Provides
        @Singleton
        fun provideServerManager(
            @ApplicationContext context: Context,
            getRoomTrustedGuestsUseCase: GetRoomTrustedGuestsUseCase
        ): ServerManager = ServerManager(context, getRoomTrustedGuestsUseCase)

        @Provides
        @Singleton
        fun provideHostStreamer(
        ): HostStreamer = HostStreamer()

        @Provides
        @Singleton
        fun provideWifiManager(
            @ApplicationContext context: Context,
        ): WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager



        @Provides
        @Singleton
        fun provideHostUseCase(
            hostRepository: HostRepository,
            soundRoomRepository: SoundRoomRepository,
        ): HostUseCases =
            HostUseCases(hostRepository = hostRepository, soundRoomRepository = soundRoomRepository)


        //hotSpotImplementation
        @Provides
        @Singleton
        fun provideLocalHotSpotController(
            wifiManager: WifiManager,
            @ApplicationContext context: Context,
        ): LocalHotspotController = LocalHotspotController(wifiManager, context)

    }
}
