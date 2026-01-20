package com.kunano.wavesynch.ui.main_screen.drawer

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kunano.wavesynch.R
import java.time.Year

sealed interface DrawerAction {
    data object AboutUs : DrawerAction
    data object PrivacyPolicies : DrawerAction
    data object Help : DrawerAction
    data object ShareApp : DrawerAction
    data object RateApp : DrawerAction
}

@Composable
fun AppDrawerContent(
    modifier: Modifier = Modifier,
    appName: String = stringResource(R.string.app_name),
    onAction: (DrawerAction) -> Unit,
) {
    val drawerItemModifier = Modifier.size(35.dp)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(min = 280.dp, max = 320.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.menu),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))


        DrawerItem(
            icon = {  Image( modifier = drawerItemModifier, painter = painterResource(id = R.drawable.info_48px), contentDescription = null) },
            title = stringResource(R.string.about) + " " + stringResource(R.string.app_name),
            onClick = { onAction(DrawerAction.AboutUs) }
        )

        DrawerItem(
            icon = { Image(modifier = drawerItemModifier,  painter = painterResource(id = R.drawable.privacy_tip_48px), contentDescription = null) },
            title = stringResource(R.string.privacy_policies),
            onClick = { onAction(DrawerAction.PrivacyPolicies) }
        )

        DrawerItem(
            icon = { Image(modifier = drawerItemModifier, painter = painterResource(id = R.drawable.help_48px), contentDescription = null) },
            title = stringResource(R.string.help),
            onClick = { onAction(DrawerAction.Help) }
        )

        DrawerItem(
            icon = { Image(modifier = drawerItemModifier, painter = painterResource(id = R.drawable.share_48px), contentDescription = null) },
            title = stringResource(R.string.share_this_app),
            onClick = { onAction(DrawerAction.ShareApp) }
        )

        DrawerItem(
            icon = { Image(modifier = drawerItemModifier, painter = painterResource(id = R.drawable.star_rate_48px), contentDescription = null) },
            title = stringResource(R.string.rate_app),
            onClick = { onAction(DrawerAction.RateApp) }
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        Text(
            text = "© ${Year.now()} $appName",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DrawerItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp).background(color = MaterialTheme.colorScheme.secondary),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

/**
 * Optional helper: a default "Share" implementation if you don't want to handle it in ViewModel.
 */
fun shareApp(context: android.content.Context, message: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
