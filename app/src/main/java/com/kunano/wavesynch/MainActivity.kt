package com.kunano.wavesynch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kunano.wavesynch.ui.guest.current_room.CurrentRoomCompose
import com.kunano.wavesynch.ui.guest.join_room.JoinRoomViewCompose
import com.kunano.wavesynch.ui.host.active_room.ActiveRoomCompose
import com.kunano.wavesynch.ui.main_screen.SyncWaveMainScreenWithAppBar
import com.kunano.wavesynch.ui.main_screen.drawer.screens.AboutUsScreen
import com.kunano.wavesynch.ui.main_screen.drawer.screens.HelpScreen
import com.kunano.wavesynch.ui.main_screen.drawer.screens.PrivacyPoliciesScreen
import com.kunano.wavesynch.ui.nav.Screen
import com.kunano.wavesynch.ui.onboarding.OnboardingScreen
import com.kunano.wavesynch.ui.theme.WavesynchTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    data class RoomNavArgs(val roomName: String, val hostName: String)


    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force enable collection while testing (remove later if you want)
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled =
            true

        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            .log("App started")

        enableEdgeToEdge()
        setContent {
            WavesynchTheme {
                val vm: MainActivityViewModel = hiltViewModel()
                var isFirstOpening by remember { mutableStateOf(vm.getIsFirstOpening()) }

                if (isFirstOpening) {
                    OnboardingScreen(navigateToMainScreen = {
                        isFirstOpening = false
                        vm.setIsFirstOpening(false)
                    })
                } else {
                    WaveSyncApp(intent)
                }


            }
        }
    }


    //Navigation graph
    @Composable
    fun WaveSyncApp(intent1: Intent) {
        val vm: MainActivityViewModel = hiltViewModel()

        vm.handleIntent(intent1)
        val navController = rememberNavController()

        val navEvent by vm.pendingRoomNavFlow.collectAsState()




        NavHost(navController = navController, startDestination = Screen.MainScreen) {
            composable<Screen.MainScreen>() {
                SyncWaveMainScreenWithAppBar(
                    navigateTo = { navController.navigate(it) })
            }

            composable<Screen.ActiveRoomScreen>() {

                ActiveRoomCompose(onBack = {
                    navController.popBackStack(
                        Screen.MainScreen,
                        inclusive = false
                    )
                })

            }


            composable<Screen.JoinRoomScreen>() {
                JoinRoomViewCompose(
                    onBack = { navController.popBackStack(Screen.MainScreen, inclusive = false) },
                    navigateTo = { navController.navigate(it) })
            }

            composable<Screen.CurrentRoomScreen>() {
                CurrentRoomCompose(
                    onBack = { navController.popBackStack(Screen.MainScreen, inclusive = false) },
                    navigateTo = { navController.navigate(it) })
            }

            composable<Screen.AboutUsScreen>() {
                AboutUsScreen(onBack = { navController.popBackStack(Screen.MainScreen, inclusive = false) })
            }

            composable<Screen.PrivacyPoliciesScreen>() {
                PrivacyPoliciesScreen(onBack = { navController.popBackStack(Screen.MainScreen, inclusive = false) })
            }

            composable<Screen.HelpScreen>() {
                HelpScreen(onBack = { navController.popBackStack(Screen.MainScreen, inclusive = false) })
            }

        }


        LaunchedEffect(navEvent) {
            //I also need to create a flow of the receiving audio state to synchronize the button states of the notification and the  current room
            //I need to pass the argument to the composable and for that I need to make some changes
            val args = navEvent ?: return@LaunchedEffect

            // Navigate to CurrentRoomScreen
            navController.navigate(Screen.CurrentRoomScreen)

            // IMPORTANT: consume the event so it doesn't navigate again on recomposition
            vm.emptyRoomNavArgs()

        }


    }


}

@Preview(showBackground = true)
@Composable
fun WavesynchThemePreview() {
    WavesynchTheme(darkTheme = false) {

        SyncWaveMainScreenWithAppBar(
            navigateTo = {})
    }
}
