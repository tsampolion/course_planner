package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.*
import gr.edusuite.pacer.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class UnitRevisionModelTest {
    @Test fun editingRevisionKeepsTeacherTitleNotesAndHomeworkPair() {
        val old = Assignment("revision", AssignmentType.TEST, "My title", note = "Keep notes", linkedId = "homework")
        val edited = revisionAssignment(old, RevisionKind.WORKSHEET, LocalDate.of(2026, 10, 26), emptyMap(), "Unit revision")!!
        assertEquals("revision", edited.id)
        assertEquals("My title", edited.title)
        assertEquals("Keep notes", edited.note)
        assertEquals("homework", edited.linkedId)
        assertEquals(AssignmentType.WORKSHEET, edited.type)
        assertTrue(edited.takesLesson)
        assertNull(revisionAssignment(old, RevisionKind.NONE, null, emptyMap(), "Unit revision"))
    }
    @Test fun unavailableRevisionIsStillAnExplicitUnscheduledChoice() {
        val a = revisionAssignment(null, RevisionKind.TEST, null, emptyMap(), "Unit 1 revision")!!
        assertNull(a.due)
        assertTrue(a.takesLesson)
        val g = seedPlan().groups.first().withUnitRevision(UnitRevision(0, RevisionKind.TEST), a)
        assertEquals(a.id, g.unitRevisions.single().assignmentId)
        assertEquals(a, g.assignments.single { it.id == a.id })
    }
    @Test fun noneIsPersistedAndTargetKeepsClassIdentity() {
        val g = seedPlan().groups.first().withUnitRevision(UnitRevision(0, RevisionKind.NONE))
        assertEquals(RevisionKind.NONE, g.unitRevisions.single().kind)
        assertNotEquals(UnitRevisionTarget("a", "g", 0), UnitRevisionTarget("b", "g", 0))
    }
    @Test fun revisionDateSkipsTodayBlockedPinnedAndTeacherOwnedContent() {
        val today = LocalDate.of(2026, 10, 26)
        val base = seedPlan()
        val b = base.books.first()
        val g = base.groups.first().copy(days = java.time.DayOfWeek.entries.toSet(), from = today,
            assignments = listOf(Assignment("blocked", AssignmentType.TEST, "Test", due = today.plusDays(1), takesLesson = true)),
            extras = listOf(ExtraLesson("extra", today.plusDays(2), mapOf(b.id to listOf(1)))),
            custom = CustomPlan(true, listOf(
                PlannedLesson("today"), PlannedLesson("manual", touched = true),
                PlannedLesson("supp", inLesson = setOf("reader")), PlannedLesson("notes", note = "Teacher note"),
                PlannedLesson("available"))))
        val p = base.copy(start = today, end = today.plusDays(7), closures = emptyList(), events = emptyList(), groups = listOf(g))
        val r = GroupReport(p, g)
        val candidates = revisionDates(r, 0, today)
        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.all { it > today })
        assertFalse(today.plusDays(1) in candidates)
        assertFalse(today.plusDays(2) in candidates)
        val excluded = r.rows.filter { it.lessonId in setOf("manual", "supp", "notes") }.map { it.date }
        assertTrue(excluded.none { it in candidates })
    }
    @Test fun homeRevisionWritesOnlyTargetClassAndKeepsSelectedClass() {
        val y = seedYear()
        val selected = y.classes.first()
        val other = selected.copy(id = "other-class", groups = listOf(selected.groups.first().copy(id = "other-group")))
        val data = AppData(years = listOf(y.copy(classes = listOf(selected, other))), activeYear = y.id, activeClass = selected.id)
        val a = revisionAssignment(null, RevisionKind.TEST, null, emptyMap(), "Unit 1 revision")!!
        val target = UnitRevisionTarget(other.id, "other-group", 0)
        val changed = data.copy(years = data.years.map { it.withUnitRevision(target, UnitRevision(0, RevisionKind.TEST), a) })
        assertEquals(data.activeClass, changed.activeClass)
        assertEquals(selected, changed.year!!.classes.first())
        assertEquals(a.id, changed.year!!.classes.last().groups.first().unitRevisions.single().assignmentId)
    }

    @Test fun backDismissesOnlyForThisSessionAndBulkDoneQueuesEachUnitOnce() {
        val session = UnitRevisionPrompts()
        val first = UnitRevisionTarget("class", "group", 0)
        val second = first.copy(unitIndex = 1)
        session.offer(listOf(first, second))
        assertEquals(first, session.current)
        session.dismiss(first)
        assertEquals(second, session.current)
        session.dismiss(first) // Closing animation cannot dismiss the next sheet.
        assertEquals(second, session.current)
        session.dismiss(second)
        session.offer(listOf(first, second))
        assertNull(session.current)
        val restarted = UnitRevisionPrompts()
        restarted.offer(listOf(first))
        assertEquals(first, restarted.current)
    }

    @Test fun tickingDoneKeepsTheNextLessonsPositionWithinItsUnit() {
        val start = LocalDate.of(2026, 10, 26)
        val book = Book("lead", "Lead", 1, 4, units = "1, 3")
        val g = seedPlan().groups.first().copy(days = java.time.DayOfWeek.entries.toSet(), from = start)
        val p = seedPlan().copy(start = start, end = start.plusDays(3), closures = emptyList(), events = emptyList(),
            books = listOf(book), groups = listOf(g), lead = book.id, unitsEnabled = true)
        val before = GroupReport(p, g)
        val done = before.markDone(setOf(start))
        val positions = unitLessonPositions(GroupReport(p, done))
        assertEquals(UnitLessonPosition(0, 2, 2), positions[start.plusDays(1)])
        assertEquals(UnitLessonPosition(0, 1, 2), positions[start])
    }

}
