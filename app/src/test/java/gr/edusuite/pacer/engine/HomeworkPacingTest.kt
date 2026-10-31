package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class HomeworkPacingTest {
    private val today = LocalDate.of(2027, 2, 1)
    private val base = seedPlan()
    private val group = base.groups.first()
    private val book = base.books.first()
    private fun plan(a: Assignment) = base.copy(groups = listOf(group.copy(assignments = listOf(a))))
    private fun hw() = Assignment("h", AssignmentType.HOMEWORK, "Read", today.minusDays(2), today.plusDays(1), mapOf(book.id to listOf(2, 3)))

    @Test fun completionNeverChangesPagesOrSchedule() {
        val open = hw()
        val done = open.copy(completed = true)
        assertEquals(plan(open).homeworkPages(group.id, book.id), plan(done).homeworkPages(group.id, book.id))
        val a = plan(open); val b = plan(done)
        assertEquals(GroupReport(a, a.groups.first()).rows, GroupReport(b, b.groups.first()).rows)
    }
    @Test fun homeworkPagesLeavePacingAtOnceWhateverTheDates() {
        val dated = listOf(
            hw(), hw().copy(due = today.plusDays(30)), hw().copy(due = null, setOn = today.plusDays(1)),
            hw().copy(due = null, setOn = today), hw().copy(due = null, setOn = null))
        for (a in dated) {
            val p = plan(a)
            assertEquals(setOf(2, 3), p.homeworkPages(group.id, book.id))
            assertFalse(p.remainingPages(p.groups.first(), book).any { it in setOf(2, 3) })
        }
    }
    @Test fun homeworkAbsentChangesNothing() {
        val before = GroupReport(base, group)
        val other = group.copy(assignments = listOf(hw().copy(type = AssignmentType.WORKSHEET, setOn = null, due = null)))
        val after = GroupReport(base.copy(groups = listOf(other)), other)
        assertEquals(before.rows, after.rows)
        assertEquals(before.banked, after.banked)
        assertEquals(before.bookStats, after.bookStats)
        assertEquals(before.toCustomPlan().lessons.map { it.pages }, after.toCustomPlan().lessons.map { it.pages })
    }
}
