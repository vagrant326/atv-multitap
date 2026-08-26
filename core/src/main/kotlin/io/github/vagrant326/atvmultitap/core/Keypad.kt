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

    private val BY_LETTER: Map<Char, Char> =
        KEYS.entries.flatMap { (digit, letters) -> letters.map { it to digit } }.toMap()

    fun lettersOn(digit: Char): String = KEYS[digit] ?: ""

    /**
     * What repeated presses of [digit] actually produce, which is the letters and then the digit
     * itself.
     *
     * The digit on the end is not decoration. There is no dictionary here and no letter this
     * keyboard cannot reach, so a digit being unreachable would be the one hole in an otherwise
     * complete method — and every phone this muscle memory came from put the digit exactly
     * there. It also means the digit mode is a convenience rather than the only way to type `2`.
     */
    fun cycleOf(digit: Char): String {
        val letters = KEYS[digit] ?: return ""
        return letters + digit
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
