package at.oderwieoderw.plankr.data.local

import android.content.Context
import android.provider.Settings
import at.oderwieoderw.plankr.domain.PlankTiming
import at.oderwieoderw.plankr.domain.TimeEntries
import at.oderwieoderw.plankr.domain.TimeEntry
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Small on-device store; the active session is saved as soon as the start hold completes. */
class TimeEntryStore(context: Context) {
    private val preferences = context.getSharedPreferences("time_entries", Context.MODE_PRIVATE)
    private val resolver = context.contentResolver
    private val currentBootCount by lazy {
        Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT, -1)
    }

    val activeStart: Long?
        get() = preferences.getLong("active_start", -1L).takeIf { it >= 0L }

    fun entries(): List<TimeEntry> {
        val json = try {
            JSONArray(preferences.getString("entries", "[]"))
        } catch (_: JSONException) {
            return emptyList()
        }
        return buildList {
            for (index in 0 until json.length()) {
                try {
                    val item = json.getJSONObject(index)
                    // Prototype manual contributions are not part of the timer-only challenge.
                    if (item.optBoolean("manual", false)) continue
                    val start = item.getLong("start")
                    val end = item.getLong("end")
                    if (start >= 0 && end > start) add(TimeEntry(start, end))
                } catch (_: JSONException) {
                    // A damaged entry should not hide other saved sessions.
                }
            }
        }
    }

    /** Elapsed milliseconds for the active session, or zero when idle. */
    fun activeDuration(nowWallMillis: Long, nowElapsedMillis: Long): Long {
        val start = activeStart ?: return 0L
        val startElapsed = preferences.getLong("active_elapsed_start", -1L).takeIf { it >= 0L }
        val startBoot = preferences.getInt("active_boot_count", -1).takeIf { it >= 0 }
        return PlankTiming.duration(
            startWallMillis = start,
            startElapsedMillis = startElapsed,
            startBootCount = startBoot,
            nowWallMillis = nowWallMillis,
            nowElapsedMillis = nowElapsedMillis,
            nowBootCount = currentBootCount
        )
    }

    /** Persists both clocks when the start hold completes; does nothing if already active. */
    fun start(nowWallMillis: Long, nowElapsedMillis: Long) {
        if (activeStart != null) return
        preferences.edit()
            .putLong("active_start", nowWallMillis)
            .putLong("active_elapsed_start", nowElapsedMillis)
            .putInt("active_boot_count", currentBootCount)
            .commit()
    }

    /**
     * Clears the active timer and saves its exact positive duration.
     * Returns null if idle, no time elapsed, or the disk write failed.
     */
    fun stop(nowWallMillis: Long, nowElapsedMillis: Long): TimeEntry? {
        val start = activeStart ?: return null
        val duration = activeDuration(nowWallMillis, nowElapsedMillis)
        // Anchor the interval to its original calendar start, even if the wall clock changed.
        val entry = if (duration > 0L) TimeEntry(start, start + duration) else null
        // An attempt with no elapsed time has nothing to save. Clear the timer either way.
        val edit = preferences.edit()
            .putLong("active_start", -1L)
            .remove("active_elapsed_start")
            .remove("active_boot_count")
        if (entry != null) edit.putString("entries", serialize(entries() + entry))
        return if (edit.commit()) entry else null
    }

    fun updateEntry(index: Int, original: TimeEntry, replacement: TimeEntry): Boolean {
        val saved = entries()
        val active = activeStart
        if (
            saved.getOrNull(index) != original ||
            !TimeEntries.canReplace(saved, index, replacement) ||
            replacement.end > System.currentTimeMillis() ||
            (active != null && replacement.end > active)
        ) {
            return false
        }
        val updated = saved.toMutableList().apply { this[index] = replacement }
        return saveEntries(updated)
    }

    fun deleteEntry(index: Int, original: TimeEntry): Boolean {
        val saved = entries()
        if (saved.getOrNull(index) != original) return false
        val updated = saved.toMutableList().apply { removeAt(index) }
        return saveEntries(updated)
    }

    private fun saveEntries(entries: List<TimeEntry>): Boolean {
        return preferences.edit().putString("entries", serialize(entries)).commit()
    }

    private fun serialize(entries: List<TimeEntry>): String {
        val json = JSONArray()
        entries.forEach { entry ->
            json.put(JSONObject().put("start", entry.start).put("end", entry.end))
        }
        return json.toString()
    }
}
