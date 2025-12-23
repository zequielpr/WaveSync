package com.kunano.wavesynch.ui.host.active_room

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunano.wavesynch.R
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.ui.utils.ActiveRoomUiEvent
import com.kunano.wavesynch.ui.utils.CustomBottomSheetCompose
import com.kunano.wavesynch.ui.utils.CustomDialogueCompose
import com.kunano.wavesynch.ui.utils.CustomToggleCompose
import com.kunano.wavesynch.ui.utils.UiEvent
import com.kunano.wavesynch.ui.utils.generateQrBitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun ActiveRoomCompose(viewModel: ActiveRoomViewModel = hiltViewModel(), onBack: () -> Unit) {
    val UIState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState by remember { mutableStateOf(SnackbarHostState()) }
    var askToTrustGuestEvent by remember {
        mutableStateOf<ActiveRoomUiEvent.AskToAcceptGuestRequest?>(
            null
        )
    }

    LaunchedEffect(UIState.guests) {
        Log.d("UI", "Guests: ${UIState.guests.map { it.userId to it.isPlaying }}")
    }
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                is ActiveRoomUiEvent.AskToAcceptGuestRequest -> {
                    askToTrustGuestEvent = it
                    viewModel.setShowAskTrustGuestState(true)
                    Log.d(
                        "ActiveRoomCompose",
                        "AskToTrustGuest: ${askToTrustGuestEvent?.guestName}"
                    )
                }

                is UiEvent.NavigateBack -> TODO()
                is UiEvent.NavigateTo -> TODO()
                is UiEvent.ShowSnackBar -> {
                    snackbarHostState.showSnackbar(it.message)

                }
            }


        }

    }




    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { CustomTopAppBar(onBack = onBack) },
        floatingActionButton = { AudioCaptureRequestCompose() }) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Column(modifier = Modifier.padding(start = 30.dp, end = 30.dp)) {
                //Launch audio capture request and start streaming


                if (UIState.isQRCodeExpanded) {
                    ExpandedQRCodeCompose()
                } else {
                    ShrunkQRCodeCompose()
                }

                GuestsListCompose(UIState.guests)

            }
        }

    }

    // Floating button overlay
   @Composable
   fun FloatingButtonOverlay() {

    }

    CheckIfDeviceIsGuest(navigateBack = onBack)
    AskToStopStreaming()
    AskToEmptyRoom()
    AskToExpelGuest(
        show = UIState.showAskToExpelGuest,
        onConfirm = viewModel::expelGuest,
        onDismiss = {viewModel.setShowAskToExpelGuestState(false, null)}
    )

    //Ask to trust guest and connection request
    if (UIState.showJoinRoomRequest && askToTrustGuestEvent != null) {
        var hostTrustGuest by remember { mutableStateOf(false) }
        Log.d("ActiveRoomCompose", "AskToTrustGuest: ${askToTrustGuestEvent!!.guestName}")
        CustomDialogueCompose(
            title = askToTrustGuestEvent!!.deviceName + ":" + stringResource(R.string.wants_to_join_room),
            content = {
                Column {
                    Text(askToTrustGuestEvent!!.guestName + ": " + stringResource(R.string.wants_to_join_room))
                    CustomToggleCompose(stringResource(R.string.trust_guest)) {
                        hostTrustGuest = it
                    }
                }
            },
            acceptButtonText = stringResource(R.string.accept),
            dismissButtonText = stringResource(R.string.decline),
            onDismiss = {
                askToTrustGuestEvent!!.decision.complete(false)
                viewModel.setShowAskTrustGuestState(false)
            },
            onConfirm = {
                askToTrustGuestEvent!!.decision.complete(true)
                askToTrustGuestEvent!!.guestTrusted.complete(hostTrustGuest)
                viewModel.setShowAskTrustGuestState(false)
            },
            show = UIState.showJoinRoomRequest
        )
    }
}

@Composable
fun CheckIfDeviceIsGuest(navigateBack: () -> Unit) {
    val viewModel: ActiveRoomViewModel = hiltViewModel()
    val isThisDeviceHost = viewModel.checkIfDeviceIsGuest()
    var showDialog by remember { mutableStateOf(isThisDeviceHost) }


    CustomDialogueCompose(
        title = stringResource(R.string.leave_room),
        text = stringResource(R.string.you_are_joined_to_room),
        acceptButtonText = stringResource(R.string.yes),
        onDismiss = {
            navigateBack()
            showDialog = false
        },
        onConfirm = {
            viewModel.stopBeingAGuest()
            showDialog = false
        },
        show = showDialog,
    )


}

