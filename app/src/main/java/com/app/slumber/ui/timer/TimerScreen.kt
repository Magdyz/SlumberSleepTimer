// file: app/src/main/java/com/app/slumber/ui/timer/TimerScreen.kt

package com.app.slumber.ui.timer

// ✨ ADDED: Import for AnimatedVisibility to fade the text in and out smoothly.
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
// ✨ ADDED: Import for TextAlign to center the instructional text.
import androidx.compose.ui.text.style.TextAlign
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
                .padding(32.dp), // Increased padding
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // The time display now takes up the most space.
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // ✨ ADDED: A Column to hold both the timer and the new instructional text.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (!isTimerRunning && !isTimerPaused) {
                            // show initial duration when idle
                            "${timerDurationMinutes.roundToInt()}:00"
                        } else {
                            formattedTime
                        }
                        ,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Medium
                    )

                    // ✨ ADDED: Animated instructional text that only appears when the timer is idle.
                    AnimatedVisibility(visible = !isTimerRunning && !isTimerPaused) {
                        Text(
                            text = "Set duration to begin your slumber", // ✅ sentence case
                            style = MaterialTheme.typography.bodyMedium, // ✅ smaller, subtler
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Slider section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set duration: ${timerDurationMinutes.roundToInt()} min", // ✅ sentence case
                    style = MaterialTheme.typography.titleMedium // ✅ stronger, modern
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = timerDurationMinutes,
                    onValueChange = onDurationChange,
                    valueRange = 1f..120f,
                    steps = 118,
                    enabled = !isTimerRunning && !isTimerPaused,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Redesigned control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary cancel button (less prominent)
                IconButton(
                    onClick = onCancelClick,
                    enabled = isTimerRunning || isTimerPaused,
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Cancel Timer",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Large, circular primary action button
                Button(
                    onClick = {
                        when {
                            isTimerRunning -> onPauseClick()
                            isTimerPaused -> onResumeClick()
                            else -> onStartClick()
                        }
                    },
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isTimerRunning) "Pause Timer" else "Start Timer",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Spacer to balance the layout since there isn't a button on the right
                Spacer(modifier = Modifier.width(24.dp))
                Spacer(modifier = Modifier.size(32.dp)) // This spacer is the same size as the icon in IconButton
            }
        }
    }
}