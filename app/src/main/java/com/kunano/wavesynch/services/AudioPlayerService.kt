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
import com.kunano.wavesynch.data.stream.guest.AudioReceiver
import com.kunano.wavesynch.data.wifi.client.ClientManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.DatagramSocket
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

    // Create a CoroutineScope tied to the service's lifecycle
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

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
        serviceScope.launch {
            audioReceiver.isPlayingState.collect {
                updatePlaybackState(it)
                Log.d("AudioPlayerService", "collectIsplayinState: $it")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            MediaButtonReceiver.handleIntent(mediaSession, intent)
        }

        if (intent?.action != Intent.ACTION_MEDIA_BUTTON) {
            val notification = buildNotification(true)

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
            collectIsplayinState()

            val udpSocket: DatagramSocket = clientManager.openUdpSocket()
            audioReceiver.start(udpSocket)
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
                stopSelf()
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
        mediaSession.release()// Release the media session
        clientManager.disconnectFromServer()
        serviceScope.cancel()  // Cancel all coroutines started by this service
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
            R.drawable.delete_48px,
            "Stop",
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("${getString(R.string.playing_audio_from)} $hostName")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(playPauseAction)
            .addAction(stopAction).setContentIntent(contentPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
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
        refreshNotif(isPlaying)
    }
}
