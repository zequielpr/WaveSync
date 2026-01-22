@file:OptIn(ExperimentalMaterial3Api::class)

package com.kunano.wavesynch.ui.main_screen

import PermissionHandler
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunano.wavesynch.R
import com.kunano.wavesynch.ui.main_screen.drawer.AppDrawerContent
import com.kunano.wavesynch.ui.main_screen.drawer.DrawerAction
import com.kunano.wavesynch.ui.main_screen.drawer.shareApp
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.theme.AppDimens
import com.kunano.wavesynch.ui.theme.WavesynchTheme
import com.kunano.wavesynch.ui.utils.GoToSettingsDialog
import com.kunano.wavesynch.ui.utils.PermissionRationaleDialog
import com.kunano.wavesynch.ui.utils.UiEvent
import com.kunano.wavesynch.ui.utils.openAppSettings
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SyncWaveMainScreenWithAppBar(
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    navigateTo: (screen: Screen) -> Unit,
    viewModel: MainScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.UIState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    var granted by remember { mutableStateOf(false) }
    var relaunchPermissionHandler by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showPermissionSettings by remember { mutableStateOf(false) }


    ModalNavigationDrawer(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
                AppDrawerContent(
                    appName = "WaveSync",
                    onAction = { action ->
                        // Close drawer first (feels better UX)
                        scope.launch { drawerState.close() }

                        when (action) {
                            DrawerAction.AboutUs -> {
                                navigateTo(Screen.AboutUsScreen)
                            }

                            DrawerAction.PrivacyPolicies -> {
                                navigateTo(Screen.PrivacyPoliciesScreen)
                            }

                            DrawerAction.Help -> {
                                navigateTo(Screen.HelpScreen)
                            }

                            DrawerAction.ShareApp -> {
                                shareApp(
                                    context,
                                    "Check out WaveSync: https://play.google.com/store/apps/details?id=com.your.package"
                                )
                            }

                            DrawerAction.RateApp -> {}
                        }
                    }
                )
            }
        }
    ) {

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = {scope.launch { drawerState.open() }}){
                        Image(painter = painterResource(R.drawable.menu_48px), contentDescription = null)
                    } },

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
                    navigateToJoinRoom = { if (granted) viewModel.joinRoom() },
                    navigateToActiveRoom = { if (granted) navigateTo(Screen.ActiveRoomScreen)}
                )
            }
        }
    }






    if (!granted) {
        Log.d("MainScreen", "Launching permission handler")
        PermissionHandler(
            onAllGranted = {
                Log.d("MainScreen", "Permission granted")
                granted = true
            },
            onNeedUserActionInSettings = {
                Log.d("MainScreen", "User needs to go to settings")
                showPermissionSettings = true
            },
            onShowRationale = {
                showPermissionRationale = true
                Log.d("MainScreen", "Showing permission rationale")
            }
        )
    }

    if (relaunchPermissionHandler) {
        PermissionHandler(
            onAllGranted = {
                Log.d("MainScreen", "Permission granted")
                granted = true
            },
            onNeedUserActionInSettings = { showPermissionSettings = true },
            onShowRationale = { showPermissionRationale = true }
        )
    }


    if (showPermissionRationale) {
        PermissionRationaleDialog(onRetry = {
            relaunchPermissionHandler = true
            showPermissionRationale = false
        }, onCancel = { showPermissionRationale = false })

    } else if (showPermissionSettings) {
        GoToSettingsDialog(onOpenSettings = {
            openAppSettings(context = context)
            showPermissionSettings = false
        }, onCancel = { showPermissionSettings = false })

    }



    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect {
            when (it) {
                is UiEvent.NavigateTo -> navigateTo(it.screen)
                else -> {}
            }

        }

    }




}

@Composable
fun SynchWaveMainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = hiltViewModel(),
    navigateToActiveRoom: () -> Unit, navigateToJoinRoom: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = AppDimens.Padding.horizontal,
                    vertical = AppDimens.Padding.vertical
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(0.1f))

            // Share sound card
            MainActionCard(
                title = stringResource(id = R.string.share_sound),
                // Replace with your real icon
                iconPainter = painterResource(R.drawable.create_room),
                onClick = { viewModel.shareSound(navigateToShareSound = navigateToActiveRoom) }
            )

            Spacer(modifier = Modifier.weight(0.1f))

            // Join room card
            MainActionCard(
                title = stringResource(id = R.string.join_sound_room),
                // Replace with your real icon
                iconPainter = painterResource(R.drawable.join_sound_room),
                onClick = navigateToJoinRoom
            )
            //An ad banner will be added on this section
            Spacer(modifier = Modifier.weight(0.5f))
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

        SyncWaveMainScreenWithAppBar(navigateTo = {})
    }
}
