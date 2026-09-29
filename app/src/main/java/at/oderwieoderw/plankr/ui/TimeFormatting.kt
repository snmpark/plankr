package at.oderwieoderw.plankr.ui

import java.util.Locale

object TimeFormatting {
    fun elapsed(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60)
    }

    fun duration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val hours = seconds / 3600
        val minutes = (seconds / 60) % 60
        return when {
            milliseconds in 1..999 -> "<1s"
            hours > 0 -> String.format(Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds % 60)
            seconds >= 60 -> String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds % 60)
            else -> String.format(Locale.getDefault(), "%ds", seconds)
        }
    }

    fun countdown(milliseconds: Long): String {
        val seconds = milliseconds / 1000 + if (milliseconds % 1000 > 0) 1 else 0
        return String.format(Locale.getDefault(), "%dh %02dm %02ds", seconds / 3600, (seconds / 60) % 60, seconds % 60)
    }
}
