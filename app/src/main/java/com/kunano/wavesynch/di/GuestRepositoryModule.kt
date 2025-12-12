package com.kunano.wavesynch.di

import android.content.Context
import com.kunano.wavesynch.data.repository.GuestRepositoryImpl
import com.kunano.wavesynch.data.stream.AudioReceiver
import com.kunano.wavesynch.data.wifi.WifiP2pGuestManager
import com.kunano.wavesynch.domain.repositories.GuestRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GuestRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGuestRepository(
        impl: GuestRepositoryImpl
    ): GuestRepository

    companion object {
        @Provides
        @Singleton
        fun provideWifiP2pGuestManager(
            @ApplicationContext context: Context
        ): WifiP2pGuestManager = WifiP2pGuestManager(
            context,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )

        @Provides
        @Singleton
        fun provideAudioReceiver(): AudioReceiver = AudioReceiver()


    }

}