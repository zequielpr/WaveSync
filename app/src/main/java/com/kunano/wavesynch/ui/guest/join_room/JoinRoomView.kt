package com.kunano.wavesynch.ui.guest.join_room

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
                is UiEvent.NavigateTo -> {
                    navigateTo(event.screen)
                }

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
            CheckIfDeviceIsHost(navigateBack = onBack)
            QrScannerScreen(
                onResult = { qrContent ->
                    val ssid = qrContent.split("\"")[0]
                    val password = qrContent.split("\"")[1]


                    if (ssid.isNotEmpty()) {
                        viewModel.connectToHotspot(ssid, password)
                    }
                }
            )


        }
    }
}


@Composable
fun CheckIfDeviceIsHost(navigateBack: () -> Unit) {
    val viewModel: JoinRoomViewModel = hiltViewModel()
    val isThisDeviceHost = viewModel.checkIfHotspotIsRunning()
    var showDialog by remember { mutableStateOf(isThisDeviceHost) }


    CustomDialogueCompose(
        title = stringResource(R.string.stop_hosting_room),
        text = stringResource(R.string.you_are_hosting_room),
        acceptButtonText = stringResource(R.string.yes),
        onDismiss = {
            navigateBack()
            showDialog = false
        },
        onConfirm = {
            viewModel.finishSessionAsHost()
            showDialog = false
        },
        show = showDialog,
    )


}


@Preview(showBackground = true)
@Composable
fun QrScannerScreenPreview() {

}
