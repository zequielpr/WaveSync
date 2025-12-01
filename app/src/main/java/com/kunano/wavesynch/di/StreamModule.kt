package com.kunano.wavesynch.di

import android.content.Context
import com.kunano.wavesynch.data.stream.AudioStreamRepositoryImpl
import com.kunano.wavesynch.data.wifi.WifiDirectManager
import com.kunano.wavesynch.domain.repositories.sound_streaming_repository.AudioStreamRepository
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
abstract class StreamModule {

    @Binds
    @Singleton
    abstract fun bindAudioStreamRepository(
        impl: AudioStreamRepositoryImpl
    ): AudioStreamRepository

    companion object {
        @Provides
        @Singleton
        fun provideWifiDirectManager(
            @ApplicationContext context: Context
        ): WifiDirectManager = WifiDirectManager(context)
    }
}

