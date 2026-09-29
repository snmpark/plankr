package at.oderwieoderw.plankr.ui.tracking

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import at.oderwieoderw.plankr.R
import at.oderwieoderw.plankr.data.local.TimeEntryStore
import at.oderwieoderw.plankr.domain.TimeEntries
import at.oderwieoderw.plankr.domain.TimeEntry
import at.oderwieoderw.plankr.domain.MonthlyMotivation
import at.oderwieoderw.plankr.ui.TimeFormatting

/** Owns the plank timer and monthly challenge. */
class TrackingScreen(
    private val activity: ComponentActivity,
    private val store: TimeEntryStore,
    private val onChanged: () -> Unit
) {
    private data class PlankQuote(val text: Int, val author: Int)
    private data class ActiveMessage(
        val text: String,
        val author: String? = null,
        val isRecall: Boolean = false,
        val isQuote: Boolean = false
    ) {
        val isQuestion: Boolean get() = !isQuote && !isRecall
    }

    private val sessionButton: Button = activity.findViewById(R.id.session_button)
    private val trackDetails: View = activity.findViewById(R.id.track_details)
    private val activeMotivation: LinearLayout = activity.findViewById(R.id.active_motivation)
    private val activeMessageText: TextView = activity.findViewById(R.id.active_message_text)
    private val activeMessageAuthor: TextView = activity.findViewById(R.id.active_message_author)
    private val activeQuotes = listOf(
        PlankQuote(R.string.plank_quote_james, R.string.plank_quote_author_james),
        PlankQuote(R.string.plank_quote_franklin, R.string.plank_quote_author_franklin),
        PlankQuote(R.string.plank_quote_epictetus, R.string.plank_quote_author_epictetus),
        PlankQuote(R.string.plank_quote_virgil, R.string.plank_quote_author_virgil),
        PlankQuote(R.string.plank_quote_earhart, R.string.plank_quote_author_earhart),
        PlankQuote(R.string.plank_quote_keller, R.string.plank_quote_author_keller)
    )
    private val featuredQuote = ActiveMessage(activity.getString(R.string.plank_quote_oderwie), isQuote = true)
    private val activeMessages = activeQuotes.map {
        ActiveMessage(activity.getString(it.text), activity.getString(it.author), isQuote = true)
    } + featuredQuote + activity.resources.getStringArray(R.array.plank_questions).map { ActiveMessage(it) } +
        activity.resources.getStringArray(R.array.plank_recall_questions).map { ActiveMessage(it, isRecall = true) }
    private val trackPage: View = activity.findViewById(R.id.track_page)
    private val actionContainer: FrameLayout = activity.findViewById(R.id.session_action_container)
    private val actionTitle: TextView = activity.findViewById(R.id.session_action_title)
    private val actionIcon: ImageView = activity.findViewById(R.id.session_action_icon)
    private val actionHint: TextView = activity.findViewById(R.id.action_hint)
    private val timerLabel: TextView = activity.findViewById(R.id.timer_label)
    private val counter: TextView = activity.findViewById(R.id.counter_text)
    private val todayTotal: TextView = activity.findViewById(R.id.today_total)
    private val remainingLabel: TextView = activity.findViewById(R.id.remaining_label)
    private val remainingTime: TextView = activity.findViewById(R.id.remaining_time)
    private val monthlyProgress: TextView = activity.findViewById(R.id.monthly_progress)
    private val progressBar: ProgressBar = activity.findViewById(R.id.monthly_progress_bar)

    private var feedbackUntil = 0L
    private var completedDuration = 0L
    private var lastActionAt = 0L
    private var displayedSession: Long? = null
    private var currentMessage: ActiveMessage? = null
    private var nextMessage: ActiveMessage? = null
    private var nextMessageAt = 0L

    init {
        sessionButton.setOnClickListener {
            val actionAt = SystemClock.elapsedRealtime()
            if (actionAt - lastActionAt < 700L) return@setOnClickListener
            lastActionAt = actionAt
            val now = System.currentTimeMillis()
            val active = store.activeStart
            if (active == null) {
                feedbackUntil = 0L
                actionHint.animate().cancel()
                actionHint.alpha = 1f
                store.start(now)
            } else {
                store.stop(now)
                if (store.activeStart == null) {
                    completedDuration = now - active
                    feedbackUntil = now + 6_000L
                }
            }
            if ((store.activeStart != null) != (active != null)) {
                sessionButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            onChanged()
            actionContainer.animate().cancel()
            actionContainer.scaleX = 0.98f
            actionContainer.scaleY = 0.98f
            actionContainer.animate().scaleX(1f).scaleY(1f).setDuration(180L).start()
            if (active != null && store.activeStart == null) {
                actionHint.animate().cancel()
                actionHint.alpha = 0f
                actionHint.animate().alpha(1f).setDuration(300L).start()
            }
        }
    }

    fun render(now: Long, entries: List<TimeEntry>, active: Long?) {
        trackDetails.visibility = if (active == null) View.VISIBLE else View.GONE
        renderActiveMotivation(active)
        sessionButton.isSelected = active != null
        sessionButton.contentDescription = activity.getString(if (active == null) R.string.start_working else R.string.finish_work)
        actionTitle.setText(if (active == null) R.string.start_working else R.string.finish_work)
        actionIcon.setImageResource(if (active == null) R.drawable.ic_start_work else R.drawable.ic_finish_work)
        actionHint.text = when {
            active != null -> activity.getString(R.string.finish_hint)
            now < feedbackUntil -> activity.getString(R.string.session_complete, TimeFormatting.duration(completedDuration))
            else -> activity.getString(R.string.start_hint)
        }
        timerLabel.setText(if (active == null) R.string.ready_to_track else R.string.tracking_now)
        counter.text = TimeFormatting.elapsed((now - (active ?: now)).coerceAtLeast(0L))
        counter.contentDescription = activity.getString(R.string.elapsed_time_value, counter.text)
        todayTotal.text = TimeFormatting.duration(TimeEntries.totalForDay(entries, now))
        renderMonthlyGoal(entries, now)
    }

    private fun renderActiveMotivation(active: Long?) {
        activeMotivation.visibility = if (active == null) View.GONE else View.VISIBLE
        if (active == null) {
            activeMotivation.animate().cancel()
            activeMotivation.alpha = 1f
            displayedSession = null
            currentMessage = null
            nextMessage = null
            return
        }
        val newSession = displayedSession != active
        val elapsed = SystemClock.elapsedRealtime()
        if (newSession) {
            displayedSession = active
            currentMessage = null
            nextMessage = pickNextMessage(null)
            nextMessageAt = elapsed + (10_000L..20_000L).random()
            activeMotivation.animate().cancel()
            activeMotivation.alpha = 0f
            activeMotivation.contentDescription = null
            activeMessageText.text = ""
            activeMessageAuthor.visibility = View.GONE
        }
        if (trackPage.visibility != View.VISIBLE) return
        // A recall prompt must refer to the question shown about ten seconds ago, not one
        // that went unseen while the app was in the background or on the calendar page.
        if (nextMessage?.isRecall == true && elapsed - nextMessageAt > 2_000L) {
            nextMessage = pickNextMessage(currentMessage, allowRecall = false)
            nextMessageAt = elapsed
        }
        if (elapsed < nextMessageAt) return
        val message = nextMessage ?: return
        val firstMessage = currentMessage == null
        currentMessage = message
        nextMessage = pickNextMessage(message)
        nextMessageAt = elapsed + if (nextMessage?.isRecall == true) 10_000L else 25_000L
        activeMotivation.animate().cancel()
        if (firstMessage) {
            showMessage(message)
            activeMotivation.alpha = 0f
            activeMotivation.animate().alpha(1f).setDuration(600L).start()
        } else {
            activeMotivation.animate().alpha(0f).setDuration(200L).withEndAction {
                if (displayedSession == active && activeMotivation.visibility == View.VISIBLE) {
                    showMessage(message)
                    activeMotivation.animate().alpha(1f).setDuration(600L).start()
                }
            }.start()
        }
    }

    private fun pickNextMessage(previous: ActiveMessage?, allowRecall: Boolean = true): ActiveMessage {
        if (allowRecall && previous?.isQuestion == true && (1..5).random() == 1) {
            return activeMessages.filter { it.isRecall }.random()
        }
        // Give the featured quote roughly one in five regular slots without repeating it twice.
        if (previous != featuredQuote && (1..5).random() == 1) return featuredQuote
        return activeMessages.filter { !it.isRecall && it != previous }.random()
    }

    private fun showMessage(message: ActiveMessage) {
        activeMessageText.text = message.text
        activeMessageAuthor.visibility = if (message.author == null) View.GONE else View.VISIBLE
        message.author?.let { activeMessageAuthor.text = activity.getString(R.string.quote_author, it) }
        activeMotivation.contentDescription = message.author?.let {
            activity.getString(R.string.quote_with_author, message.text, it)
        } ?: message.text
    }

    private fun renderMonthlyGoal(entries: List<TimeEntry>, now: Long) {
        val tracked = TimeEntries.totalForMonth(entries, now)
        val goal = MonthlyMotivation.GOAL_MILLIS
        val remaining = (goal - tracked).coerceAtLeast(0L)
        remainingLabel.setText(when {
            tracked > goal -> R.string.monthly_extra_label
            remaining > 0L -> R.string.remaining_this_month
            else -> R.string.monthly_goal_reached
        })
        remainingTime.text = if (remaining == 0L) activity.getString(R.string.monthly_goal_done)
            else TimeFormatting.countdown(remaining)
        progressBar.progress = ((tracked * 100L / goal).coerceIn(0L, 100L)).toInt()
        val progress = activity.getString(R.string.monthly_progress, TimeFormatting.duration(tracked))
        monthlyProgress.text = if (tracked > goal) {
            "$progress · ${activity.getString(R.string.monthly_extra, TimeFormatting.duration(tracked - goal))}"
        } else {
            progress
        }
    }

}
