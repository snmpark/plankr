package at.oderwieoderw.plankr.domain

object RapidTapProtection {
    const val INTERVAL_MILLIS = 2_000L

    fun blocks(
        elapsedNow: Long,
        lastActionAt: Long?,
        wallNow: Long,
        activeStart: Long?
    ): Boolean =
        (lastActionAt != null && elapsedNow - lastActionAt in 0 until INTERVAL_MILLIS) ||
            (activeStart != null && wallNow - activeStart in 0 until INTERVAL_MILLIS)
}
