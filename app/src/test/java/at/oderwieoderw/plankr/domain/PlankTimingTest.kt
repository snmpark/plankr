package at.oderwieoderw.plankr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PlankTimingTest {
    @Test
    fun monotonicDurationIgnoresWallClockChanges() {
        assertEquals(800L, PlankTiming.duration(10_000, 2_000, 7, 100_000, 2_800, 7))
        assertEquals(1_200L, PlankTiming.duration(10_000, 2_000, 7, 9_000, 3_200, 7))
    }

    @Test
    fun olderSessionsAndRebootUseWallClockFallback() {
        assertEquals(1_200L, PlankTiming.duration(10_000, null, null, 11_200, 3_200, 7))
        assertEquals(1_200L, PlankTiming.duration(10_000, 2_000, 7, 11_200, 3_200, 8))
        assertEquals(0L, PlankTiming.duration(10_000, 2_000, 7, 9_000, 100, 8))
    }

    @Test
    fun shortElapsedTimeIsPreservedWithoutARoundingThreshold() {
        assertEquals(1L, PlankTiming.duration(10_000, 2_000, 7, 10_001, 2_001, 7))
        assertEquals(800L, PlankTiming.duration(10_000, 2_000, 7, 10_800, 2_800, 7))
    }
}
