package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.assignmentsOn
import gr.edusuite.pacer.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class GroupHomeModelTest {
    private val today = LocalDate.of(2026, 10, 23)
    private fun work(id: String, day: Int?, type: AssignmentType = AssignmentType.HOMEWORK) =
        Assignment(id, type, id, due = day?.let { today.plusDays(it.toLong()) })
    private fun report(work: List<Assignment>): GroupReport {
        val p = seedPlan()
        val g = p.groups.first().copy(assignments = work)
        return GroupReport(p.copy(groups = listOf(g)), g)
    }

    @Test fun dueSoonIsDatedOrderedAndLimitedWithoutPastWork() {
        val r = report(listOf(work("past", -1), work("two", 2), work("today", 0), work("none", null),
            work("one", 1), work("four", 4), work("three", 3)))
        assertEquals(listOf("today", "one", "two", "three"), dueSoon(r, today).map { it.id })
        assertEquals(7, r.group.assignments.size)
    }

    @Test fun workSectionsKeepUndatedAndEarlierWorkAndFilterByType() {
        val r = report(listOf(work("past", -1), work("week", 1, AssignmentType.TEST),
            work("later", 10), work("none", null)))
        val sections = workSections(r.group.assignments, today)
        assertEquals(listOf(WorkPeriod.THIS_WEEK, WorkPeriod.LATER, WorkPeriod.UNDATED, WorkPeriod.EARLIER), sections.map { it.period })
        assertEquals(listOf("week"), workSections(r.group.assignments, today, AssignmentType.TEST).flatMap { it.assignments }.map { it.id })
        assertTrue(workSections(emptyList(), today).isEmpty())
    }
    @Test fun assignmentPagesRespectTheSupplementaryIncludeList() {
        val b = Book("b", "Reader", 2, 8, supplementary = true, include = "2-3, 8")
        assertEquals(listOf(2, 3, 8), assignmentPages("2-3, 8", b))
        assertNull(assignmentPages("4", b))
        assertNull(assignmentPages("bad", b))
        assertEquals(emptyList<Int>(), assignmentPages("", b))
    }

    @Test fun takingALessonNeedsADateAndDueCannotPrecedeSetOn() {
        val a = Assignment("a", AssignmentType.TEST, "Test", takesLesson = true)
        assertFalse(assignmentDatesValid(a))
        assertTrue(assignmentDatesValid(a.copy(due = today)))
        assertTrue(assignmentDatesValid(a.copy(takesLesson = false)))
        assertFalse(assignmentDatesValid(a.copy(setOn = today, due = today.minusDays(1))))
        assertTrue(assignmentDatesValid(a.copy(setOn = today, due = today)))
    }
    @Test fun worksheetsKeepTheirTypeInDoneAndUpcomingLessonRows() {
        val p = seedPlan()
        val day = p.start!!
        val a = Assignment("w", AssignmentType.WORKSHEET, "Practice", due = day, takesLesson = true)
        val g = p.groups.first().copy(assignments = listOf(a))
        val r = GroupReport(p.copy(groups = listOf(g)), g)
        val row = lessonsOf(r).first { it.date == day }
        assertEquals(LessonKind.WORKSHEET, row.kind(emptyList()))
        assertEquals(listOf(a), row.assignments)
        val done = g.copy(done = listOf(DoneLesson(day)))
        val taught = lessonsOf(GroupReport(p.copy(groups = listOf(done)), done)).first { it.date == day }
        assertEquals(LessonKind.WORKSHEET, taught.kind(emptyList()))
    }
    @Test fun homeUsesTheActivePlanWhilePlanCanPreviewAnInactiveCustomPlan() {
        val p = seedPlan()
        val custom = CustomPlan(false, listOf(PlannedLesson("custom", mapOf(p.books.first().id to listOf(99)), "Preview")))
        val g = p.groups.first().copy(custom = custom)
        val r = GroupReport(p.copy(groups = listOf(g)), g)
        assertEquals(r.rows, groupPageReport(r, GroupPage.HOME, true).rows)
        assertTrue(groupPageCanTick(r, GroupPage.HOME, true))
        assertEquals(listOf(99), groupPageReport(r, GroupPage.PLAN, true).rows.first().pages.first())
        assertFalse(groupPageCanTick(r, GroupPage.PLAN, true))
        assertEquals(r.rows, groupPageReport(r, GroupPage.PLAN, false).rows)
        assertTrue(groupPageCanTick(r, GroupPage.PLAN, false))
    }
    @Test fun editingWorkKeepsPagesFromAnUnselectedOptionalBook() {
        val old = mapOf("optional" to listOf(2, 3), "core" to listOf(4))
        assertEquals(mapOf("optional" to listOf(2, 3), "core" to listOf(5)),
            assignmentEditedPages(old, mapOf("core" to listOf(5))))
        assertEquals(mapOf("optional" to listOf(2, 3)), assignmentEditedPages(old, mapOf("core" to emptyList())))
    }
}
