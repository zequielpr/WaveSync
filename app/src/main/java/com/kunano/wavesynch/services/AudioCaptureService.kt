package com.kunano.wavesynch.services

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.kunano.wavesynch.R
import com.kunano.wavesynch.data.stream.host.HostAudioCapturer
import com.kunano.wavesynch.domain.repositories.HostRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@AndroidEntryPoint
class AudioCaptureService : Service() {
    @Inject
    lateinit var hostRepository: HostRepository

    @Inject
    @ApplicationContext
    lateinit var context: Context


    companion object {
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "AudioCaptureChannel"
    }

    private var hostAudioCapturer: HostAudioCapturer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: return START_NOT_STICKY
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
            ?: return START_NOT_STICKY

        // 1) Build and show notification
        val notification = buildNotification()

        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        // 2) Get MediaProjection here (service context)
        val projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        // 3) Start your HostAudioCapturer + HostStreamer here
        //startCapture(mediaProjection)

        mediaProjection?.let {
            startCapture(mediaProjection)
        }



        //If the system kills the service, we want to restart it
        return START_STICKY
    }


    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Audio Capture Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.audio_capture_service))
            .setContentText(context.getString(R.string.streaming_audio))
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // Replace with your icon
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startCapture(mediaProjection: MediaProjection) {
        hostAudioCapturer = HostAudioCapturer(mediaProjection)


        hostAudioCapturer?.let {
            hostRepository.startStreamingAsHost(it)
        }

    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        //Stop streaming
        hostRepository.stopStreaming()
        super.onDestroy()
    }
}