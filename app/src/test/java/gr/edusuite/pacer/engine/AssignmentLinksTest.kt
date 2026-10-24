package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AssignmentLinksTest {
    private val start = LocalDate.of(2027, 2, 1)
    private val base = seedPlan().let { p -> p.copy(start = start, end = start.plusDays(18), closures = emptyList(),
        groups = listOf(p.groups.first().copy(from = start, done = emptyList()))) }
    private fun report(vararg work: Assignment) = GroupReport(base, base.groups.single().copy(assignments = work.toList()))
    private fun hw() = Assignment("h", AssignmentType.HOMEWORK, "Practice", start, start.plusDays(7), contentType = AssignmentType.WORKSHEET)

    @Test fun worksheetHomeworkSuggestsAClassLesson() {
        assertEquals(start, suggestSchedule(report(), hw()))
        assertEquals(start.plusDays(2), suggestSchedule(report(), hw().copy(setOn = start.plusDays(1))))
    }
    @Test fun testOnDateSuggestsHomeworkOnPreviousLesson() {
        assertEquals(start.plusDays(4) to start.plusDays(7),
            suggestHomework(report(), Assignment("t", AssignmentType.TEST, "Quiz", due = start.plusDays(7))))
    }
    @Test fun noSuggestionWhenAlreadyLinked() {
        val h = hw().copy(linkedId = "w")
        val w = Assignment("w", AssignmentType.WORKSHEET, "Practice", due = start.plusDays(2), linkedId = "h")
        assertNull(suggestSchedule(report(h, w), h))
        assertNull(suggestHomework(report(h, w), w))
        assertNotNull(suggestSchedule(report(h.copy(linkedId = "gone")), h.copy(linkedId = "gone")))
    }
    @Test fun suggestionSkipsDaysOff() {
        val closed = base.copy(closures = listOf(Closure("c", "Closed", start, start.plusDays(2), ALL_GROUPS)))
        assertEquals(start.plusDays(4), suggestSchedule(GroupReport(closed, closed.groups.single()), hw()))
    }
    @Test fun blockedLessonsAndDueBoundaryAreExcluded() {
        val blocking = Assignment("b", AssignmentType.TEST, "Other", due = start, takesLesson = true)
        assertEquals(start.plusDays(2), suggestSchedule(report(blocking), hw()))
        assertNull(suggestSchedule(report(blocking), hw().copy(due = start.plusDays(2))))
        assertNull(suggestHomework(report(), Assignment("t", AssignmentType.TEST, "Quiz", due = start)))
        assertNull(suggestSchedule(report(), hw().copy(contentType = null)))
    }
    @Test fun titleReferenceFindsAlreadyScheduledWork() {
        val w = Assignment("w", AssignmentType.WORKSHEET, "Practice", due = start.plusDays(2))
        assertNull(suggestSchedule(report(w), hw().copy(contentType = null, title = "Finish Practice")))
        assertEquals(AssignmentType.TEST, homeworkContent(report(), hw().copy(contentType = null, title = "Unit test")))
    }
    @Test fun pairingUpdatesBothSidesAndDeletionKeepsCounterpart() {
        val h = hw()
        val w = Assignment("w", AssignmentType.WORKSHEET, "Practice", due = start)
        val g = base.groups.single().withAssignment(h).withLinkedAssignment(h.id, w)
        assertEquals("w", g.assignments.first { it.id == "h" }.linkedId)
        assertEquals("h", g.assignments.first { it.id == "w" }.linkedId)
        assertEquals(listOf(h), g.withoutAssignment("w").assignments)
        assertEquals(2, g.withAssignment(h.copy(note = "Changed", linkedId = "w")).assignments.size)
    }
    @Test fun duplicateTitleDoesNotCreateExtraCounterparts() {
        val t = Assignment("t", AssignmentType.TEST, "Quiz", due = start.plusDays(7))
        val h = Assignment("h", AssignmentType.HOMEWORK, "Quiz", start.plusDays(4), t.due)
        assertNull(suggestHomework(report(h), t))
    }
    @Test fun changingTypeReleasesAnIncompatiblePair() {
        val h = hw()
        val w = Assignment("w", AssignmentType.WORKSHEET, "Practice", due = start)
        val paired = base.groups.single().withAssignment(h).withLinkedAssignment(h.id, w)
        val changed = paired.withAssignment(paired.assignments.first().copy(type = AssignmentType.TEST))
        assertTrue(changed.assignments.all { it.linkedId == null })
        assertEquals(base.groups.single().withAssignment(h), base.groups.single().withAssignment(h).withLinkedAssignment(h.id, h.copy(id = "other")))
    }
}
