package com.kunano.wavesynch.ui.host.active_room

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunano.wavesynch.R
import com.kunano.wavesynch.domain.model.Guest
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.theme.AppDimens
import com.kunano.wavesynch.ui.utils.ActiveRoomUiEvent
import com.kunano.wavesynch.ui.utils.CustomBottomSheetCompose
import com.kunano.wavesynch.ui.utils.CustomDialogueCompose
import com.kunano.wavesynch.ui.utils.CustomDropDownMenuItem
import com.kunano.wavesynch.ui.utils.CustomToggleCompose
import com.kunano.wavesynch.ui.utils.UiEvent
import com.kunano.wavesynch.ui.utils.generateQrBitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun ActiveRoomCompose(
    viewModel: ActiveRoomViewModel = hiltViewModel(),
    onBack: () -> Unit,
    navigateTo: (screen: Screen) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val uIState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState by remember { mutableStateOf(SnackbarHostState()) }
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }

    val textColor: Color = MaterialTheme.colorScheme.onSurface
    var showDeletionDialogue by remember { mutableStateOf(false) }
    val textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)


    //Bottom sheet properties
    var showBottomSheet by remember { mutableStateOf(false) }
    val buttonColor = MaterialTheme.colorScheme.primary
    val titleStyle: TextStyle =
        MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    var textFieldValue: String by remember { mutableStateOf("") }

    val contentModifier: Modifier = Modifier.fillMaxWidth()

    var askToTrustGuestEvent by remember {
        mutableStateOf<ActiveRoomUiEvent.AskToAcceptGuestRequest?>(
            null
        )
    }





    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CustomTopAppBar(onBack = onBack, action = { it ->
                when (it) {
                    DropDownMenuActions.DeleteRoom -> {
                        showDeletionDialogue = true
                    }

                    DropDownMenuActions.EditRoomName -> {
                        showBottomSheet = true
                    }

                    DropDownMenuActions.EmptyRoom -> {
                        viewModel.setShowAskToEmptyRoom(true)
                    }

                    DropDownMenuActions.GoToTrustedUsers -> {
                        uIState.room?.id?.let { roomId ->
                            scope.launch {
                                viewModel.setOverFlowMenuExpandedState(false)
                                delay(80)
                                navigateTo(Screen.TrustedUsersScreen(roomId))
                            }

                        }

                    }
                }
            })
        },
        floatingActionButton = { AudioCaptureRequestCompose() }) {
        Box(

            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(it)
                .fillMaxWidth()
        ) {

            Column(
                modifier = Modifier
                    .padding(
                        horizontal = AppDimens.Padding.horizontal,
                        vertical = AppDimens.Padding.vertical
                    )
                    .fillMaxHeight()
            ) {
                //Launch audio capture request and start streaming

                QrCardCompose()

                GuestsListCompose(uIState.guests)

            }
        }

    }




    LaunchedEffect(uIState.guests) {
        Log.d("UI", "Guests: ${uIState.guests.map { it.userId to it.isPlaying }}")
    }
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                is ActiveRoomUiEvent.AskToAcceptGuestRequest -> {
                    askToTrustGuestEvent = it
                    viewModel.setShowAskTrustGuestState(true)
                    Log.d(
                        "ActiveRoomCompose", "AskToTrustGuest: ${askToTrustGuestEvent?.guestName}"
                    )
                }

                is UiEvent.NavigateBack -> onBack()
                is UiEvent.ShowSnackBar -> {
                    snackbarHostState.showSnackbar(it.message)

                }

                is UiEvent.NavigateTo -> navigateTo(it.screen)

                is UiEvent.ShowDialog -> {
                    dialogTitle = it.title
                    dialogMessage = it.message

                }
            }


        }

    }


    //Show dialog
    if (dialogTitle != null && dialogMessage != null) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = {
                dialogTitle = null
                dialogMessage = null
            },
            title = {
                Text(
                    dialogTitle!!,
                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            },
            text = { Text(dialogMessage!!, style = textStyle) },
            confirmButton = {
                TextButton(onClick = {
                    dialogTitle = null
                    dialogMessage = null
                }) {
                    Text(
                        text = stringResource(R.string.ok),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }
            },
        )
    }


    //Edit room name
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
                    uIState.room?.id?.let { id ->
                        viewModel.editRoomName(id, textFieldValue)
                    }
                    showBottomSheet = false
                }) {
                Text(
                    text = stringResource(R.string.update),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.surface)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))


        }
    }, showSheet = showBottomSheet, onDismiss = { showBottomSheet = false })

    CustomDialogueCompose(
        title = stringResource(R.string.delete_room),
        text = stringResource(R.string.ask_to_delete_room),
        onDismiss = { showDeletionDialogue = false },
        onConfirm = {
            viewModel.deleteRoom(uIState.room?.id ?: 0)
            showDeletionDialogue = false
        },
        show = showDeletionDialogue
    )



    AskToStopStreaming()
    AskToEmptyRoom()
    AskToExpelGuest(
        show = uIState.showAskToExpelGuest,
        onConfirm = viewModel::expelGuest,
        onDismiss = { viewModel.setShowAskToExpelGuestState(false, null) })

    //Ask to trust guest and connection request
    if (uIState.showJoinRoomRequest && askToTrustGuestEvent != null) {
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
            show = uIState.showJoinRoomRequest
        )
    }
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
        onConfirm = {
            viewModel.stopStreaming()
            viewModel.setShowAskStopStreaming(false)
        },
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
        onConfirm = {
            viewModel.emptyRoom()
            viewModel.setShowAskToEmptyRoom(false)
        },
        show = uIState.showAskToEmptyRoom,
    )


}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    viewModel: ActiveRoomViewModel = hiltViewModel(),
    onBack: () -> Unit,
    action: (action: DropDownMenuActions) -> Unit = { },
) {
    val uIState = viewModel.uiState.collectAsStateWithLifecycle()
    TopAppBar(
        colors = TopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ), navigationIcon = {
            IconButton(onClick = onBack) {
                Image(
                    painter = painterResource(id = R.drawable.arrow_back_ios_48px),
                    contentDescription = "Back"
                )
            }
        }, actions = {

            IconButton(onClick = { viewModel.setOverFlowMenuExpandedState(!uIState.value.overFlowMenuExpanded) }) {
                Image(
                    painter = painterResource(R.drawable.more_vert_48px),
                    contentDescription = "More"
                )


            }

            OverFlowMenuCompose(action = action)


        }, title = {
            uIState.value.room?.name?.let {
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
    action: (action: DropDownMenuActions) -> Unit = { },
) {
    val modifier: Modifier = Modifier.size(30.dp)
    val uIState by viewModel.uiState.collectAsStateWithLifecycle()
    val textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)



    DropdownMenu(
        containerColor = MaterialTheme.colorScheme.surface,
        expanded = uIState.overFlowMenuExpanded,
        onDismissRequest = { viewModel.setOverFlowMenuExpandedState(false) },

        ) {

        CustomDropDownMenuItem(
            title = stringResource(R.string.empty_room),
            onClick = { action(DropDownMenuActions.EmptyRoom) },
            modifier = modifier,
            painter = painterResource(id = R.drawable.unarchive_48px),
            textStyle = textStyle
        )

        CustomDropDownMenuItem(
            title = stringResource(R.string.edit_room_name),
            onClick = { action(DropDownMenuActions.EditRoomName) },
            modifier = modifier,
            painter = painterResource(id = R.drawable.edit_48px),
            textStyle = textStyle
        )

        CustomDropDownMenuItem(
            title = stringResource(R.string.delete_room),
            onClick = { action(DropDownMenuActions.DeleteRoom) },
            modifier = modifier,
            painter = painterResource(id = R.drawable.delete_48px),
            textStyle = textStyle
        )
        CustomDropDownMenuItem(
            title = stringResource(R.string.trusted_guests),
            onClick = { action(DropDownMenuActions.GoToTrustedUsers) },
            modifier = modifier,
            painter = painterResource(id = R.drawable.handshake_48px),
            textStyle = textStyle
        )


    }

}


@Composable
fun QrCode(
    hostIp: String,
    modifier: Modifier = Modifier,
    size: Int = 200,
) {
    val qrContent = remember(hostIp) { hostIp }
    val qrBackgroundColor = MaterialTheme.colorScheme.surface
    val qrBitmap = remember(qrContent) {
        generateQrBitmap(qrContent, size, bgColor = qrBackgroundColor)
    }


    Image(
        bitmap = qrBitmap.asImageBitmap(),
        contentDescription = "Host QR code",
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
    )
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

        items(guestsList) { guest ->
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
                modifier = Modifier.padding(start = 20.dp), onClick = {
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
fun QrCardCompose(
    viewModel: ActiveRoomViewModel = hiltViewModel(),
    cardColor: Color = MaterialTheme.colorScheme.secondary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expanded = uiState.isQRCodeExpanded
    //val ssid = uiState.hotspotInfo?.ssid ?: ""
    //val password = uiState.hotspotInfo?.password ?: ""

    val hostIp = uiState.hostIp ?: ""


    val animSpec: TweenSpec<Dp> = tween<Dp>(durationMillis = 260, easing = FastOutSlowInEasing)

    val cardHeight by animateDpAsState(
        targetValue = if (expanded) 420.dp else 180.dp,
        animationSpec = animSpec,
        label = "cardHeight"
    )

    val columnPadding by animateDpAsState(
        targetValue = if (expanded) 0.dp else 20.dp,
        animationSpec = animSpec,
        label = "columnPadding"
    )

    val qrSize by animateDpAsState(
        targetValue = if (expanded) 200.dp else 80.dp, animationSpec = animSpec, label = "qrSize"
    )


    val textStyleBig: TextStyle = MaterialTheme.typography.titleLarge.copy(color = textColor)
    val textStyleSmall: TextStyle = MaterialTheme.typography.labelSmall.copy(color = textColor)


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .padding(top = 60.dp)
            .clickable { viewModel.setIsQRCodeExpandedState(!expanded) },
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .padding(columnPadding)
        ) {
            AnimatedVisibility(
                visible = !expanded,
                enter = fadeIn(tween(120)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(220))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hostIp.isNotEmpty()) {
                        QrCode(hostIp = hostIp, size = qrSize.value.toInt())
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    }

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        textAlign = TextAlign.Start,
                        text = stringResource(R.string.scan_qr_code_to_connect),
                        style = textStyleSmall
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = { viewModel.setIsQRCodeExpandedState(true) }) {
                        Image(
                            painter = painterResource(R.drawable.open_in_full_48px),
                            contentDescription = "Expand"
                        )
                    }
                }
            }

            // EXPANDED CONTENT (only exists when expanded)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(120)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(220))
            ) {

                Box() {
                    IconButton(
                        onClick = { viewModel.setIsQRCodeExpandedState(false) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Image(
                            painter = painterResource(
                                R.drawable.hide_48px
                            ), contentDescription = "Hide"
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp, start = 20.dp, end = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (hostIp.isNotEmpty()) {
                            QrCode(hostIp = hostIp)
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Text(
                                stringResource(R.string.generating_qr_code),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        Text(
                            modifier = Modifier.padding(top = 30.dp),
                            textAlign = TextAlign.Center,
                            text = stringResource(R.string.scan_qr_code_to_connect),
                            style = textStyleBig
                        )
                    }
                }
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

