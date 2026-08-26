package io.github.vagrant326.atvmultitap.core

/**
 * Whether the next letter is a capital, and for how long.
 *
 * One gesture and three states rather than a key for the one-off and another for the lock: this
 * remote has no spare buttons, and `KeyBindings.RESERVED` says why — `0`-`9` *are* the keyboard.
 *
 * The order is not a matter of taste. A corpus records how many capitals a language contains and
 * also how many of them stand alone, and isolated capitals — sentence openings, proper nouns —
 * outnumber runs of them by a wide margin in both alphabets here. So the first press buys the
 * common case and the lock costs one more, which is the arrangement every phone keypad arrived
 * at from the same measurement.
 *
 * [ONCE] cannot be forgotten, because it spends itself on the letter it capitalised. That
 * matters more here than on a phone: nothing is printed on this remote, so a mode the user
 * cannot see and did not mean to be in produces text that reads as a typo rather than as a mode,
 * and they would go looking for the mistake in their own thumbs.
 */
enum class LetterCase {

    LOWER,

    /** The next letter, and then back to [LOWER] on its own. */
    ONCE,

    /** Every letter until switched off. */
    LOCKED,
    ;

    fun next(): LetterCase = entries[(ordinal + 1) % entries.size]

    /**
     * Digits and marks come back unchanged, which is why this is applied to everything the
     * keyboard writes rather than only to letters: a caller that has to ask whether a character
     * is a letter first is a caller that will eventually forget to.
     *
     * `uppercaseChar` rather than `uppercase`: the locale-aware version returns a *string*, to
     * cover the languages where one letter becomes two — none of which occur in either alphabet
     * here — and on a Turkish device it would make `İ` out of `i`. The whole Polish set maps one
     * to one, `ł` to `Ł` included.
     */
    fun apply(character: Char): Char = if (this == LOWER) character else character.uppercaseChar()

    /**
     * What the state becomes once a capital has actually reached the field.
     *
     * Only a letter spends [ONCE]. A space, a mark or a digit cannot be capitalised, so
     * consuming the state on one would quietly take back the capital the user asked for — and
     * with nothing printed on the remote, the only way they could find out is by reading the
     * result and not believing it.
     */
    fun afterLetter(): LetterCase = if (this == ONCE) LOWER else this
}
