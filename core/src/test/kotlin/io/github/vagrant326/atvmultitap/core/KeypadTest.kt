package io.github.vagrant326.atvmultitap.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KeypadTest {

    @Test
    fun `every Polish letter sits on the key its base letter is on`() {
        val folds = mapOf(
            'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l',
            'ń' to 'n', 'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
        )
        for ((accented, base) in folds) {
            assertEquals(
                Keypad.digitOf(base),
                Keypad.digitOf(accented),
                "$accented has to sit where $base sits, or the user has to be taught a new keypad",
            )
        }
    }

    @Test
    fun `no letter is on two keys`() {
        val letters = (Keypad.FIRST_DIGIT..Keypad.LAST_DIGIT).joinToString("") { Keypad.lettersOn(it) }
        assertEquals(35, letters.length)
        assertEquals(letters.length, letters.toSet().size)
    }

    @Test
    fun `the English letters cost what they cost on a phone`() {
        // The Polish tail is at the end of each key precisely so this stays true.
        assertEquals(1, Keypad.pressesFor('a'))
        assertEquals(3, Keypad.pressesFor('c'))
        assertEquals(3, Keypad.pressesFor('i'))
        assertEquals(4, Keypad.pressesFor('s'))
        assertEquals(4, Keypad.pressesFor('z'))
    }

    @Test
    fun `the Polish letters cost the tail of their key`() {
        assertEquals(4, Keypad.pressesFor('ą'))
        assertEquals(5, Keypad.pressesFor('ć'))
        assertEquals(6, Keypad.pressesFor('ż'))
    }

    @Test
    fun `the digit is reachable without a mode`() {
        assertEquals("abcąć2", Keypad.cycleOf('2'))
        assertEquals("ghi4", Keypad.cycleOf('4'))
    }

    @Test
    fun `the keys with no letters carry no cycle either`() {
        assertEquals("", Keypad.cycleOf('0'))
        assertEquals("", Keypad.cycleOf('1'))
    }

    @Test
    fun `case does not reach the keypad`() {
        assertEquals(Keypad.digitOf('a'), Keypad.digitOf('A'))
        assertEquals(Keypad.pressesFor('w'), Keypad.pressesFor('W'))
    }

    @Test
    fun `anything the keys cannot reach reports itself rather than being mangled`() {
        assertNull(Keypad.pressesFor('\''))
        assertNull(Keypad.pressesFor('&'))
        assertNull(Keypad.pressesFor('2'), "the digit is a stop on a cycle, not a character with a key")
        assertTrue(Keypad.isDigit('2'))
    }
}
