package com.kunano.wavesynch.domain.repositories

import com.google.android.gms.cast.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor() {

    private val _session = MutableStateFlow(SessionData())
    val session: StateFlow<SessionData> = _session.asStateFlow()

    fun updateRoomName(name: String?) {
        _session.update { it.copy(roomName = name) }
    }

    fun updateHostName(name: String?) {
        _session.update { it.copy(hostName = name) }
    }

    fun clear() {
        _session.value = SessionData()
    }
}

data class SessionData(
    var roomName: String? = null,
    var hostName: String? = null
)