package com.kunano.wavesynch.ui.host.active_room

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunano.wavesynch.R
import com.kunano.wavesynch.services.AudioCaptureService
import com.kunano.wavesynch.ui.theme.StreamingButtons

@Composable
fun AudioCaptureRequestCompose(
) {
    val context = LocalContext.current
    val projectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    val viewModel = hiltViewModel<ActiveRoomViewModel>()
    val UIState by viewModel.uiState.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val startIntent = Intent(context, AudioCaptureService::class.java).apply {
                putExtra(AudioCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(AudioCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            // must be startForegroundService for Android 8+
            ContextCompat.startForegroundService(context, startIntent)
        }
    }



    ExtendedFloatingActionButton(
        contentColor = MaterialTheme.colorScheme.onSurface,
        containerColor = StreamingButtons,
        shape = ShapeDefaults.Large,
        modifier = Modifier.padding(bottom = 50.dp),
        onClick = {
            if (UIState.isHostStreaming) viewModel.setShowAskStopStreaming(true) else {
                val intent = projectionManager.createScreenCaptureIntent()
                launcher.launch(intent)
            }
        }
    ) {

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 15.dp, bottom = 15.dp)) {
            Icon(
                painter = if (UIState.isHostStreaming) painterResource(R.drawable.pause_48px) else painterResource(
                    R.drawable.play_arrow_48px
                ),
                contentDescription = if (UIState.isHostStreaming) "Stop streaming" else "Start streaming"
            )
            Text(
                if (UIState.isHostStreaming) stringResource(R.string.stop_streaming) else stringResource(
                    R.string.start_streaming
                )
            )
        }
    }


}
