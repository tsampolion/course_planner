package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.assignmentsOn
import gr.edusuite.pacer.engine.markDone
import gr.edusuite.pacer.engine.plannedPages
import gr.edusuite.pacer.engine.recordLesson
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

    private fun lesson(blocked: Boolean = false) = Lesson(1, today, emptyList(), blocked, listOf("1-2"), false)

    @Test fun noLessonHasNoActions() {
        assertEquals(HeroActions(canDone = false, canEditPages = false), heroActions(null, true))
    }
    @Test fun blockedLessonCannotBeEdited() {
        assertEquals(HeroActions(canDone = true, canEditPages = false), heroActions(lesson(blocked = true), true))
        assertEquals(HeroActions(canDone = true, canEditPages = true), heroActions(lesson(), true))
        assertEquals(HeroActions(canDone = false, canEditPages = false), heroActions(lesson(), false))
    }
    @Test fun savingTaughtPagesMarksDone() {
        val r = report(emptyList())
        val first = r.rows.first()
        val book = r.plan.books.first().id
        val g = r.recordLesson(first.date, mapOf(book to listOf(99)))!!
        assertTrue(g.done.any { it.date == first.date && it.pages[book] == listOf(99) && it.edited })
        assertTrue(GroupReport(r.plan, g).rows.none { it.date == first.date })
    }
    @Test fun identicalPagesStillMarkDone() {
        val r = report(emptyList())
        val first = r.rows.first()
        val g = r.recordLesson(first.date, r.plannedPages(first.date)!!)!!
        assertTrue(g.done.any { it.date == first.date && !it.edited })
    }

    @Test fun phonesUseTwoByTwo() {
        assertEquals(PillLayout.GRID_2X2, homePills(360, false))
        assertEquals(2, homePills(599, false).perLine())
    }
    @Test fun tabletsUseOneRow() {
        assertEquals(PillLayout.ROW_4, homePills(600, false))
        assertEquals(4, homePills(840, false).perLine())
    }
    @Test fun largeTextUsesColumn() {
        assertEquals(PillLayout.COLUMN, homePills(360, true))
        assertEquals(PillLayout.COLUMN, homePills(900, true))
        assertEquals(1, PillLayout.COLUMN.perLine())
    }
    @Test fun progressLineFirstLesson() {
        val r = report(emptyList())
        val line = progressLine(r)
        assertEquals(1, line.lesson)
        assertEquals(r.lessons.size, line.total)
        assertEquals(0f, line.fraction, 0f)
    }
    @Test fun progressLineCountsTaughtLessons() {
        val r = report(emptyList())
        val g = r.markDone(setOf(r.rows.first().date))
        val line = progressLine(GroupReport(r.plan, g))
        assertEquals(2, line.lesson)
        assertEquals(r.lessons.size, line.total)
        assertEquals(1f / r.lessons.size, line.fraction, 1e-6f)
    }
    @Test fun progressLineWhenNoLessons() {
        val p = seedPlan()
        val none = p.copy(end = p.start!!.minusDays(1))
        val line = progressLine(GroupReport(none, none.groups.first()))
        assertEquals(0, line.total)
        assertEquals(0, line.lesson)
        assertEquals(0f, line.fraction, 0f)
    }
    @Test fun fixShowsOnlyForAShortGroupWithALatenessWarning() {
        val behind = listOf<GroupWarning>(GroupWarning.Behind(2, 10))
        assertTrue(showsFix(ProgressStatus.SHORT, behind))
        assertFalse(showsFix(ProgressStatus.ON_PACE, behind))
        assertFalse(showsFix(ProgressStatus.UNMEASURED, behind))
        // Short, but a custom plan already answers it (no Behind warning): nothing to fix from here.
        assertFalse(showsFix(ProgressStatus.SHORT, emptyList()))
        assertFalse(showsFix(ProgressStatus.SHORT, listOf(GroupWarning.Missing(1))))
    }
    @Test fun destinationsOpenTheirScreens() {
        assertEquals(Screen.GroupSettings("g"), GroupDestination.SETTINGS.screen("g"))
        assertEquals(Screen.Group("g", GroupPage.STATS), GroupDestination.STATS.screen("g"))
    }
}
