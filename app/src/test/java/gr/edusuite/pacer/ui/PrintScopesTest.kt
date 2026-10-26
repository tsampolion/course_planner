package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintScopesTest {
    private val year = seedYear()
    private val cls = year.classes.first()
    private val a = cls.groups[0]
    private val b = cls.groups[1]

    /** [g] with no lessons left: it starts after the term ends. */
    private fun finished(g: Group) = g.copy(from = year.end!!.plusDays(1))

    private fun withGroups(vararg g: Group) = year.copy(classes = listOf(cls.copy(groups = g.toList())))

    @Test fun everyScopeIsOfferedWhileLessonsRemain() {
        assertEquals(
            listOf(PrintScope.GROUP, PrintScope.CLASS, PrintScope.YEAR),
            PrintScopes.available(year, TEST_TODAY, cls.id, a.id),
        )
    }

    @Test fun aFinishedGroupLosesItsOwnScopeButTheClassStaysPrintable() {
        val y = withGroups(finished(a), b)
        assertEquals(listOf(PrintScope.CLASS, PrintScope.YEAR), PrintScopes.available(y, TEST_TODAY, cls.id, a.id))
        // Printing the class leaves out the empty group, as the sheet always has.
        val plans = PrintScopes.plans(y, TEST_TODAY, PrintScope.CLASS, cls.id, a.id)
        assertEquals(listOf(b.id), plans.single().groups.map { it.id })
    }

    @Test fun anEmptyGroupHasNoGroupScopeToPrint() {
        val y = withGroups(a.copy(days = emptySet()), b)
        assertFalse(PrintScope.GROUP in PrintScopes.available(y, TEST_TODAY, cls.id, a.id))
        assertTrue(PrintScopes.plans(y, TEST_TODAY, PrintScope.GROUP, cls.id, a.id).isEmpty())
    }

    @Test fun aYearWithNothingLeftOffersNothingAndPrintsNothing() {
        val y = withGroups(finished(a), finished(b))
        assertTrue(PrintScopes.available(y, TEST_TODAY, cls.id, a.id).isEmpty())
        PrintScope.entries.forEach { assertTrue(PrintScopes.plans(y, TEST_TODAY, it, cls.id, a.id).isEmpty()) }
        assertTrue(PrintScopes.available(year.copy(classes = emptyList()), TEST_TODAY, null, null).isEmpty())
    }

    @Test fun aScheduleOfOnlyTestsStaysPrintable() {
        val day = LocalDate.of(2026, 10, 26)
        assertEquals(DayOfWeek.MONDAY, day.dayOfWeek)
        val g = a.copy(from = day, days = setOf(DayOfWeek.MONDAY))
        val y = year.copy(
            end = day,
            closures = emptyList(),
            classes = listOf(cls.copy(groups = listOf(g), events = listOf(TestEvent("t", "Unit test", day, g.id, consumes = true)))),
        )
        assertEquals(
            listOf(PrintScope.GROUP, PrintScope.CLASS, PrintScope.YEAR),
            PrintScopes.available(y, TEST_TODAY, cls.id, g.id),
        )
        assertTrue(PrintSheet.html(PrintScopes.plans(y, TEST_TODAY, PrintScope.YEAR, cls.id, g.id)).contains("Unit test"))
    }

    @Test fun mixedScopesPrintOnlyWhatTheReportHasRowsFor() {
        val other = cls.copy(id = "c2", name = "Empty class", groups = listOf(finished(a.copy(id = "x"))))
        val y = year.copy(classes = listOf(cls, other))
        assertTrue(PrintScopes.available(y, TEST_TODAY, other.id, "x").let { PrintScope.GROUP !in it && PrintScope.CLASS !in it && PrintScope.YEAR in it })
        val plans = PrintScopes.plans(y, TEST_TODAY, PrintScope.YEAR, other.id, "x")
        assertEquals(listOf(a.id, b.id), plans.single().groups.map { it.id })
        // Every group handed to the sheet has rows: no blank page.
        val html = PrintSheet.html(plans)
        assertEquals(plans.sumOf { it.groups.size }, html.split("<section>").size - 1)
    }
}
