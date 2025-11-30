package com.kunano.wavesynch

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kunano.wavesynch.ui.guest.current_room.CurrentRoomCompose
import com.kunano.wavesynch.ui.host.active_room.ActiveRoomCompose
import com.kunano.wavesynch.ui.main_screen.SyncWaveMainScreenWithAppBar
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.theme.WavesynchTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WavesynchTheme {

                WaveSyncApp()
            }
        }
    }
}


//Navigation graph
@Composable
fun WaveSyncApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.MainScreen.route) {
        composable(route = Screen.MainScreen.route) {
            SyncWaveMainScreenWithAppBar(
                navigateToActiveRoom = { navController.navigate(Screen.ActiveRoomScreen.route) },
                navigateToJoinRoom = {navController.navigate(Screen.CurrentRoomScreen.route)})
        }

        composable(route = Screen.ActiveRoomScreen.route) {
            ActiveRoomCompose(onBack = { navController.popBackStack() })

        }
        composable(route = Screen.CurrentRoomScreen.route) {
            CurrentRoomCompose(onBack = { navController.popBackStack() })
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
