package io.github.vagrant326.atvmultitap.ime

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import io.github.vagrant326.atvmultitap.core.Keypad
import io.github.vagrant326.atvmultitap.core.LetterCase
import io.github.vagrant326.atvmultitap.core.Multitap
import io.github.vagrant326.atvmultitap.core.Press
import io.github.vagrant326.atvmultitap.settings.Preferences

/**
 * The keyboard.
 *
 * Number keys carry the letters and you tap until the one you want appears. No dictionary, no
 * model, no candidates, nothing learnt and nothing stored: the presses for a word are the same
 * on the first day as on the thousandth, and the keyboard has no opinion that can turn out to be
 * wrong. That costs roughly twice the presses of T9 and buys the one thing none of the predictive
 * methods in this programme can offer — you never have to read the screen to find out what the
 * keyboard did with your last press.
 *
 * **Only the letter in progress is held here.** Everything before it is already real text in the
 * editor, which is what makes the caret keys, the delete key and a search box's own live
 * filtering behave the way they do everywhere else.
 */
class MultitapImeService : InputMethodService() {

    private lateinit var preferences: Preferences
    private lateinit var strip: MultitapStripView

    private val multitap = Multitap()

    private var punctuationAt = -1

    /**
     * Digits instead of letters. Set by the field when it asks for a number, and by the user's
     * key otherwise.
     *
     * Less load-bearing here than in the sibling keyboards: every digit is also the last stop on
     * its own key's cycle, so a field with no assigned mode key can still be typed into. The mode
     * turns six presses into one, and a numeric field turns it on unasked.
     */
    private var digits = false

    /**
     * Applied where characters are written rather than inside [Multitap], which stays a question
     * about position in a cycle. `a` and `A` are the same stop on key `2`; the case is what the
     * field is told, not where the taps have got to.
     */
    private var letterCase = LetterCase.LOWER

    /** The key whose meaning is waiting on its release. See [Action.DeferToRelease]. */
    private var deferredKey = KeyEvent.KEYCODE_UNKNOWN

    private val clock = Handler(Looper.getMainLooper())

    /**
     * Says on screen what the engine already believes: this letter is finished.
     *
     * Without it the timeout is invisible. [Multitap] only compares timestamps when a key
     * arrives, so nothing happened when the window closed — the character stayed underlined, the
     * run of letters stayed on the strip and the key stayed lit, all of it advertising a letter
     * the next press was going to treat as closed. On a keyboard whose entire claim is that you
     * never have to look at the screen to know what happened, an indicator that lies about the
     * one ambiguous moment in the method is worse than no indicator.
     *
     * Settling is all three signals at once: [endLetter] ends the composing region, which drops
     * the underline, and clears the active digit, which empties the strip and unlights the grid.
     * It also hands the d-pad back — while a letter is pending the arrows are the cycle, so
     * before this the keyboard went on swallowing them long after it had any use for them.
     */
    private val lapse = Runnable {
        endLetter()
        render()
    }

    override fun onCreate() {
        super.onCreate()
        preferences = Preferences(this)
    }

    override fun onCreateInputView(): View {
        strip = MultitapStripView(this)
        return strip
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        clock.removeCallbacks(lapse)
        multitap.settle()
        multitap.timeoutMillis = preferences.letterTimeout
        punctuationAt = -1
        deferredKey = KeyEvent.KEYCODE_UNKNOWN

        // Like the digit mode below, the case belongs to the field and not to the app: a lock
        // left on in one box must not follow the user into the next one.
        letterCase = LetterCase.LOWER

        // A field that wants a number gets digits without being asked. Anything else starts in
        // letters even if the mode was left on: the mode belongs to the field, not to the app.
        val classification = info?.inputType?.and(InputType.TYPE_MASK_CLASS)
        digits = classification == InputType.TYPE_CLASS_NUMBER ||
            classification == InputType.TYPE_CLASS_PHONE ||
            classification == InputType.TYPE_CLASS_DATETIME
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        render()
    }

    /**
     * The letter in progress is left where it is, as real text.
     *
     * There is no word to abandon and no candidate that might have been wrong, so the honest
     * thing to do with a half-cycled letter is keep it: the user can see it in the field and
     * delete it if they did not want it. Dropping it would delete a character the field was
     * already showing.
     */
    override fun onFinishInput() {
        clock.removeCallbacks(lapse)
        multitap.settle()
        currentInputConnection?.finishComposingText()
        super.onFinishInput()
    }

    override fun onDestroy() {
        clock.removeCallbacks(lapse)
        super.onDestroy()
    }

