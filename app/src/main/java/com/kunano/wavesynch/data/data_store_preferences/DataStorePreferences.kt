package com.kunano.wavesynch.data.data_store_preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

class DataStorePreferences(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val IS_FIRST_OPENING = booleanPreferencesKey("is_first_opening")
    }

    suspend fun setIsFirstOpening(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_FIRST_OPENING] = value
        }
    }

    suspend fun isFirstOpening(): Boolean {
        val prefs = dataStore.data.first()
        return prefs[Keys.IS_FIRST_OPENING] ?: true
    }
}