package io.github.vagrant326.atvmultitap.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MultitapTest {

    @Test
    fun `tapping one key cycles its letters`() {
        val multitap = Multitap()
        assertEquals('a', multitap.press('2', 0)?.pending)
        assertEquals('b', multitap.press('2', 100)?.pending)
        assertEquals('c', multitap.press('2', 200)?.pending)
    }

    @Test
    fun `cycling replaces the letter rather than adding one`() {
        val multitap = Multitap()
        multitap.press('2', 0)
        assertNull(multitap.press('2', 100)?.settled, "b replaces a, so nothing is finished")
    }

    @Test
    fun `the Polish letters are at the end of their key`() {
        val multitap = Multitap()
        repeat(5) { multitap.press('9', it * 100L) }
        assertEquals('ź', multitap.letter, "w x y z ź")
    }

    @Test
    fun `the digit is the last stop on the cycle`() {
        val multitap = Multitap()
        repeat(4) { multitap.press('4', it * 100L) }
        assertEquals('4', multitap.letter, "g h i 4")
    }

    @Test
    fun `the cycle closes rather than sticking on the last stop`() {
        val multitap = Multitap()
        repeat(5) { multitap.press('4', it * 100L) }
        assertEquals('g', multitap.letter, "g h i 4 then round again")
    }

    @Test
    fun `a different key finishes the letter in progress`() {
        val multitap = Multitap()
        multitap.press('5', 0)
        val press = multitap.press('6', 50)
        assertEquals('j', press?.settled)
        assertEquals('m', press?.pending)
    }

    @Test
    fun `the same key after the timeout finishes the letter rather than cycling it`() {
        val multitap = Multitap(timeoutMillis = 500)
        multitap.press('2', 0)
        val press = multitap.press('2', 900)
        assertEquals('a', press?.settled)
        assertEquals('a', press?.pending, "aa is reachable by waiting")
    }

    @Test
    fun `settling makes a doubled letter cost a press instead of a wait`() {
        val multitap = Multitap()
        multitap.press('2', 0)
        assertEquals('a', multitap.settle())
        val press = multitap.press('2', 10)
        assertNull(press?.settled, "the a was already handed over by settling")
        assertEquals('a', press?.pending)
    }

    @Test
    fun `stepping back is one press out of an overshoot`() {
        val multitap = Multitap()
        repeat(6) { multitap.press('9', it * 100L) }
        assertEquals('ż', multitap.letter)
        assertEquals('ź', multitap.back())
    }

    @Test
    fun `stepping back wraps to the end of the cycle`() {
        val multitap = Multitap()
        multitap.press('2', 0)
        assertEquals('2', multitap.back(), "back from a lands on the digit")
    }

    @Test
    fun `stepping back is not limited by the timeout`() {
        val multitap = Multitap(timeoutMillis = 500)
        multitap.press('2', 0)
        assertEquals('2', multitap.back(), "a long pause is not a reason to refuse a correction")
    }

    @Test
    fun `there is nothing to step back through once the letter is settled`() {
        val multitap = Multitap()
        multitap.press('2', 0)
        multitap.settle()
        assertNull(multitap.back())
        assertFalse(multitap.isPending)
    }

    @Test
    fun `keys carrying no letters are refused rather than swallowed`() {
        val multitap = Multitap()
        assertNull(multitap.press('0', 0))
        assertNull(multitap.press('1', 0))
        assertFalse(multitap.isPending)
    }

    @Test
    fun `discarding reports whether there was a letter to drop`() {
        val multitap = Multitap()
        multitap.press('7', 0)
        assertTrue(multitap.discard())
        assertFalse(multitap.discard())
    }

    @Test
    fun `the active key and position are what the strip needs to draw the cycle`() {
        val multitap = Multitap()
        multitap.press('7', 0)
        multitap.press('7', 100)
        assertEquals('7', multitap.activeDigit)
        assertEquals(1, multitap.activeIndex)
        assertEquals('q', multitap.letter)
    }
}
