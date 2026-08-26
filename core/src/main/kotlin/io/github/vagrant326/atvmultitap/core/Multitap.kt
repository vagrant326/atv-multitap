package io.github.vagrant326.atvmultitap.core

/**
 * What a press finished and what it started.
 *
 * [settled] is a letter the user is done with and the editor must keep; [pending] is the letter
 * now in progress, which further presses of the same key will replace. Returning both is what
 * lets the service hold exactly one character in the composing region and nothing else — the
 * text in the field is real text as soon as the user moves on from it.
 */
data class Press(val settled: Char?, val pending: Char)

/**
 * One key, tapped until the letter wanted appears.
 *
 * Slow by construction — `ż` is six taps of `9` — and that is the whole trade this application
 * makes. `docs/00-overview.md` §5 puts multitap at KSPC 2.0342 against T9's 1.0072, and §M2
 * puts its visual-check rate at approximately zero: the presses are known before you make them,
 * the screen never has to be consulted, and nothing the keyboard guesses can be wrong. That is
 * a real preference and not a worse version of one, which is why this ships beside the other
 * three rather than instead of any of them.
 *
 * **Only the letter in progress lives here.** Everything before it has already gone into the
 * editor. There is no word buffer and nothing to abandon: the editor owns the text, this owns
 * the one character that repeated presses can still change.
 *
 * The clock is a parameter rather than a call to the system, so the timeout is testable and the
 * class stays free of Android.
 */
class Multitap(var timeoutMillis: Long = DEFAULT_TIMEOUT) {

    private var digit: Char? = null
    private var index = 0
    private var lastAt = Long.MIN_VALUE

    /** The key the letter in progress came from, or null when no letter is in progress. */
    val activeDigit: Char? get() = digit

    /** How far into that key's cycle the letter sits. For the strip, which draws the cycle. */
    val activeIndex: Int get() = index

    val isPending: Boolean get() = digit != null

    val letter: Char? get() = digit?.let { Keypad.cycleOf(it).getOrNull(index) }

    /**
     * Registers one press of a number key, or null if that key carries nothing.
     *
     * The same key inside the timeout replaces the letter in progress; anything else — a
     * different key, or the same key after the timeout — finishes it and starts a new one. That
     * timeout is the only ambiguity in the method, and [settle] exists so the user never has to
     * wait it out.
     */
    fun press(digit: Char, atMillis: Long): Press? {
        val cycle = Keypad.cycleOf(digit)
        if (cycle.isEmpty()) {
            return null
        }
        if (digit == this.digit && atMillis - lastAt <= timeoutMillis) {
            index = (index + 1) % cycle.length
            lastAt = atMillis
            return Press(settled = null, pending = cycle[index])
        }
        val finished = letter
        this.digit = digit
        index = 0
        lastAt = atMillis
        return Press(settled = finished, pending = cycle[0])
    }

    /**
     * Steps back one position in the current key's cycle.
     *
     * The cheap way out of an overshoot. Tapping past `ż` on `9` otherwise costs six more presses
     * to come round again, and overshooting is the characteristic mistake of this method — the
     * cycle is long, the taps are fast, and the letters are not printed on the remote. Not
     * limited by the timeout, because unlike a second tap of the same key it cannot mean anything
     * else.
     */
    fun back(): Char? {
        val cycle = Keypad.cycleOf(digit ?: return null)
        index = (index - 1 + cycle.length) % cycle.length
        return cycle[index]
    }

    /**
     * Finishes the letter in progress and returns it, or null if there was none.
     *
     * This is what makes a double letter cost a press instead of a wait. `hello` has `ll` on one
     * key: without this the user has to sit through the timeout in the middle of a word, which
     * is the single most-complained-about property of multitap and the reason every phone
     * eventually grew this key.
     */
    fun settle(): Char? {
        val finished = letter
        digit = null
        index = 0
        lastAt = Long.MIN_VALUE
        return finished
    }

    /** Drops the letter in progress. Reports whether there was one to drop. */
    fun discard(): Boolean {
        val had = digit != null
        settle()
        return had
    }

    companion object {
        /**
         * Long by phone standards, and the default rather than the rule — see
         * `Preferences.letterTimeout`.
         *
         * A remote is held at arm's length and the letters are not printed on it, so a new user
         * is reading the screen between presses rather than typing from memory, and a
         * phone-length timeout commits the letter while they are still looking. Nobody pays for
         * the length of it once they know [settle] is there.
         */
        const val DEFAULT_TIMEOUT = 1_200L
    }
}
