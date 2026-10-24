package gr.edusuite.pacer.ui

import org.junit.Assert.*
import org.junit.Test

class UnitsSettingsModelTest {
    @Test fun startsMustCoverTheBookFromItsFirstPageWithoutEmptyUnits() {
        assertEquals(listOf(5, 10, 20), validateUnitStarts(5, 25, listOf("5", "10", "20")))
        listOf(listOf("6", "10"), listOf("5", "5"), listOf("5", "4"), listOf("5", "26"), listOf("5", ""), emptyList()).forEach {
            assertNull(validateUnitStarts(5, 25, it))
        }
    }
    @Test fun spacingCoversTheWholeBookAndRejectsMoreUnitsThanPages() {
        assertEquals(listOf(5, 8, 11), evenlySpacedUnits(5, 13, 3))
        assertEquals(listOf(1, 4, 7), evenlySpacedUnits(1, 10, 3))
        assertNull(evenlySpacedUnits(5, 7, 4))
        assertNull(evenlySpacedUnits(5, 7, 0))
    }
    @Test fun unitsRouteRetainsItsOriginAndClosesWithItsClass() {
        val origin = Nav.root + Screen.Classes + Screen.Class
        val stack = Nav.goTo(origin, Screen.Units)
        assertEquals(origin, Nav.pop(stack))
        assertEquals(origin.take(2), Nav.afterClassSwitch(stack))
        assertEquals(Nav.root, Nav.afterYearSwitch(stack))
        assertNull(Nav.panes(stack, WindowLayout.EXPANDED))
        assertTrue(Screen.Units.showsUndo)
    }
    @Test fun unitFillUsesRemainingPagesAcrossTheLinkedBooks() {
        val p = gr.edusuite.pacer.model.seedPlan()
        val book = p.books.first().copy(first = 1, last = 10, units = "1, 6")
        val plan = p.copy(books = listOf(book))
        val progress = gr.edusuite.pacer.engine.UnitProgress(0, 2, mapOf(book.id to 2), false)
        assertEquals(0.6f, unitCompletion(plan, progress), 0.001f)
        assertEquals(1f, unitCompletion(plan, progress.copy(remaining = mapOf(book.id to 0), complete = true)), 0.001f)
    }

    @Test fun checklistIncludesTransitivelyLinkedBooksFromImportedSets() {
        val plan = gr.edusuite.pacer.model.seedPlan().copy(lead = "lead", bookLinks = listOf(setOf("lead", "work"), setOf("work", "reader"), setOf("other", "separate")))
        assertEquals(setOf("lead", "work", "reader"), linkedUnitBooks(plan))
        assertEquals(emptySet<String>(), linkedUnitBooks(plan.copy(lead = null)))
    }

}
