package at.oderwieoderw.plankr.domain

/** Elapsed-time calculations for persisted plank sessions. */
object PlankTiming {
    /**
     * Uses monotonic milliseconds while both readings belong to the same device boot.
     * Legacy sessions and sessions spanning a reboot fall back to wall-clock time.
     */
    fun duration(
        startWallMillis: Long,
        startElapsedMillis: Long?,
        startBootCount: Int?,
        nowWallMillis: Long,
        nowElapsedMillis: Long,
        nowBootCount: Int
    ): Long {
        val sameBoot =
            startBootCount != null && startBootCount >= 0 && startBootCount == nowBootCount
        return if (
            sameBoot && startElapsedMillis != null && nowElapsedMillis >= startElapsedMillis
        ) {
            nowElapsedMillis - startElapsedMillis
        } else {
            (nowWallMillis - startWallMillis).coerceAtLeast(0L)
        }
    }
}
