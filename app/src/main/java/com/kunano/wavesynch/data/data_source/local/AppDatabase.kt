package com.kunano.wavesynch.data.data_source.local

import android.content.Context
import com.kunano.wavesynch.data.data_source.local.dao.RoomDao
import kotlin.jvm.java
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kunano.wavesynch.data.data_source.local.entity.RoomEntity

@Database(
    entities = [RoomEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
}


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wave_sync_db"
        ).build()

    @Provides
    fun provideRoomDao(db: AppDatabase): RoomDao = db.roomDao()
}
