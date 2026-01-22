package com.kunano.wavesynch.ui.host.active_room.trusted_guests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.domain.model.TrustedGuest
import com.kunano.wavesynch.domain.usecase.host.HostUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrustedGuestsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val hostUseCases: HostUseCases,
) : ViewModel() {

    private val roomId: Long = checkNotNull(savedStateHandle["roomId"])

    private val _uiStateFlow = MutableStateFlow(TrustedGuestsUiState())
    val uiState = _uiStateFlow.asStateFlow()


    init {
        collectTrustedUsers()
    }

    fun toggleSelectGuest(guestId: String) {
        val selectedIds = _uiStateFlow.value.selectedIds.toMutableSet()
        if (selectedIds.contains(guestId)) {
            selectedIds.remove(guestId)
        } else {
            selectedIds.add(guestId)
        }
        _uiStateFlow.update { it.copy(selectedIds = selectedIds) }
    }

    fun onLongPressSelect(guestId: String) {
        if (_uiStateFlow.value.selectedIds.isEmpty()) {
            _uiStateFlow.update {
                it.copy(
                    selectedIds = setOf(guestId),
                    selectionModeActivate = true
                )
            }
        }
    }

    fun selectAll() {
        if (_uiStateFlow.value.trustedGuests.isNotEmpty()) {
            if (!_uiStateFlow.value.isAllSelected) {
                val selectedIds = _uiStateFlow.value.trustedGuests.map { it.userId }.toSet()
                _uiStateFlow.update { it.copy(selectedIds = selectedIds) }
                updateIsAllSelected(!_uiStateFlow.value.isAllSelected)
            } else {
                clearSelection()
                updateIsAllSelected(!_uiStateFlow.value.isAllSelected)
            }


        }

    }

    fun updateIsAllSelected(isAllSelected: Boolean) {
        _uiStateFlow.update { it.copy(isAllSelected = isAllSelected) }
    }

    fun clearSelection() {
        _uiStateFlow.update { it.copy(selectedIds = emptySet(), selectionModeActivate = false) }

    }

    fun deleteAllGuests(){
        viewModelScope.launch {
            _uiStateFlow.value.trustedGuests.forEach {
                hostUseCases.deleteTrustedGuest(it)
            }
        }
    }

    fun deleteSelectedGuests(selectedIds: List<String>) {
        viewModelScope.launch {
            selectedIds.forEach {
                hostUseCases.deleteTrustedGuest(
                    TrustedGuest(
                        userId = it,
                        userName = "",
                        deviceName = "TODO()",
                        isConnected = false
                    )
                )

            }
        }
    }

    fun deleteTrustedGuest(trustedGuest: TrustedGuest) {
        viewModelScope.launch {
            val result = hostUseCases.deleteTrustedGuest(trustedGuest)

            if (result > 0) {
                clearSelection()
            } else {

            }
        }
    }


    fun collectTrustedUsers() {
        viewModelScope.launch {
            hostUseCases.observerRoomGuests(roomId).collect { guests ->
                _uiStateFlow.value = _uiStateFlow.value.copy(trustedGuests = guests)
            }
        }
    }

}



