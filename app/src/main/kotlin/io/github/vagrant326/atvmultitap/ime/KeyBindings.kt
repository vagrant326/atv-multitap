package io.github.vagrant326.atvmultitap.ime

import android.view.KeyEvent

sealed interface Action {

    /** One of `2`-`9`. Tapped again it replaces its own letter; that is the whole method. */
    data class Digit(val digit: Char) : Action

    /**
     * Finishes the letter and adds a space. `0` on every phone ever made — but on the way up
     * rather than the way down, because that key now also carries [ToggleCase]. See
     * [DeferToRelease].
     */
    data object Space : Action

    /** Cycles the marks a query actually needs. `1`, again by convention. */
    data object Punctuation : Action

    /**
     * Ends the letter in progress so the next press of the same key starts a new one.
     *
     * The one key multitap cannot do without. `hello` has `ll` on a single key, and without this
     * the user sits out a timeout in the middle of a word — the complaint every phone keyboard of
     * that generation eventually answered with exactly this key.
     */
    data object NextLetter : Action

    /**
     * Steps back one letter in the current key's cycle.
     *
     * The cheap way out of an overshoot, which is the characteristic mistake here: the cycles are
     * long, the taps are fast, and nothing is printed on the remote. Going round again costs six
     * presses on `9`; this costs one.
     *
     * Reachable from `CHANNEL_UP` and from nothing else. It used to sit on the left arrow too,
     * which does not survive contact with the hardware: the numpad and the d-pad are at opposite
     * ends of a television remote, and this correction is *reactive* — the user overshoots, then
     * notices, then starts moving their thumb. The timeout is 800ms at its shortest. The race was
     * lost before it began, and the arrow was meanwhile unavailable for the caret. `CHANNEL_UP`
     * sits beside the numpad on the remotes that have it, which is the one place the gesture is
     * physically plausible.
     */
    data object PreviousLetter : Action

    /**
     * The caret, one character back, ending the letter in progress on the way.
     *
     * The counterpart of the reasoning on [PreviousLetter]: moving the caret is deliberate rather
     * than reactive, so the walk across the remote costs nothing and there is no window to miss.
     */
    data object CaretLeft : Action

    /** Ends the letter and submits, which for a search box is what OK means. */
    data object Commit : Action

    /**
     * The caret, one word at a time, from holding left or right with no letter in progress.
     *
     * Held rather than tapped because caret movement is inherently repetitive and a TV query is
     * eleven characters: as single steps, walking back over one word is most of the query. Only
     * with no letter in progress — during one, left and right work the cycle, which is the hotter
     * path by a wide margin.
     */
    data class WordJump(val forward: Boolean) : Action

    data object Delete : Action

    /** The rest of the word, from holding delete. The tap already took one character. */
    data object WordDelete : Action

    /** Digits in one press instead of six, for a field that wants a number. */
    data object ToggleDigits : Action

    /**
     * `abc` → `Abc` → `ABC` → `abc`, from holding `0`.
     *
     * Held rather than tapped because there is nothing left to tap: the reserved list below is
     * the whole numeric row and the whole d-pad. `0` is the one key whose short press has no
     * cycle to interfere with — it writes a space and stops — so it is the only one that can
     * carry a second meaning without a letter run becoming ambiguous.
     */
    data object ToggleCase : Action

    /**
     * Swaps the letters on `2`-`9` for the full set of QWERTY marks, from holding `1`.
     *
     * `1` because that is already the punctuation key, so the hold is more of what the tap does
     * rather than an unrelated function parked on a free button — and there is no free button.
     * The layer is spent by one symbol, like [ToggleCase]'s one-off: an address needs `@` once
     * and a password needs `!` once, and a sticky layer would make both of them three presses
     * instead of two while adding a mode that can be forgotten.
     */
    data object ToggleSymbols : Action

    /**
     * A key whose meaning is not settled yet. Resolved in `MultitapImeService.onKeyUp`: released,
     * `0` is a space and `1` is the next mark; held, they are [ToggleCase] and [ToggleSymbols].
     *
     * `0` and `1` are the two keys whose short press commits text outright, and Android delivers
     * a hold as a *second* key-down after the first — so their character would already be in the
     * field by the time the hold announced itself, and un-typing it is visible. `2`-`9` only set
     * composing text, which a hold can replace for free, so they still act on the way down and
     * the letter appears as you type. Ported from LetterWise, which hit this first.
     */
    data class DeferToRelease(val keyCode: Int) : Action

    /** Consume the event and do nothing, which is what a key held down past the first repeat
     *  has to do: a number key that repeated would append letters nobody pressed. */
    data object Ignore : Action
}

/**
 * Custom bindings, because remotes disagree about which keys exist and about what they report.
 * The user's `TEXT` key sits where a phone has `*` and reports keycode 300, well outside the
 * standard range — nothing in the app could have guessed that.
 *
 * Three, where T9 has five: there is no spelling mode to reach and no language to switch, because
 * this keyboard has neither. Both remaining conveniences are optional — deleting is `DPAD_UP`
 * unconditionally, and every digit is also the last stop on its own key's cycle. The trigger is
 * the exception: it cannot be reached any other way, because the keyboard is not on screen at
 * the moment it is needed.
 */
data class CustomKeys(
    val trigger: Int,
    val delete: Int,
    val digits: Int,
)

object KeyBindings {

    const val NO_KEY = 0

