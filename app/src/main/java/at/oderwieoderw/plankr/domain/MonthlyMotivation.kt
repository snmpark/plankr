package at.oderwieoderw.plankr.domain

enum class GoalMessageStage {
    GETTING_STARTED, ON_TRACK, NEARLY_THERE, GOAL_MET, EXTRA
}

object MonthlyMotivation {
    const val GOAL_MILLIS = 60 * 60_000L

    fun stage(trackedMillis: Long): GoalMessageStage {
        val remaining = GOAL_MILLIS - trackedMillis
        return when {
            remaining < 0L -> GoalMessageStage.EXTRA
            remaining == 0L -> GoalMessageStage.GOAL_MET
            remaining <= GOAL_MILLIS / 4 -> GoalMessageStage.NEARLY_THERE
            remaining <= GOAL_MILLIS * 3 / 4 -> GoalMessageStage.ON_TRACK
            else -> GoalMessageStage.GETTING_STARTED
        }
    }
}
