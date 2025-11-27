package com.kunano.wavesynch.ui.host.active_room

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunano.wavesynch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRoomCompose(viewModel: ActiveRoomViewModel = hiltViewModel(), onBack: () -> Unit) {
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { CustomTopAppBar(onBack = onBack) }) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.padding(start = 30.dp, end = 30.dp)) {

                if (UIState.value.isQRCodeExpanded) {
                    ExpandedQRCodeCompose()
                } else {
                    ShrunkQRCodeCompose()
                }
                GuestsListCompose()

            }
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    viewModel: ActiveRoomViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()
    TopAppBar(
        navigationIcon = {IconButton(onClick = onBack) {Image(painter = painterResource(id = R.drawable.arrow_back_ios_48px), contentDescription = "Back") }},
        actions = {

            IconButton(onClick = { viewModel.setIsPlayingInHostState(!UIState.value.playingInHost) }) {

                if (UIState.value.playingInHost) {
                    Image(
                        painter = painterResource(id = R.drawable.volume_up_48px),
                        contentDescription = "Pause"
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.volume_off_48px),
                        contentDescription = "Play"
                    )
                }


            }

            IconButton(onClick = { viewModel.setOverFlowMenuExpandedState(!UIState.value.overFlowMenuExpanded) }) {
                Image(
                    painter = painterResource(R.drawable.more_vert_48px),
                    contentDescription = "More"
                )


            }

            OverFlowMenuCompose()


        },
        title = {
            UIState.value.roomName?.let {
                Text(
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    text = it
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
    val textSyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()

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
            },
            text = {
                Text(
                    text = stringResource(R.string.empty_room),
                    style = textSyle
                )
            },
            onClick = {
                UIState.value.roomId?.let { viewModel.emptyRoom(it) }
                viewModel.setOverFlowMenuExpandedState(false)})

        DropdownMenuItem(
            leadingIcon = {
                Image(
                    modifier = modifier,
                    painter = painterResource(id = R.drawable.edit_48px),
                    contentDescription = "Edit room name"
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.edit_room_name),
                    style = textSyle
                )
            },
            onClick = { UIState.value.roomId?.let { viewModel.editRoomName(it) }
                viewModel.setOverFlowMenuExpandedState(false)})

        DropdownMenuItem(
            leadingIcon = {
                Image(
                    modifier = modifier,
                    painter = painterResource(id = R.drawable.delete_48px),
                    contentDescription = "Delete room"
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_room),
                    style = textSyle
                )
            },
            onClick = { UIState.value.roomId?.let { viewModel.deleteRoom(it) }
                viewModel.setOverFlowMenuExpandedState(false)})

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
        modifier = cardModifier
            .clickable(onClick = {}),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            IconButton(onClick = { viewModel.setIsQRCodeExpandedState(!UIState.value.isQRCodeExpanded) }) {
                Image(
                    painter = painterResource(R.drawable.hide_48px),
                    contentDescription = "Hide"
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = 20.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                modifier = Modifier
                    .size(200.dp)
                    .padding(0.dp),
                alignment = Alignment.Center,
                painter = painterResource(R.drawable.qr_code_test), contentDescription = "QR code"
            )

            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 30.dp),
                text = stringResource(R.string.scan_qr_code_to_connect), style = textSyle
            )
        }

    }
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
        modifier = cardModifier
            .clickable(onClick = {}),
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
                painter = painterResource(R.drawable.qr_code_test), contentDescription = "QR code"
            )

            Text(
                modifier = Modifier.padding(start = 20.dp),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.scan_qr_code_to_connect), style = textSyle
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
fun GuestsListCompose(viewModel: ActiveRoomViewModel = hiltViewModel()) {
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
    ) {
        items(UIState.value.guests) { guest ->
            GuestItem(guestInfo = guest)
        }
    }
}

@Composable
fun GuestItem(
    guestInfo: GuestInfo,
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
                contentDescription = "Guest Icon",
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = guestInfo.name,
                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton( modifier = Modifier.padding(start = 20.dp),
                onClick = {viewModel.expelGuest(guestId = guestInfo.id)}) {
                Image(
                    painter = painterResource(id = R.drawable.output_48px),
                    contentDescription = "Close"
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ActiveRoomComposePreview() {
    ActiveRoomCompose(onBack = {})
}


@Preview(showBackground = true)
@Composable
fun ShrunkQRCodeComposePreview() {
    ShrunkQRCodeCompose()
}

