package at.oderwieoderw.plankr.ui

import java.text.DateFormat
import java.text.FieldPosition
import java.util.Date
import java.util.Locale

/** Locale-aware time labels preserving millisecond precision. */
object TimeFormatting {
    /** Live timer text including the millisecond remainder. */
    fun elapsed(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d.%03d",
            seconds / 3600,
            (seconds / 60) % 60,
            seconds % 60,
            milliseconds % 1000
        )
    }

    /** Compact duration with three fractional second digits, including for whole seconds. */
    fun duration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val hours = seconds / 3600
        val minutes = (seconds / 60) % 60
        val remainder = milliseconds % 1000
        return when {
            hours > 0 -> {
                String.format(
                    Locale.getDefault(), "%dh %02dm %02d.%03ds",
                    hours, minutes, seconds % 60, remainder
                )
            }
            seconds >= 60 -> {
                String.format(
                    Locale.getDefault(), "%dm %02d.%03ds", minutes, seconds % 60, remainder
                )
            }
            else -> String.format(Locale.getDefault(), "%d.%03ds", seconds, remainder)
        }
    }

    /** Remaining time without rounding up or discarding fractional seconds. */
    fun countdown(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        return String.format(
            Locale.getDefault(),
            "%dh %02dm %02d.%03ds",
            seconds / 3600,
            (seconds / 60) % 60,
            seconds % 60,
            milliseconds % 1000
        )
    }

    /** Local time of day, retaining the locale's clock format and adding milliseconds. */
    fun timeOfDay(timestampMillis: Long): String {
        val secondsField = FieldPosition(DateFormat.SECOND_FIELD)
        val text = DateFormat.getTimeInstance(DateFormat.MEDIUM)
            .format(Date(timestampMillis), StringBuffer(), secondsField)
        val fraction = String.format(Locale.getDefault(), ".%03d", timestampMillis % 1000)
        return text.insert(secondsField.endIndex, fraction).toString()
    }
}
