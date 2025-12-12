package com.kunano.wavesynch

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

object AppIdProvider {
    const val APP_ID = "com.kunano.wavesynch"


    private const val KEY = "user_uuid"

    fun getUserId(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY, null)
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        prefs.edit { putString(KEY, newId) }
        return newId
    }
}