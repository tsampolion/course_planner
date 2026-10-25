package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class UnitsTest {
    private val start = LocalDate.of(2027, 1, 4)
    private val g = Group("g", "Group", DayOfWeek.entries.toSet(), start)
    private val lead = Book("l", "Lead", 1, 12, units = "1, 4, 9")
    private val workbook = Book("w", "Workbook", 1, 18, units = "1, 11, 15")
    private fun plan(days: Int = 8) = Plan("p", "P", start, start.plusDays(days - 1L), 60, 2.0,
        listOf(g), emptyList(), listOf(lead, workbook), emptyList(), emptyList(), "l",
        unitsEnabled = true, bookLinks = listOf(setOf("l", "w")), today = TEST_TODAY)

    private fun assertBoundaries(p: Plan, group: Group = g) {
        val rows = GroupReport(p, group).rows
        for (u in 0 until (p.books.minOf { it.validUnitStarts()!!.size } - 1)) {
            val ends = p.books.map { it.validUnitStarts()!![u + 1] }
            val last = rows.indexOfLast { r -> r.pages.indices.any { b -> r.pages[b].any { it < ends[b] } } }
            val next = rows.indexOfFirst { r -> r.pages.indices.any { b -> r.pages[b].any { it >= ends[b] } } }
            if (last >= 0 && next >= 0) assertTrue("unit $u crosses rows $last and $next", last < next)
        }
    }
    @Test fun linkedBooksFinishUnitTogether() {
        val p = plan()
        assertBoundaries(p)
        p.books.forEachIndexed { i, b -> assertEquals(p.remainingPages(g, b), GroupReport(p, g).rows.flatMap { it.pages[i] }) }
    }
    @Test fun unlinkedBookIgnoresUnits() {
        val p = plan().copy(bookLinks = emptyList())
        assertEquals(GroupReport(p.copy(unitsEnabled = false), g).rows.map { it.pages[1] }, GroupReport(p, g).rows.map { it.pages[1] })
    }
    @Test fun unitsOffChangesNothing() {
        val p = plan().copy(unitsEnabled = false)
        val off = p.copy(bookLinks = emptyList())
        assertEquals(GroupReport(off, g).rows, GroupReport(p, g).rows)
        val custom = GroupReport(p, g).toCustomPlan { "lesson" }
        val own = g.copy(custom = custom)
        assertEquals(GroupReport(off, own).repaceFrom(0), GroupReport(p, own).repaceFrom(0))
        assertEquals(GroupReport(off, g).catchUpPlan(3, id = { "lesson" }), GroupReport(p, g).catchUpPlan(3, id = { "lesson" }))
    }
    @Test fun groupOverrideBeatsClass() {
        assertFalse(plan().unitsActive(g.copy(unitsOverride = false)))
        assertTrue(plan().unitsActive(g.copy(unitsOverride = true)))
        assertFalse(plan().copy(unitsEnabled = false).unitsActive(g.copy(unitsOverride = true)))
    }
    @Test fun invalidMissingUnsortedAndDuplicateUnitsAreRejected() {
        for (text in listOf("", "2, 4", "1, 9, 4", "1, 4, 4", "1, 13", "1, bad", "1-4")) {
            assertNull(text, lead.copy(units = text).validUnitStarts())
        }
        assertEquals(listOf(1, 4, 9), lead.validUnitStarts())
    }
    @Test fun unitCountMismatchWarns() {
        val p = plan().copy(books = listOf(lead, workbook.copy(units = "1, 11")))
        assertTrue(unitPlan(p, g)!!.issues.any { it.kind == UnitIssueKind.COUNT_MISMATCH })
    }
    @Test fun zeroPageUnitsAreSafe() {
        val p = plan().copy(books = listOf(lead.copy(skip = "4-8"), workbook.copy(skip = "11-14")))
        assertBoundaries(p)
        assertTrue(unitPlan(p, g)!!.windows[1].dates.isEmpty())
    }
    @Test fun revisionTakesALesson() {
        val a = Assignment("r", AssignmentType.TEST, "Revision", due = start.plusDays(3), takesLesson = true)
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.TEST, "r"), a)
        val p = plan()
        assertEquals(7, GroupReport(p, group).teaching)
        assertTrue(GroupReport(p, group).rows.single { it.date == a.due }.blocked)
        assertEquals(g, group.withoutUnitRevision(0))
    }
    @Test fun optionalMembershipSkipHomeworkAndTaughtPagesRemainAccountedFor() {
        val p = plan().copy(books = listOf(lead, workbook.copy(groupOnly = true)))
        val group = g.copy(skip = mapOf("l" to "2"), done = listOf(DoneLesson(start, mapOf("l" to listOf(1)))),
            assignments = listOf(Assignment("h", AssignmentType.HOMEWORK, "HW", due = start, pages = mapOf("l" to listOf(3)))))
        val r = GroupReport(p, group)
        assertEquals(emptyList<Int>(), r.rows.flatMap { it.pages.getOrElse(1) { emptyList() } })
        assertEquals(p.remainingPages(group, lead), r.rows.flatMap { it.pages[0] })
    }
    @Test fun insufficientWindowAndDeadlineWarnWithoutMixingUnits() {
        val p = plan(2).copy(books = listOf(lead, workbook.copy(due = start)))
        val units = unitPlan(p, g)!!
        assertTrue(units.issues.any { it.kind == UnitIssueKind.INSUFFICIENT_WINDOW })
        assertTrue(units.issues.any { it.kind == UnitIssueKind.DEADLINE })
        assertBoundaries(p)
        assertEquals(30, units.windows.sumOf { it.pages.values.sumOf(List<Int>::size) })
    }
    @Test fun catchUpAndRepaceKeepTouchedContentAndUnitBoundaries() {
        val p = plan()
        val c = GroupReport(p, g).toCustomPlan { java.util.UUID.randomUUID().toString() }
        val touched = c.lessons.first().copy(touched = true, note = "Keep")
        val group = g.copy(custom = c.copy(lessons = listOf(touched) + c.lessons.drop(1)))
        val repaced = GroupReport(p, group).repaceFrom(0)!!
        assertEquals(touched, repaced.lessons.first())
        val catch = GroupReport(p, group).catchUpPlan(3)!!
        assertEquals(touched, catch.lessons.first())
        assertBoundaries(p, group.copy(custom = catch))
    }
    @Test fun completedUnitsPromptOnceAndSkipRecordsDecision() {
        val p = plan()
        val group = g.copy(done = listOf(DoneLesson(start, mapOf("l" to (1..3).toList(), "w" to (1..10).toList()))))
        assertEquals(listOf(0), pendingUnitRevisions(p, group))
        assertEquals(emptyList<Int>(), pendingUnitRevisions(p, group.withUnitRevision(UnitRevision(0, RevisionKind.NONE))))
        assertEquals(listOf(0), pendingUnitRevisions(p, group.copy(id = "other")))
    }
    @Test fun completedLeadUnitWaitsForUnfinishedLinkedBook() {
        val p = plan()
        val group = g.copy(pos = mapOf("l" to 4))
        val first = unitPlan(p, group)!!.windows.first()
        assertTrue(first.pages.getValue("l").isEmpty())
        assertEquals((1..10).toList(), first.pages.getValue("w"))
        assertTrue(first.dates.isNotEmpty())
        assertBoundaries(p, group)
    }
    @Test fun revisionsRemoveOnlyTheirOwnedAssignmentAndClearPairReferences() {
        val homework = Assignment("h", AssignmentType.HOMEWORK, "HW", linkedId = "r")
        val assignment = Assignment("r", AssignmentType.TEST, "Revision", due = start.plusDays(4), takesLesson = true, linkedId = "h")
        val other = Assignment("other", AssignmentType.TEST, "Other", due = start.plusDays(6))
        val group = g.copy(assignments = listOf(homework, other))
            .withUnitRevision(UnitRevision(0, RevisionKind.TEST, "r"), assignment)
        val paired = group.withLinkedAssignment("h", assignment)
        val removed = paired.withoutUnitRevision(0)
        assertEquals(setOf("h", "other"), removed.assignments.map { it.id }.toSet())
        assertNull(removed.assignments.single { it.id == "h" }.linkedId)
        assertEquals(group, group.withUnitRevision(UnitRevision(1, RevisionKind.TEST, "other"), other.copy(takesLesson = true)))
    }
    @Test fun noRevisionDateProducesExplicitWarningAndLeavesTaughtHistoryUntouched() {
        val done = DoneLesson(start, mapOf("l" to listOf(1)))
        val group = g.copy(done = listOf(done)).withUnitRevision(UnitRevision(0, RevisionKind.PAGES),
            Assignment("r", AssignmentType.WORKSHEET, "Pages", takesLesson = true, pages = mapOf("w" to listOf(3))))
        assertEquals(listOf(done), group.done)
        assertTrue(unitPlan(plan(), group)!!.issues.any { it.kind == UnitIssueKind.REVISION_UNSCHEDULED })
    }
    @Test fun editingRevisionWithSameAssignmentKeepsHomeworkPairAndTeacherFields() {
        val homework = Assignment("h", AssignmentType.HOMEWORK, "Teacher homework", linkedId = "r", note = "Keep homework")
        val assignment = Assignment("r", AssignmentType.TEST, "Teacher revision", setOn = start,
            due = start.plusDays(4), takesLesson = true, linkedId = "h", note = "Keep revision", pages = mapOf("w" to listOf(2, 3)))
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.TEST), assignment)
            .withLinkedAssignment("r", homework)
        val edited = group.assignments.single { it.id == "r" }.copy(due = start.plusDays(5))
        val updated = group.withUnitRevision(UnitRevision(0, RevisionKind.TEST), edited)
        assertEquals(edited, updated.assignments.single { it.id == "r" })
        assertEquals(homework, updated.assignments.single { it.id == "h" })
        assertEquals(listOf(UnitRevision(0, RevisionKind.TEST, "r")), updated.unitRevisions)
        val removed = updated.withoutUnitRevision(0)
        assertEquals(listOf(homework.copy(linkedId = null)), removed.assignments)
    }
    @Test fun pagesRevisionDisplaysAndRecordsSelectedPagesInBothPlans() {
        val selected = mapOf("w" to listOf(2, 3))
        val a = Assignment("r", AssignmentType.WORKSHEET, "Revision pages", due = start.plusDays(3), takesLesson = true, pages = selected)
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.PAGES, pages = selected), a)
        for (own in listOf(group, group.copy(custom = GroupReport(plan(), group).toCustomPlan()))) {
            val report = GroupReport(plan(), own)
            val row = report.rows.single { it.date == a.due }
            assertTrue(row.blocked)
            assertEquals(listOf(2, 3), row.pages[1])
            assertEquals(collapse(listOf(2, 3)), row.cells[1])
            assertEquals(selected, report.markDone(setOf(a.due!!)).done.single().pages)
        }
    }
    @Test fun chosenRevisionAnchorsTeachingBeforeAndAfterIt() {
        val p = plan(5).copy(books = listOf(lead.copy(last = 10, units = "1, 7")), bookLinks = emptyList())
        val a = Assignment("r", AssignmentType.TEST, "Revision", due = start.plusDays(3), takesLesson = true)
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.TEST), a)
        val rows = GroupReport(p, group).rows
        assertEquals(start.plusDays(2), rows.last { it.pages[0].any { page -> page < 7 } }.date)
        assertTrue(rows.filter { it.pages[0].any { page -> page >= 7 } }.all { it.date > a.due })
        val completed = group.copy(done = listOf(DoneLesson(start, mapOf("l" to (1..6).toList()))))
        assertTrue(GroupReport(p, completed).rows.filter { it.pages[0].isNotEmpty() }.all { it.date > a.due })
    }
    @Test fun impossibleRevisionWindowLeavesLaterUnitUnscheduledAndWarns() {
        val p = plan(5)
        val a = Assignment("r", AssignmentType.TEST, "Revision", due = start.plusDays(20), takesLesson = true)
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.TEST), a)
        assertTrue(GroupReport(p, group).rows.none { row -> row.pages[0].any { it >= 4 } })
        assertTrue(unitPlan(p, group)!!.issues.any { it.kind == UnitIssueKind.REVISION_UNSCHEDULED })
    }
    @Test fun catchUpBoostSurvivesUnitWindowAllocation() {
        val p = plan(10).copy(books = listOf(lead.copy(last = 24, units = "1, 13")), pace = 2.0, bookLinks = emptyList())
        val normal = GroupReport(p, g).catchUpPlan(1)!!
        val faster = GroupReport(p, g).catchUpPlan(4)!!
        assertNotEquals(normal.lessons.map { it.pages }, faster.lessons.map { it.pages })
        assertTrue(faster.lessons.first().pages.getValue("l").size > faster.lessons[3].pages.getValue("l").size)
        assertEquals((1..24).toList(), faster.lessons.flatMap { it.pages["l"].orEmpty() })
        assertBoundaries(p, g.copy(custom = faster))
    }

    @Test fun completedUnitWindowDoesNotIncludeRepeatedRevisionPages() {
        val a = Assignment("r", AssignmentType.WORKSHEET, "Revision", due = start.plusDays(2), takesLesson = true)
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.PAGES, pages = mapOf("l" to listOf(1))), a)
            .copy(done = listOf(DoneLesson(start, mapOf("l" to (1..3).toList(), "w" to (1..10).toList())),
                DoneLesson(start.plusDays(2), mapOf("l" to listOf(1)))))
        assertEquals(start, unitPlan(plan(), group)!!.windows.first().end)
    }

    @Test fun revisionBeforeRequiredTeachingDoesNotReleaseLaterUnits() {
        val p = plan(5)
        val a = Assignment("r", AssignmentType.TEST, "Too early", due = start, takesLesson = true)
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.TEST), a)
        assertTrue(GroupReport(p, group).rows.all { it.pages.all(List<Int>::isEmpty) })
        assertTrue(unitPlan(p, group)!!.issues.any { it.kind == UnitIssueKind.INSUFFICIENT_WINDOW && it.unitIndex == 0 })
    }
    @Test fun importedRevisionDatesOutOfUnitOrderDoNotReleaseLaterUnits() {
        val first = Assignment("r0", AssignmentType.TEST, "First", due = start.plusDays(3), takesLesson = true)
        val second = Assignment("r1", AssignmentType.TEST, "Second", due = start.plusDays(1), takesLesson = true)
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.TEST), first)
            .withUnitRevision(UnitRevision(1, RevisionKind.TEST), second)
        assertTrue(GroupReport(plan(), group).rows.none { it.pages[0].any { page -> page >= 4 } })
        assertTrue(unitPlan(plan(), group)!!.issues.any { it.unitIndex == 1 })
    }
    @Test fun manualNextUnitBeforeRevisionReportsActionableConflict() {
        val a = Assignment("r", AssignmentType.TEST, "Revision", due = start.plusDays(4), takesLesson = true)
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.TEST), a)
            .copy(pos = mapOf("l" to 4, "w" to 11), custom = CustomPlan(true, listOf(PlannedLesson("manual", mapOf("l" to listOf(4)), touched = true))))
        assertTrue(GroupReport(plan(), group).unitIssues().any { it.kind == UnitIssueKind.BOUNDARY_VIOLATION })
    }

    @Test fun pagesRevisionUsesTheOwnedAssignmentsEditedPages() {
        val a = Assignment("r", AssignmentType.WORKSHEET, "Revision", due = start.plusDays(3), takesLesson = true, pages = mapOf("w" to listOf(2)))
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.PAGES, pages = a.pages), a)
            .withAssignment(a.copy(pages = mapOf("w" to listOf(5, 6))))
        val report = GroupReport(plan(), group)
        assertEquals(listOf(5, 6), report.rows.single { it.date == a.due }.pages[1])
        assertEquals(mapOf("w" to listOf(5, 6)), report.markDone(setOf(a.due!!)).done.single().pages)
    }

    @Test fun pagesRevisionExportsItsCoreAndSupplementaryPages() {
        val p = plan().copy(books = plan().books + Book("s", "Supplement", 1, 20, supplementary = true))
        val pages = mapOf("w" to listOf(2, 3), "s" to listOf(7, 8))
        val a = Assignment("r", AssignmentType.WORKSHEET, "Revision export", due = start.plusDays(3), takesLesson = true, pages = pages)
        val group = g.withUnitRevision(UnitRevision(0, RevisionKind.PAGES, pages = pages), a)
        for (own in listOf(group, group.copy(custom = GroupReport(p, group).toCustomPlan()))) {
            val row = GroupReport(p, own).asTsv().lines().single { it.contains("Revision export") }
            assertTrue(row.contains("2–3"))
            assertTrue(row.contains("7–8"))
        }
    }

    @Test fun ordinaryWholeLessonTestStillHasBlankCells() {
        val a = Assignment("ordinary", AssignmentType.TEST, "Test", due = start, takesLesson = true)
        val row = GroupReport(plan(), g.copy(assignments = listOf(a))).rows.first()
        assertEquals(listOf("", ""), row.cells)
    }

    @Test fun foldBacklogDurationSurvivesLinkedUnitWindows() {
        val p = plan(10).copy(books = listOf(lead.copy(last = 24, units = "1, 13"), workbook.copy(last = 24, units = "1, 13")))
        val initial = g.copy(pos = mapOf("l" to 13))
        val custom = GroupReport(p, initial).toCustomPlan()
        val manual = custom.lessons.last().copy(touched = true, note = "Keep this lesson")
        val group = initial.copy(custom = custom.copy(lessons = custom.lessons.dropLast(1) + manual))
        val short = GroupReport(p, group).foldBacklog("w", 1)!!
        val long = GroupReport(p, group).foldBacklog("w", 4)!!
        assertEquals(manual, short.lessons.last())
        assertEquals(manual, long.lessons.last())
        assertEquals(short.lessons.map { it.pages["l"] }, long.lessons.map { it.pages["l"] })
        assertNotEquals(short.lessons.map { it.pages }, long.lessons.map { it.pages })
        assertTrue(long.lessons[1].pages.getValue("w").size > short.lessons[1].pages.getValue("w").size)
        assertEquals((1..24).toList(), long.lessons.flatMap { it.pages["w"].orEmpty() })
        assertBoundaries(p, group.copy(custom = long))
    }

}
