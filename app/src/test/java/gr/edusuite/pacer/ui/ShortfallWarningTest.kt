package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.*
import gr.edusuite.pacer.model.*
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ShortfallWarningTest {
    private val group = Group("g", "Monday", setOf(MONDAY), null)
    private val schoolClass = SchoolClass("c", "Class", 60, 2.0, listOf(group), listOf(Book("b", "Book", 1, 22)), emptyList(), emptyList())
    private val year = SchoolYear("y", "Year", LocalDate.parse("2027-03-01"), LocalDate.parse("2027-03-31"), emptyList(), listOf(schoolClass))
    private val plan = year.planFor(schoolClass, TEST_TODAY)

    @Test fun completeContentBeyondTermDoesNotSilenceSixLessonShortfall() {
        val custom = CustomPlan(true, (0..10).map { i -> PlannedLesson("l$i", mapOf("b" to listOf(i * 2 + 1, i * 2 + 2))) })
        val report = GroupReport(plan, group.copy(custom = custom))
        assertEquals(6, report.shortfallCoverage()?.short)
        assertFalse(report.shortfallCoverage()!!.complete)
        assertTrue(groupWarnings(report).any { it == GroupWarning.Behind(6, 12) })
        assertTrue(groupWarnings(report).any { it == GroupWarning.Overflow(6) })
    }

    @Test fun completeInTermPlanHandlesShortfallButProposedPreviewStillWarns() {
        val custom = GroupReport(plan, group).toCustomPlan()
        val g = group.copy(custom = custom)
        assertTrue(GroupReport(plan, g).shortfallCoverage()!!.complete)
        assertFalse(groupWarnings(GroupReport(plan, g)).any { it is GroupWarning.Behind })
        assertTrue(groupWarnings(GroupReport(plan, g, Source.PROPOSED)).any { it is GroupWarning.Behind })
    }

    @Test fun inactiveCustomPlanDoesNotHideBehindWarning() {
        val custom = GroupReport(plan, group).toCustomPlan().copy(active = false)
        assertTrue(groupWarnings(GroupReport(plan, group.copy(custom = custom))).any { it is GroupWarning.Behind })
    }
}