    /**
     * Never. The default says yes to every landscape screen, and a television is landscape
     * always — so leaving this alone puts the keyboard into extract mode permanently, which
     * covers the whole display with a white text editor and hides the field the user was
     * actually filling in. It reads as the keyboard failing to open rather than as a mode.
     */
    override fun onEvaluateFullscreenMode(): Boolean = false

    /**
     * The keyboard is not always visible when a key arrives, and this is where a previous
     * version of a sibling app left a television unnavigable: consuming d-pad events while
     * hidden means nothing on the device can be reached any more.
     *
     * So while hidden exactly one key is looked at — the trigger the user assigned, unassigned
     * by default — and every other event is handed straight back to the system.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isInputViewShown) {
            val trigger = preferences.triggerKeyCode
            if (trigger != KeyBindings.NO_KEY && keyCode == trigger && event.repeatCount == 0) {
                // requestShowSelf is the supported route and arrived in API 28. Below that
                // showWindow is the only way in, and it is what every IME used before 28.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    requestShowSelf(0)
                } else {
                    @Suppress("DEPRECATION")
                    showWindow(true)
                }
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        val action = KeyBindings.of(
            keyCode,
            event.repeatCount,
            preferences.customKeys,
            multitap.isPending,
            digits,
        ) ?: return super.onKeyDown(keyCode, event)

        return handle(action)
    }

    /**
     * `0` is the only key here that commits nothing until it is released, because it is the only
     * one that means two things.
     *
     * Android delivers a hold as a second key-down, so a space written on the way down is
     * already in the field by the time the hold announces itself as the case switch — and taking
     * it back is visible in a field the user is looking at. Nothing else on this keyboard needs
     * the deferral: `1` replaces its own mark in place and `2`-`9` only set composing text.
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != deferredKey) {
            return super.onKeyUp(keyCode, event)
        }
        deferredKey = KeyEvent.KEYCODE_UNKNOWN
        return handle(Action.Space)
    }

    private fun handle(action: Action): Boolean {
        when (action) {
            is Action.Ignore -> Unit

            is Action.Digit -> {
                if (digits) {
                    endLetter()
                    currentInputConnection?.commitText(action.digit.toString(), 1)
                } else {
                    multitap.press(action.digit, System.currentTimeMillis())?.let { press ->
                        show(press)
                        // Armed from the press and from nothing else, because that is what
                        // [Multitap] measures from. Stepping back does not push it out: the
                        // engine's window closes at a fixed distance from the last press
                        // whatever else happens, and a strip that outlived it would be telling
                        // the same lie this whole mechanism exists to stop.
                        clock.removeCallbacks(lapse)
                        clock.postDelayed(lapse, multitap.timeoutMillis)
                    }
                }
            }

            is Action.NextLetter -> endLetter()

            is Action.PreviousLetter -> {
                multitap.back()?.let { letter ->
                    punctuationAt = -1
                    currentInputConnection?.setComposingText(cased(letter), 1)
                }
            }

            /**
             * The letter in progress is still the composing region, so the switch applies to it
             * as well as to what follows. Pressing the case key *after* seeing the wrong case is
             * the order people actually press them in, and making that a delete-and-retype
             * instead of one press would waste the only advantage a composing region has.
             */
            is Action.ToggleCase -> {
                // The hold has claimed the press, so the release must not also write a space.
                deferredKey = KeyEvent.KEYCODE_UNKNOWN
                letterCase = letterCase.next()
                multitap.letter?.let { letter ->
                    currentInputConnection?.setComposingText(cased(letter), 1)
                }
            }

            is Action.DeferToRelease -> deferredKey = KeyEvent.KEYCODE_0

            is Action.Space -> {
                endLetter()
                currentInputConnection?.commitText(" ", 1)
            }

            /**
             * Ends the letter and submits, in one press.
             *
             * T9 needs two here because OK is also the key that accepts a candidate. Nothing on
             * this keyboard is a guess, so there is nothing to accept and no reason to make the
             * user press OK twice to search for what the field is already showing.
             */
            is Action.Commit -> {
                val ended = multitap.isPending
                endLetter()
                render()
                return sendDefaultEditorAction(true) || ended
            }

            is Action.Delete -> {
                if (multitap.discard()) {
                    punctuationAt = -1
                    val connection = currentInputConnection
                    connection?.setComposingText("", 1)
                    connection?.finishComposingText()
                } else {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                }
            }

            is Action.Punctuation -> punctuate()

            is Action.ToggleDigits -> {
                endLetter()
                digits = !digits
            }

            /**
             * The caret, a word at a time. The letter in progress is settled first: leaving it
             * composing while the caret walks away puts the editor's composing region somewhere
             * the user is no longer looking, and what it does next is the editor's business.
             */
            is Action.WordJump -> {
                endLetter()
                jumpWord(action.forward)
            }

