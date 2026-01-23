package com.kunano.wavesynch.ui.guest.join_room

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunano.wavesynch.R
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.utils.ConnectingToHostScreen
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
    val uIState by viewModel.uiState.collectAsStateWithLifecycle()
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
        containerColor = MaterialTheme.colorScheme.surface,
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
                    Text(
                        text = stringResource(R.string.scan_host_qr_code),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                })
        }) { it ->
        Box(
            modifier = Modifier
                .padding(it)
        ) {
            CheckIfDeviceIsHost(navigateBack = onBack)
            if (!uIState.isThisDeviceHost && !uIState.waitingForHostAnswer && !uIState.isConnectingToHotspot) {
                QrScannerScreen(
                    navigateBack = onBack,
                    onResult = { qrContent ->
                        Log.d("JoinRoomViewCompose", "QrScannerScreen: $qrContent")
                        val hostIpAddress = qrContent

                        if (hostIpAddress.isNotEmpty()) {
                            viewModel.connectToHostOverLocalWifi(hostIpAddress)
                        }
                    }
                )
            }

            //Connecting to host screen
            ConnectingToHostScreen(visible = uIState.isConnectingToHotspot)


            WaitingForHostScreen(
                onCancel = { viewModel.setShowCancelRequestDialog(true) },
                visible = uIState.waitingForHostAnswer
            )

            CustomDialogueCompose(
                title = stringResource(R.string.cancel_request),
                text = stringResource(R.string.are_you_sure_you_want_to_cancel_request),
                onDismiss = { viewModel.setShowCancelRequestDialog(false) },
                onConfirm = viewModel::cancelJoinRoomRequest,
                show = uIState.showCancelRequestDialog
            )


        }
    }
}

@Composable
fun WaitingForHostScreen(
    onCancel: () -> Unit,
    visible: Boolean = true,
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surface)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(24.dp)
        ) {

            CircularProgressIndicator(
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = stringResource(R.string.waiting_for_host),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(R.string.youll_join_automatically_when_host_answers),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = onCancel
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WaitingForHostScreenPreview() {
    WaitingForHostScreen(
        onCancel = {}
    )
}


@Composable
fun CheckIfDeviceIsHost(navigateBack: () -> Unit) {
    val viewModel: JoinRoomViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(uiState.isThisDeviceHost) }


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
