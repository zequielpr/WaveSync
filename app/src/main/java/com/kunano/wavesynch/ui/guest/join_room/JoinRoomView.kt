package com.kunano.wavesynch.ui.guest.join_room

import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunano.wavesynch.R
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.theme.AppDimens
import com.kunano.wavesynch.ui.utils.CustomDialogueCompose
import com.kunano.wavesynch.ui.utils.QrScannerScreen
import com.kunano.wavesynch.ui.utils.UiEvent


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomViewCompose(
    viewModel: JoinRoomViewModel = hiltViewModel(),
    onBack: () -> Unit,
    navigateTo: (screen: Screen) -> Unit,
) {
    val UIState = viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHostState by remember { mutableStateOf(SnackbarHostState()) }


    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            Log.d("JoinRoomViewCompose", "LaunchedEffect: $event")
            when (event) {
                is UiEvent.ShowSnackBar -> snackBarHostState.showSnackbar(event.message)
                is UiEvent.NavigateBack -> onBack()
                is UiEvent.NavigateTo -> {navigateTo(event.screen)}
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
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
                    if (UIState.value.discoveringPeers) {
                        Text(text = stringResource(R.string.searching_for_hosts))
                    } else {
                        Text(text = "Available hosts")
                    }
                })
        }) { it ->
        Box(
            modifier = Modifier
                .padding(it)
                .padding(start = AppDimens.Padding.left, end = AppDimens.Padding.right)
        ) {

            QrScannerScreen(
                onResult = { qrContent ->
                    // Assuming format: WIFI:S:MySSID;T:WPA;P:MyPassword;;
                    val ssid = qrContent.split("\"")[0]
                    val password = qrContent.split("\"")[1]


                    if (ssid.isNotEmpty() ) {
                        viewModel.connectToHotspot(ssid, password)
                    }
                    
                    Log.d("JoinRoomViewCompose", "QrScannerScreen results: $qrContent")
                }
            )


        }
    }
}



@Preview(showBackground = true)
@Composable
fun QrScannerScreenPreview() {

}
