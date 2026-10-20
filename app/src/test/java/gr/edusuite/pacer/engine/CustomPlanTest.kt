package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Closure
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.model.seedPlan
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPlanTest {
    private fun d(s: String) = LocalDate.parse(s)

    /** Mondays and Wednesdays in March 2027: 1,3,8,10,15,17,22,24,29,31 = 10 lessons. Books of 20 and 10 pages. */
    private fun plan(closures: List<Closure> = emptyList(), events: List<TestEvent> = emptyList(), due: LocalDate? = null) = Plan(
        id = "p", name = "Test", start = d("2027-03-01"), end = d("2027-03-31"), mins = 60, pace = 3.0,
        groups = listOf(Group("g", "G", setOf(MONDAY, WEDNESDAY), d("2027-03-01"))),
        closures = closures, books = listOf(Book("a", "SB", 1, 20), Book("b", "WB", 1, 10, due = due)), homework = emptyList(), events = events,
    )

    private var n = 0
    private fun ids(): () -> String = { "l${n++}" }

    /** [p]'s group with a custom plan made from the proposed one. */
    private fun withPlan(p: Plan, g: Group = p.groups[0]): Group = g.copy(custom = GroupReport(p, g).toCustomPlan(ids()))

    private fun rows(p: Plan, g: Group, source: Source = Source.ACTIVE) = GroupReport(p, g, source).rows.map { it.date to it.pages }

    @Test fun noCustomPlanChangesNothing() {
        val p = seedPlan()
        for (g in p.groups) {
            assertNull(GroupReport(p, g).custom)
            assertEquals(GroupReport(p, g, Source.PROPOSED).rows, GroupReport(p, g).rows)
            assertEquals(GroupReport(p, g, Source.PROPOSED).rows, GroupReport(p, g, Source.CUSTOM).rows)
        }
    }

    @Test fun aNewCustomPlanIsTheProposedOne() {
        val p = seedPlan()
        for (g0 in p.groups) {
            val g = withPlan(p, g0)
            assertEquals(rows(p, g0), rows(p, g))
            assertTrue(GroupReport(p, g).coverage()!!.ok)
            // An inactive custom plan leaves the active schedule on the proposed one.
            val off = g.copy(custom = g.custom!!.copy(active = false))
            assertNull(GroupReport(p, off).custom)
            assertEquals(rows(p, g0), rows(p, off, Source.CUSTOM))
        }
    }

    @Test fun aDayOffMovesTheContentAndKeepsIt() {
        val p = plan()
        val g = withPlan(p)
        val before = GroupReport(p, g).rows.map { it.pages }
        val off = p.copy(closures = listOf(Closure("c", "Snow", d("2027-03-08"), d("2027-03-08"), ALL_GROUPS)))
        val after = GroupReport(off, g).rows
        assertEquals(9, after.size)
        // Lesson 3 of the plan now falls on the 10th, and the last lesson has no day left.
        assertEquals(before[2], after[2].pages)
        assertEquals(d("2027-03-10"), after[2].date)
        assertEquals(before.take(9), after.map { it.pages })
        assertEquals(1, GroupReport(off, g).coverage()!!.overflow)
        // The proposed plan re-spreads instead, so the two now differ.
        assertTrue(GroupReport(off, g, Source.PROPOSED).rows.map { it.pages } != after.map { it.pages })
    }

    @Test fun aTestThatTakesTheLessonMovesTheContentToo() {
        val p = plan()
        val g = withPlan(p)
        val before = GroupReport(p, g).rows.map { it.pages }
        val t = p.copy(events = listOf(TestEvent("t", "Quiz", d("2027-03-03"), ALL_GROUPS, true)))
        val after = GroupReport(t, g).rows
        assertTrue(after[1].blocked)
        assertEquals(before[1], after[2].pages)
    }

    @Test fun tickingTakesTheLessonAndUntickingPutsItBack() {
        val p = plan()
        val g = withPlan(p)
        val r = GroupReport(p, g)
        val planned = r.rows.map { it.pages }
        val ticked = r.markDone(setOf(r.rows[0].date))
        assertEquals(9, ticked.custom!!.lessons.size)
        assertEquals(planned[0], p.books.map { b -> ticked.done[0].pages[b.id].orEmpty() })
        // The rest of the plan keeps its days.
        assertEquals(planned.drop(1), GroupReport(p, ticked).rows.map { it.pages })
        val back = p.unmarkLesson(ticked, r.rows[0].date)
        assertEquals(planned, GroupReport(p, back).rows.map { it.pages })
        assertTrue(back.done.isEmpty())
    }

    @Test fun tickingOutOfOrderKeepsTheOtherDays() {
        val p = plan()
        val g = withPlan(p)
        val r = GroupReport(p, g)
        val planned = r.rows
        val ticked = r.markDone(setOf(planned[3].date))
        val rows = GroupReport(p, ticked).rows
        assertEquals(planned.filterIndexed { i, _ -> i != 3 }.map { it.date to it.pages }, rows.map { it.date to it.pages })
        val back = p.unmarkLessonsFrom(ticked, planned[0].date)
        assertEquals(planned.map { it.date to it.pages }, rows(p, back))
    }

    @Test fun tickingTheProposedPlanStillTakesTheCustomLesson() {
        val p = plan()
        val g = withPlan(p).let { it.copy(custom = it.custom!!.copy(active = false)) }
        val r = GroupReport(p, g)
        val ticked = r.markDoneUpTo(r.rows[1].date)
        assertEquals(8, ticked.custom!!.lessons.size)
    }

    @Test fun markingDoneUpToMatchesTickingOneAtATime() {
        val p = plan()
        val g = withPlan(p)
        var one = g
        repeat(4) { val rr = GroupReport(p, one); one = rr.markDone(setOf(rr.rows.first().date)) }
        val r = GroupReport(p, g)
        assertEquals(one, r.markDoneUpTo(r.rows[3].date))
    }

    @Test fun recordingOtherPagesLeavesTheRestAndFlagsWhatIsMissing() {
        val p = plan()
        val g = withPlan(p)
        val r = GroupReport(p, g)
        val first = r.rows[0]
        // Lesson 1 planned SB 1–2 and WB 1; the teacher taught SB 1 only.
        val g2 = r.recordLesson(first.date, mapOf("a" to listOf(1)))!!
        assertEquals(r.rows.drop(1).map { it.pages }, GroupReport(p, g2).rows.map { it.pages })
        val cov = GroupReport(p, g2).coverage()!!
        assertEquals(mapOf("a" to listOf(2), "b" to listOf(1)), cov.missing)
        // Re-pacing from the start puts the missed pages first, as the proposed plan would.
        val fixed = g2.copy(custom = GroupReport(p, g2, Source.CUSTOM).repaceFrom(0))
        assertTrue(GroupReport(p, fixed).coverage()!!.ok)
        assertEquals(GroupReport(p, g2, Source.PROPOSED).rows.map { it.pages }, GroupReport(p, fixed).rows.map { it.pages })
    }

    @Test fun repacingAnUntouchedPlanGivesTheProposedOneBack() {
        val p = plan(due = d("2027-03-17"))
        val g = withPlan(p)
        val repaced = g.copy(custom = GroupReport(p, g, Source.CUSTOM).repaceFrom(0))
        assertEquals(rows(p, p.groups[0]), rows(p, repaced))
    }

    @Test fun repacingKeepsTouchedLessonsAndEarlierOnes() {
        val p = plan()
        val g = withPlan(p)
        val c = g.custom!!
        // Lesson 3 teaches SB 5–9 by hand.
        val edited = c.editLesson(c.lessons[2].id, mapOf("a" to (5..9).toList()), "Unit test")
        val g2 = g.copy(custom = edited)
        val cov = GroupReport(p, g2).coverage()!!
        assertTrue(cov.doubled.isNotEmpty())
        val repaced = GroupReport(p, g2, Source.CUSTOM).repaceFrom(3)!!
        assertEquals(edited.lessons.take(3), repaced.lessons.take(3))
        val after = GroupReport(p, g2.copy(custom = repaced)).coverage()!!
        // SB 7–9 were doubled by lessons 4 onwards; those lessons no longer have them.
        assertTrue(after.doubled["a"].orEmpty().none { it in 7..9 })
        assertTrue(after.missing.isEmpty())
    }

    @Test fun editingMarksTouchedAndNotesDoNot() {
        val c = CustomPlan(true, listOf(PlannedLesson("x", mapOf("a" to listOf(1)))))
        assertEquals(false, c.editLesson("x", mapOf("a" to listOf(1)), "Video").lessons[0].touched)
        assertEquals("Video", c.editLesson("x", mapOf("a" to listOf(1)), " Video ").lessons[0].note)
        assertEquals(true, c.editLesson("x", mapOf("a" to listOf(1, 2)), "").lessons[0].touched)
    }

    @Test fun removingALessonMovesItsPagesToTheNextOne() {
        val c = CustomPlan(true, listOf(PlannedLesson("x", mapOf("a" to listOf(1, 2))), PlannedLesson("y", mapOf("a" to listOf(3)))))
        assertEquals(listOf(PlannedLesson("y", mapOf("a" to listOf(1, 2, 3)), touched = true)), c.removeLesson("x").lessons)
        assertEquals(listOf(PlannedLesson("x", mapOf("a" to listOf(1, 2, 3)), touched = true)), c.removeLesson("y").lessons)
    }

    @Test fun movingAndInsertingLessons() {
        val c = CustomPlan(true, listOf(PlannedLesson("x"), PlannedLesson("y"), PlannedLesson("z")))
        assertEquals(listOf("y", "z", "x"), c.moveLesson("x", 2).lessons.map { it.id })
        assertTrue(c.moveLesson("x", 2).lessons.last().touched)
        assertEquals(c, c.moveLesson("x", 0))
        assertEquals(listOf("x", "r", "y", "z"), c.insertLesson(1, PlannedLesson("r", note = "Revision")).lessons.map { it.id })
    }

    @Test fun anInsertedLessonPushesTheRestPastTheTerm() {
        val p = plan()
        val g = withPlan(p)
        val g2 = g.copy(custom = g.custom!!.insertLesson(0, PlannedLesson("r", note = "Revision")))
        val r = GroupReport(p, g2)
        assertEquals("Revision", r.rows[0].note)
        assertEquals(1, r.coverage()!!.overflow)
        // Fit to term: the pages of the lesson past the end re-spread over the lessons after the revision.
        val fit = g2.copy(custom = r.fitToTerm())
        val cov = GroupReport(p, fit).coverage()!!
        assertTrue(cov.toString(), cov.ok)
        assertEquals("Revision", GroupReport(p, fit).rows[0].note)
    }

    @Test fun tidyTakesOutPagesThatNeedNoTeaching() {
        val p = plan()
        val g = withPlan(p)
        val c = g.custom!!
        // Lesson 2 also teaches SB 1, which lesson 1 has, and SB 25, which the book doesn't have.
        val l2 = c.lessons[1]
        val messy = g.copy(custom = c.editLesson(l2.id, l2.pages + ("a" to listOf(1) + l2.pages["a"].orEmpty() + 25), ""))
        val cov = GroupReport(p, messy).coverage()!!
        assertEquals(mapOf("a" to listOf(1)), cov.doubled)
        assertEquals(mapOf("a" to listOf(25)), cov.stale)
        val tidy = messy.copy(custom = GroupReport(p, messy).tidy())
        assertTrue(GroupReport(p, tidy).coverage()!!.ok)
    }

    @Test fun copyingToAGroupWithOtherDays() {
        val p = plan()
        val other = Group("h", "H", setOf(TUESDAY), d("2027-03-01"), pos = mapOf("a" to 3))
        val p2 = p.copy(groups = p.groups + other)
        val g = withPlan(p2)
        val copied = other.copy(custom = p2.copyCustomPlan(g.custom!!, other, ids()))
        val r = GroupReport(p2, copied)
        // Tuesdays: 2, 9, 16, 23, 30. The same lessons in order, without SB 1–2, which group H has taught.
        assertEquals(5, r.rows.size)
        assertEquals(5, r.coverage()!!.overflow)
        assertTrue(r.rows.flatMap { it.pages[0] }.none { it < 3 })
    }

    @Test fun notesGetAColumnInTheTsvOnlyWhenThereAreSome() {
        val p = plan()
        val g = withPlan(p)
        assertTrue(!GroupReport(p, g).asTsv().lines()[0].endsWith("Note"))
        val c = g.custom!!
        val noted = g.copy(custom = c.editLesson(c.lessons[0].id, c.lessons[0].pages, "Video"))
        val lines = GroupReport(p, noted).asTsv().lines()
        assertTrue(lines[0].endsWith("\tNote"))
        assertTrue(lines[1].endsWith("\tVideo"))
    }

    @Test fun problemsNameTheCustomPlan() {
        val p = plan()
        val g = withPlan(p)
        val c = g.custom!!
        val gap = g.copy(custom = c.editLesson(c.lessons[0].id, emptyMap(), ""))
        val msgs = p.copy(groups = listOf(gap)).problems()
        assertTrue(msgs.toString(), msgs.contains("G's plan leaves 3 pages out."))
    }
}
