package com.kunano.wavesynch.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.stream.AudioReceiver
import com.kunano.wavesynch.data.wifi.client.ClientManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
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


    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "AudioPlayerChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start receiving audio using the stream from ClientManager
        clientManager.inputStream?.let {
            audioReceiver.start(it)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("AudioPlayerService", "onDestroy: ")
        super.onDestroy()
        audioReceiver.stop()
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
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WaveSynch Player")
            .setContentText("Playing audio from host...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}