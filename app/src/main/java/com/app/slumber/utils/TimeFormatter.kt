// file: app/src/main/java/com/app/slumber/utils/TimeFormatter.kt

package com.app.slumber.utils

import java.util.concurrent.TimeUnit

object TimeFormatter {
    fun format(seconds: Long): String {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}