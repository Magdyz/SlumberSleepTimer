// file: app/src/main/java/com/app/slumber/ui/timer/TimerViewModel.kt

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit // ✅ ADDED this missing import
import javax.inject.Inject

data class TimerUiState(
    val serviceState: TimerServiceState = TimerServiceState(),
    val initialDurationMinutes: Float = 30f
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState = _uiState.asStateFlow()

    private var timerService: TimerService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true

            // Once connected, start observing the service's state.
            timerService?.state?.onEach { serviceState ->
                _uiState.update { it.copy(serviceState = serviceState) }
            }?.launchIn(viewModelScope)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
        }
    }

    fun bindToService() {
        if (!isBound) {
            Intent(context, TimerService::class.java).also { intent ->
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun unbindFromService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            timerService = null
        }
    }

    // ✅ UPDATED this function to use TimeFormatter
    fun setDuration(minutes: Float) {
        // Only update if the timer is not active
        if (!uiState.value.serviceState.isRunning && !uiState.value.serviceState.isPaused) {
            val newDurationSeconds = TimeUnit.MINUTES.toSeconds(minutes.toLong())
            _uiState.update {
                it.copy(
                    initialDurationMinutes = minutes,
                    // Update the formatted time in the UI state immediately
                    serviceState = it.serviceState.copy(
                        formattedTime = TimeFormatter.format(newDurationSeconds)
                    )
                )
            }
        }
    }

    fun startTimer() {
        // Start service with the stored duration in minutes
        timerService?.startTimer(_uiState.value.initialDurationMinutes.toLong())
    }

    fun pauseTimer() {
        timerService?.pauseTimer()
    }

    fun cancelTimer() {
        timerService?.cancelTimer()
    }
}
