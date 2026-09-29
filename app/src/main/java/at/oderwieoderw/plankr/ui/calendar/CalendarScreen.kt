package at.oderwieoderw.plankr.ui.calendar

import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import at.oderwieoderw.plankr.R
import at.oderwieoderw.plankr.data.local.TimeEntryStore
import at.oderwieoderw.plankr.domain.TimeEntries
import at.oderwieoderw.plankr.domain.TimeEntry
import at.oderwieoderw.plankr.ui.TimeFormatting
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Calendar selection and the completed sessions belonging to the selected day. */
class CalendarScreen(
    private val activity: ComponentActivity,
    store: TimeEntryStore,
    initialDate: Long,
    private val onChanged: () -> Unit
) {
    private data class SessionRenderKey(
        val selectedDay: Long,
        val today: Long,
        val saved: List<TimeEntry>,
        val activeStart: Long?
    )

    var selectedDate: Long = initialDate
        private set

    private val grid: GridLayout = activity.findViewById(R.id.calendar_grid)
    private val monthLabel: TextView = activity.findViewById(R.id.month_label)
    private val selectedDateLabel: TextView = activity.findViewById(R.id.selected_date_label)
    private val selectedTotal: TextView = activity.findViewById(R.id.selected_total)
    private val sessionList: LinearLayout = activity.findViewById(R.id.session_list)
    private val dialogs = SessionDialogs(activity, store, onChanged)
    private var renderedCalendarKey: String? = null
    private var renderedSessions: SessionRenderKey? = null

    init {
        activity.findViewById<Button>(R.id.previous_month).setOnClickListener { changeMonth(-1) }
        activity.findViewById<Button>(R.id.next_month).setOnClickListener { changeMonth(1) }
    }

    fun render(now: Long, savedEntries: List<TimeEntry>, active: Long?) {
        val entries = savedEntries + listOfNotNull(active?.let { TimeEntry(it, maxOf(now, it + 1L)) })
        renderCalendar(entries, now)
        selectedDateLabel.text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(selectedDate))
        selectedTotal.text = activity.getString(R.string.day_total, TimeFormatting.duration(TimeEntries.totalForDay(entries, selectedDate)))
        renderSessions(savedEntries, active, now)
    }

    private fun changeMonth(offset: Int) {
        selectedDate = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            add(Calendar.MONTH, offset)
        }.timeInMillis
        onChanged()
    }

    private fun renderCalendar(entries: List<TimeEntry>, now: Long) {
        val first = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val year = first.get(Calendar.YEAR)
        val month = first.get(Calendar.MONTH)
        val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val selectedDay = Calendar.getInstance().apply { timeInMillis = selectedDate }.get(Calendar.DAY_OF_MONTH)
        val markedDays = (1..daysInMonth).filter { day ->
            first.set(Calendar.DAY_OF_MONTH, day)
            TimeEntries.totalForDay(entries, first.timeInMillis) > 0L
        }.toSet()
        val key = "$year:$month:$selectedDay:${markedDays.joinToString(",")}:${TimeEntries.dayBounds(now).first}"
        if (key == renderedCalendarKey) return
        renderedCalendarKey = key

        first.set(Calendar.DAY_OF_MONTH, 1)
        monthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(first.time)
        grid.removeAllViews()
        val weekStart = first.firstDayOfWeek
        val weekdayNames = DateFormatSymbols.getInstance().shortWeekdays
        for (column in 0..6) {
            val weekday = (weekStart - 1 + column) % 7 + 1
            grid.addView(TextView(activity).apply {
                text = weekdayNames[weekday]
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(activity.getColor(R.color.muted))
                layoutParams = gridParams(0, column, 32)
            })
        }

        val leadingDays = (first.get(Calendar.DAY_OF_WEEK) - weekStart + 7) % 7
        val todayStart = TimeEntries.dayBounds(now).first
        val dateLabelFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val cellCount = ((leadingDays + daysInMonth + 6) / 7) * 7
        for (index in 0 until cellCount) {
            val day = index - leadingDays + 1
            val row = index / 7 + 1
            val column = index % 7
            if (day !in 1..daysInMonth) {
                grid.addView(View(activity).apply { layoutParams = gridParams(row, column, 52) })
                continue
            }
            first.set(Calendar.DAY_OF_MONTH, day)
            val date = first.timeInMillis
            val isSelected = day == selectedDay
            val isToday = TimeEntries.dayBounds(date).first == todayStart
            val hasTime = day in markedDays
            val label = dateLabelFormat.format(Date(date))
            val description = if (hasTime) activity.getString(R.string.tracked_day, label) else label
            grid.addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = gridParams(row, column, 52)
                if (isSelected) setBackgroundResource(R.drawable.selected_day_background)
                contentDescription = if (isSelected) activity.getString(R.string.selected_day, description) else description
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedDate = date
                    onChanged()
                }
                addView(TextView(activity).apply {
                    text = day.toString()
                    gravity = Gravity.CENTER
                    textSize = 16f
                    setTextColor(activity.getColor(if (isToday || isSelected) R.color.accent else R.color.ink))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
                addView(TextView(activity).apply {
                    text = "•"
                    textSize = 18f
                    gravity = Gravity.CENTER
                    setTextColor(activity.getColor(R.color.accent))
                    visibility = if (hasTime) View.VISIBLE else View.INVISIBLE
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
            })
        }
    }

    private fun renderSessions(savedEntries: List<TimeEntry>, active: Long?, now: Long) {
        val (dayStart, nextDayStart) = TimeEntries.dayBounds(selectedDate)
        val key = SessionRenderKey(dayStart, TimeEntries.dayBounds(now).first, savedEntries, active)
        if (key == renderedSessions) return
        renderedSessions = key
        sessionList.removeAllViews()
        val dayEntries = savedEntries.withIndex()
            .filter { TimeEntries.overlap(it.value, dayStart, nextDayStart) > 0L }
            .sortedByDescending { it.value.start }
        val activeEntry = active?.let { TimeEntry(it, maxOf(now, it + 1L)) }
            ?.takeIf { TimeEntries.overlap(it, dayStart, nextDayStart) > 0L }
        if (dayEntries.isEmpty() && activeEntry == null) {
            sessionList.addView(sessionRow(activity.getString(R.string.no_sessions)))
        } else {
            val timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM)
            if (activeEntry != null) {
                val start = timeFormat.format(Date(maxOf(activeEntry.start, dayStart)))
                sessionList.addView(sessionRow(activity.getString(R.string.in_progress, start)))
            }
            dayEntries.forEach { (index, entry) ->
                val start = timeFormat.format(Date(maxOf(entry.start, dayStart)))
                val end = timeFormat.format(Date(minOf(entry.end, nextDayStart)))
                val duration = TimeFormatting.duration(TimeEntries.overlap(entry, dayStart, nextDayStart))
                val text = activity.getString(R.string.session_range, start, end, duration)
                sessionList.addView(editableSessionRow(text, index, entry))
            }
        }
    }

    private fun editableSessionRow(text: String, index: Int, entry: TimeEntry): View =
        activity.layoutInflater.inflate(R.layout.item_session, sessionList, false).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * activity.resources.displayMetrics.density).toInt() }
            findViewById<TextView>(R.id.session_description).text = text
            val start = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.start))
            findViewById<Button>(R.id.edit_session).apply {
                contentDescription = activity.getString(R.string.edit_session_description, start)
                setOnClickListener { dialogs.showEditor(index, entry) }
            }
            findViewById<Button>(R.id.delete_session).apply {
                contentDescription = activity.getString(R.string.delete_session_description, start)
                setOnClickListener { dialogs.confirmDelete(index, entry) }
            }
        }

    private fun gridParams(row: Int, column: Int, heightDp: Int): GridLayout.LayoutParams =
        GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(column, 1f)).apply {
            width = 0
            height = (heightDp * activity.resources.displayMetrics.density).toInt()
        }

    private fun sessionRow(text: String): TextView = TextView(activity).apply {
        this.text = text
        textSize = 15f
        setTextColor(activity.getColor(R.color.ink))
        setBackgroundResource(R.drawable.card_background)
        gravity = Gravity.CENTER_VERTICAL
        val horizontal = (16 * activity.resources.displayMetrics.density).toInt()
        val vertical = (14 * activity.resources.displayMetrics.density).toInt()
        setPadding(horizontal, vertical, horizontal, vertical)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = (8 * activity.resources.displayMetrics.density).toInt() }
    }
}
