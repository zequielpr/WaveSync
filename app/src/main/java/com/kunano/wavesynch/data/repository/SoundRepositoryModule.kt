package com.kunano.wavesynch.data.repository

import com.kunano.wavesynch.domain.repositories.SoundRoomRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


//Module to bind interface to implementation
@Module
@InstallIn(SingletonComponent::class)
abstract class SoundRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSoundRoomRepository(
        impl: SoundRoomRepositoryImpl
    ): SoundRoomRepository
}