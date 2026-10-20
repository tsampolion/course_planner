package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.lessonPages
import gr.edusuite.pacer.model.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplacementTest {
    private val sb = Book("a", "SB", 1, 20)
    private val parse = { t: String -> lessonPages(t, sb) }

    @Test fun zeroOrTheSamePagesAsk() {
        assertTrue(needsKeepChoice(listOf(5, 6), "0", parse))
        assertTrue(needsKeepChoice(listOf(5, 6), "5-6", parse))
        assertTrue(needsKeepChoice(listOf(5, 6), "5, 6", parse))
        assertFalse(needsKeepChoice(listOf(5, 6), "5", parse))
        assertFalse(needsKeepChoice(listOf(5, 6), "", parse))
        // A book with no pages when the window opened never asks.
        assertFalse(needsKeepChoice(emptyList(), "0", parse))
        assertFalse(needsKeepChoice(emptyList(), "", parse))
    }

    @Test fun theAnswerSettlesThePages() {
        assertEquals(listOf(5, 6), settledPages(listOf(5, 6), "0", true, parse))
        assertEquals(emptyList<Int>(), settledPages(listOf(5, 6), "5-6", false, parse))
        assertEquals(listOf(5, 7), settledPages(listOf(5, 6), "5, 7", null, parse))
        assertEquals(emptyList<Int>(), settledPages(emptyList(), "0", null, parse))
        assertNull(settledPages(listOf(5, 6), "x", null, parse))
    }
}
