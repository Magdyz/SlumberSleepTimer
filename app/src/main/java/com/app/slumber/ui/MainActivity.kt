// file: app/src/main/java/com/app/slumber/ui/MainActivity.kt

package com.app.slumber.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.slumber.ui.theme.SlumberTheme
import com.app.slumber.ui.timer.TimerScreen
import com.app.slumber.ui.timer.TimerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val timerViewModel: TimerViewModel by viewModels()

    // Set up the permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    private fun askNotificationPermission() {
        // This is only required on API level 33+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask for permission when the app starts
        askNotificationPermission()

        setContent {
            val uiState by timerViewModel.uiState.collectAsStateWithLifecycle()

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                timerViewModel.bindToService()
                onDispose {
                    timerViewModel.unbindFromService()
                }
            }

            SlumberTheme {
                TimerScreen(
                    formattedTime = uiState.serviceState.formattedTime,
                    timerDurationMinutes = uiState.initialDurationMinutes,
                    isTimerRunning = uiState.serviceState.isRunning,
                    isTimerPaused = uiState.serviceState.isPaused,
                    onDurationChange = { timerViewModel.setDuration(it) },
                    onStartClick = { timerViewModel.startTimer() },
                    onPauseClick = { timerViewModel.pauseTimer() },
                    onResumeClick = { timerViewModel.startTimer() },
                    onCancelClick = { timerViewModel.cancelTimer() }
                )
            }
        }
    }
}