            is Action.WordDelete -> {
                endLetter()
                deleteWord()
            }
        }
        // Delete drops the letter without settling it, so the timer goes with it rather than
        // firing later over a letter that is no longer there.
        if (!multitap.isPending) {
            clock.removeCallbacks(lapse)
        }
        render()
        return true
    }

    private fun cased(letter: Char): String = letterCase.apply(letter).toString()

    /** Hands the finished letter to the editor and puts the new one in the composing region. */
    private fun show(press: Press) {
        punctuationAt = -1
        val connection = currentInputConnection ?: return
        val settled = press.settled
        if (settled != null) {
            connection.finishComposingText()
            spendCase(settled)
        }
        connection.setComposingText(cased(press.pending), 1)
    }

    /**
     * A one-off capital is spent by the letter that used it, and by nothing else.
     *
     * [Multitap] reports the settled character in lower case whatever went into the field, so
     * this is asking what the user typed, not what it looks like. The digit on the end of a
     * key's cycle is not a letter and does not spend the state — reaching `2` by tapping past
     * `abcąć` would otherwise swallow a capital and give a character that cannot show one.
     */
    private fun spendCase(settled: Char) {
        if (settled.isLetter()) {
            letterCase = letterCase.afterLetter()
        }
    }

    /**
     * Finishes the letter in progress, leaving it in the field as real text.
     *
     * `finishComposingText` rather than `commitText`: the character is already where it belongs
     * and already visible, so this only ends the editor's claim that it might still change.
     */
    private fun endLetter() {
        punctuationAt = -1
        val settled = multitap.settle() ?: return
        clock.removeCallbacks(lapse)
        currentInputConnection?.finishComposingText()
        spendCase(settled)
    }

    /**
     * Moves the caret to the next or previous word boundary.
     *
     * Reads the text around the cursor from the editor rather than tracking a buffer here. The
     * editor owns the text — it may already contain something this keyboard never typed, and a
     * local copy would be wrong the moment it did.
     */
    private fun jumpWord(forward: Boolean) {
        val connection = currentInputConnection ?: return
        val extracted = connection.getExtractedText(ExtractedTextRequest(), 0) ?: return
        val text = extracted.text ?: return
        val at = extracted.selectionEnd.coerceIn(0, text.length)

        var target = at
        if (forward) {
            while (target < text.length && text[target].isWhitespace()) target++
            while (target < text.length && !text[target].isWhitespace()) target++
        } else {
            while (target > 0 && text[target - 1].isWhitespace()) target--
            while (target > 0 && !text[target - 1].isWhitespace()) target--
        }
        connection.setSelection(target, target)
    }

    /** Deletes back to the previous word boundary, whitespace included. */
    private fun deleteWord() {
        val connection = currentInputConnection ?: return
        val before = connection.getTextBeforeCursor(MAX_CONTEXT, 0) ?: return
        if (before.isEmpty()) {
            return
        }
        var count = 0
        while (count < before.length && before[before.length - 1 - count].isWhitespace()) count++
        while (count < before.length && !before[before.length - 1 - count].isWhitespace()) count++
        connection.deleteSurroundingText(count, 0)
    }

    /**
     * Cycles the marks on `1`, replacing the previous one in place.
     *
     * A query needs a handful of marks and an E.161 keypad has one key spare for them, so cycling
     * is the only arrangement that fits. Replacing in place rather than appending is what makes a
     * wrong choice one more press instead of a delete and a retry — the same bargain the letter
     * keys make, on the one key that has no letters.
     */
    private fun punctuate() {
        endLetter()
        val connection = currentInputConnection ?: return
        punctuationAt = if (punctuationAt < 0) 0 else (punctuationAt + 1) % Keypad.MARKS.length
        if (punctuationAt > 0) {
            connection.deleteSurroundingText(1, 0)
        }
        connection.commitText(Keypad.MARKS[punctuationAt].toString(), 1)
    }

    private fun render() {
        if (!::strip.isInitialized) {
            return
        }
        val digit = multitap.activeDigit
        strip.render(
            StripState(
                cycle = digit?.let { Keypad.cycleOf(it) }.orEmpty(),
                cycleDigit = digit,
                cycleIndex = multitap.activeIndex,
                hintMode = preferences.hintMode,
                letterCase = letterCase,
                digits = digits,
                hasEditor = currentInputConnection != null,
                customKeys = preferences.customKeys,
            )
        )
    }

    private companion object {
        /**
         * How much text before the caret a word-delete will look at. A TV query is a line, so
         * this is far more than one word ever needs — the cap exists because the editor is under
         * no obligation to be small and a novel would be copied across the process boundary.
         */
        const val MAX_CONTEXT = 512
    }
}
