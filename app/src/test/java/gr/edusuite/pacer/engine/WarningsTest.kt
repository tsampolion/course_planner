package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Plan
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WarningsTest {
    private fun d(s: String) = LocalDate.parse(s)

    /**
     * Mondays and Wednesdays in March 2027: 1,3,8,10,15,17,22,24,29,31 = 10 teaching lessons.
     * Books "a" (1–20) and "b" (1–10), plus any [extra].
     */
    private fun plan(pace: Double, pos: Map<String, Int> = emptyMap(), extra: List<Book> = emptyList()) = Plan(
        id = "p", name = "Test", start = d("2027-03-01"), end = d("2027-03-31"), mins = 60, pace = pace,
        groups = listOf(Group("g", "G", setOf(MONDAY, WEDNESDAY), d("2027-03-01"), pos)),
        closures = emptyList(), books = listOf(Book("a", "SB", 1, 20), Book("b", "WB", 1, 10)) + extra,
        homework = emptyList(), events = emptyList(),
    )

    private var n = 0
    private fun ids(): () -> String = { "l${n++}" }
    private fun report(p: Plan, source: Source = Source.ACTIVE) = GroupReport(p, p.groups[0], source)

    @Test fun equalWeightsMatchAllocatePages() {
        for (count in 0..40) for (lessons in 1..15) {
            val pages = (1..count).toList()
            for (w in listOf(1L, 7L, 300L)) {
                assertEquals("n=$count L=$lessons w=$w", allocatePages(pages, lessons), allocateWeighted(pages, List(lessons) { w }))
            }
        }
    }

    @Test fun unequalWeights() {
        // W = 5: ends at round(1*10/5)=2, round(4*10/5)=8, 10.
        assertEquals(listOf((1..2).toList(), (3..8).toList(), listOf(9, 10)), allocateWeighted((1..10).toList(), listOf(1, 3, 1)))
        // Half rounds up: 3 pages over [1, 1] ends at round(1.5) = 2.
        assertEquals(listOf(listOf(1, 2), listOf(3)), allocateWeighted(listOf(1, 2, 3), listOf(1, 1)))
        assertEquals(listOf(listOf(1, 2), emptyList()), allocateWeighted(listOf(1, 2), listOf(0, 0)))
        assertEquals(emptyList<List<Int>>(), allocateWeighted(listOf(1, 2), emptyList()))
    }

    @Test fun catchUpNumbers() {
        // 30 pages at 2.0 a lesson need 15 lessons; the term has 10. cutPages = ceil(30 - 10 * 2) = 10.
        val r = report(plan(2.0))
        // boost 1: ceil(10/1) = 10 lessons, the 10th is 31 March.
        assertEquals(CatchUp(10, 1, 10, d("2027-03-31"), true), r.catchUp(1))
        // boost 2: ceil(10/2) = 5 lessons, the 5th is 15 March.
        assertEquals(CatchUp(10, 2, 5, d("2027-03-15"), true), r.catchUp(2))
        // boost 3: ceil(10/3) = 4 lessons, the 4th is 10 March.
        assertEquals(CatchUp(10, 3, 4, d("2027-03-10"), true), r.catchUp(3))
        // Boost coerced to 1.
        assertEquals(r.catchUp(1), r.catchUp(0))
        // A plan that fits has nothing to catch up: 30 pages at 3.0 is exactly 10 lessons.
        assertNull(report(plan(3.0)).catchUp(2))
    }

    @Test fun catchUpInfeasible() {
        // At 1.0: cutPages = 30 - 10 = 20; boost 1 needs 20 lessons, the term has 10.
        val c = report(plan(1.0)).catchUp(1)!!
        assertEquals(20, c.lessons)
        assertFalse(c.feasible)
        assertNull(c.until)
    }

    @Test fun catchUpPlanFrontLoads() {
        val p = plan(2.0)
        val before = report(p, Source.PROPOSED).rows
        val c = report(p).catchUpPlan(2, ids())!!
        assertTrue(c.active)
        assertEquals(10, c.lessons.size)
        // Weights 400 x5 then 200 x5: the first 5 lessons carry s + boost = 4 pages, the rest 2.
        assertEquals(listOf(4, 4, 4, 4, 4, 2, 2, 2, 2, 2), c.lessons.map { l -> l.pages.values.sumOf { it.size } })
        assertEquals((1..20).toList(), c.lessons.flatMap { it.pages["a"].orEmpty() })
        assertEquals((1..10).toList(), c.lessons.flatMap { it.pages["b"].orEmpty() })
        assertTrue(c.lessons.none { it.touched })
        // Pure: the proposed plan is untouched.
        assertEquals(before, report(p, Source.PROPOSED).rows)
    }

    @Test fun repacingCatchUpPlanPreservesItsChosenDistribution() {
        val p = plan(2.0)
        val original = report(p).catchUpPlan(2, ids())!!
        val g = p.groups[0].copy(custom = original)
        val repaced = GroupReport(p, g).repaceFrom(0)!!
        assertEquals(original.lessons.map { it.pages }, repaced.lessons.map { it.pages })
    }

    @Test fun repacingCatchUpKeepsEarlierAndTouchedLessons() {
        val p = plan(2.0)
        val original = report(p).catchUpPlan(2, ids())!!
        val chosen = original.lessons[3]
        val edited = original.editLesson(chosen.id, chosen.pages.mapValues { it.value.take(1) }, "Speaking")
        val g = p.groups[0].copy(custom = edited)
        val repaced = GroupReport(p, g).repaceFrom(2)!!
        assertEquals(2, repaced.catchUpBoost)
        assertEquals(edited.lessons.take(2), repaced.lessons.take(2))
        assertEquals(edited.lessons[3], repaced.lessons[3])
        assertTrue(GroupReport(p, g.copy(custom = repaced)).coverage()!!.ok)
    }

    @Test fun tickingThenRepacingRecalculatesTheRemainingCatchUp() {
        val p = plan(2.0)
        val original = report(p).catchUpPlan(2, ids())!!
        val g = p.groups[0].copy(custom = original)
        val r = GroupReport(p, g)
        val done = r.markDone(setOf(r.rows.first().date))
        val remaining = GroupReport(p, done)
        assertEquals(4, remaining.catchUp(2)!!.lessons)
        val repaced = remaining.repaceFrom(0)!!
        assertEquals(2, repaced.catchUpBoost)
        assertEquals(4 * 4, repaced.lessons.take(4).sumOf { it.pages.values.sumOf { ps -> ps.size } })
        assertTrue(GroupReport(p, done.copy(custom = repaced)).coverage()!!.ok)
        val again = GroupReport(p, done.copy(custom = repaced)).repaceFrom(0)!!
        assertEquals(repaced, again)
    }

    @Test fun finishedCatchUpReturnsToNormalPacingAndKeepsItsChoice() {
        val p = plan(2.0)
        val original = report(p).catchUpPlan(2, ids())!!
        val g = p.groups[0].copy(custom = original)
        val r = GroupReport(p, g)
        val done = r.markDoneUpTo(r.rows[4].date)
        val remaining = GroupReport(p, done)
        assertNull(remaining.catchUp(2))
        val repaced = remaining.repaceFrom(0)!!
        assertEquals(2, repaced.catchUpBoost)
        assertEquals(List(5) { 2 }, repaced.lessons.map { it.pages.values.sumOf { ps -> ps.size } })
    }

    @Test fun repacingAnInactiveCatchUpDoesNotActivateIt() {
        val p = plan(2.0)
        val original = report(p).catchUpPlan(2, ids())!!.copy(active = false)
        val g = p.groups[0].copy(custom = original)
        val repaced = GroupReport(p, g, Source.CUSTOM).repaceFrom(0)!!
        assertEquals(original, repaced)
    }

    @Test fun copyingCatchUpKeepsItsChoice() {
        val p = plan(2.0)
        val original = report(p).catchUpPlan(2, ids())!!
        val copy = p.copyCustomPlan(original, p.groups[0].copy(id = "other"), ids())
        assertEquals(2, copy.catchUpBoost)
        val repaced = GroupReport(p, p.groups[0].copy(custom = copy)).repaceFrom(0)!!
        assertEquals(copy.lessons.map { it.pages }, repaced.lessons.map { it.pages })
    }

    @Test fun catchUpPlanKeepsTouchedLessons() {
        val p0 = plan(2.0)
        val g0 = p0.groups[0]
        val base = GroupReport(p0, g0).toCustomPlan(ids())
        val mine = base.editLesson(base.lessons[1].id, mapOf("a" to listOf(1, 2, 3, 4, 5)), "mine")
        val g = g0.copy(custom = mine)
        val p = p0.copy(groups = listOf(g))
        val c = GroupReport(p, g).catchUpPlan(2, ids())!!
        assertEquals(mine.lessons[1], c.lessons[1])
        assertEquals((1..20).toList(), c.lessons.flatMap { it.pages["a"].orEmpty() }.sorted())
        assertEquals(20, c.lessons.flatMap { it.pages["a"].orEmpty() }.distinct().size)
        assertEquals((1..10).toList(), c.lessons.flatMap { it.pages["b"].orEmpty() })
    }

    @Test fun bookLagsAgainstReferenceAndMedian() {
        // a half done (pos 11: f = 10/20 = 0.5), b not started (f = 0). b's rate = 10 pages / 10 lessons = 1.
        val p = plan(3.0, mapOf("a" to 11))
        // (0.5 - 0) * 10 / 1 = 5 lessons behind.
        assertEquals(listOf(BookLag("b", 5.0)), report(p).bookLags())
        assertEquals(listOf(BookLag("b", 5.0)), report(p).bookLags(reference = "a"))
        assertEquals(emptyList<BookLag>(), report(p).bookLags(reference = "b"))
        // Strictly beyond the threshold.
        assertEquals(emptyList<BookLag>(), report(p).bookLags(threshold = 5.0))
        // A third book c (1–10, pos 9: f = 0.8). Median of a and c is 0.65: (0.65 - 0) * 10 / 1 = 6.5.
        val p3 = plan(3.0, mapOf("a" to 11, "c" to 9), listOf(Book("c", "Gr", 1, 10)))
        val lags = report(p3).bookLags()
        assertEquals(1, lags.size)
        assertEquals("b", lags[0].bookId)
        assertEquals(6.5, lags[0].lessonsBehind, 1e-9)
        assertEquals(5.0, report(p3).bookLags(reference = "a").single().lessonsBehind, 1e-9)
        // One book: nothing to compare.
        assertEquals(emptyList<BookLag>(), report(p.copy(books = p.books.take(1))).bookLags())
    }

    @Test fun foldBacklogMovesPagesEarlier() {
        val p = plan(3.0, mapOf("a" to 11))
        val proposed = report(p, Source.PROPOSED).rows
        // backlog = round(0.5 * 10) = 5; over 2: extra = 500/2 = 250, weights 350, 350, then 100 x8 (W = 1500).
        // b ends at round(350*10/1500)=2, round(700*10/1500)=5, then 5, 6, 7, 7, 8, 9, 9, 10.
        val c = report(p).foldBacklog("b", 2, ids())!!
        assertEquals(listOf(listOf(1, 2), listOf(3, 4, 5)), c.lessons.take(2).map { it.pages["b"].orEmpty() })
        assertEquals((1..10).toList(), c.lessons.flatMap { it.pages["b"].orEmpty() })
        // a keeps the proposed pages, lesson by lesson.
        assertEquals(proposed.map { it.pages[0] }, c.lessons.map { it.pages["a"].orEmpty() })
        assertEquals(proposed, report(p, Source.PROPOSED).rows)
        // a is not behind.
        assertNull(report(p).foldBacklog("a", 2, ids()))
    }
}