@Composable
fun AskToStopStreaming() {
    val viewModel: ActiveRoomViewModel = hiltViewModel()
    val uIState by viewModel.uiState.collectAsStateWithLifecycle()


    CustomDialogueCompose(
        title = stringResource(R.string.stop_streaming),
        text = stringResource(R.string.are_you_sure_you_stop_streaming),
        acceptButtonText = stringResource(R.string.yes),
        onDismiss = {
            viewModel.setShowAskStopStreaming(false)
        },
        onConfirm = { viewModel.stopStreaming()
            viewModel.setShowAskStopStreaming(false)},
        show = uIState.showAskToStopStreaming,
    )
}

@Composable
fun AskToEmptyRoom() {
    val viewModel: ActiveRoomViewModel = hiltViewModel()
    val uIState by viewModel.uiState.collectAsStateWithLifecycle()


    CustomDialogueCompose(
        title = stringResource(R.string.empty_room),
        text = stringResource(R.string.are_you_sure_you_empty_room),
        acceptButtonText = stringResource(R.string.yes),
        onDismiss = {
            viewModel.setShowAskToEmptyRoom(false)
        },
        onConfirm = { viewModel.emptyRoom()
            viewModel.setShowAskToEmptyRoom(false)},
        show = uIState.showAskToEmptyRoom,
    )


}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    viewModel: ActiveRoomViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()
    TopAppBar(navigationIcon = {
        IconButton(onClick = onBack) {
            Image(
                painter = painterResource(id = R.drawable.arrow_back_ios_48px),
                contentDescription = "Back"
            )
        }
    }, actions = {

        IconButton(onClick = { viewModel.setOverFlowMenuExpandedState(!UIState.value.overFlowMenuExpanded) }) {
            Image(
                painter = painterResource(R.drawable.more_vert_48px),
                contentDescription = "More"
            )


        }

        OverFlowMenuCompose()


    }, title = {
        UIState.value.room?.name?.let {
            Text(
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), text = it
            )
        }
    })
}


@Composable
fun OverFlowMenuCompose(
    viewModel: ActiveRoomViewModel = hiltViewModel(),
    textColor: Color = MaterialTheme.colorScheme.onSurface,

    ) {
    val modifier: Modifier = Modifier.size(30.dp)
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()
    var showDeletionDialogue by remember { mutableStateOf(false) }
    val textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)

    //Bottom sheet properties
    var showBottomSheet by remember { mutableStateOf(false) }
    val buttonColor = MaterialTheme.colorScheme.primary
    val titleStyle: TextStyle =
        MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    var textFieldValue: String by remember { mutableStateOf("") }

    val contentModifier: Modifier = Modifier.fillMaxWidth()
    CustomBottomSheetCompose(conetent = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.update_room_name), style = titleStyle)
            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.secondary),
                textStyle = textStyle,
                modifier = contentModifier,
                onValueChange = { it -> textFieldValue = it },
                value = textFieldValue,
                label = { Text(text = stringResource(R.string.new_room_name)) },
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                onClick = {
                    UIState.value.room?.id?.let { id ->
                        viewModel.editRoomName(id, textFieldValue)
                    }
                    showBottomSheet = false
                }) {
                Text(text = stringResource(R.string.update), style = textStyle)
            }
            Spacer(modifier = Modifier.height(20.dp))


        }
    }, showSheet = showBottomSheet, onDismiss = { showBottomSheet = false })





    CustomDialogueCompose(
        title = stringResource(R.string.delete_room),
        text = stringResource(R.string.ask_to_delete_room),
        onDismiss = { showDeletionDialogue = false },
        onConfirm = { viewModel.deleteRoom(UIState.value.room?.id ?: 0) },
        show = showDeletionDialogue
    )


    DropdownMenu(
        expanded = UIState.value.overFlowMenuExpanded,
        onDismissRequest = { viewModel.setOverFlowMenuExpandedState(false) },

        ) {
        DropdownMenuItem(

            leadingIcon = {
                Image(
                    modifier = modifier,
                    painter = painterResource(id = R.drawable.unarchive_48px),
                    contentDescription = "Empty room"
                )
            }, text = {
                Text(
                    text = stringResource(R.string.empty_room), style = textStyle
                )
            }, onClick = {
                UIState.value.room?.id?.let { viewModel.setShowAskToEmptyRoom(true) }
                viewModel.setOverFlowMenuExpandedState(false)
            })

        DropdownMenuItem(leadingIcon = {
            Image(
                modifier = modifier,
                painter = painterResource(id = R.drawable.edit_48px),
                contentDescription = "Edit room name"
            )
        }, text = {
            Text(
                text = stringResource(R.string.edit_room_name), style = textStyle
            )
        }, onClick = {
            UIState.value.room?.id?.let { showBottomSheet = true }
            viewModel.setOverFlowMenuExpandedState(false)
        })

        DropdownMenuItem(leadingIcon = {
            Image(
                modifier = modifier,
                painter = painterResource(id = R.drawable.delete_48px),
                contentDescription = "Delete room"
            )
        }, text = {
            Text(
                text = stringResource(R.string.delete_room), style = textStyle
            )
        }, onClick = {
            UIState.value.room?.id?.let { showDeletionDialogue = true }
            viewModel.setOverFlowMenuExpandedState(false)
        })

    }

}


