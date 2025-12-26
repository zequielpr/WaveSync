package com.kunano.wavesynch.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.kunano.wavesynch.MainActivity
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.stream.AudioReceiver
import com.kunano.wavesynch.data.wifi.client.ClientConnectionsState
import com.kunano.wavesynch.data.wifi.client.ClientManager
import com.kunano.wavesynch.domain.usecase.GuestUseCases
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AudioPlayerService : Service() {

    @Inject
    lateinit var audioReceiver: AudioReceiver

    @Inject
    lateinit var clientManager: ClientManager

    @Inject
    @ApplicationContext
    lateinit var context: Context

    var notificationManager: NotificationManager? = null
    lateinit var mediaSession: MediaSessionCompat
    lateinit var mainActivityIntent: Intent
    lateinit var contentPendingIntent: PendingIntent





    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "AudioPlayerChannel"

    }

    override fun onCreate() {
        super.onCreate()
        mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ROOM_NAME", clientManager.handShakeFromHost?.roomName)
            putExtra("HOST_NAME", clientManager.handShakeFromHost?.deviceName)
        }
        contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        createNotificationChannel()
        mediaSession = MediaSessionCompat(context, "AudioSession")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        setMediaSessionCallBack(mediaSession)
        mediaSession.isActive = true
    }

    private fun collectIsplayinState() {
        CoroutineScope(Dispatchers.IO).launch {
            audioReceiver.isPlayingState.collect {
                updatePlaybackState(it)
                Log.d("AudioPlayerService", "collectIsplayinState: $it")
            }

        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Let the MediaButtonReceiver parse the intent and call the right callback.
        // This is the correct way to handle media button events.
        if (intent != null) {
            MediaButtonReceiver.handleIntent(mediaSession, intent)
        }

        // The service is started for the first time (not from a media button press).
        if (intent?.action != Intent.ACTION_MEDIA_BUTTON) {
            val notification = buildNotification(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            collectIsplayinState()

            // Start receiving audio and set the initial playback state
            clientManager.inputStream?.let {
                audioReceiver.start(it)
            }
        }

        return START_STICKY
    }

    private fun setMediaSessionCallBack(mediaSession: MediaSessionCompat) {
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                audioReceiver.resume()
            }

            override fun onPause() {
                audioReceiver.pause()
            }

            override fun onStop() {
                stopSelf() // This will trigger onDestroy
            }
        })
    }

    private fun refreshNotif(isPlaying: Boolean) {
        val notification = buildNotification(isPlaying)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        Log.d("AudioPlayerService", "onDestroy: ")
        audioReceiver.stop()
        killMediaSessionFully()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    private fun killMediaSessionFully() {
        // Make it look non-resumable + inactive
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(0)
                .setState(PlaybackStateCompat.STATE_NONE, 0L, 0f)
                .build()
        )
        mediaSession.setMetadata(null)
        mediaSession.setCallback(null)
        mediaSession.isActive = false
        mediaSession.release() // This is the crucial final step
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }else{
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Audio Player Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun buildNotification(
        isPlaying: Boolean = true,
    ): Notification {
        val hostName = clientManager.handShakeFromHost?.deviceName ?: "Unknown Host"

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.pause_48px, "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.play_arrow_48px, "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
            )
        }

        val stopAction = NotificationCompat.Action(
            R.drawable.delete_48px, // Using delete icon for stop
            "Stop",
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("${getString(R.string.playing_audio_from)} $hostName")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(playPauseAction)
            .addAction(stopAction).setContentIntent(contentPendingIntent) // Add the stop action
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    // Show play/pause and stop in compact view
                    .setShowActionsInCompactView(0, 1)
            )
            .build()
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        val actions = PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_STOP
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
        )
        // Refresh notification to show the correct icon
        refreshNotif(isPlaying)
    }
}
