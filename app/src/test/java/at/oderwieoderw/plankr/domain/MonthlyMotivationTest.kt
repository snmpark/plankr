package at.oderwieoderw.plankr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlyMotivationTest {
    private val minute = 60_000L

    @Test
    fun messagesFollowRemainingMonthlyPlankTime() {
        assertEquals(GoalMessageStage.GETTING_STARTED, MonthlyMotivation.stage(0))
        assertEquals(GoalMessageStage.ON_TRACK, MonthlyMotivation.stage(15 * minute))
        assertEquals(GoalMessageStage.NEARLY_THERE, MonthlyMotivation.stage(45 * minute))
        assertEquals(GoalMessageStage.GOAL_MET, MonthlyMotivation.stage(60 * minute))
        assertEquals(GoalMessageStage.EXTRA, MonthlyMotivation.stage(60 * minute + 1))
    }
}
