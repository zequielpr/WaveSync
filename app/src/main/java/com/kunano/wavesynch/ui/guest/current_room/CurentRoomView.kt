package com.kunano.wavesynch.ui.guest.current_room

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunano.wavesynch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentRoomCompose(viewModel: CurrentRoomViewModel = hiltViewModel(), onBack: () -> Unit) {
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Image(
                        painter = painterResource(id = R.drawable.arrow_back_ios_48px),
                        contentDescription = "Back"
                    )
                }
            },
            title = {
                UIState.value.roomName?.let {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = stringResource(
                            R.string.connected_to
                        ) + " " + it
                    )
                }
            })
    }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
        ) {
            CurrentRoomCardCompose()
            Spacer(modifier = Modifier.height(300.dp))
            Button(
                modifier = Modifier.width(150.dp),
                shape = RoundedCornerShape(15.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                onClick = viewModel::leaveRoom
            ) {
                Text(
                    text = stringResource(R.string.leave_room),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }

        }
    }
}


@Composable
fun CurrentRoomCardCompose(viewModel: CurrentRoomViewModel = hiltViewModel()) {

    val textColor = MaterialTheme.colorScheme.onSurface
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()
    val trackTitleStyle = MaterialTheme.typography.titleLarge.copy(color = textColor)
    val hostNameStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)


    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        modifier = Modifier
            .width(300.dp)
            .height(300.dp)
            .padding(top = 50.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp)
        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                painter = painterResource(R.drawable.join_sound_room),
                contentDescription = "current room"
            )
            Spacer(modifier = Modifier.weight(0.2f))
            UIState.value.hostName?.let { Text(text = it, style = hostNameStyle) }

            Spacer(modifier = Modifier.weight(1f))
            UIState.value.playingTrackName?.let { Text(text = it, style = trackTitleStyle) }

        }
    }


}

@Preview(showBackground = true)
@Composable
fun CurrentRoomComposePreview() {
    CurrentRoomCompose(onBack = {})
}