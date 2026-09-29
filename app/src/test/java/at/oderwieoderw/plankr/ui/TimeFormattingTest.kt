package at.oderwieoderw.plankr.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TimeFormattingTest {
    @Test
    fun shortPlanksKeepTheirSeconds() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("0s", TimeFormatting.duration(0))
            assertEquals("<1s", TimeFormatting.duration(1))
            assertEquals("<1s", TimeFormatting.duration(999))
            assertEquals("1s", TimeFormatting.duration(1_000))
            assertEquals("39s", TimeFormatting.duration(39_000))
            assertEquals("1m 05s", TimeFormatting.duration(65_000))
            assertEquals("1h 00m 00s", TimeFormatting.duration(3_600_000))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun remainingTimeDoesNotLoseASecondUntilAWholeSecondIsTracked() {
        assertEquals("1h 00m 00s", TimeFormatting.countdown(3_600_000 - 999))
        assertEquals("0h 59m 59s", TimeFormatting.countdown(3_600_000 - 1_000))
    }
}
