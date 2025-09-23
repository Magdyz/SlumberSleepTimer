package com.app.slumber.service

import android.app.PendingIntent
import com.app.slumber.ui.MainActivity
import androidx.core.app.TaskStackBuilder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.app.slumber.R
import com.app.slumber.utils.TimeFormatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

// Data class to hold the service's state.
data class TimerServiceState(
    val formattedTime: String = "00:00",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val initialDurationMinutes: Long = 45 // ✅ default duration tracked here
)

class TimerService : Service() {

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = TimerBinder()
    private var timerJob: Job? = null
    private var remainingSeconds = 0L
    private var initialDurationMinutes: Long = 45 // ✅ keep track of last chosen duration

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(
        TimerServiceState(
            formattedTime = TimeFormatter.format(TimeUnit.MINUTES.toSeconds(45)),
            isRunning = false,
            isPaused = false,
            initialDurationMinutes = 45
        )
    )
    val state = _state.asStateFlow()

    private lateinit var audioManager: AudioManager

    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "SlumberTimerChannel"
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun startTimer(durationMinutes: Long) {
        initialDurationMinutes = durationMinutes // ✅ save initial duration

        val totalSeconds = if (_state.value.isPaused) {
            remainingSeconds
        } else {
            TimeUnit.MINUTES.toSeconds(durationMinutes)
        }

        _state.value = _state.value.copy(
            isRunning = true,
            isPaused = false,
            initialDurationMinutes = durationMinutes
        )

        startCountdown(totalSeconds)
        startForegroundService()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _state.value = _state.value.copy(isRunning = false, isPaused = true)
        updateNotification()
    }

    fun cancelTimer() {
        timerJob?.cancel()
        remainingSeconds = 0L
        _state.value = TimerServiceState(
            formattedTime = TimeFormatter.format(TimeUnit.MINUTES.toSeconds(initialDurationMinutes)),
            isRunning = false,
            isPaused = false,
            initialDurationMinutes = initialDurationMinutes
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startCountdown(totalSeconds: Long) {
        remainingSeconds = totalSeconds
        timerJob = serviceScope.launch {
            while (remainingSeconds > 0) {
                _state.value = _state.value.copy(
                    formattedTime = TimeFormatter.format(remainingSeconds),
                    initialDurationMinutes = initialDurationMinutes
                )
                updateNotification()
                delay(1000)
                remainingSeconds--
            }
            onTimerFinished()
        }
    }

    private fun onTimerFinished() {
        pauseMediaPlayback()
        cancelTimer()
    }

    private fun pauseMediaPlayback() {
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )
    }

    private fun startForegroundService() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): android.app.Notification {
        val resultIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Slumber Sleep Timer")
            .setContentText("Time remaining: ${_state.value.formattedTime}")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Slumber Timer Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
