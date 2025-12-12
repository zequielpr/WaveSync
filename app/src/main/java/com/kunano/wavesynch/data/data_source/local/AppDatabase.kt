package com.kunano.wavesynch.data.data_source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kunano.wavesynch.data.data_source.local.dao.RoomDao
import com.kunano.wavesynch.data.data_source.local.dao.RoomTrustedGuestCrossRefDao
import com.kunano.wavesynch.data.data_source.local.dao.TrustedGuestDao
import com.kunano.wavesynch.data.data_source.local.entity.RoomEntity
import com.kunano.wavesynch.data.data_source.local.entity.RoomTrustedGuestCrossRefEntity
import com.kunano.wavesynch.data.data_source.local.entity.TrustedGuestEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [RoomEntity::class, TrustedGuestEntity::class, RoomTrustedGuestCrossRefEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    abstract fun trustedGuestDao(): TrustedGuestDao
    abstract fun roomTrustedGuestCrossRefDao(): RoomTrustedGuestCrossRefDao
}


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wave_sync_db"
        )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideRoomDao(db: AppDatabase): RoomDao = db.roomDao()

    @Provides
    fun provideTrustedGuestDao(db: AppDatabase): TrustedGuestDao = db.trustedGuestDao()

    @Provides
    fun provideRoomTrustedGuestCrossRefDao(db: AppDatabase): RoomTrustedGuestCrossRefDao =
        db.roomTrustedGuestCrossRefDao()
}
