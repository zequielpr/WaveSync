package com.kunano.wavesynch

import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporter {
    private val c: FirebaseCrashlytics get() = FirebaseCrashlytics.getInstance()

    fun log(msg: String) {
        c.log(msg)
    }

    fun set(key: String, value: String) {
        c.setCustomKey(key, value)
    }

    fun record(e: Throwable, msg: String? = null) {
        if (msg != null) c.log(msg)
        c.recordException(e)
    }
}