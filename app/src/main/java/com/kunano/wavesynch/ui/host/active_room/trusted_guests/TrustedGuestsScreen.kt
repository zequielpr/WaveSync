@file:OptIn(ExperimentalMaterial3Api::class)

package com.kunano.wavesynch.ui.host.active_room.trusted_guests

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunano.wavesynch.R
import com.kunano.wavesynch.domain.model.TrustedGuest


@Composable
fun TrustedGuestsViewCompose(
    onBack: () -> Unit,
    viewModel: TrustedGuestsViewModel = hiltViewModel(),
) {

    val uIState by viewModel.uiState.collectAsStateWithLifecycle()
    val disabledAlpha = 0.50f



    // Confirm dialogs
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }
    var confirmDeleteOne by remember { mutableStateOf<TrustedGuest?>(null) }


    if (confirmDeleteAll) {
        ConfirmDeleteDialog(
            title = "Delete all trusted guests?",
            body = "This will remove everyone from your trusted list.",
            confirmText = "Delete all",
            onConfirm = {
                confirmDeleteAll = false
                viewModel.clearSelection()
                viewModel.deleteAllGuests()
            },
            onDismiss = { confirmDeleteAll = false }
        )
    }

    if (confirmDeleteSelected) {
        ConfirmDeleteDialog(
            title = "Delete selected guests?",
            body = "This will remove ${uIState.selectedIds.size} guest(s) from your trusted list.",
            confirmText = "Delete",
            onConfirm = {
                confirmDeleteSelected = false
                val ids = uIState.selectedIds.toList()
                viewModel.clearSelection()
                viewModel.deleteSelectedGuests(ids)
            },
            onDismiss = { confirmDeleteSelected = false }
        )
    }

    confirmDeleteOne?.let { trustedGuest ->
        ConfirmDeleteDialog(
            title = "Delete this trusted guest?",
            body = "They’ll need to be trusted again to rejoin quickly.",
            confirmText = "Delete",
            onConfirm = {
                confirmDeleteOne = null
                viewModel.deleteTrustedGuest(trustedGuest)
            },
            onDismiss = { confirmDeleteOne = null }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    if (uIState.selectionModeActivate) {
                        Text("${uIState.selectedIds.size} selected")
                    } else {
                        Text("Trusted guests")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uIState.selectionModeActivate) viewModel.clearSelection() else onBack()
                    }) {
                        Image(
                            if (uIState.selectionModeActivate) painterResource(R.drawable.close_48px) else painterResource(
                                R.drawable.arrow_back_ios_48px
                            ), contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uIState.selectionModeActivate) {
                        IconButton(
                            onClick = { viewModel.selectAll() },
                            enabled = uIState.trustedGuests.isNotEmpty()
                        ) {
                            Image(
                                alpha = if (uIState.trustedGuests.isEmpty()) disabledAlpha else 1f,
                                painter = if (uIState.isAllSelected) painterResource(id = R.drawable.check_circle_48px) else painterResource(
                                    id = R.drawable.circle_48px
                                ), contentDescription = "Select all"
                            )
                        }

                        IconButton(
                            onClick = { confirmDeleteSelected = true },
                            enabled = uIState.selectedIds.isNotEmpty()
                        ) {
                            Image(
                                alpha = if (uIState.selectedIds.isEmpty()) disabledAlpha else 1f,
                                painter = painterResource(id = R.drawable.delete_48px),
                                contentDescription = "Delete selected"
                            )
                        }
                    } else {

                        IconButton(
                            onClick = { confirmDeleteAll = true },
                            enabled = uIState.trustedGuests.isNotEmpty()
                        ) {
                            Image(
                                alpha = if (uIState.trustedGuests.isEmpty()) disabledAlpha else 1f,
                                painter = painterResource(id = R.drawable.delete_48px),
                                contentDescription = "Delete all"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uIState.trustedGuests.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = uIState.trustedGuests, key = { it.userId }) { guest ->
                    val isSelected = uIState.selectedIds.contains(guest.userId)
                    TrustedGuestRow(
                        guest = guest,
                        selectionMode = uIState.selectionModeActivate,
                        selected = isSelected,
                        onClick = {
                            if (uIState.selectionModeActivate) viewModel.toggleSelectGuest(guest.userId)
                        },
                        onLongPressSelect = { viewModel.onLongPressSelect(guest.userId) },
                        onToggleSelected = { viewModel.toggleSelectGuest(guest.userId) },
                        onDeleteOne = { confirmDeleteOne = guest }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustedGuestRow(
    guest: TrustedGuest,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPressSelect: () -> Unit,
    onToggleSelected: () -> Unit,
    onDeleteOne: () -> Unit,
) {
    // Long-press in pure Compose requires combinedClickable; kept simple here:
    // If you want long-press: use Modifier.combinedClickable(...).
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        modifier = Modifier
            .fillMaxWidth().combinedClickable(
                onLongClick = onLongPressSelect,

                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                IconButton(onClick = onToggleSelected) {
                    if (selected) {
                        Image(
                            painterResource(R.drawable.check_circle_48px),
                            contentDescription = null
                        )
                    } else {
                        // empty checkbox look without extra dependency
                        Image(painterResource(R.drawable.circle_48px), contentDescription = null)
                    }
                }
            }else{
                Image(painterResource(R.drawable.mobile_48px), contentDescription = null)
            }
            Spacer(Modifier.width(6.dp))

            guest.userName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.weight(1f))

            // Delete one-by-one is always available (even outside selection mode)
            IconButton(onClick = onDeleteOne) {
                Image(painterResource(R.drawable.delete_48px), contentDescription = null)
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No trusted guests yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            textAlign = TextAlign.Center,
            text = "Guests you trust will show up here so they can rejoin faster next time.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}