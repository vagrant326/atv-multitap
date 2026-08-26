package io.github.vagrant326.atvmultitap.ime

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.vagrant326.atvmultitap.R
import io.github.vagrant326.atvmultitap.core.Keypad
import io.github.vagrant326.atvmultitap.core.LetterCase
import io.github.vagrant326.atvmultitap.settings.HintMode

/**
 * The strip: which key is under the thumb, what it carries, and where in that run the letter has
 * got to.
 *
 * T9's strip exists to show what the presses *might* have meant. Nothing is ambiguous here, so
 * this one answers the only two questions this method actually raises — how many more taps to the
 * letter I want, and how many I have already spent. Both are unanswerable on this hardware
 * without a screen: **the remote has no letters printed on it**, which is the one respect in
 * which multitap on a television is harder than multitap on the phone everyone learnt it on.
 *
 * The letter in progress is deliberately *not* duplicated here. It is in the field, underlined,
 * where the user is already looking — and the field is where it will stay.
 */
class MultitapStripView(context: Context) : LinearLayout(context) {

    /**
     * The run of letters on the key being tapped, with the current one filled.
     *
     * Always the same height whether or not a letter is in progress. An input view that grows and
     * shrinks on every keystroke moves the field it is attached to, and on a search screen that
     * means the results underneath jump on every press.
     */
    private val cycle = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(46)
    }

    private val status = TextView(context).apply {
        setTextColor(MUTED)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    }

    /**
     * The physical numpad, with the letters the remote does not carry.
     *
     * Ported from LetterWise by way of T9. It earns its place here for a reason neither of them
     * has: on this keyboard the number of taps *is* the position in the run, so the grid is not
     * merely a reminder of where `w` lives — it is the price list.
     */
    private val keypad = LinearLayout(context).apply {
        orientation = VERTICAL
        layoutParams = LayoutParams(dp(300), LayoutParams.WRAP_CONTENT)
    }

    private val keypadCells = mutableMapOf<Char, TextView>()

    private val deleteValue = hintValue()

    /**
     * The assigned keys, named rather than drawn into the grid, and set beside it. A key printed
     * `TEXT` does not sit where a phone has `*`, so putting it in a cell would lie about where
     * to reach for it. Beside, because the space right of a three-cell grid was going spare and
     * vertical space is what the field underneath is short of.
     */
    private val hints = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(20), 0, 0, 0)
        layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            gravity = Gravity.TOP
            topMargin = dp(2)
        }
        addView(hintLine(context.getString(R.string.strip_hint_next), hintValue().apply {
            text = context.getString(R.string.strip_next_keys)
        }))
        addView(hintLine(context.getString(R.string.strip_hint_back), hintValue().apply {
            text = context.getString(R.string.strip_back_keys)
        }))
        addView(hintLine(context.getString(R.string.strip_hint_commit), hintValue().apply {
            text = context.getString(R.string.strip_commit_keys)
        }))
        addView(hintLine(context.getString(R.string.strip_hint_delete), deleteValue))
        addView(hintLine(context.getString(R.string.strip_hint_case), hintValue().apply {
            text = context.getString(R.string.strip_case_keys)
        }))
    }

    /**
     * A weighted spacer mirroring [hints] keeps the grid centred while the hints sit to its
     * right. Without it the grid is pushed left by whatever is beside it.
     */
    private val hintRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(6) }
        addView(View(context).apply { layoutParams = LayoutParams(0, 1, 1f) })
        addView(keypad)
        addView(hints)
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(BACKGROUND)
        setPadding(dp(20), dp(12), dp(20), dp(12))
        buildKeypad()

        addView(
            HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                addView(cycle)
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
        )
        addView(status.apply { setPadding(0, dp(6), 0, 0) })
        addView(hintRow)
    }

    /**
     * `123` / `456` / `789` / `0`. No `*` or `#` row — this is not a phone and those keys are
     * not on every remote, so drawing them promises buttons that may not exist.
     *
     * Built once. Unlike LetterWise, whose partition changes with the language, E.161 is fixed
     * and Polish simply puts more letters on the same eight keys, so both alphabets are shown at
     * once and there is nothing that could change them.
     */
    private fun buildKeypad() {
        for (row in listOf("123", "456", "789", " 0 ")) {
            val line = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }
            for (key in row) {
                line.addView(cell(key))
            }
            keypad.addView(line)
        }
    }

    /**
     * What one cell says, in the case that is currently in force.
     *
     * The grid is where the case is legible rather than merely announced. A tag reading `ABC`
     * tells a user who already knows what the tag means; eight cells reading `ABCĄĆ` tell
     * everyone else, and this is the surface that exists precisely because the remote itself
     * says nothing.
     */
    private fun cellText(key: Char, letterCase: LetterCase): String {
        if (key == ' ') {
            return ""
        }
        val letters = when (key) {
            '0' -> context.getString(R.string.strip_space)
            '1' -> Keypad.MARKS
            else -> Keypad.lettersOn(key).map(letterCase::apply).joinToString("")
        }
        return "$key\n$letters"
    }

    private fun cell(key: Char): TextView {
        return TextView(context).apply {
            text = cellText(key, LetterCase.LOWER)
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setLineSpacing(0f, 0.95f)
            setPadding(dp(6), dp(4), dp(6), dp(4))
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
                topMargin = dp(3)
            }
            if (key != ' ') {
                setBackgroundColor(CELL)
            }
            keypadCells[key] = this
        }
    }

    /** Two columns, so the values line up instead of drifting with label length. */
    private fun hintLine(label: String, value: TextView) = LinearLayout(context).apply {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(3) }
        addView(
            TextView(context).apply {
                text = label
                setTextColor(MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                layoutParams = LayoutParams(dp(84), LayoutParams.WRAP_CONTENT)
            }
        )
        addView(value)
    }

    private fun hintValue() = TextView(context).apply {
        setTextColor(SECONDARY)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
    }

    private fun keyLabel(keyCode: Int, fallback: String): String =
        if (keyCode == KeyBindings.NO_KEY) {
            fallback
        } else {
            KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        }

    fun render(state: StripState) {
        cycle.removeAllViews()
        if (state.cycle.isEmpty() || state.digits) {
            cycle.addView(idle(state))
        } else {
            cycle.addView(keyBadge(state.cycleDigit))
            state.cycle.forEachIndexed { index, letter ->
                cycle.addView(
                    stop(
                        state.letterCase.apply(letter),
                        reached = index == state.cycleIndex,
                        passed = index < state.cycleIndex,
                    )
                )
            }
        }

        // Named as well as drawn, because the two hint modes below the default hide the grid and
        // a locked case has to survive turning the grid off.
        val caseTag = when (state.letterCase) {
            LetterCase.LOWER -> ""
            LetterCase.ONCE -> context.getString(R.string.strip_case_once)
            LetterCase.LOCKED -> context.getString(R.string.strip_case_locked)
        }
        val message = when {
            // Says so rather than looking broken: raised by the trigger over an app that never
            // asked for input, there is nowhere to send characters.
            !state.hasEditor -> context.getString(R.string.strip_no_editor)
            else -> listOf(
                if (state.digits) context.getString(R.string.strip_digits) else "",
                caseTag,
            ).filter { it.isNotEmpty() }.joinToString(" · ")
        }
        status.text = message
        status.visibility = if (message.isEmpty()) GONE else VISIBLE

        // Digits are one press each, so the grid has nothing to price and the field underneath
        // gets the room back.
        val gridVisible = state.hintMode == HintMode.KEYPAD && !state.digits && state.hasEditor
        hintRow.visibility = if (gridVisible) VISIBLE else GONE

        // OFF assumes everything, including the run of letters. It can, and nothing else in this
        // programme can: the letter in progress is in the field, underlined, so a user who knows
        // the keypad loses no state by turning the whole strip off.
        cycle.visibility = if (state.hintMode == HintMode.OFF) GONE else VISIBLE
        if (!gridVisible) {
            return
        }

        deleteValue.text = keyLabel(
            state.customKeys.delete,
            context.getString(R.string.strip_fallback_delete),
        )

        for ((key, view) in keypadCells) {
            view.text = cellText(key, state.letterCase)
            view.setTextColor(if (key == state.cycleDigit) ACCENT else MUTED)
        }
    }

    /** What the row says when no key is being tapped, so the height never changes. */
    private fun idle(state: StripState) = TextView(context).apply {
        text = context.getString(
            if (state.digits) R.string.strip_idle_digits else R.string.strip_idle
        )
        setTextColor(MUTED)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setPadding(0, dp(8), 0, dp(8))
    }

    /** Which key this run belongs to, because the grid highlight is small and far away. */
    private fun keyBadge(digit: Char?) = TextView(context).apply {
        text = digit?.toString().orEmpty()
        setTextColor(SECONDARY)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        setPadding(dp(10), dp(6), dp(16), dp(8))
    }

    /**
     * One stop on the run. The one reached is filled rather than merely brighter: at three metres
     * on a panel of unknown calibration, a colour difference is not reliably a difference at all.
     *
     * The stops before it are dimmed and the ones after are not, so the row reads as a distance
     * left to travel rather than as a set of options — which is what it is. Nothing here is a
     * choice the keyboard is making; it is a count of presses the user has not made yet.
     */
    private fun stop(letter: Char, reached: Boolean, passed: Boolean) = TextView(context).apply {
        text = letter.toString()
        setTextColor(
            when {
                reached -> Color.BLACK
                passed -> MUTED
                else -> SECONDARY
            }
        )
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (reached) 22f else 20f)
        setPadding(dp(12), dp(6), dp(12), dp(8))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(if (reached) ACCENT else Color.TRANSPARENT)
        }
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { marginEnd = dp(4) }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val BACKGROUND = 0xFF08080B.toInt()
        const val ACCENT = 0xFF7FD1FF.toInt()
        const val SECONDARY = 0xFFB0B0BC.toInt()
        const val MUTED = 0xFF6B6B78.toInt()
        const val CELL = 0xFF1A1A22.toInt()
    }
}

/** Everything the strip draws, so rendering has no opinion of its own about keyboard state. */
data class StripState(
    val cycle: String,
    val cycleDigit: Char?,
    val cycleIndex: Int,
    val hintMode: HintMode,
    val letterCase: LetterCase,
    val digits: Boolean,
    val hasEditor: Boolean,
    val customKeys: CustomKeys,
)
