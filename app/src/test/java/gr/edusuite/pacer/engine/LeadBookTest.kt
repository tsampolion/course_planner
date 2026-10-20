package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.seedPlan
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeadBookTest {
    private fun d(s: String) = LocalDate.parse(s)

    /** Mondays and Wednesdays in March 2027: 10 lessons. */
    private fun plan(books: List<Book>, lead: String?) = Plan(
        id = "p", name = "Test", start = d("2027-03-01"), end = d("2027-03-31"), mins = 60, pace = 2.0,
        groups = listOf(Group("g", "G", setOf(MONDAY, WEDNESDAY), d("2027-03-01"))),
        closures = emptyList(), books = books, homework = emptyList(), events = emptyList(), lead = lead,
    )

    private fun report(p: Plan) = GroupReport(p, p.groups.first())

    /** Pages book [i] gets in each row. */
    private fun col(p: Plan, i: Int) = report(p).rows.map { it.pages[i] }

    /** Cumulative pages book [i] has after each row. */
    private fun ends(p: Plan, i: Int) = col(p, i).runningFold(0) { a, l -> a + l.size }.drop(1)

    @Test fun noLeadChangesNothing() {
        val seed = seedPlan()
        assertEquals(null, seed.lead)
        for (g in seed.groups) {
            assertEquals(GroupReport(seed, g).rows, GroupReport(seed.copy(lead = null), g).rows)
            assertEquals(GroupReport(seed, g).rows, GroupReport(seed.copy(lead = "nope"), g).rows)
        }
        val one = plan(listOf(Book("sb", "SB", 1, 23)), null)
        assertEquals(report(one).rows, report(one.copy(lead = "sb")).rows)
    }

    @Test fun followerMatchesLeadWhenProportionsAgree() {
        val p = plan(listOf(Book("sb", "SB", 1, 20), Book("wb", "WB", 1, 10)), "sb")
        assertEquals((1..10).map { listOf(it) }, col(p, 1))
    }

    @Test fun followerTracksTheLeadsRounding() {
        val books = listOf(Book("sb", "SB", 1, 23), Book("wb", "WB", 1, 7))
        val led = plan(books, "sb")
        // Lead: 2,5,7,9,12,14,16,18,21,23. WB lesson i ends at (14 c_i + 23) / 46.
        assertEquals(listOf(2, 5, 7, 9, 12, 14, 16, 18, 21, 23), ends(led, 0))
        assertEquals(listOf(1, 2, 2, 3, 4, 4, 5, 5, 6, 7), ends(led, 1))
        // The plain rule, for comparison.
        assertEquals(listOf(1, 1, 2, 3, 4, 4, 5, 6, 6, 7), ends(plan(books, null), 1))
        // The lead is untouched, and every follower page lands once, in order.
        assertEquals(col(plan(books, null), 0), col(led, 0))
        assertEquals((1..7).toList(), col(led, 1).flatten())
    }

    @Test fun unitsLinkTheFollower() {
        val p = plan(listOf(Book("sb", "SB", 1, 20, units = "1, 11"), Book("wb", "WB", 1, 10, units = "1, 4")), "sb")
        val wb = col(p, 1)
        assertEquals(
            listOf(listOf(1), listOf(), listOf(2), listOf(), listOf(3), listOf(4), listOf(5, 6), listOf(7), listOf(8, 9), listOf(10)),
            wb,
        )
        // WB's unit 2 (p. 4) starts in the lesson SB reaches p. 11.
        val sb = col(p, 0)
        assertEquals(sb.indexOfFirst { 11 in it }, wb.indexOfFirst { 4 in it })
        assertTrue(p.problems().none { "units" in it })
    }

    @Test fun unitMismatchFallsBackToProportional() {
        val linked = plan(listOf(Book("sb", "SB", 1, 23, units = "1, 11"), Book("wb", "WB", 1, 7, units = "1, 3, 5")), "sb")
        assertEquals(listOf(1, 2, 2, 3, 4, 4, 5, 5, 6, 7), ends(linked, 1))
        assertTrue("WB: its units don't match SB (3 vs 2), so it follows page by page." in linked.problems())
    }

    @Test fun followerWithDueDateKeepsItsOwnPace() {
        val books = listOf(Book("sb", "SB", 1, 23), Book("wb", "WB", 1, 7, due = d("2027-03-15")))
        assertEquals(col(plan(books, null), 1), col(plan(books, "sb"), 1))
    }

    /** A custom plan made with a lead book, re-paced untouched, is the proposed plan with the lead book again. */
    @Test fun repacingACustomPlanFollowsTheLead() {
        val p = plan(listOf(Book("sb", "SB", 1, 23, units = "1, 12"), Book("wb", "WB", 1, 7, units = "1, 4")), "sb")
        val g = p.groups.first()
        val mine = g.copy(custom = report(p).toCustomPlan())
        val repaced = mine.copy(custom = GroupReport(p, mine, Source.CUSTOM).repaceFrom(0))
        assertEquals(report(p).rows.map { it.pages }, GroupReport(p, repaced).rows.map { it.pages })
        // Proportional too.
        val q = p.copy(books = p.books.map { it.copy(units = "") })
        val mine2 = g.copy(custom = report(q).toCustomPlan())
        val repaced2 = mine2.copy(custom = GroupReport(q, mine2, Source.CUSTOM).repaceFrom(0))
        assertEquals(report(q).rows.map { it.pages }, GroupReport(q, repaced2).rows.map { it.pages })
    }
}
