package at.oderwieoderw.plankr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeEntriesTest {
    private fun date(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun sessionCrossingMidnightCountsOnlyItsOverlapOnEachDay() {
        val entry = TimeEntry(date(2026, 9, 28, 23, 30), date(2026, 9, 29, 1, 15))
        assertEquals(30 * 60_000L, TimeEntries.totalForDay(listOf(entry), date(2026, 9, 28, 12)))
        assertEquals(75 * 60_000L, TimeEntries.totalForDay(listOf(entry), date(2026, 9, 29, 12)))
    }

    @Test
    fun multipleSessionsAreSummedAndOtherDaysIgnored() {
        val entries = listOf(
            TimeEntry(date(2026, 9, 29, 9), date(2026, 9, 29, 10)),
            TimeEntry(date(2026, 9, 29, 11), date(2026, 9, 29, 11, 30)),
            TimeEntry(date(2026, 9, 30, 9), date(2026, 9, 30, 10))
        )
        assertEquals(90 * 60_000L, TimeEntries.totalForDay(entries, date(2026, 9, 29, 12)))
    }

    @Test
    fun sessionEndingAtMidnightDoesNotMarkTheNextDay() {
        val entry = TimeEntry(date(2026, 9, 28, 23), date(2026, 9, 29, 0))
        assertEquals(0L, TimeEntries.totalForDay(listOf(entry), date(2026, 9, 29, 12)))
    }

    @Test
    fun monthTotalsSplitTimedSessionsAtTheMonthBoundary() {
        val (monthStart, nextMonthStart) = TimeEntries.monthBounds(date(2026, 9, 29, 12))
        val crossingStart = TimeEntry(monthStart - 30 * 60_000L, monthStart + 45 * 60_000L)
        val crossingEnd = TimeEntry(nextMonthStart - 15 * 60_000L, nextMonthStart + 20 * 60_000L)

        assertEquals(60 * 60_000L, TimeEntries.totalForMonth(listOf(crossingStart, crossingEnd), monthStart))
        assertEquals(20 * 60_000L, TimeEntries.totalForMonth(listOf(crossingStart, crossingEnd), nextMonthStart))
    }

    @Test
    fun editingCanMoveASessionWithoutOverlappingAnother() {
        val entries = listOf(TimeEntry(1_000L, 2_000L), TimeEntry(3_000L, 4_000L))
        assertTrue(TimeEntries.canReplace(entries, 0, TimeEntry(2_000L, 3_000L)))
        assertFalse(TimeEntries.canReplace(entries, 0, TimeEntry(2_000L, 3_001L)))
        assertFalse(TimeEntries.canReplace(entries, 0, TimeEntry(2_000L, 2_000L)))
        assertFalse(TimeEntries.canReplace(entries, 2, TimeEntry(4_000L, 5_000L)))
    }
}
