package com.app.slumber.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.*
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.app.slumber.R
import com.app.slumber.ui.MainActivity
import com.app.slumber.utils.TimeFormatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.TimeUnit

data class TimerServiceState(
    val formattedTime: String = "00:00",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val initialDurationMinutes: Long = 45
)

class TimerService : Service() {

    private companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "SlumberTimerChannel"
        private const val KEY_REMAINING = "remaining_seconds"
        private const val KEY_INITIAL = "initial_duration_minutes"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_IS_PAUSED = "is_paused"
    }

    private val POST_FINISH_SETTLE_MS = 1_500L
    private val ENFORCEMENT_WINDOW_MS = 6_000L
    private val ENFORCEMENT_POLL_MS = 500L

    inner class TimerBinder : Binder() { fun getService(): TimerService = this@TimerService }

    private fun countdownFlow(startSeconds: Long) = flow {
        var s = startSeconds
        while (s > 0) {
            emit(s)
            delay(1000)
            s--
        }
    }

    private val binder = TimerBinder()
    private var timerJob: Job? = null
    private var remainingSeconds = 0L
    private var initialDurationMinutes: Long = 45
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
    private var audioFocusRequest: AudioFocusRequest? = null
    private var mediaSessionManager: MediaSessionManager? = null

    private val prefs by lazy { getSharedPreferences("slumber_timer_prefs", Context.MODE_PRIVATE) }

    private fun pausePlayingSessionsOnce(): Boolean {
        val component = ComponentName(this, SlumberNotificationListener::class.java)
        val controllers: List<MediaController> = try {
            mediaSessionManager?.getActiveSessions(component).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        var didPauseAny = false
        controllers.forEach { controller ->
            try {
                val state = controller.playbackState
                val actions = state?.actions ?: 0L
                val playing = state?.state == PlaybackState.STATE_PLAYING ||
                        state?.state == PlaybackState.STATE_BUFFERING

                if (playing) {
                    when {
                        (actions and PlaybackState.ACTION_STOP) != 0L -> {
                            controller.transportControls.stop()
                            didPauseAny = true
                        }
                        (actions and PlaybackState.ACTION_PAUSE) != 0L -> {
                            controller.transportControls.pause()
                            didPauseAny = true
                        }
                        (actions and PlaybackState.ACTION_PLAY_PAUSE) != 0L -> {
                            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                            didPauseAny = true
                        }
                        else -> {
                            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
                            didPauseAny = true
                        }
                    }
                }
            } catch (_: Exception) { }
        }
        return didPauseAny
    }

    private suspend fun enforcePausedForWindow(
        windowMs: Long = ENFORCEMENT_WINDOW_MS,
        everyMs: Long = ENFORCEMENT_POLL_MS
    ) {
        val until = SystemClock.uptimeMillis() + windowMs
        while (SystemClock.uptimeMillis() < until) {
            pausePlayingSessionsOnce()
            delay(everyMs)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaSessionManager = try {
            getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        } catch (_: Exception) { null }
        createNotificationChannel()

        val savedRemaining = prefs.getLong(KEY_REMAINING, 0L)
        val savedInitial = prefs.getLong(KEY_INITIAL, 45L)
        val wasRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val wasPaused  = prefs.getBoolean(KEY_IS_PAUSED,  false)

        if (savedRemaining > 0 && (wasRunning || wasPaused)) {
            remainingSeconds = savedRemaining
            initialDurationMinutes = savedInitial
            _state.value = TimerServiceState(
                formattedTime = TimeFormatter.format(savedRemaining),
                isRunning = false,
                isPaused  = true,
                initialDurationMinutes = savedInitial
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    fun startTimer(durationMinutes: Long) {
        initialDurationMinutes = durationMinutes
        val totalSeconds = if (_state.value.isPaused) remainingSeconds
        else TimeUnit.MINUTES.toSeconds(durationMinutes)

        _state.value = _state.value.copy(
            isRunning = true,
            isPaused  = false,
            initialDurationMinutes = durationMinutes
        )
        persistState()
        startCountdown(totalSeconds)
        startForegroundService()
    }

    fun pauseTimer() {
        serviceScope.launch {
            timerJob?.cancelAndJoin()
            _state.value = _state.value.copy(isRunning = false, isPaused = true)
            persistState()
            updateNotification()
        }
    }

    fun cancelTimer() {
        serviceScope.launch {
            timerJob?.cancelAndJoin()
            remainingSeconds = 0L
            _state.value = TimerServiceState(
                formattedTime = TimeFormatter.format(TimeUnit.MINUTES.toSeconds(initialDurationMinutes)),
                isRunning = false,
                isPaused = false,
                initialDurationMinutes = initialDurationMinutes
            )
            clearPersistedState()
            safelyAbandonAudioFocus()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startCountdown(totalSeconds: Long) {
        remainingSeconds = totalSeconds
        timerJob?.cancel()

        timerJob = serviceScope.launch {
            countdownFlow(totalSeconds)
                .onEach { secs ->
                    remainingSeconds = secs
                    _state.value = _state.value.copy(
                        formattedTime = TimeFormatter.format(secs),
                        initialDurationMinutes = initialDurationMinutes
                    )
                    if (secs <= 60 || secs % 5 == 0L) {
                        persistState()
                        updateNotification()
                    }
                }
                .onCompletion { cause -> if (cause == null) onTimerFinished() }
                .collect { }
        }
    }

    private fun onTimerFinished() {
        _state.value = _state.value.copy(isRunning = false, isPaused = true)
        persistState()
        updateNotification()

        serviceScope.launch {
            pausePlayingSessionsOnce()
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
            delay(150)
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_STOP)
            requestLongAudioFocus()
            delay(POST_FINISH_SETTLE_MS)
            enforcePausedForWindow()
            cancelTimer()
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up   = KeyEvent(now, now, KeyEvent.ACTION_UP,   keyCode, 0)
        audioManager.dispatchMediaKeyEvent(down)
        audioManager.dispatchMediaKeyEvent(up)
    }

    private fun requestLongAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { }
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun safelyAbandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (_: Exception) { }
    }

    private fun startForegroundService() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): android.app.Notification {
        val resultIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Slumber Sleep Timer")
            .setContentText("Time remaining: ${_state.value.formattedTime}")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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

    private fun persistState(commitNow: Boolean = false) {
        val editor = prefs.edit()
            .putLong(KEY_REMAINING, remainingSeconds)
            .putLong(KEY_INITIAL, initialDurationMinutes)
            .putBoolean(KEY_IS_RUNNING, _state.value.isRunning)
            .putBoolean(KEY_IS_PAUSED,  _state.value.isPaused)
        if (commitNow) editor.commit() else editor.apply()
    }

    private fun clearPersistedState() {
        prefs.edit().clear().apply()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (_state.value.isRunning) {
            serviceScope.launch {
                timerJob?.cancelAndJoin()
                _state.value = _state.value.copy(isRunning = false, isPaused = true)
                persistState(commitNow = true)
                updateNotification()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        safelyAbandonAudioFocus()
        serviceScope.cancel()
    }
}
