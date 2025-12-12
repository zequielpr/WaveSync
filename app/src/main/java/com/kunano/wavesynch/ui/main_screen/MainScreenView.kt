@file:OptIn(ExperimentalMaterial3Api::class)

package com.kunano.wavesynch.ui.main_screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunano.wavesynch.R
import com.kunano.wavesynch.ui.theme.WavesynchTheme


@Composable
fun SyncWaveMainScreenWithAppBar(
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    navigateToActiveRoom: () -> Unit, navigateToJoinRoom: () -> Unit,
) {
    var granted by remember { mutableStateOf(false) }

    if (!granted) {
        Log.d("MainScreen", "Launching permission handler")
        PermissionHandler(
            onAllGranted = {
                Log.d("MainScreen", "Permission granted")
                granted = true }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(

                title = {
                    Text(
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(color = textColor)
                    )
                },
            )
        }
    )
    {
        Box(modifier = Modifier.padding(it)) {
            SynchWaveMainScreen(
                navigateToJoinRoom = {if (granted) navigateToJoinRoom()},
                navigateToActiveRoom = {if (granted) navigateToActiveRoom() else
                Log.d("MainScreen", "Permission not granted")}
            )
        }
    }

}

@Composable
fun SynchWaveMainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = hiltViewModel(),
    navigateToActiveRoom: () -> Unit, navigateToJoinRoom: () -> Unit,
) {
    val UIState = viewModel.UIState.collectAsStateWithLifecycle()
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            // Share sound card
            MainActionCard(
                title = stringResource(id = R.string.share_sound),
                // Replace with your real icon
                iconPainter = painterResource(R.drawable.create_room),
                onClick = { viewModel.shareSound(navigateToShareSound = navigateToActiveRoom) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Join room card
            MainActionCard(
                title = stringResource(id = R.string.join_sound_room),
                // Replace with your real icon
                iconPainter = painterResource(R.drawable.join_sound_room),
                onClick = navigateToJoinRoom
            )
        }
    }
}

@Composable
private fun MainActionCard(
    title: String,
    iconPainter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColors: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.secondary,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .width(300.dp)
            .height(300.dp) // Increased height to fit everything better
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = iconPainter,
                contentDescription = title,
                modifier = Modifier
                    .size(200.dp) // Explicit size for the icon
                    .padding(bottom = 16.dp), // Padding instead of spacer if desired, or keep spacer
                contentScale = ContentScale.Fit
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColors
                )
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun WavesynchThemePreview() {
    WavesynchTheme(darkTheme = false) {

        SyncWaveMainScreenWithAppBar(navigateToActiveRoom = {}, navigateToJoinRoom = {})
    }
}
