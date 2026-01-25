@file:OptIn(ExperimentalMaterial3Api::class)

package com.kunano.wavesynch.ui.main_screen

import PermissionHandler
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {

                AppDrawerContent(

                    appName = stringResource(R.string.app_name),
                    onAction = { action ->
                        scope.launch { drawerState.close() }
                        when (action) {
                            DrawerAction.AboutUs -> navigateTo(Screen.AboutUsScreen)
                            DrawerAction.PrivacyPolicies -> navigateTo(Screen.PrivacyPoliciesScreen)
                            DrawerAction.Help -> navigateTo(Screen.HelpScreen)
                            DrawerAction.ShareApp ->
                                shareApp(context, "Check out WaveSync on Google Play")

                            DrawerAction.RateApp -> {}
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Image(
                                painter = painterResource(R.drawable.menu_48px),
                                contentDescription = null
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(color = textColor)
                        )
                    },
                    actions = {
                        // future icons go here
                    }
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                SynchWaveMainScreen(
                    navigateToJoinRoom = { if (granted) viewModel.joinRoom() },
                    navigateToActiveRoom = { if (granted) navigateTo(Screen.ActiveRoomScreen) }
                )
            }
        }
    }

    if (!granted) {
        PermissionHandler(
            onAllGranted = { granted = true },
            onNeedUserActionInSettings = { showPermissionSettings = true },
            onShowRationale = { showPermissionRationale = true }
        )
    }

    if (relaunchPermissionHandler) {
        PermissionHandler(
            onAllGranted = { granted = true },
            onNeedUserActionInSettings = { showPermissionSettings = true },
            onShowRationale = { showPermissionRationale = true }
        )
    }

    if (showPermissionRationale) {
        PermissionRationaleDialog(
            onRetry = {
                relaunchPermissionHandler = true
                showPermissionRationale = false
            },
            onCancel = { showPermissionRationale = false }
        )
    } else if (showPermissionSettings) {
        GoToSettingsDialog(
            onOpenSettings = {
                openAppSettings(context)
                showPermissionSettings = false
            },
            onCancel = { showPermissionSettings = false }
        )
    }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect {
            if (it is UiEvent.NavigateTo) navigateTo(it.screen)
        }
    }
}

@Composable
fun SynchWaveMainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = hiltViewModel(),
    navigateToActiveRoom: () -> Unit,
    navigateToJoinRoom: () -> Unit,
) {
    val config = LocalConfiguration.current
    val isLandscape =
        config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val cardSize = 250.dp
    val spacing = 16.dp

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = AppDimens.Padding.horizontal,
                        vertical = AppDimens.Padding.vertical
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainActionCard(
                    modifier = Modifier.size(cardSize),
                    title = stringResource(R.string.share_sound),
                    iconPainter = painterResource(R.drawable.create_room),
                    onClick = { viewModel.shareSound(navigateToActiveRoom) }
                )

                Spacer(Modifier.width(spacing))

                MainActionCard(
                    modifier = Modifier.size(cardSize),
                    title = stringResource(R.string.join_sound_room),
                    iconPainter = painterResource(R.drawable.join_sound_room),
                    onClick = navigateToJoinRoom
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = AppDimens.Padding.horizontal,
                        vertical = AppDimens.Padding.vertical
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MainActionCard(
                    modifier = Modifier.size(cardSize),
                    title = stringResource(R.string.share_sound),
                    iconPainter = painterResource(R.drawable.create_room),
                    onClick = { viewModel.shareSound(navigateToActiveRoom) }
                )

                Spacer(Modifier.height(spacing))

                MainActionCard(
                    modifier = Modifier.size(cardSize),
                    title = stringResource(R.string.join_sound_room),
                    iconPainter = painterResource(R.drawable.join_sound_room),
                    onClick = navigateToJoinRoom
                )
            }
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
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                modifier = Modifier.size(150.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(12.dp))

            Text(
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = title,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(color = textColors)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMain() {
    WavesynchTheme {
        SyncWaveMainScreenWithAppBar(navigateTo = {})
    }
}
