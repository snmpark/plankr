package at.oderwieoderw.plankr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlankPromptsInstrumentedTest {
    @Test
    fun activePromptsContainAtLeastOneHundredDistinctItems() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val questions = resources.getStringArray(R.array.plank_questions).toList()
        val recall = resources.getStringArray(R.array.plank_recall_questions).toList()
        val total = questions + recall

        assertTrue(total.size >= 100) // The six attributed quotes are additional items.
        assertEquals(total.size, total.distinct().size)
        assertTrue(total.all { it.isNotBlank() })
    }
}
