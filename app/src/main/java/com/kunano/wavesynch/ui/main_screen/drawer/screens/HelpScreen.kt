package com.kunano.wavesynch.ui.main_screen.drawer.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kunano.wavesynch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    appName: String = "WaveSync",
    modifier: Modifier = Modifier,
    onBack: (() -> Unit),
    supportEmail: String = "support@yourdomain.com",
    playStorePackageName: String? = null, // default uses context.packageName
) {
    val context = LocalContext.current
    val pkg = playStorePackageName ?: context.packageName
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
    val cardTitleStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Help") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(
                            painter = painterResource(R.drawable.arrow_back_ios_48px),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                // Quick idea
                Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.quick_idea),
                            style = cardTitleStyle,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$appName " + stringResource(R.string.quick_idea_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SectionTitle(stringResource(R.string.getting_started))

                StepCard(
                    cardTitleStyle,
                    cardColors,
                    title = stringResource(R.string.share_sound_host),
                    steps = stringResource(R.string.share_sound_host_steps).split("/")
                )

                StepCard(
                    cardTitleStyle,
                    cardColors,
                    title = stringResource(R.string.join_sound_room_be_guest),
                    steps = stringResource(R.string.join_sound_room_be_guest_steps).split("/")
                )

                SectionTitle("Common issues")

                FaqItem(
                    cardTitleStyle,
                    cardColors,
                    question = stringResource(R.string.cannot_joni_room),
                    answer = stringResource(R.string.cannot_joni_room_solution)
                )

                FaqItem(cardTitleStyle,
                    cardColors,
                    question = stringResource(R.string.joined_but_not_sound),
                    answer = stringResource(R.string.joined_but_not_sound_solution)
                )

                FaqItem(
                    cardTitleStyle,
                    cardColors,
                    question = stringResource(R.string.sound_delayed_not_sync),
                    answer = stringResource(R.string.sound_delayed_not_sync_solution)
                )

                FaqItem(
                    cardTitleStyle,
                    cardColors,
                    question = stringResource(R.string.sound_stutter_cuts),
                    answer = stringResource(R.string.sound_stutter_cuts_solution)
                )

                FaqItem(
                    cardTitleStyle,
                    cardColors,
                    question = stringResource(R.string.work_at_home_but_not_public_wifi),
                    answer = stringResource(R.string.work_at_home_but_not_public_wifi_solution)
                )

                SectionTitle(stringResource(R.string.best_results))

                BulletCard(
                    cardColors,
                    bullets = stringResource(R.string.best_results_checklist_solution).split("/")
                )

                SectionTitle(stringResource(R.string.more))

                // Actions row (Rate / Share / Contact)
                /*ActionCard(
                    titleStyle = cardTitleStyle,
                    cardColors,
                    onRate = { openPlayStore(context, pkg) },
                    onShare = {
                        shareApp(
                            context = context,
                            message = "Check out $appName: https://play.google.com/store/apps/details?id=$pkg"
                        )
                    },
                    onContact = {
                        contactSupport(
                            context = context,
                            email = supportEmail,
                            subject = "$appName support",
                            body = buildSupportBody(context, appName)
                        )
                    }
                )

                 */

                Text(
                    text = stringResource(R.string.tip_for_report_issue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ---------- UI bits ---------- */

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun StepCard(titleStyle: TextStyle, colors: CardColors, title: String, steps: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = colors) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                style = titleStyle,
                fontWeight = FontWeight.Medium
            )
            steps.forEachIndexed { index, step ->
                Text(
                    text = "${index + 1}. $step",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BulletCard(cardColors: CardColors, bullets: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bullets.forEach { b ->
                Text(
                    text = "• $b",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FaqItem(titleStyle: TextStyle, cardColors: CardColors, question: String, answer: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(

                question,
                style = titleStyle,
                fontWeight = FontWeight.Medium
            )
            Text(
                answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/*
@Composable
private fun ActionCard(
    titleStyle: TextStyle,
    cardColors: CardColors,
    onRate: () -> Unit,
    onShare: () -> Unit,
    onContact: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.rate) + " / " + stringResource(R.string.share_this_app) + " / " + stringResource(R.string.contact),
                style = titleStyle,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onRate
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.rate))
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onShare
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share))
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onContact,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Email, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.contact_support))
            }
        }
    }
}
 */
/* ---------- Intents ---------- */

fun shareApp(context: Context, message: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

fun openPlayStore(context: Context, packageName: String) {
    val marketUri = Uri.parse("market://details?id=$packageName")
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")

    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, marketUri))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

fun contactSupport(
    context: Context,
    email: String,
    subject: String,
    body: String,
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Email support"))
}

private fun buildSupportBody(context: Context, appName: String): String {
    val androidVersion = android.os.Build.VERSION.RELEASE ?: "Unknown"
    val model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
    val pkg = context.packageName

    return """
        Describe what happened:

        Steps to reproduce:

        What you expected:

        ---
        App: $appName
        Package: $pkg
        Device: $model
        Android: $androidVersion
        Wi-Fi setup: (router / hotspot / public Wi-Fi)
    """.trimIndent()
}
