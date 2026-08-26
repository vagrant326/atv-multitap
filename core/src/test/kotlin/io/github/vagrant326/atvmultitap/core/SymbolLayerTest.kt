package io.github.vagrant326.atvmultitap.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SymbolLayerTest {

    /** Every printable mark on a US QWERTY keyboard, which is the promise this layer makes. */
    private val qwerty = "`~!@#$%^&*()-_=+[]{}\\|;:'\",.<>/?"

    @Test
    fun `every QWERTY mark is on some key`() {
        val carried = (Keypad.FIRST_DIGIT..Keypad.LAST_DIGIT)
            .joinToString("") { Keypad.symbolsOn(it) }
        for (mark in qwerty) {
            assertTrue(mark in carried, "$mark is unreachable, which is the whole bug this fixes")
        }
    }

    @Test
    fun `no mark is on two keys`() {
        val carried = (Keypad.FIRST_DIGIT..Keypad.LAST_DIGIT)
            .joinToString("") { Keypad.symbolsOn(it) }
        assertEquals(qwerty.length, carried.length)
        assertEquals(carried.length, carried.toSet().size)
    }

    @Test
    fun `nothing costs more than four taps`() {
        for (digit in Keypad.FIRST_DIGIT..Keypad.LAST_DIGIT) {
            val run = Keypad.symbolsOn(digit)
            assertTrue(run.length <= 4, "key $digit carries ${run.length} marks")
        }
    }

    @Test
    fun `the marks a password and an address need are one tap`() {
        // The reason for the ordering inside each group. If these drift, the layer still works
        // but the two use cases that motivated it get slower.
        assertEquals('@', Keypad.symbolsOn('4').first())
        assertEquals('!', Keypad.symbolsOn('3').first())
        assertEquals('-', Keypad.symbolsOn('8').first())
        assertEquals('.', Keypad.symbolsOn('2').first())
    }

    @Test
    fun `the digit is not appended in the symbol layer`() {
        // In the letter layer it is the last stop, so a digit is always reachable. Here it would
        // be a third route to a character that already has two, paid for by every mark behind it.
        assertEquals("@#/\\", Keypad.cycleOf('4', Layer.SYMBOLS))
        assertEquals("ghi4", Keypad.cycleOf('4', Layer.LETTERS))
    }

    @Test
    fun `tapping one key walks its marks`() {
        val multitap = Multitap()
        multitap.use(Layer.SYMBOLS)
        assertEquals('@', multitap.press('4', 0)?.pending)
        assertEquals('#', multitap.press('4', 10)?.pending)
        assertEquals('/', multitap.press('4', 20)?.pending)
    }

    @Test
    fun `swapping the layer finishes what was in progress`() {
        // A position in a run that outlived the run changing would come back as a different
        // character than the one the field is showing.
        val multitap = Multitap()
        multitap.press('4', 0)
        assertEquals('g', multitap.letter)
        assertEquals('g', multitap.use(Layer.SYMBOLS))
        assertFalse(multitap.isPending)
    }

    @Test
    fun `a second tap of the same key cycles rather than finishing`() {
        // What makes `#` two taps of `4` instead of unreachable: the service only returns the
        // layer to letters on a press that would settle, and this is not one.
        val multitap = Multitap()
        multitap.use(Layer.SYMBOLS)
        multitap.press('4', 0)
        assertFalse(multitap.wouldSettle('4', 10))
    }

    @Test
    fun `a different key finishes the mark, and so does the timeout`() {
        val multitap = Multitap(timeoutMillis = 1_000)
        multitap.use(Layer.SYMBOLS)
        multitap.press('4', 0)
        assertTrue(multitap.wouldSettle('5', 10), "another key ends the mark")
        assertTrue(multitap.wouldSettle('4', 2_000), "so does waiting out the window")
    }

    @Test
    fun `nothing in progress settles nothing`() {
        val multitap = Multitap()
        assertFalse(multitap.wouldSettle('4', 0))
    }

    @Test
    fun `the letter layer is untouched by any of this`() {
        assertEquals("abcąć2", Keypad.cycleOf('2'))
        assertEquals(1, Keypad.pressesFor('a'))
        assertEquals(".,-'&:/", Keypad.MARKS)
    }
}
