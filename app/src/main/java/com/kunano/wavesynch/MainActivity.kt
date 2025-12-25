package com.kunano.wavesynch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kunano.wavesynch.ui.guest.current_room.CurrentRoomCompose
import com.kunano.wavesynch.ui.guest.join_room.JoinRoomViewCompose
import com.kunano.wavesynch.ui.host.active_room.ActiveRoomCompose
import com.kunano.wavesynch.ui.main_screen.SyncWaveMainScreenWithAppBar
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.theme.WavesynchTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    data class RoomNavArgs(val roomName: String, val hostName: String)

    private val pendingRoomNav = MutableStateFlow<RoomNavArgs?>(null)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            WavesynchTheme {

                WaveSyncApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }


    private fun handleIntent(intent: Intent?) {
        val roomName = intent?.getStringExtra("ROOM_NAME") ?: return
        val hostName = intent.getStringExtra("HOST_NAME") ?: return
        pendingRoomNav.value = RoomNavArgs(roomName, hostName)

        Log.d("MainActivity", "handleIntent: $roomName $hostName")
    }


    //Navigation graph
    @Composable
    fun WaveSyncApp() {
        val navController = rememberNavController()

        val navEvent by pendingRoomNav.collectAsState()

        NavHost(navController = navController, startDestination = Screen.MainScreen) {
            composable<Screen.MainScreen>() {
                SyncWaveMainScreenWithAppBar(
                    navigateToActiveRoom = { navController.navigate(Screen.ActiveRoomScreen) },
                    navigateToJoinRoom = { navController.navigate(Screen.JoinRoomScreen) },
                    navigateToCurrentRoom = { navController.navigate(Screen.CurrentRoomScreen) }
                )
            }

            composable<Screen.ActiveRoomScreen>() {

                ActiveRoomCompose(onBack = { navController.popBackStack() })

            }


            composable<Screen.JoinRoomScreen>() {
                JoinRoomViewCompose(
                    onBack = { navController.popBackStack() },
                    navigateTo = { navController.navigate(it) })
            }

            composable<Screen.CurrentRoomScreen>() {
                CurrentRoomCompose(
                    onBack = { navController.navigate(Screen.MainScreen) },
                    navigateTo = { navController.navigate(it) })
            }

        }

        LaunchedEffect(navEvent) {
            //I also need to create a flow of the receiving audio state to synchronize the button states of the notification and the  current room
            //I need to pass the argument to the composable and for that I need to make some changes
            val args = navEvent ?: return@LaunchedEffect

            // Navigate to CurrentRoomScreen
            navController.navigate(Screen.CurrentRoomScreen)

            // IMPORTANT: consume the event so it doesn't navigate again on recomposition
            pendingRoomNav.value = null
        }


    }


    @Preview(showBackground = true)
    @Composable
    fun WavesynchThemePreview() {
        WavesynchTheme(darkTheme = false) {

            SyncWaveMainScreenWithAppBar(
                navigateToActiveRoom = {},
                navigateToJoinRoom = {},
                navigateToCurrentRoom = {})
        }
    }
}
