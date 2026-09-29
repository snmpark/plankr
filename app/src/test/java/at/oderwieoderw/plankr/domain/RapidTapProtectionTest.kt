package at.oderwieoderw.plankr.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RapidTapProtectionTest {
    @Test
    fun ignoresQuickRepeatActions() {
        assertFalse(RapidTapProtection.blocks(1_000, null, 10_000, null))
        assertTrue(RapidTapProtection.blocks(1_500, 1_000, 10_500, 10_000))
        assertTrue(RapidTapProtection.blocks(2_500, 1_000, 11_500, null))
        assertFalse(RapidTapProtection.blocks(3_000, 1_000, 12_000, 10_000))
    }

    @Test
    fun protectsRecentActiveSessionAfterScreenRecreation() {
        assertTrue(RapidTapProtection.blocks(100, null, 10_500, 10_000))
        assertFalse(RapidTapProtection.blocks(100, null, 12_000, 10_000))
    }
}
