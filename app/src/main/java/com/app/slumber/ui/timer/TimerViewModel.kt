package com.app.slumber.ui.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.slumber.service.TimerService
import com.app.slumber.service.TimerServiceState
import com.app.slumber.utils.TimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ✅ UI state is just a wrapper around the service state
data class TimerUiState(
    val serviceState: TimerServiceState = TimerServiceState()
) {
    val initialDurationMinutes: Float
        get() = serviceState.initialDurationMinutes.toFloat()
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState = _uiState.asStateFlow()

    private var timerService: TimerService? = null
    private var isBound = false
    private var serviceStateJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? TimerService.TimerBinder ?: return
            timerService = binder.getService()
            isBound = true

            timerService?.let { svc ->
                // ✅ Push current service state immediately
                _uiState.update { it.copy(serviceState = svc.state.value) }

                // ✅ Cancel previous collector if any
                serviceStateJob?.cancel()

                // ✅ Collect fresh updates
                serviceStateJob = svc.state.onEach { serviceState ->
                    _uiState.update { it.copy(serviceState = serviceState) }
                }.launchIn(viewModelScope)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
            serviceStateJob?.cancel()
            serviceStateJob = null
        }
    }

    fun bindToService() {
        if (!isBound) {
            Intent(context, TimerService::class.java).also { intent ->
                // Start the service so it survives temporary unbinds during rotation
                context.startService(intent)
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun unbindFromService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            timerService = null
            serviceStateJob?.cancel()
            serviceStateJob = null
        }
    }

    // ✅ Update duration only when timer is idle
    fun setDuration(minutes: Float) {
        if (!uiState.value.serviceState.isRunning && !uiState.value.serviceState.isPaused) {
            val newDurationSeconds = TimeUnit.MINUTES.toSeconds(minutes.toLong())
            _uiState.update {
                it.copy(
                    serviceState = it.serviceState.copy(
                        initialDurationMinutes = minutes.toLong(),
                        formattedTime = TimeFormatter.format(newDurationSeconds)
                    )
                )
            }
        }
    }

    fun startTimer() {
        timerService?.startTimer(_uiState.value.serviceState.initialDurationMinutes)
    }

    fun pauseTimer() {
        timerService?.pauseTimer()
    }

    fun cancelTimer() {
        timerService?.cancelTimer()
    }
}
