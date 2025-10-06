// file: app/src/main/java/com/app/slumber/ui/timer/TimerScreen.kt
package com.app.slumber.ui.timer

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->

        if (isLandscape) {
            // 🌐 LANDSCAPE MODE: Timer left, controls right
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Timer (hero element)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (!isTimerRunning && !isTimerPaused) {
                            "${timerDurationMinutes.roundToInt()}:00"
                        } else {
                            formattedTime
                        },
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Medium
                    )

                    AnimatedVisibility(visible = !isTimerRunning && !isTimerPaused) {
                        Text(
                            text = "Set duration to begin your slumber",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Right: Slider + buttons
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Slider section
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Set duration: ${timerDurationMinutes.roundToInt()} min",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = timerDurationMinutes,
                            onValueChange = onDurationChange,
                            valueRange = 1f..120f,
                            steps = 118,
                            enabled = !isTimerRunning && !isTimerPaused,
                            modifier = Modifier.width(200.dp), // 🖥 shorter for landscape
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Controls
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                imageVector = when {
                                    isTimerRunning -> Icons.Default.Pause
                                    else -> Icons.Default.PlayArrow
                                },
                                contentDescription = when {
                                    isTimerRunning -> "Pause Timer"
                                    isTimerPaused -> "Resume Timer"
                                    else -> "Start Timer"
                                },
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        } else {
            // 📱 PORTRAIT MODE: Existing layout (unchanged)
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // Timer display
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (!isTimerRunning && !isTimerPaused) {
                                "${timerDurationMinutes.roundToInt()}:00"
                            } else {
                                formattedTime
                            },
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Medium
                        )

                        AnimatedVisibility(visible = !isTimerRunning && !isTimerPaused) {
                            Text(
                                text = "Set duration to begin your slumber",
                                style = MaterialTheme.typography.bodyMedium,
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
                        text = "Set duration: ${timerDurationMinutes.roundToInt()} min",
                        style = MaterialTheme.typography.titleMedium
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

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            imageVector = when {
                                isTimerRunning -> Icons.Default.Pause
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = when {
                                isTimerRunning -> "Pause Timer"
                                isTimerPaused -> "Resume Timer"
                                else -> "Start Timer"
                            },
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
