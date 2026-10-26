package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.*
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AutoTickTest {
    // Mondays 1, 8, 15 and 22 March 2027, 20 pages of one book.
    private val start = LocalDate.of(2027, 3, 1)
    private val p = Plan("p", "P", start, start.plusDays(27), 60, 2.0,
        listOf(Group("g", "G", setOf(MONDAY), start)), emptyList(), listOf(Book("b", "Book", 1, 20)), emptyList(), emptyList(), today = TEST_TODAY)
    private val g = p.groups.single()
    private val ever = LocalDate.of(1970, 1, 1)

    private fun byHand(today: LocalDate): Group {
        var group = g
        while (true) {
            val r = GroupReport(p, group)
            val next = r.rows.firstOrNull()?.takeIf { it.date.isBefore(today) } ?: return group
            group = r.markDone(setOf(next.date))
        }
    }

    @Test fun settlingEqualsTickingEachPastLessonInOrder() {
        val today = start.plusDays(15)
        assertEquals(byHand(today), GroupReport(p, g).settlePast(today, ever))
        assertEquals(listOf(start, start.plusDays(7), start.plusDays(14)), GroupReport(p, g).settlePast(today, ever).done.map { it.date })
    }

    @Test fun todaysLessonStaysToTeach() {
        val monday = start.plusDays(14)
        val settled = GroupReport(p, g).settlePast(monday, ever)
        assertEquals(listOf(start, start.plusDays(7)), settled.done.map { it.date })
        assertEquals(monday, GroupReport(p, settled).rows.first().date)
    }

    @Test fun nothingBeforeTodayChangesNothing() {
        assertSame(g, GroupReport(p, g).settlePast(start, ever))
        assertSame(g, GroupReport(p, g).settlePast(start.minusDays(30), ever))
    }

    @Test fun lessonsLeftUntickedByChoiceAreNotTickedAroundAndTickAgainOnceCleared() {
        val today = start.plusDays(22)
        val from = start.plusDays(10)
        assertSame(g, GroupReport(p, g).settlePast(today, from))
        // The teacher ticks the two old lessons by hand; the one after them then ticks itself, with the one after it.
        val cleared = GroupReport(p, GroupReport(p, g).markDone(setOf(start))).let { it.markDone(setOf(start.plusDays(7))) }
        val next = GroupReport(p, cleared)
        assertEquals(start.plusDays(14), next.rows.first().date)
        assertEquals(listOf(start, start.plusDays(7), start.plusDays(14), start.plusDays(21)), next.settlePast(today, from).done.map { it.date })
    }

    @Test fun aLessonThatDidntHappenKeepsItsDateAndGivesItsPagesBack() {
        val r = GroupReport(p, g)
        val after = r.recordNotTaught(start)!!
        val done = after.done.single()
        assertTrue(done.notTaught)
        assertEquals(start, done.date)
        assertTrue(after.custom == null && p.closures.isEmpty())
        val rows = GroupReport(p, after).rows
        assertEquals(3, rows.size)
        assertEquals((1..20).toList(), rows.flatMap { it.pages.single() }.sorted())
    }

    @Test fun anOrdinaryTickIsNeverNotTaught() {
        val ticked = GroupReport(p, g).markDone(setOf(start))
        assertFalse(ticked.done.single().notTaught)
    }
}