    /**
     * Keys the keyboard needs for itself, and therefore cannot be assigned as a binding.
     *
     * Longer than the equivalent list in H4-Writer, and that is the trade this method makes:
     * `0`-`9` *are* the keyboard here, so a remote with number keys gets a keyboard and a remote
     * without one gets nothing. `docs/00-overview.md` §3 relaxes C5 for exactly this reason.
     *
     * The whole d-pad is reserved, up and down included. They work the cycle alongside left and
     * right, and beyond that a keyboard that let one arrow become a function while its three
     * neighbours still navigated would be a trap rather than a preference.
     */
    val RESERVED: Set<Int> = buildSet {
        addAll(KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9)
        add(KeyEvent.KEYCODE_DPAD_UP)
        add(KeyEvent.KEYCODE_DPAD_DOWN)
        add(KeyEvent.KEYCODE_DPAD_LEFT)
        add(KeyEvent.KEYCODE_DPAD_RIGHT)
        add(KeyEvent.KEYCODE_DPAD_CENTER)
        add(KeyEvent.KEYCODE_ENTER)
        add(KeyEvent.KEYCODE_BACK)
        add(KeyEvent.KEYCODE_HOME)
    }

    /**
     * @param repeatCount straight from the [KeyEvent]. Only `1` counts as a hold; later repeats
     *   are swallowed, so one hold is one action rather than a rate. That is what keeps a held
     *   caret from crossing the whole field — Android repeats at roughly twenty a second.
     * @param pending whether a letter is in progress. `BACK` and the arrows mean something only
     *   then — otherwise they belong to whatever is behind the keyboard, and a keyboard that eats
     *   the d-pad on a TV leaves the whole device unnavigable.
     * @param digits whether the number keys are typing digits rather than letters.
     *
     * Returns null for anything this keyboard has no use for, which the service passes through
     * untouched rather than consuming.
     */
    fun of(
        keyCode: Int,
        repeatCount: Int,
        custom: CustomKeys,
        pending: Boolean,
        digits: Boolean,
    ): Action? {
        val longPress = repeatCount == 1

        if (longPress) {
            if (custom.delete != NO_KEY && keyCode == custom.delete) {
                return Action.WordDelete
            }
            return when (keyCode) {
                // Nothing to capitalise in a digit field, and a gesture that silently does
                // nothing is worse than one that is not there.
                KeyEvent.KEYCODE_0 -> if (digits) Action.Ignore else Action.ToggleCase
                KeyEvent.KEYCODE_1 -> if (digits) Action.Ignore else Action.ToggleSymbols

                // Unconditional, unlike right: left is the caret whether or not a letter is in
                // progress, so holding it is the word-sized version of the same thing.
                KeyEvent.KEYCODE_DPAD_LEFT -> Action.WordJump(forward = false)

                KeyEvent.KEYCODE_DPAD_RIGHT ->
                    if (pending) Action.Ignore else Action.WordJump(forward = true)

                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DEL -> Action.WordDelete
                else -> Action.Ignore
            }
        }

        if (repeatCount > 0) {
            return Action.Ignore
        }

        if (custom.trigger != NO_KEY && keyCode == custom.trigger) {
            return null // handled before the keyboard is showing; see MultitapImeService
        }
        if (custom.delete != NO_KEY && keyCode == custom.delete) {
            return Action.Delete
        }
        if (custom.digits != NO_KEY && keyCode == custom.digits) {
            return Action.ToggleDigits
        }

        // In digit mode the row is deterministic: every key is the digit printed on it, and there
        // is no cycle to step through.
        if (digits && keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            return Action.Digit('0' + (keyCode - KeyEvent.KEYCODE_0))
        }

        return when (keyCode) {
            in KeyEvent.KEYCODE_2..KeyEvent.KEYCODE_9 ->
                Action.Digit('0' + (keyCode - KeyEvent.KEYCODE_0))

            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1 -> Action.DeferToRelease(keyCode)

            // Right ends the letter, left steps back through it. Up and down do the same job, and
            // so do CHANNEL_UP and CHANNEL_DOWN, which sit beside the numpad on the remotes that
            // have one — a second way in for a remote whose d-pad is awkward, never the only one.
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_CHANNEL_DOWN,
            ->
                if (pending) Action.NextLetter else null

            KeyEvent.KEYCODE_CHANNEL_UP -> if (pending) Action.PreviousLetter else null

            // Left is the caret, always. While a letter is in progress it is settled first and
            // the arrow forwarded, so the composing region is never left behind somewhere the
            // user has walked away from.
            KeyEvent.KEYCODE_DPAD_LEFT -> if (pending) Action.CaretLeft else null

            // Up deletes, whether or not a letter is in progress, and it is the only route that
            // is always there. The other three are conditional in ways a user cannot see: the
            // assigned key needs a spare button and a trip through the settings, `KEYCODE_DEL`
            // needs a remote that has one and a television remote does not, and `BACK` only
            // deletes while a letter is in progress, which is a window rather than something a
            // user can rely on being in.
            //
            // Up because that is what up already means in H4-Writer's edit mode. It cost the
            // duplicate of the step-back that also sits on left, which is the cheapest thing on
            // the d-pad to give away.
            KeyEvent.KEYCODE_DPAD_UP -> Action.Delete

            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> Action.Commit
            KeyEvent.KEYCODE_DEL -> Action.Delete
            KeyEvent.KEYCODE_BACK -> if (pending) Action.Delete else null
            else -> null
        }
    }
}
