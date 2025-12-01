package com.kunano.wavesynch.di

import com.kunano.wavesynch.data.stream.AudioStreamRepositoryImpl
import com.kunano.wavesynch.domain.repositories.sound_streaming_repository.AudioStreamRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAudioStreamRepository(
        impl: AudioStreamRepositoryImpl
    ): AudioStreamRepository
}