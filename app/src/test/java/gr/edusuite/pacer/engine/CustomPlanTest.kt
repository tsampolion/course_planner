package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Closure
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.ui.GroupWarning
import gr.edusuite.pacer.ui.daysOff
import gr.edusuite.pacer.ui.groupWarnings
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

    @Test fun plannedDatesMatchProposedWhenPlanIsCreatedWithHolidays() {
        val base = plan()
        val holidays = greekHolidays(base.start!!, base.end!!).mapIndexed { i, (date, label) ->
            Closure("holiday$i", label, date, date, ALL_GROUPS)
        }
        val p = base.copy(closures = holidays)
        val g = p.groups[0]
        val proposed = GroupReport(p, g, Source.PROPOSED).rows
        assertTrue(proposed.size < GroupReport(base, g).rows.size)
        assertEquals(proposed.map { it.date to it.pages }, rows(p, withPlan(p)))
    }

    @Test fun daysOffStillListedInCustomView() {
        val p = plan(closures = listOf(Closure("holiday", "Day off", d("2027-03-08"), d("2027-03-08"), ALL_GROUPS)))
        val g = withPlan(p)
        val report = GroupReport(p, g)
        assertTrue(report.custom != null)
        assertEquals(listOf(d("2027-03-08")), p.daysOff(g, report.rows.mapTo(HashSet()) { it.date }).map { it.date })
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

    /** Every touched lesson of [before] is in [after] with the same pages, note, inLesson, replacementBase and id. */
    private fun assertTouchedKept(before: CustomPlan, after: CustomPlan) {
        for (l in before.lessons.filter { it.touched }) assertEquals(l, after.lessons.singleOrNull { it.id == l.id })
    }

    /** Lesson [i] of [g]'s custom plan set to [l], as the teacher's hand edit leaves it. */
    private fun withLesson(g: Group, i: Int, l: PlannedLesson): Group =
        g.copy(custom = g.custom!!.let { c -> c.copy(lessons = c.lessons.mapIndexed { j, old -> if (j == i) l else old }) })

    @Test fun tidyKeepsTouchedLessons() {
        val p = plan()
        val g = withPlan(p)
        // Lesson 2 repeats SB 1 (lesson 1 has it) and holds SB 25 (the book doesn't); the teacher made it.
        val mine = PlannedLesson(g.custom!!.lessons[1].id, mapOf("a" to listOf(1, 3, 4, 25), "b" to listOf(2)), "Video", true, setOf("s"), mapOf("a" to listOf(3, 4)))
        val messy = withLesson(g, 1, mine)
        val tidy = GroupReport(p, messy).tidy()!!
        assertEquals(mine, tidy.lessons[1])
        // The untouched duplicate goes: the touched lesson reserved page 1 first.
        assertEquals(mapOf("a" to listOf(2), "b" to listOf(1)), tidy.lessons[0].pages)
        val cov = GroupReport(p, messy.copy(custom = tidy)).coverage()!!
        assertTrue(cov.doubled.isEmpty())
        assertEquals(mapOf("a" to listOf(25)), cov.stale)
        assertTrue(cov.missing.isEmpty())
        assertTouchedKept(messy.custom!!, tidy)
        // Two touched lessons that repeat a page: both stay and Doubled stays.
        val both = withLesson(messy, 0, messy.custom!!.lessons[0].copy(touched = true))
        val kept = GroupReport(p, both).tidy()!!
        assertEquals(both.custom, kept)
        assertEquals(mapOf("a" to listOf(1)), GroupReport(p, both.copy(custom = kept)).coverage()!!.doubled)
    }

    @Test fun tidyTakesOutPagesThatNeedNoTeaching() {
        val p = plan()
        val g = withPlan(p)
        val l2 = g.custom!!.lessons[1]
        // Lesson 2 also teaches SB 1, which lesson 1 has, and SB 25, which the book doesn't have; nobody touched it.
        val messy = withLesson(g, 1, l2.copy(pages = l2.pages + ("a" to listOf(1) + l2.pages["a"].orEmpty() + 25)))
        val cov = GroupReport(p, messy).coverage()!!
        assertEquals(mapOf("a" to listOf(1)), cov.doubled)
        assertEquals(mapOf("a" to listOf(25)), cov.stale)
        val tidy = messy.copy(custom = GroupReport(p, messy).tidy())
        assertTrue(GroupReport(p, tidy).coverage()!!.ok)
    }

    @Test fun tidyKeepsRemovedBookPages() {
        val base = plan()
        val p = base.copy(books = base.books + Book("r", "RB", 1, 10))
        val g0 = withPlan(p, p.groups[0].copy(removedBooks = setOf("r")))
        val c = g0.custom!!
        val mine = c.lessons[1].copy(pages = c.lessons[1].pages + ("r" to listOf(3)), touched = true)
        val g = withLesson(g0.copy(custom = c.copy(lessons = c.lessons.mapIndexed { j, l -> if (j == 0) l.copy(pages = l.pages + ("r" to listOf(1, 2))) else l })), 1, mine)
        val tidy = GroupReport(p, g).tidy()!!
        assertEquals(listOf(1, 2), tidy.lessons[0].pages["r"])
        assertEquals(mine, tidy.lessons[1])
        assertEquals(mapOf("r" to listOf(1, 2, 3)), GroupReport(p, g.copy(custom = tidy)).coverage()!!.stale)
    }

    /** The plan with a revision lesson in front, which pushes the last lesson past the term, and [tail] after it. */
    private fun overflowing(p: Plan, vararg tail: PlannedLesson): Group {
        val g = withPlan(p)
        val c = g.custom!!.insertLesson(0, PlannedLesson("r", note = "Revision"))
        return g.copy(custom = c.copy(lessons = c.lessons + tail))
    }

    @Test fun fitKeepsTouchedOverflow() {
        val p = plan()
        val mine = PlannedLesson("t", note = "Review", touched = true)
        val g = overflowing(p, mine)
        val fit = GroupReport(p, g).fitToTerm()!!
        assertEquals(mine, fit.lessons.last())
        assertEquals(11, fit.lessons.size)
        assertTouchedKept(g.custom!!, fit)
        val cov = GroupReport(p, g.copy(custom = fit)).coverage()!!
        assertTrue(cov.missing.isEmpty() && cov.doubled.isEmpty() && cov.stale.isEmpty())
        assertEquals(1, cov.overflow)
        assertEquals(1, cov.overflowEdited)
        // The untouched overflow lesson went back into the term; the edited one stays where it is.
        assertTouchedKept(g.custom!!, GroupReport(p, g.copy(custom = fit)).repaceFrom(0)!!)
    }

    @Test fun fitNeverMergesIntoTouched() {
        val p = plan()
        val open = overflowing(p).custom!!
        // Every lesson in the term is the teacher's; the last lesson, past the term, is not.
        val c = open.copy(lessons = open.lessons.mapIndexed { i, l -> if (i < 10) l.copy(touched = true) else l })
        val g = withPlan(p).copy(custom = c)
        val fit = GroupReport(p, g).fitToTerm()!!
        assertEquals(c.lessons.take(10), fit.lessons.take(10))
        assertTouchedKept(c, fit)
        val cov = GroupReport(p, g.copy(custom = fit)).coverage()!!
        assertTrue(cov.doubled.isEmpty() && cov.missing.isEmpty())
    }

    @Test fun fitWithNoSafeSlotKeepsTheWarning() {
        val p = plan()
        val open = overflowing(p).custom!!
        val c = open.copy(lessons = open.lessons.mapIndexed { i, l -> if (i < 10) l.copy(touched = true) else l })
        val g = withPlan(p).copy(custom = c)
        val fit = GroupReport(p, g).fitToTerm()!!
        // Nothing could move, so every core page is still taught once, past the term, and the warning says so.
        assertEquals(c, fit)
        val r = GroupReport(p, g.copy(custom = fit))
        assertEquals(1, r.coverage()!!.overflow)
        assertEquals(GroupWarning.Overflow(1, edited = 0, canFit = false), groupWarnings(r).filterIsInstance<GroupWarning.Overflow>().single())
    }

    @Test fun fitThenRepaceKeepsTouchedLessons() {
        val p = plan()
        val g = overflowing(p, PlannedLesson("t", note = "Review", touched = true)).let { it.copy(custom = it.custom!!.copy(lessons = it.custom!!.lessons.mapIndexed { i, l -> if (i == 3) l.copy(touched = true) else l })) }
        val fit = GroupReport(p, g).fitToTerm()!!
        val both = GroupReport(p, g.copy(custom = fit)).repaceFrom(0)!!
        assertTouchedKept(g.custom!!, both)
        assertTrue(GroupReport(p, g.copy(custom = both)).coverage()!!.let { it.missing.isEmpty() && it.doubled.isEmpty() })
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
    @Test fun ownRateBuildsPlanAtThatRate() {
        val p = plan()
        val r = GroupReport(p, p.groups[0])
        val c = r.customAtRate(2.0)
        assertEquals(15, c.lessons.size)
        assertEquals(30, c.lessons.sumOf { it.pages.values.sumOf(List<Int>::size) })
        assertEquals(5, GroupReport(p, r.group.copy(custom = c)).coverage()!!.overflow)
    }

    @Test fun ownRateEqualsProposedWhenRateIsNeeded() {
        val p = plan(due = d("2027-03-17")).copy(lead = "a")
        val r = GroupReport(p, p.groups[0])
        val c = r.customAtRate(r.rate)
        assertEquals(r.toCustomPlan().lessons.map { it.pages }, c.lessons.map { it.pages })
    }

    @Test fun ownRateKeepsTouchedLessons() {
        val p = plan()
        val g = withPlan(p)
        val c = g.custom!!
        val edited = c.editLesson(c.lessons[2].id, mapOf("a" to (5..9).toList()), "Keep")
        val out = GroupReport(p, g.copy(custom = edited)).customAtRate(2.0)
        assertEquals(edited.lessons[2], out.lessons[2])
        val cov = GroupReport(p, g.copy(custom = out)).coverage()!!
        assertTrue(cov.missing.isEmpty())
        assertTrue(cov.doubled.isEmpty())
    }

    @Test fun ownRateRejectsNonPositiveAndNonFiniteRates() {
        val p = plan()
        val r = GroupReport(p, p.groups[0])
        for (rate in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, 1e-300)) {
            assertTrue(runCatching { r.customAtRate(rate) }.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test fun shortGroupWithCompleteCustomPlanStillWarnsBehind() {
        val p = plan().copy(pace = 1.875)
        val g = p.groups[0]
        // All pages exist in the plan, but six lessons fall after the term.
        val c = CustomPlan(true, (0 until 16).map { i ->
            PlannedLesson("slow$i", mapOf("a" to allocatePages((1..20).toList(), 16)[i], "b" to allocatePages((1..10).toList(), 16)[i]))
        })
        val r = GroupReport(p, g.copy(custom = c))
        assertTrue(r.coverage()!!.missing.isEmpty())
        assertEquals(6, r.shortfallCoverage()!!.short)
        assertEquals(0, r.coverage()!!.shortCovered)
        assertTrue(!r.shortfallCoverage()!!.complete)
    }

    @Test fun coveringPlanSilencesBehind() {
        val p = plan().copy(pace = 1.875)
        val r = GroupReport(p, withPlan(p))
        assertEquals(6, r.coverage()!!.shortCovered)
        assertTrue(r.shortfallCoverage()!!.complete)
    }

    @Test fun earlyCompletionWithEmptyTrailingSlotsStillCoversShortfall() {
        val p = plan().copy(pace = 1.875)
        val g = p.groups[0]
        val c = GroupReport(p, g).customAtRate(6.0)
        val r = GroupReport(p, g.copy(custom = c.copy(lessons = c.lessons + List(5) { PlannedLesson("empty$it") })))
        assertEquals(6, r.coverage()!!.shortCovered)
        assertTrue(r.shortfallCoverage()!!.complete)
    }

    @Test fun ownRateKeepsSupplementaryPlacementAndLeadUnits() {
        val supp = Book("s", "Extra", 1, 20, supplementary = true, include = "1-20")
        val p = plan().copy(lead = "a", books = listOf(Book("a", "SB", 1, 20, units = "1,11"), Book("b", "WB", 1, 10, units = "1,6"), supp))
        val g = withPlan(p)
        val first = g.custom!!.lessons[0].copy(pages = g.custom!!.lessons[0].pages + ("s" to listOf(3,4)), inLesson = setOf("s"))
        val c = g.custom!!.copy(lessons = listOf(first) + g.custom!!.lessons.drop(1))
        val out = GroupReport(p, g.copy(custom = c)).customAtRate(6.0)
        assertEquals(listOf(3,4), out.lessons[0].pages["s"])
        assertEquals(setOf("s"), out.lessons[0].inLesson)
        assertEquals(listOf(2,2,2,2,2), out.lessons.map { it.pages["b"].orEmpty().size })
        assertTrue(GroupReport(p, g.copy(custom = out)).coverage()!!.ok)
    }

    @Test fun veryHighOwnRateStillSchedulesRemainingPages() {
        val p = plan()
        val c = GroupReport(p, p.groups[0]).customAtRate(1e300)
        assertEquals(1, c.lessons.size)
        assertEquals(30, c.lessons[0].pages.values.sumOf { it.size })
    }

}
