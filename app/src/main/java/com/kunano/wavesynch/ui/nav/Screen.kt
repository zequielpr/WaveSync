package com.kunano.wavesynch.ui.nav

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {

    @Serializable
    data object MainScreen : Screen()

    @Serializable
    data object ActiveRoomScreen : Screen()

    @Serializable
    data object JoinRoomScreen : Screen()

    @Serializable
    data object CurrentRoomScreen : Screen()

    @Serializable
    data object AboutUsScreen : Screen()

    @Serializable
    data object PrivacyPoliciesScreen : Screen()

    @Serializable
    data object HelpScreen : Screen()

    @Serializable
    data class TrustedUsersScreen(val roomId: Long) : Screen()


}
