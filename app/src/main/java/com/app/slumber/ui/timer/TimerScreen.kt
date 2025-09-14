// file: app/src/main/java/com/app/slumber/ui/timer/TimerScreen.kt

package com.app.slumber.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun TimerScreen(
    // State properties
    formattedTime: String,
    timerDurationMinutes: Float,
    isTimerRunning: Boolean,
    isTimerPaused: Boolean,

    // Event callbacks
    onDurationChange: (Float) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ✅ ADDED a minimalist title
            Text(
                text = "Slumber",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light
            )

            // ✅ ADDED a small spacer for balance
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(64.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Set Duration: ${timerDurationMinutes.roundToInt()} min",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = timerDurationMinutes,
                    onValueChange = onDurationChange,
                    valueRange = 1f..120f,
                    steps = 118,
                    enabled = !isTimerRunning && !isTimerPaused
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isTimerRunning) {
                    FilledTonalButton(onClick = onPauseClick) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause Timer")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pause")
                    }
                } else {
                    FilledTonalButton(onClick = if (isTimerPaused) onResumeClick else onStartClick) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start Timer")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isTimerPaused) "Resume" else "Start")
                    }
                }

                FilledTonalButton(
                    onClick = onCancelClick,
                    enabled = isTimerRunning || isTimerPaused
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Cancel Timer")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }
            }
        }
    }
}