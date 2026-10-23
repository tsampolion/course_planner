package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.*
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class GroupOverridesTest {
    private fun d(v: String) = LocalDate.parse(v)
    private val g = Group("g", "Group", setOf(MONDAY), null)
    private val b = Book("b", "Book", 1, 20)
    private val plan = Plan("p", "Plan", d("2027-03-01"), d("2027-03-31"), 60, 4.0, listOf(g), emptyList(), listOf(b), emptyList(), emptyList())

    @Test fun groupEndStopsLessonsAndCannotExtendTerm() {
        assertEquals(listOf(d("2027-03-01"), d("2027-03-08")), plan.lessonsFor(g.copy(end = d("2027-03-10"))))
        assertEquals(plan.lessonsFor(g), plan.lessonsFor(g.copy(end = d("2027-12-31"))))
    }

    @Test fun groupSkipAddsToBookSkip() {
        val book = b.copy(skip = "2-3")
        assertEquals(listOf(1) + (6..20).toList(), plan.remainingPages(g.copy(skip = mapOf("b" to "3-5")), book))
        assertEquals(listOf(1) + (4..20).toList(), plan.remainingPages(g, book))
    }

    @Test fun extraBeyondGroupEndReturnsPinnedPagesToPool() {
        val group = g.copy(end = d("2027-03-10"), extras = listOf(ExtraLesson("e", d("2027-03-29"), mapOf("b" to listOf(20)))))
        assertTrue(plan.activeExtras(group).isEmpty())
        assertTrue(20 in plan.remainingPages(group, b))
    }

    @Test fun optionalBooksOnlyAffectSelectedGroupsAndKeepRowIndicesAligned() {
        val optional = Book("z", "Optional", 1, 5, groupOnly = true)
        val p = plan.copy(books = listOf(optional, b), lead = optional.id)
        val normal = GroupReport(p, g)
        assertEquals(listOf("b"), normal.plan.books.map { it.id })
        assertNull(normal.plan.lead)
        assertTrue(normal.rows.all { it.pages.size == 1 })
        assertEquals(20, normal.pages)
        assertFalse(normal.asTsv().lineSequence().first().contains("Optional"))
        assertTrue(p.remainingPages(g, optional).isEmpty())
        val selected = g.copy(extraBooks = setOf(optional.id))
        val report = GroupReport(p, selected)
        val baseline = GroupReport(p.copy(books = listOf(optional.copy(groupOnly = false), b)), g)
        assertEquals(25, report.pages)
        assertEquals(baseline.rows, report.rows)
        assertTrue(report.asTsv().lineSequence().first().contains("Optional"))
    }

    @Test fun copyingPlanDropsOptionalSupplementaryPagesForUnselectedTarget() {
        val optional = Book("s", "Reader", 1, 5, supplementary = true, include = "1-5", groupOnly = true)
        val p = plan.copy(books = listOf(b, optional))
        val custom = CustomPlan(true, listOf(PlannedLesson("l", mapOf("b" to listOf(1), "s" to listOf(2)))))
        assertEquals(mapOf("b" to listOf(1)), p.copyCustomPlan(custom, g).lessons.single().pages)
    }

    @Test fun noGroupOverridesChangesNothing() {
        val report = GroupReport(plan, g)
        val absent = GroupReport(plan, g.copy(end = null, skip = emptyMap(), extraBooks = emptySet()))
        assertEquals(report.rows, absent.rows)
        assertEquals(report.verdict(), absent.verdict())
        assertEquals(report.asTsv(), absent.asTsv())
    }
}
