package io.github.vagrant326.atvmultitap.core

/**
 * What one query cost, and where the cost went.
 *
 * [nextLetter] is broken out because it is the only part of a multitap query that is not fixed
 * by the layout: it is the presses spent moving on from a letter whose successor sits on the
 * same key, and the user can pay it as a press or as a wait. Reporting it separately is what
 * keeps this figure comparable with published multitap numbers, which count the wait as free.
 */
data class Cost(
    val presses: Int,
    val characters: Int,
    val words: Int,
    val nextLetter: Int,
    val unreachable: Int,
) {
    val kspc: Double get() = if (characters == 0) 0.0 else presses.toDouble() / characters

    /** The same query with every same-key boundary waited out instead of pressed through. */
    val waitedKspc: Double
        get() = if (characters == 0) 0.0 else (presses - nextLetter).toDouble() / characters

    operator fun plus(other: Cost) = Cost(
        presses + other.presses,
        characters + other.characters,
        words + other.words,
        nextLetter + other.nextLetter,
        unreachable + other.unreachable,
    )

    companion object {
        val ZERO = Cost(0, 0, 0, 0, 0)
    }
}

/**
 * Counts the presses a query takes on this keyboard.
 *
 * Nothing is modelled here, which is the point: multitap has no dictionary, no prediction and no
 * state beyond the letter in progress, so the press count for a string is arithmetic over
 * [Keypad] and not a simulation of anything. The figure it produces is exact rather than
 * estimated, and it is the same for every user on the first day and the thousandth — which is
 * precisely what the other three keyboards in the programme cannot say and what makes this one
 * the control they are measured against.
 *
 * The space between words counts as one press and one character, which is MacKenzie's convention
 * and the reason these figures can sit beside the published ones at all. A character that no key
 * carries at all counts as one press, which flatters the keyboard; the count of them is reported
 * so the flattery is visible rather than buried in the total.
 */
class Simulator {

    fun cost(query: String): Cost {
        var total = Cost.ZERO
        val words = query.trim().split(WHITESPACE).filter { it.isNotEmpty() }

        for ((index, word) in words.withIndex()) {
            total += costOf(word)
            if (index < words.size - 1) {
                total += Cost(presses = 1, characters = 1, words = 0, nextLetter = 0, unreachable = 0)
            }
        }
        return total
    }

    private fun costOf(word: String): Cost {
        var presses = 0
        var nextLetter = 0
        var unreachable = 0
        var previous: Char? = null

        for (character in word) {
            val letterKey = Keypad.digitOf(character)
            val markPosition = Keypad.MARKS.indexOf(character)
            val key: Char?
            when {
                letterKey != null -> {
                    key = letterKey
                    presses += Keypad.pressesFor(character) ?: 1
                }

                // A digit is the last stop on its own key's cycle, so it costs the whole cycle
                // rather than the one press the digit mode would make it. The mode is the
                // cheaper route and needs an assigned button; this is the route that always
                // works, so it is the one counted.
                Keypad.isDigit(character) -> {
                    key = character
                    presses += Keypad.cycleOf(character).length
                }

                markPosition >= 0 -> {
                    key = '1'
                    presses += markPosition + 1
                }

                else -> {
                    key = null
                    presses++
                    unreachable++
                }
            }

            // Two characters on one key: one press to move on, or a timeout to sit through.
            if (key != null && key == previous) {
                presses++
                nextLetter++
            }
            previous = key
        }

        return Cost(presses, word.length, words = 1, nextLetter = nextLetter, unreachable = unreachable)
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
