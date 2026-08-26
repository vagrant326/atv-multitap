package io.github.vagrant326.atvmultitap.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LetterCaseTest {

    @Test
    fun `one gesture reaches every state and comes back`() {
        assertEquals(LetterCase.ONCE, LetterCase.LOWER.next())
        assertEquals(LetterCase.LOCKED, LetterCase.ONCE.next())
        assertEquals(
            LetterCase.LOWER,
            LetterCase.LOCKED.next(),
            "a cycle that cannot be left strands the user in a mode the remote does not show",
        )
    }

    @Test
    fun `the one-off comes before the lock`() {
        // Isolated capitals outnumber runs of them in both alphabets, so the cheaper press is
        // the one that buys a single capital. Reversing these two would be measurably worse.
        assertEquals(LetterCase.ONCE, LetterCase.LOWER.next())
    }

    @Test
    fun `every Polish letter has a capital and keeps it`() {
        val pairs = mapOf(
            'ą' to 'Ą', 'ć' to 'Ć', 'ę' to 'Ę', 'ł' to 'Ł', 'ń' to 'Ń',
            'ó' to 'Ó', 'ś' to 'Ś', 'ź' to 'Ź', 'ż' to 'Ż',
        )
        for ((lower, upper) in pairs) {
            assertEquals(upper, LetterCase.ONCE.apply(lower), "$lower must reach $upper")
            assertEquals(upper, LetterCase.LOCKED.apply(lower))
        }
    }

    @Test
    fun `the whole keypad is reachable in capitals`() {
        val letters = (Keypad.FIRST_DIGIT..Keypad.LAST_DIGIT)
            .joinToString("") { Keypad.lettersOn(it) }
        val capitals = letters.map(LetterCase.LOCKED::apply)
        assertEquals(
            letters.length,
            capitals.toSet().size,
            "two letters sharing one capital would make a password unreachable, not merely odd",
        )
    }

    @Test
    fun `marks and digits pass through untouched`() {
        // Why the case is applied to everything the keyboard writes rather than only to letters:
        // the caller never has to ask, so it can never forget to.
        for (mark in Keypad.MARKS) {
            assertEquals(mark, LetterCase.LOCKED.apply(mark))
        }
        for (digit in Keypad.FIRST_DIGIT..Keypad.LAST_DIGIT) {
            assertEquals(digit, LetterCase.LOCKED.apply(digit))
        }
    }

    @Test
    fun `lower case leaves everything alone`() {
        for (letter in "abcząćęłńóśźż") {
            assertEquals(letter, LetterCase.LOWER.apply(letter))
        }
    }

    @Test
    fun `a letter spends the one-off and nothing else does`() {
        assertEquals(LetterCase.LOWER, LetterCase.ONCE.afterLetter())
        assertEquals(
            LetterCase.LOCKED,
            LetterCase.LOCKED.afterLetter(),
            "the lock is the state that survives letters; that is the whole difference",
        )
        assertEquals(LetterCase.LOWER, LetterCase.LOWER.afterLetter())
    }
}
