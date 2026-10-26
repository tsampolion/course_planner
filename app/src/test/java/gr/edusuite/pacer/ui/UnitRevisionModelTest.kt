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

    @Test fun theSheetOpensOnlyWhenAskedAndDismissOnlyClosesTheOneOpen() {
        val session = UnitRevisionPrompts()
        val first = UnitRevisionTarget("class", "group", 0)
        val second = first.copy(unitIndex = 1)
        assertNull(session.current)
        session.open(first)
        assertEquals(first, session.current)
        session.dismiss(second) // A closing animation of another sheet cannot dismiss this one.
        assertEquals(first, session.current)
        session.dismiss(first)
        assertNull(session.current)
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

    @Test fun editingOwnedRevisionKeepsReservedDateBeforeTouchedCustomLesson() {
        val start = LocalDate.of(2026, 10, 26)
        val a = Assignment("revision", AssignmentType.TEST, "Revision", due = start.plusDays(1), takesLesson = true)
        val g = seedPlan().groups.first().copy(days = java.time.DayOfWeek.entries.toSet(), from = start,
            assignments = listOf(a), unitRevisions = listOf(UnitRevision(0, RevisionKind.TEST, assignmentId = a.id)),
            custom = CustomPlan(true, listOf(PlannedLesson("first"), PlannedLesson("manual", touched = true))))
        val p = seedPlan().copy(start = start, end = start.plusDays(3), books = listOf(Book("b", "Book", 1, 8)),
            closures = emptyList(), events = emptyList(), groups = listOf(g))
        val report = GroupReport(p, g)
        assertEquals("manual", report.rows.first { it.date == start.plusDays(2) }.lessonId)
        val dates = revisionDates(report, 0, start)
        assertTrue("Owned date must survive editing", start.plusDays(1) in dates)
        val withUnits = p.copy(books = listOf(Book("b", "Book", 1, 8, units = "1, 5")), lead = "b", unitsEnabled = true)
        assertTrue("Owned date must keep the existing unit boundary", start.plusDays(1) in revisionDates(GroupReport(withUnits, g), 0, start))
        assertFalse("Touched lesson still owns D3", start.plusDays(2) in dates)
        assertFalse(start.plusDays(1) in revisionDates(report, 0, start.plusDays(1)))
        val blocked = g.copy(assignments = g.assignments + a.copy(id = "other"))
        assertFalse(start.plusDays(1) in revisionDates(GroupReport(p, blocked), 0, start))
        val pinned = g.copy(extras = listOf(ExtraLesson("pin", start.plusDays(1), mapOf("b" to listOf(2)))))
        assertFalse(start.plusDays(1) in revisionDates(GroupReport(p, pinned), 0, start))
        val taught = g.copy(done = listOf(DoneLesson(start.plusDays(1))))
        assertFalse(start.plusDays(1) in revisionDates(GroupReport(p, taught), 0, start))
    }

    @Test fun pageRevisionsOfferAndAcceptOrdinaryAndSupplementaryBooks() {
        val ordinary = Book("core", "Core", 1, 8)
        val supplementary = Book("reader", "Reader", 1, 8, supplementary = true, include = "1-8")
        val plan = seedPlan().copy(books = listOf(ordinary, supplementary))
        assertEquals(listOf("core", "reader"), revisionBooks(plan).map { it.id })
        assertTrue(validRevisionPages(plan, mapOf("core" to listOf(2))))
        assertTrue(validRevisionPages(plan, mapOf("reader" to listOf(2))))
        assertFalse(validRevisionPages(plan, mapOf("missing" to listOf(2))))
        assertFalse(validRevisionPages(plan, emptyMap()))
        assertFalse(validRevisionPages(plan, mapOf("core" to listOf(9))))
    }

    @Test fun repeatedCoreRevisionPagesDoNotAddLessonsToTheOriginalUnit() {
        val start = LocalDate.of(2026, 10, 26)
        val book = Book("lead", "Lead", 1, 4, units = "1, 3")
        val assignment = Assignment("revision", AssignmentType.WORKSHEET, "Revision", due = start.plusDays(2), takesLesson = true)
        val g = seedPlan().groups.first().copy(days = java.time.DayOfWeek.entries.toSet(), from = start.plusDays(3),
            done = listOf(DoneLesson(start, mapOf(book.id to listOf(1))), DoneLesson(start.plusDays(1), mapOf(book.id to listOf(2))),
                DoneLesson(start.plusDays(2), mapOf(book.id to listOf(1, 2)))),
            assignments = listOf(assignment), unitRevisions = listOf(UnitRevision(0, RevisionKind.PAGES, assignmentId = assignment.id, pages = mapOf(book.id to listOf(1, 2)))))
        val p = seedPlan().copy(start = start, end = start.plusDays(4), closures = emptyList(), events = emptyList(),
            books = listOf(book), groups = listOf(g), lead = book.id, unitsEnabled = true)
        val positions = unitLessonPositions(GroupReport(p, g))
        assertEquals(UnitLessonPosition(0, 1, 2), positions[start])
        assertEquals(UnitLessonPosition(0, 2, 2), positions[start.plusDays(1)])
        assertNull(positions[start.plusDays(2)])
        val history = lessonsOf(GroupReport(p, g)).single { it.date == start.plusDays(2) }
        assertTrue(history.blocked)
        assertEquals(listOf("1–2"), history.cells)
    }

}
