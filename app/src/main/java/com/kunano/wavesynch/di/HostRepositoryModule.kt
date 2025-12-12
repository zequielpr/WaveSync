package com.kunano.wavesynch.di

import android.content.Context
import com.kunano.wavesynch.data.repository.HostRepositoryImpl
import com.kunano.wavesynch.data.stream.HostStreamer
import com.kunano.wavesynch.data.wifi.WifiDirectManager
import com.kunano.wavesynch.domain.repositories.HostRepository
import com.kunano.wavesynch.domain.usecase.trusted_guest_use_cases.GetRoomTrustedGuestsUseCase
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
        impl: HostRepositoryImpl
    ): HostRepository

    companion object {
        @Provides
        @Singleton
        fun provideWifiDirectManager(
            @ApplicationContext context: Context,
            getRoomTrustedGuestsUseCase: GetRoomTrustedGuestsUseCase
        ): WifiDirectManager = WifiDirectManager(context, getRoomTrustedGuestsUseCase)

        @Provides
        @Singleton
        fun provideHostStreamer(
            @ApplicationContext context: Context,
        ): HostStreamer = HostStreamer()
    }
}

