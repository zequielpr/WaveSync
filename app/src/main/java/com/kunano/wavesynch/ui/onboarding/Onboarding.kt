package com.kunano.wavesynch.ui.onboarding

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kunano.wavesynch.R
import com.kunano.wavesynch.ui.theme.transParent

@Composable
fun OnboardingScreen(navigateToMainScreen: () -> Unit = {}) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        var isMuted by remember { mutableStateOf(false) }
        OnboardingVideo(
            videoUri = rawVideoUri(context.packageName, R.raw.onboarding_video),
            modifier = Modifier.fillMaxSize(),
            mute = isMuted,
            loop = true
        )

        IconButton( onClick = { isMuted = !isMuted },
            modifier = Modifier
                .align(Alignment.TopEnd).padding(top = 80.dp, end = 30.dp)) {
            Image(
                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.surface),
                painter = painterResource(if (isMuted) R.drawable.volume_off_48px else R.drawable.volume_up_48px),
                contentDescription = null
            )
        }


        FloatingActionButton(
            shape = androidx.compose.material3.MaterialTheme.shapes.small,
            containerColor = MaterialTheme.colorScheme.secondary,
            onClick = navigateToMainScreen,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .width(100.dp)
        ) {

            Text(
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                text = stringResource(R.string.start),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

        }
    }
}

/**
 * Plays a short onboarding video (typically 2–6 seconds).
 *
 * @param videoUri Uri to video (raw resource, asset via file://, or http(s))
 * @param mute Mute audio (recommended for onboarding)
 * @param loop Loop the video forever
 * @param autoPlay Start automatically
 **/
@OptIn(UnstableApi::class)
@Composable
fun OnboardingVideo(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    mute: Boolean = true,
    loop: Boolean = true,
    autoPlay: Boolean = true,
) {
    val context = LocalContext.current

    // Create and remember ExoPlayer
    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = if (mute) 0f else 1f
            playWhenReady = autoPlay
            prepare()
        }
    }

    // Keep volume/repeat updated if params change
    LaunchedEffect(mute) { player.volume = if (mute) 0f else 1f }
    LaunchedEffect(loop) {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }
    LaunchedEffect(autoPlay) { player.playWhenReady = autoPlay }

    // Lifecycle handling (pause/resume + release)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (autoPlay) player.play()
                Lifecycle.Event.ON_PAUSE -> player.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = player
                resizeMode =
                    AspectRatioFrameLayout.RESIZE_MODE_FILL// looks nice for fullscreen onboarding
            }
        },
        update = { it.player = player }
    )
}

/** Helper: Uri for a raw resource video (e.g., res/raw/onboarding.mp4). */
fun rawVideoUri(packageName: String, rawResId: Int): Uri =
    Uri.parse("android.resource://$packageName/$rawResId")


@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen()
}