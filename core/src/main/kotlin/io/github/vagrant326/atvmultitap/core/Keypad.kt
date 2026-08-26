package io.github.vagrant326.atvmultitap.core

/**
 * ITU E.161, the layout printed on every phone made between 1995 and 2007 and on the number
 * keys of the user's remote.
 *
 * Nothing here is chosen, and that is the point of this application. The other keyboards in the
 * programme each pick a layout and then have to teach it; this one inherits the only keypad
 * mapping a large number of people already have in their thumbs, which is the entire reason it
 * is worth shipping a method with a KSPC of two.
 *
 * The Polish letters sit at the end of their base key's run rather than interleaved. Position in
 * the run is press count, so interleaving would make `c` cost four presses to buy `ą` a cheaper
 * one — the wrong trade in both languages. Where they are, `abc` costs exactly what it costs on
 * a phone and the diacritics cost the tail.
 */
object Keypad {

    const val FIRST_DIGIT = '2'
    const val LAST_DIGIT = '9'

    /**
     * What key `1` carries, in order, and therefore what a mark costs in presses.
     *
     * What a television query actually contains, not a general punctuation set and not meant as
     * one. It lives here rather than in the keyboard because the press counter has to agree with
     * the keyboard about it, and a second copy of this string is a second answer to how much a
     * hyphen costs.
     */
    const val MARKS = ".,-'&:/"

    private val KEYS = mapOf(
        '2' to "abcąć",
        '3' to "defę",
        '4' to "ghi",
        '5' to "jklł",
        '6' to "mnońó",
        '7' to "pqrsś",
        '8' to "tuv",
        '9' to "wxyzźż",
    )

    /**
     * Every printable mark on a QWERTY keyboard, four to a key.
     *
     * All thirty-two rather than the twenty-five [MARKS] leaves out, so there is one rule to
     * learn — this layer is the whole set, and key `1` is a shortcut to the seven a television
     * query actually uses. A layer holding "the leftovers" would be a list nobody could predict
     * the contents of.
     *
     * Grouped by kind, and the grouping is the feature. Nothing is printed on the remote, so the
     * legend on screen is the only place these can be found, and a reader scanning eight cells
     * for a bracket does better with brackets kept together than with any frequency order. Within
     * a group the commonest goes first, so the marks a password or an address actually needs —
     * `@`, `!`, `-`, `.` — are one tap each.
     *
     * Exactly four per key is not a coincidence worth relying on, but it does mean no symbol
     * costs more than four taps.
     */
    private val SYMBOLS = mapOf(
        '2' to ".,;:",
        '3' to "!?'\"",
        '4' to "@#/\\",
        '5' to "$%&*",
        '6' to "()<>",
        '7' to "[]{}",
        '8' to "-_+=",
        '9' to "`~^|",
    )

    private val BY_LETTER: Map<Char, Char> =
        KEYS.entries.flatMap { (digit, letters) -> letters.map { it to digit } }.toMap()

    fun lettersOn(digit: Char): String = KEYS[digit] ?: ""

    fun symbolsOn(digit: Char): String = SYMBOLS[digit] ?: ""

    /** What [layer] puts on [digit], before the digit itself is appended by [cycleOf]. */
    fun runOn(digit: Char, layer: Layer): String = when (layer) {
        Layer.LETTERS -> lettersOn(digit)
        Layer.SYMBOLS -> symbolsOn(digit)
    }

    /**
     * What repeated presses of [digit] actually produce, which is the letters and then the digit
     * itself.
     *
     * The digit on the end is not decoration. There is no dictionary here and no letter this
     * keyboard cannot reach, so a digit being unreachable would be the one hole in an otherwise
     * complete method — and every phone this muscle memory came from put the digit exactly
     * there. It also means the digit mode is a convenience rather than the only way to type `2`.
     */
    fun cycleOf(digit: Char, layer: Layer = Layer.LETTERS): String {
        val run = runOn(digit, layer)
        if (run.isEmpty()) {
            return ""
        }
        // Only the letter layer carries the digit at the end. In the symbol layer it would be a
        // third route to a character that already has two, paid for by every symbol behind it.
        return if (layer == Layer.LETTERS) run + digit else run
    }

    fun digitOf(letter: Char): Char? = BY_LETTER[letter.lowercaseChar()]

    fun isDigit(character: Char): Boolean = character in FIRST_DIGIT..LAST_DIGIT

    /**
     * How many presses of one key it takes to reach [character], or null if no key carries it.
     *
     * Null is how the caller learns a character is unreachable, which is true of everything
     * outside the two alphabets — an ampersand, an apostrophe. Those come off key `1`, and
     * anything not on key `1` either cannot be typed on this keyboard at all or is a digit.
     */
    fun pressesFor(character: Char): Int? {
        val digit = digitOf(character) ?: return null
        return cycleOf(digit).indexOf(character.lowercaseChar()) + 1
    }
}
