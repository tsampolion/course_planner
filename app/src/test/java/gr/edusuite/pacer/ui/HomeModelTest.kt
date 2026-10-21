package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.markDone
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.model.seedYear
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModelTest {
    private val seed = seedYear()
    private val sample = seed.classes.single()

    /** The seed with a second class whose group starts earlier than the sample's (Wednesday 9 September). */
    private fun twoClasses(): SchoolYear {
        val early = sample.copy(
            id = "c2", name = "Early class",
            groups = listOf(sample.groups.first().copy(id = "g2", name = "Early group", from = LocalDate.parse("2026-09-07"))),
        )
        return seed.copy(classes = listOf(sample, early))
    }

    @Test fun theEarliestLessonAcrossClassesWins() {
        val next = nextLesson(twoClasses(), LocalDate.parse("2026-09-01"))!!
        assertEquals("c2", next.classId)
        assertEquals("g2", next.groupId)
        assertEquals(LocalDate.parse("2026-09-07"), next.date)
        assertFalse(next.overdue)
        assertTrue(next.what.startsWith("Student's Book "))
    }

    @Test fun aTieGoesToTheFirstClassThenTheFirstGroup() {
        val tied = sample.copy(groups = sample.groups.map { it.copy(days = sample.groups.first().days) })
        val next = nextLesson(seed.copy(classes = listOf(tied)), LocalDate.parse("2026-09-01"))!!
        assertEquals("ga", next.groupId)
    }

    @Test fun aPastLessonIsOverdue() {
        assertTrue(nextLesson(seed, LocalDate.parse("2026-10-01"))!!.overdue)
    }

    @Test fun aLessonATestTakesNamesTheTest() {
        val day = LocalDate.parse("2026-09-09")
        val withTest = sample.copy(events = listOf(TestEvent("t", "Unit 1 test", day, "ga", consumes = true)))
        val next = nextLesson(seed.copy(classes = listOf(withTest)), day)!!
        assertTrue(next.blocked)
        assertEquals("Unit 1 test", next.what)
    }

    @Test fun noClassesOrNoLessonsGiveNothing() {
        assertNull(nextLesson(seed.copy(classes = emptyList()), LocalDate.parse("2026-09-01")))
        assertNull(nextLesson(seed.copy(start = null, end = null), LocalDate.parse("2026-09-01")))
    }

    /** Home ticks a group of any class; the result must equal ticking it on the group screen of the open class. */
    @Test fun tickingFromHomeEqualsTickingTheOpenClass() {
        val year = twoClasses()
        val ticked = year.markNextLessonDone("c2", "g2")
        val c = year.classes[1]
        val plan = year.planFor(c)
        val r = GroupReport(plan, plan.groups.single())
        val expected = r.markDone(setOf(r.rows.first().date))
        assertEquals(expected, ticked.classes[1].groups.single())
        assertEquals(year.classes[0], ticked.classes[0])
        // That group's next lesson moves on to its second one.
        val after = ticked.planFor(ticked.classes[1])
        assertEquals(r.rows[1].date, GroupReport(after, after.groups.single()).rows.first().date)
    }

    @Test fun unknownIdsChangeNothing() {
        assertEquals(seed, seed.markNextLessonDone("nope", "ga"))
        assertEquals(seed, seed.markNextLessonDone(sample.id, "nope"))
    }

    @Test fun todayCountsEveryGroupsLessonsTodayDoneOrNot() {
        val wed = LocalDate.parse("2026-09-09") // Group A (Mon, Wed, Fri) only
        assertEquals(TodayCount(1, 1), lessonsToday(seed, wed))
        val ticked = seed.markNextLessonDone(sample.id, "ga")
        assertEquals(TodayCount(0, 1), lessonsToday(ticked, wed))
        assertEquals(TodayCount(1, 1), lessonsToday(ticked, wed.plusDays(1))) // Group B's first
        assertEquals(TodayCount(0, 0), lessonsToday(seed, LocalDate.parse("2026-09-13"))) // a Sunday
        assertEquals(TodayCount(2, 2), lessonsToday(twoClasses(), wed))
    }

    @Test fun theBoardHasOneCellPerGroupByClassFromTheSameVerdict() {
        val year = twoClasses().let { y -> y.copy(classes = y.classes + sample.copy(id = "c3", name = "Empty", groups = emptyList())) }
        val board = pacingBoard(year)
        assertEquals(listOf(sample.id, "c2"), board.map { it.classId })
        assertEquals(listOf("ga", "gb"), board[0].cells.map { it.groupId })
        val status = yearStatus(year)
        board.flatMap { it.cells }.forEach { assertEquals(status[it.groupId], it.short) }
    }

    @Test fun behindCellsAreTheOnesShortOfTheTerm() {
        val slow = seed.copy(classes = listOf(sample.copy(pace = 0.5)))
        val cells = pacingBoard(slow).single().cells
        assertTrue(cells.all { it.behind && it.short!! >= 1 })
        assertEquals(BoardSummary(2, 2), pacingBoard(slow).summary())
        assertEquals(groupsBehind(seed), pacingBoard(seed).summary().behind)
        assertEquals(BoardSummary(0, 0), pacingBoard(seed.copy(classes = emptyList())).summary())
        // Nothing to measure is neither on pace nor behind.
        assertFalse(BoardCell("c", "g", "G", null).behind)
    }

    @Test fun theDefaultYearIsTheOneThatStartsLast() {
        val a = seed.copy(id = "a", start = LocalDate.parse("2025-09-10"))
        val b = seed.copy(id = "b", start = LocalDate.parse("2026-09-09"))
        val undated = seed.copy(id = "u", start = null)
        assertEquals("b", defaultYearId(listOf(a, undated, b)))
        assertEquals("u", defaultYearId(listOf(undated)))
        assertNull(defaultYearId(emptyList()))
    }
}
