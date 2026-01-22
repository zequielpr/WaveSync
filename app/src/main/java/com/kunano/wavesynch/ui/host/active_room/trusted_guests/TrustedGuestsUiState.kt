package com.kunano.wavesynch.ui.host.active_room.trusted_guests

import com.kunano.wavesynch.domain.model.TrustedGuest

data class TrustedGuestsUiState(
    val trustedGuests: List<TrustedGuest> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val selectionModeActivate: Boolean = false,
    val isAllSelected: Boolean = false,
)
