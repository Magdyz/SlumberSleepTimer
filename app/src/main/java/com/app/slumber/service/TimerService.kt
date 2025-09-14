// file: app/src/main/java/com/app/slumber/service/TimerService.kt

package com.app.slumber.service

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
import com.app.slumber.utils.TimeFormatter // ✅ IMPORT the new utility
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

// Data class to hold the service's state.
data class TimerServiceState(
    val formattedTime: String = "00:00",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
)

class TimerService : Service() {

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = TimerBinder()
    private var timerJob: Job? = null
    private var remainingSeconds = 0L

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(TimerServiceState())
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
        val totalSeconds = if (_state.value.isPaused) {
            remainingSeconds
        } else {
            TimeUnit.MINUTES.toSeconds(durationMinutes)
        }
        _state.value = _state.value.copy(isRunning = true, isPaused = false)
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
        _state.value = TimerServiceState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startCountdown(totalSeconds: Long) {
        remainingSeconds = totalSeconds
        timerJob = serviceScope.launch {
            while (remainingSeconds > 0) {
                // ✅ UPDATED to use the TimeFormatter utility
                _state.value = _state.value.copy(formattedTime = TimeFormatter.format(remainingSeconds))
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

    private fun buildNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("Slumber Sleep Timer")
        .setContentText("Time remaining: ${_state.value.formattedTime}")
        .setSmallIcon(R.drawable.ic_stat_name)
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .build()

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

    // ✅ DELETED the old, redundant formatTime function from this file.

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
