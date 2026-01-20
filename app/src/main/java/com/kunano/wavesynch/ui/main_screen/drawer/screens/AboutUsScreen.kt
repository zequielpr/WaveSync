package com.kunano.wavesynch.ui.main_screen.drawer.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kunano.wavesynch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(
    appName: String = stringResource(R.string.app_name),
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)
) {

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {TopAppBar(
            navigationIcon = {IconButton(onClick = onBack) { Image(painter = painterResource(id = R.drawable.arrow_back_ios_48px), contentDescription = null) }},
            title = { Text(stringResource(R.string.about) + " $appName") },

        )}
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                Section(
                    title = stringResource(R.string.what_is) +" $appName?",
                    body = "$appName " + stringResource(R.string.functionality)
                )

                Section(
                    title = stringResource(R.string.why_was_built) + " Bluetooth?",
                    body = stringResource(R.string.reason_of_creation)
                )

                Section(
                    title = stringResource(R.string.how_does_it_work),
                    body = stringResource(R.string.answer_to_how_it_works)
                )

                Section(
                    title = stringResource(R.string.when_is_it_useful),
                    body = stringResource(R.string.when_is_it_useful_answer)
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, MaterialTheme.colorScheme.secondary)

                Text(
                    text = stringResource(R.string.buil_with_focus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }




}

@Composable
private fun Section(
    title: String,
    body: String,
) {
    val body_list = body.split("/")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        if (body_list.size > 1) {

            body_list.dropLast(1).forEach {
                BulletItem(text = it)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text =  body_list.last(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }else{
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

    }
}

@Composable
private fun BulletItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