@Composable
fun ExpandedQRCodeCompose(
    viewModel: ActiveRoomViewModel = hiltViewModel(),
    cardColor: Color = MaterialTheme.colorScheme.secondary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val textSyle: TextStyle = MaterialTheme.typography.titleLarge.copy(color = textColor)
    val cardModifier: Modifier = Modifier
        .fillMaxWidth()
        .height(420.dp)
        .padding(top = 60.dp)
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()

    Card(
        modifier = cardModifier.clickable(onClick = {}),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            IconButton(onClick = { viewModel.setIsQRCodeExpandedState(!UIState.value.isQRCodeExpanded) }) {
                Image(
                    painter = painterResource(R.drawable.hide_48px), contentDescription = "Hide"
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = 20.dp, start = 20.dp, end = 20.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (UIState.value.hotspotInfo != null) {
                QrCode(
                    password = UIState.value.hotspotInfo!!.password,
                    ssid = UIState.value.hotspotInfo!!.ssid
                )
            } else {
                Text("Loading qr code..")
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 30.dp),
                text = stringResource(R.string.scan_qr_code_to_connect),
                style = textSyle
            )
        }

    }
}


@Composable
fun QrCode(
    password: String,
    ssid: String,
    modifier: Modifier = Modifier,
    size: Int = 200,
) {
    val qrContent = remember(password, ssid) { password + ssid }

    val qrBitmap = remember(qrContent) {
        generateQrBitmap(qrContent, size)
    }

    Image(
        bitmap = qrBitmap.asImageBitmap(),
        contentDescription = "Host QR code",
        modifier = modifier.size(size.dp)
    )
}

@Composable
fun ShrunkQRCodeCompose(

    viewModel: ActiveRoomViewModel = hiltViewModel(),
    cardColor: Color = MaterialTheme.colorScheme.secondary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val textSyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)
    val cardModifier: Modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .padding(top = 60.dp)
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()

    Card(
        modifier = cardModifier.clickable(onClick = {}),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            Image(
                modifier = Modifier
                    .size(50.dp)
                    .padding(0.dp),
                alignment = Alignment.Center,
                painter = painterResource(R.drawable.qr_code_test),
                contentDescription = "QR code"
            )

            Text(
                modifier = Modifier.padding(start = 20.dp),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.scan_qr_code_to_connect),
                style = textSyle
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { viewModel.setIsQRCodeExpandedState(!UIState.value.isQRCodeExpanded) },
            ) {
                Image(
                    painter = painterResource(R.drawable.open_in_full_48px),
                    contentDescription = "Open fully"
                )
            }
        }

    }
}

@Composable
fun GuestsListCompose(
    guestsList: List<Guest> = emptyList(),
) {

    LazyColumn(

        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
    ) {

        items(guestsList, key = { it.userId }) { guest ->
            Log.d("ActiveRoomCompose", "GuestsListCompose: $guest")
            GuestItem(guest = guest)
        }
    }
}

@Composable
fun GuestItem(
    guest: Guest,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    viewModel: ActiveRoomViewModel = hiltViewModel(),
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.mobile_48px), // Placeholder icon
                contentDescription = "Guest Icon", modifier = Modifier.size(40.dp)
            )
            Text(
                text = guest.deviceName,
                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                modifier = Modifier.padding(start = 20.dp),
                onClick = {
                    if (guest.isPlaying) {
                        viewModel.pauseGuest(guestId = guest.userId)
                    } else {
                        viewModel.playGuest(guestId = guest.userId)
                    }
                }) {
                if (guest.isPlaying) {
                    Image(
                        painter = painterResource(id = R.drawable.pause_48px),
                        contentDescription = "Pause"
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.play_arrow_48px),
                        contentDescription = "play"
                    )
                }

            }

            Spacer(modifier = Modifier.weight(0.05f))
            IconButton(
                modifier = Modifier.padding(start = 20.dp),
                onClick = { viewModel.setShowAskToExpelGuestState(true, guest) }) {
                Image(
                    painter = painterResource(id = R.drawable.output_48px),
                    contentDescription = "Expel"
                )
            }
        }
    }
}

@Composable
fun AskToExpelGuest(show: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    CustomDialogueCompose(
        title = stringResource(R.string.expel),
        text = stringResource(R.string.are_you_sure_to_expel),
        acceptButtonText = stringResource(R.string.yes),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        show = show,
    )


}


@Preview(showBackground = true)
@Composable
fun ActiveRoomComposePreview() {
    ActiveRoomCompose(onBack = {})
}

