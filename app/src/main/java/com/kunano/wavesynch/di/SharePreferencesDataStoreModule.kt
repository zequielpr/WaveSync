package com.kunano.wavesynch.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.kunano.wavesynch.data.data_store_preferences.DataStorePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object SharedPreferencesModule {
    val Context.dataStore: DataStore<Preferences> by
    preferencesDataStore(name = "wavesync_prefs")

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun providePrefs(dataStore: DataStore<Preferences>): DataStorePreferences {
        return DataStorePreferences(dataStore)
    }

}
