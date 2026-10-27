package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.model.Closure
import gr.edusuite.pacer.model.DoneLesson
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.TEST_TODAY
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
    private fun d(v: String) = LocalDate.parse(v)

    /** The seed with a second class whose group starts earlier than the sample's (Wednesday 9 September). */
    private fun twoClasses(): SchoolYear {
        val early = sample.copy(
            id = "c2", name = "Early class",
            groups = listOf(sample.groups.first().copy(id = "g2", name = "Early group", from = LocalDate.parse("2026-09-07"))),
        )
        return seed.copy(classes = listOf(sample, early))
    }

    @Test fun theBoardHasOneCellPerGroupByClassFromTheSameVerdict() {
        val year = twoClasses().let { y -> y.copy(classes = y.classes + sample.copy(id = "c3", name = "Empty", groups = emptyList())) }
        val board = pacingBoard(year, TEST_TODAY)
        assertEquals(listOf(sample.id, "c2"), board.map { it.classId })
        assertEquals(setOf("ga", "gb"), board[0].cells.map { it.groupId }.toSet())
        val status = yearStatus(year, TEST_TODAY)
        board.flatMap { it.cells }.forEach { assertEquals(status[it.groupId], it.short) }
    }

    @Test fun groupsInsideAClassFollowThePacingOverviewOrder() {
        // Group B has most of the term off, so it is the one furthest behind.
        val slow = seed.copy(
            closures = seed.closures + Closure("trip", "Trip", d("2026-09-10"), d("2026-12-31"), "gb"),
            classes = listOf(sample.copy(pace = 0.5)),
        )
        val cells = pacingBoard(slow, TEST_TODAY).single().cells
        assertEquals(listOf("gb", "ga"), cells.map { it.groupId })
        assertTrue(cells[0].short!! > cells[1].short!!)
        val overview = sortGroups(pacingOverview(slow, TEST_TODAY), GroupOrder.MOST_BEHIND)
        assertEquals(overview.map { c -> c.groups.map { it.groupId } }, pacingBoard(slow, TEST_TODAY).map { r -> r.cells.map { it.groupId } })
    }

    @Test fun groupsWithNothingToMeasureComeLastAndTiesKeepTheClassOrder() {
        // No pace to measure against, so no verdict.
        val noPace = seed.copy(classes = listOf(sample.copy(pace = 0.0)))
        val cells = pacingBoard(noPace, TEST_TODAY).single().cells
        assertTrue(cells.all { it.short == null })
        assertEquals(listOf("ga", "gb"), cells.map { it.groupId })
    }

    @Test fun behindCellsAreTheOnesShortOfTheTerm() {
        val slow = seed.copy(classes = listOf(sample.copy(pace = 0.5)))
        val cells = pacingBoard(slow, TEST_TODAY).single().cells
        assertTrue(cells.all { it.behind && it.short!! >= 1 })
        assertEquals(BoardSummary(2, 2), pacingBoard(slow, TEST_TODAY).summary())
        assertEquals(groupsBehind(seed, TEST_TODAY), pacingBoard(seed, TEST_TODAY).summary().behind)
        assertEquals(BoardSummary(0, 0), pacingBoard(seed.copy(classes = emptyList()), TEST_TODAY).summary())
        // Nothing to measure is neither on pace nor behind.
        assertFalse(BoardCell("c", "g", "G", null).behind)
    }

    @Test fun aGroupWithALessonTodayIsMarkedAndTheMarkMovesAtMidnight() {
        val wed = d("2026-09-09") // Group A (Mon, Wed, Fri) only
        fun today(day: LocalDate) = pacingBoard(seed, day).single().cells.associate { it.groupId to it.today }
        assertEquals(mapOf("ga" to true, "gb" to false), today(wed))
        assertEquals(mapOf("ga" to false, "gb" to true), today(wed.plusDays(1)))
        assertEquals(mapOf("ga" to false, "gb" to false), today(d("2026-09-13"))) // a Sunday
    }

    @Test fun aLessonAlreadyDoneTodayStillCountsAsToday() {
        val wed = d("2026-09-09")
        val ticked = seed.copy(classes = listOf(sample.copy(groups = sample.groups.map {
            if (it.id == "ga") it.copy(done = listOf(DoneLesson(wed))) else it
        })))
        assertTrue(pacingBoard(ticked, wed).single().cells.first { it.groupId == "ga" }.today)
    }

    @Test fun aDayOffHasNoLessonSoNoMark() {
        val wed = d("2026-09-09")
        val off = seed.copy(closures = seed.closures + Closure("off", "Holiday", wed, wed, ALL_GROUPS))
        assertTrue(pacingBoard(off, wed).single().cells.none { it.today })
    }

    @Test fun theDefaultYearIsTheOneThatStartsLast() {
        val a = seed.copy(id = "a", start = LocalDate.parse("2025-09-10"))
        val b = seed.copy(id = "b", start = LocalDate.parse("2026-09-09"))
        val undated = seed.copy(id = "u", start = null)
        assertEquals("b", defaultYearId(listOf(a, undated, b)))
        assertEquals("u", defaultYearId(listOf(undated)))
        assertNull(defaultYearId(emptyList()))
    }

    private fun cell(g: String, short: Int?) = BoardCell("c", g, g, short)

    @Test fun largeBoardKeepsOnlyGroupsBehindAndNeverCallsAnUnmeasuredGroupOnPace() {
        val board = listOf(
            BoardRow("c1", "A", listOf(cell("a1", 0), cell("a2", 2), cell("a3", null))),
            BoardRow("c2", "B", listOf(cell("b1", 0))),
            BoardRow("c3", "C", listOf(cell("c1", 1), cell("c2", 3))),
        )
        val large = board.behindOnly()
        assertEquals(listOf("A", "C"), large.rows.map { it.className })
        assertEquals(listOf(listOf("a2"), listOf("c1", "c2")), large.rows.map { r -> r.cells.map { it.groupId } })
        assertEquals(2, large.onPace)
        assertEquals(1, large.setUp)
        assertEquals(LargeBoard(emptyList(), 0, 0), emptyList<BoardRow>().behindOnly())
    }

    @Test fun onlyUnmeasuredGroupsAreAllSetUpAndNoneIsOnPace() {
        val large = listOf(BoardRow("c", "A", listOf(cell("a", null), cell("b", null)))).behindOnly()
        assertEquals(LargeBoard(emptyList(), 0, 2), large)
    }

    @Test fun normalAndLargeTextCountTheSameGroupsThreeWays() {
        val board = listOf(
            BoardRow("c1", "A", listOf(cell("a1", 0), cell("a2", 2), cell("a3", null))),
            BoardRow("c2", "B", listOf(cell("b1", 5), cell("b2", null))),
        )
        val summary = board.summary()
        val large = board.behindOnly()
        assertEquals(summary.behind, large.rows.sumOf { it.cells.size })
        assertEquals(summary.groups, summary.behind + large.onPace + large.setUp)
    }

    @Test fun upcomingListsTheNextTestsAndWorksheetsEarliestFirstAndNotPastOnes() {
        val ws = Assignment("w", AssignmentType.WORKSHEET, "Reading worksheet", due = d("2026-10-05"))
        val test = Assignment("t", AssignmentType.TEST, "Mid-term", due = d("2026-10-20"))
        val homework = Assignment("h", AssignmentType.HOMEWORK, "Read", due = d("2026-10-02"))
        val old = Assignment("o", AssignmentType.TEST, "Old", due = d("2026-09-01"))
        val c = sample.copy(
            events = listOf(
                TestEvent("e1", "Unit 1 test", d("2026-10-01"), ALL_GROUPS, true),
                TestEvent("e0", "Placement", d("2026-08-30"), ALL_GROUPS, true),
                TestEvent("e2", "Unit 2 test", d("2026-11-01"), "gb", true),
            ),
            groups = sample.groups.map { if (it.id == "ga") it.copy(assignments = listOf(ws, test, homework, old)) else it },
        )
        val list = upcoming(seed.copy(classes = listOf(c)), d("2026-09-09"), limit = 10)
        assertEquals(listOf("Unit 1 test", "Reading worksheet", "Mid-term", "Unit 2 test"), list.map { it.title })
        assertEquals(listOf(UpcomingKind.TEST, UpcomingKind.WORKSHEET, UpcomingKind.TEST, UpcomingKind.TEST), list.map { it.kind })
        assertEquals(listOf(null, "Group A", "Group A", "Group B"), list.map { it.groupName })
        // Each opens a group's Work page: its own group, or the class's first when the test is for every group.
        assertEquals(listOf("ga", "ga", "ga", "gb"), list.map { it.groupId })
        assertEquals(2, upcoming(seed.copy(classes = listOf(c)), d("2026-09-09"), limit = 2).size)
        // A test on the day itself is still to come; the day after, it is gone.
        assertEquals("Unit 1 test", upcoming(seed.copy(classes = listOf(c)), d("2026-10-01")).first().title)
        assertEquals("Reading worksheet", upcoming(seed.copy(classes = listOf(c)), d("2026-10-02")).first().title)
    }

    @Test fun upcomingIsEmptyWithNothingScheduled() {
        assertTrue(upcoming(seed, d("2026-09-09")).isEmpty())
    }
}
