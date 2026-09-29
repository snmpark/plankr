package at.oderwieoderw.plankr.ui

import java.util.Calendar
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimeFormattingTest {
    private lateinit var originalLocale: Locale

    @Before
    fun useUsLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun liveCounterShowsMilliseconds() {
        assertEquals("00:00:00.000", TimeFormatting.elapsed(0))
        assertEquals("00:00:00.009", TimeFormatting.elapsed(9))
        assertEquals("00:00:01.234", TimeFormatting.elapsed(1_234))
        assertEquals("01:02:03.004", TimeFormatting.elapsed(3_723_004))
    }

    @Test
    fun durationLabelsKeepMillisecondsAtEveryScale() {
        assertEquals("0.000s", TimeFormatting.duration(0))
        assertEquals("0.001s", TimeFormatting.duration(1))
        assertEquals("0.999s", TimeFormatting.duration(999))
        assertEquals("1.000s", TimeFormatting.duration(1_000))
        assertEquals("39.123s", TimeFormatting.duration(39_123))
        assertEquals("1m 05.007s", TimeFormatting.duration(65_007))
        assertEquals("1h 00m 00.000s", TimeFormatting.duration(3_600_000))
        assertEquals("1h 02m 03.456s", TimeFormatting.duration(3_723_456))
    }

    @Test
    fun remainingTimePreservesExactMilliseconds() {
        assertEquals("1h 00m 00.000s", TimeFormatting.countdown(3_600_000))
        assertEquals("0h 59m 59.001s", TimeFormatting.countdown(3_600_000 - 999))
        assertEquals("0h 59m 59.000s", TimeFormatting.countdown(3_600_000 - 1_000))
        assertEquals("0h 00m 00.001s", TimeFormatting.countdown(1))
        assertEquals("0h 00m 00.000s", TimeFormatting.countdown(0))
    }

    @Test
    fun calendarTimesInsertMillisecondsBeforeTheAmPmMarker() {
        val timestamp = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 29, 13, 2, 3)
            set(Calendar.MILLISECOND, 4)
        }.timeInMillis
        val text = TimeFormatting.timeOfDay(timestamp)
        assertTrue(text.startsWith("1:02:03.004"))
        assertTrue(text.endsWith("PM"))

        Locale.setDefault(Locale.GERMANY)
        assertEquals("13:02:03.004", TimeFormatting.timeOfDay(timestamp))
    }
}
