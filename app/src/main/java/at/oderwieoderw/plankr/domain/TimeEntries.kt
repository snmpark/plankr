package at.oderwieoderw.plankr.domain

import java.util.Calendar

data class TimeEntry(val start: Long, val end: Long)

object TimeEntries {
    fun dayBounds(date: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return start to calendar.timeInMillis
    }

    fun overlap(entry: TimeEntry, periodStart: Long, periodEnd: Long): Long =
        (minOf(entry.end, periodEnd) - maxOf(entry.start, periodStart)).coerceAtLeast(0L)

    fun totalForDay(entries: List<TimeEntry>, date: Long): Long {
        val (start, end) = dayBounds(date)
        return entries.sumOf { overlap(it, start, end) }
    }

    fun monthBounds(date: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = dayBounds(date).first }
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        return start to calendar.timeInMillis
    }

    fun totalForMonth(entries: List<TimeEntry>, date: Long): Long {
        val (start, end) = monthBounds(date)
        return entries.sumOf { overlap(it, start, end) }
    }

    fun canReplace(entries: List<TimeEntry>, index: Int, replacement: TimeEntry): Boolean {
        if (index !in entries.indices || replacement.start < 0 || replacement.end <= replacement.start) return false
        return entries.withIndex().none { (otherIndex, entry) ->
            otherIndex != index && overlap(replacement, entry.start, entry.end) > 0L
        }
    }
}
