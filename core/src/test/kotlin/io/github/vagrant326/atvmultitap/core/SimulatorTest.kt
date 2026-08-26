package io.github.vagrant326.atvmultitap.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SimulatorTest {

    private val simulator = Simulator()

    @Test
    fun `a press count is arithmetic over the layout`() {
        // k=2, o=3, t=1
        val cost = simulator.cost("kot")
        assertEquals(6, cost.presses)
        assertEquals(3, cost.characters)
        assertEquals(2.0, cost.kspc)
    }

    @Test
    fun `a doubled key costs one press to move on`() {
        // h=2, i=3, and both are on 4.
        val cost = simulator.cost("hi")
        assertEquals(6, cost.presses)
        assertEquals(1, cost.nextLetter)
        assertEquals(5, cost.presses - cost.nextLetter, "or a timeout, which the waited figure assumes")
    }

    @Test
    fun `the space between words is one press and one character`() {
        val single = simulator.cost("kotkot")
        val spaced = simulator.cost("kot kot")
        assertEquals(single.presses + 1, spaced.presses)
        assertEquals(single.characters + 1, spaced.characters)
        assertEquals(2, spaced.words)
    }

    @Test
    fun `a Polish query costs its diacritics and nothing else`() {
        // The language is not a parameter here, because the keyboard has no language state.
        val cost = simulator.cost("piątek")
        assertEquals(4, Keypad.pressesFor('ą'))
        assertEquals(1 + 3 + 4 + 1 + 2 + 2, cost.presses)
    }

    @Test
    fun `a digit costs its whole cycle rather than going missing`() {
        val cost = simulator.cost("2")
        assertEquals(6, cost.presses, "a b c ą ć 2")
        assertEquals(0, cost.unreachable)
    }

    @Test
    fun `a mark costs its position on key 1`() {
        assertEquals(1, simulator.cost(".").presses)
        assertEquals(3, simulator.cost("-").presses)
    }

    @Test
    fun `a character no key carries is counted and reported`() {
        val cost = simulator.cost("#")
        assertEquals(1, cost.unreachable)
    }

    @Test
    fun `the whole method sits where the published figure says it does`() {
        // docs/00-overview.md §5 puts multitap at 2.0342 over English prose. This is a
        // television query set rather than prose, so the figure is not the same one — but an
        // order of magnitude apart would mean the layout above is wrong.
        val cost = simulator.cost("kung fu panda")
        assertTrue(cost.kspc in 1.4..2.8, "KSPC was ${cost.kspc}")
    }
}
