package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.*
import gr.edusuite.pacer.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class StatsModelTest {
    private val start = LocalDate.of(2026, 10, 5)
    private val book = Book(id = "b", name = "Book", first = 1, last = 20)
    private val group = Group(id = "g", name = "Group", days = setOf(java.time.DayOfWeek.MONDAY), from = null)
    private val plan = Plan("p", "Plan", start, start.plusWeeks(3), 60, 5.0, listOf(group), emptyList(), listOf(book), emptyList(), emptyList(), today = TEST_TODAY)

    @Test fun plannedPositionAdvancesWithLessonsBeforeToday() {
        val report = GroupReport(plan, group)
        assertEquals(1, plannedPositionToday(report, 0, start))
        assertEquals(6, plannedPositionToday(report, 0, start.plusDays(1)))
        assertEquals(21, plannedPositionToday(report, 0, start.plusWeeks(4)))
    }
    @Test fun onTrackGroupHasNoPlanLine() {
        assertNull(planLinePosition(GroupReport(plan, group), 0, start))
        assertEquals(6, planLinePosition(GroupReport(plan, group), 0, start.plusDays(1)))
    }
    @Test fun activeFiguresUseCustomLessonsAndRate() {
        val custom = CustomPlan(true, listOf(
            PlannedLesson("1", mapOf("b" to (1..10).toList())),
            PlannedLesson("2", mapOf("b" to (11..20).toList())),
        ))
        val g = group.copy(custom = custom)
        val active = statsFigures(GroupReport(plan, g))
        assertEquals(2, active.lessons)
        assertEquals(10.0, active.rate, 0.0)
        assertEquals(start.plusWeeks(1), active.end)
        assertEquals(4, statsFigures(GroupReport(plan, g, Source.PROPOSED)).lessons)
    }
    @Test fun monthlyCountsFollowSelectedCustomPlanAndIncludePinnedExtras() {
        val extra = ExtraLesson("extra", start.plusDays(1), mapOf("b" to listOf(20)))
        val custom = CustomPlan(true, listOf(
            PlannedLesson("1", mapOf("b" to (1..10).toList())),
            PlannedLesson("2", mapOf("b" to (11..19).toList())),
        ))
        val g = group.copy(custom = custom, extras = listOf(extra))
        assertEquals(listOf(start.withDayOfMonth(1) to 3), statsMonths(GroupReport(plan, g)))
        assertEquals(listOf(start.withDayOfMonth(1) to 5), statsMonths(GroupReport(plan, g, Source.PROPOSED)))
    }

    @Test fun emptyCustomPlanHasNoMonthlyColumns() {
        val g = group.copy(custom = CustomPlan(true, emptyList()))
        assertTrue(statsMonths(GroupReport(plan, g)).isEmpty())
    }

    @Test fun ownPaceRequiresPositiveFiniteNumber() {
        listOf("", "0", "-1", "NaN", "Infinity", "x").forEach { assertNull(ownPaceRate(it)) }
        assertEquals(2.5, ownPaceRate("2.5")!!, 0.0)
    }
}
