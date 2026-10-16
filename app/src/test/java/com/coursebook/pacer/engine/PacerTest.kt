package com.coursebook.pacer.engine

import com.coursebook.pacer.model.ALL_GROUPS
import com.coursebook.pacer.model.Book
import com.coursebook.pacer.model.Closure
import com.coursebook.pacer.model.DoneLesson
import com.coursebook.pacer.model.Group
import com.coursebook.pacer.model.Homework
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.model.TestEvent
import com.coursebook.pacer.model.seedPlan
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacerTest {
    private fun d(s: String) = LocalDate.parse(s)

    /** Mondays and Wednesdays in March 2027: 1,3,8,10,15,17,22,24,29,31 = 10 lessons. */
    private fun plan(
        books: List<Book> = listOf(Book("b", "Book", 1, 20)),
        closures: List<Closure> = emptyList(),
        events: List<TestEvent> = emptyList(),
        homework: List<Homework> = emptyList(),
    ) = Plan(
        id = "p", name = "Test", start = d("2027-03-01"), end = d("2027-03-31"), mins = 60, pace = 2.0,
        groups = listOf(Group("g", "G", setOf(MONDAY, WEDNESDAY), d("2027-03-01"))),
        closures = closures, books = books, homework = homework, events = events,
    )

    @Test fun parsesRanges() {
        assertEquals(setOf(8, 24, 25, 26, 27, 30), parseRanges("8, 24–27; 30"))
        assertEquals(setOf(3, 4, 5), parseRanges("5-3"))
        assertEquals(emptySet<Int>(), parseRanges("abc, 1-"))
        assertEquals(emptySet<Int>(), parseRanges("1-999999999"))
    }

    @Test fun collapsesRuns() {
        assertEquals("1–3, 5, 7–8", collapse(listOf(1, 2, 3, 5, 7, 8)))
        assertEquals("—", collapse(emptyList()))
    }

    @Test fun allocationCoversEveryPageOnce() {
        for (n in 0..40) for (l in 1..15) {
            val pages = (1..n).toList()
            val a = allocatePages(pages, l)
            assertEquals(l, a.size)
            assertEquals(pages, a.flatten())
        }
    }

    @Test fun countsLessonsAroundClosures() {
        val r = GroupReport(plan(closures = listOf(Closure("c", "Off", d("2027-03-15"), d("2027-03-17"), ALL_GROUPS))), plan().groups[0])
        assertEquals(8, r.lessons.size)
        assertTrue(d("2027-03-15") !in r.lessons)
    }

    @Test fun evenPaceLandsOnFinalLesson() {
        val r = GroupReport(plan(), plan().groups[0])
        assertEquals(10, r.rows.size)
        assertEquals(List(10) { 2 }, r.rows.map { it.pages[0].size })
        assertEquals(20, r.rows.last().pages[0].last())
        assertEquals(2.0, r.rate, 1e-9)
    }

    @Test fun testThatTakesTheLessonPushesPages() {
        val p = plan(events = listOf(TestEvent("e", "Unit test", d("2027-03-10"), ALL_GROUPS, consumes = true)))
        val r = GroupReport(p, p.groups[0])
        assertEquals(1, r.blocked)
        assertEquals(9, r.teaching)
        assertTrue(r.rows[3].blocked)
        assertEquals(emptyList<Int>(), r.rows[3].pages[0])
        assertEquals(20, r.rows.flatMap { it.pages[0] }.size)
    }

    @Test fun skipAndHomeworkLeaveTheSequence() {
        val p = plan(
            books = listOf(Book("b", "Book", 1, 20, skip = "1-2")),
            homework = listOf(Homework("h", "g", "b", 19, 20, null)),
        )
        val r = GroupReport(p, p.groups[0])
        assertEquals(16, r.pages)
        assertEquals(2, r.banked)
        assertEquals((3..18).toList(), r.rows.flatMap { it.pages[0] })
    }

    @Test fun finishByDateRepacesOnlyThatBook() {
        val p = plan(books = listOf(Book("a", "A", 1, 10), Book("b", "B", 1, 10, due = d("2027-03-10"))))
        val r = GroupReport(p, p.groups[0])
        assertEquals(4, r.bookStats[1].lessons)
        assertEquals(10, r.rows.take(4).sumOf { it.pages[1].size })
        assertTrue(r.rows.drop(4).all { it.pages[1].isEmpty() })
        assertEquals(10, r.bookStats[0].lessons)
    }

    @Test fun finishByBeforeNextLessonCramsIntoOne() {
        val p = plan(books = listOf(Book("b", "B", 1, 10, due = d("2027-02-01"))))
        val r = GroupReport(p, p.groups[0])
        assertTrue(r.bookStats[0].crammed)
        assertEquals(10, r.rows[0].pages[0].size)
        assertTrue(p.problems().any { "no lesson left" in it })
    }

    @Test fun markingDoneAdvancesPositionAndDate() {
        val p = plan()
        val g = GroupReport(p, p.groups[0]).afterNextLesson()!!
        assertEquals(3, g.pos["b"])
        assertEquals(d("2027-03-03"), g.from)
        val p2 = p.copy(groups = listOf(g))
        val r2 = GroupReport(p2, g)
        assertEquals(9, r2.rows.size)
        assertEquals(18, r2.pages)
    }

    /** Parity: marking lessons done in order leaves the schedule the web page's rule gives. */
    @Test fun markingDoneInOrderMatchesTheOldRule() {
        val base = seedPlan()
        for (g0 in base.groups) {
            var old = g0
            repeat(5) { old = GroupReport(base.copy(groups = listOf(old)), old).afterNextLesson()!! }
            val oldRows = GroupReport(base.copy(groups = listOf(old)), old).rows

            val r = GroupReport(base, g0)
            val ticked = r.markDoneUpTo(r.rows[4].date)
            assertEquals(5, ticked.done.size)
            assertEquals(oldRows, GroupReport(base.copy(groups = listOf(ticked)), ticked).rows)

            // One tick at a time gives the same as "up to here".
            var one = g0
            repeat(5) { val rr = GroupReport(base.copy(groups = listOf(one)), one); one = rr.markDone(setOf(rr.rows.first().date)) }
            assertEquals(ticked, one)
        }
    }

    /** Lessons ticked in order stay taught when the calendar changes later, as with the web page's rule. */
    @Test fun tickedLessonsStayPastWhenTheCalendarChanges() {
        val base = seedPlan()
        val g0 = base.groups[0]
        val r = GroupReport(base, g0)
        // Monday, Wednesday and Friday lessons up to Friday 30 October, across the Ohi Day closure on the 28th.
        val last = r.rows[21].date
        assertEquals(d("2026-10-30"), last)
        val ticked = r.markDoneUpTo(last)
        assertEquals(last.plusDays(1), ticked.from)

        // Adding Tuesday brings back no past Tuesday, and gives what moving on lesson by lesson gives.
        var old = g0
        repeat(22) { old = GroupReport(base.copy(groups = listOf(old)), old).afterNextLesson()!! }
        val now = ticked.copy(days = ticked.days + TUESDAY)
        val was = old.copy(days = old.days + TUESDAY)
        val rows = GroupReport(base.copy(groups = listOf(now)), now).rows
        assertEquals(d("2026-11-02"), rows.first().date)
        assertEquals(GroupReport(base.copy(groups = listOf(was)), was).rows, rows)

        // Removing a day off that is past doesn't bring it back either.
        val open = base.copy(closures = base.closures.filter { it.label != "Ohi Day" })
        assertTrue(d("2026-10-28") !in GroupReport(open, ticked).lessons)
    }

    @Test fun untickingPutsTheNextLessonBack() {
        val p = plan()
        val g0 = p.groups[0]
        val r = GroupReport(p, g0)
        val g = r.markDoneUpTo(r.rows[3].date)
        assertEquals(d("2027-03-11"), g.from)

        // Unticking Wednesday 3 March returns it as the next lesson, with the pages it would get back.
        assertEquals(mapOf("b" to listOf(3, 4)), GroupReport(p.copy(groups = listOf(g)), g).plannedPages(d("2027-03-03")))
        val back = g.unmarkDone(d("2027-03-03"))
        assertEquals(d("2027-03-03"), back.from)
        assertEquals(d("2027-03-03"), GroupReport(p.copy(groups = listOf(back)), back).rows.first().date)

        // Unticking everything gives the group as it started; ticking out of order leaves the next lesson alone.
        assertEquals(g0, g.unmarkDoneFrom(d("2027-03-01")))
        assertEquals(g0.from, r.markDone(setOf(r.rows[2].date)).from)
    }

    @Test fun markingDoneWithATestMatchesTheOldRule() {
        val p = plan(events = listOf(TestEvent("t", "Test", d("2027-03-03"), ALL_GROUPS, consumes = true)))
        val g = p.groups[0]
        val r = GroupReport(p, g)
        val ticked = r.markDoneUpTo(d("2027-03-03"))
        // The test day is done with no pages.
        assertEquals(DoneLesson(d("2027-03-03")), ticked.done[1])
        var a = g
        repeat(2) { a = GroupReport(p.copy(groups = listOf(a)), a).afterNextLesson()!! }
        assertEquals(GroupReport(p.copy(groups = listOf(a)), a).rows, GroupReport(p.copy(groups = listOf(ticked)), ticked).rows)
    }

    @Test fun markingOneLessonOutOfOrderRespreadsTheRest() {
        val p = plan()
        val g = p.groups[0]
        val r = GroupReport(p, g)
        // 20 pages over 10 lessons: two a lesson. Tick only the third (pages 5-6).
        assertEquals(listOf(5, 6), r.rows[2].pages[0])
        val ticked = r.markDone(setOf(r.rows[2].date))
        assertEquals(listOf(DoneLesson(d("2027-03-08"), mapOf("b" to listOf(5, 6)))), ticked.done)

        val after = GroupReport(p.copy(groups = listOf(ticked)), ticked)
        assertEquals(9, after.lessons.size)
        assertTrue(d("2027-03-08") !in after.lessons)
        assertEquals(18, after.pages)
        assertEquals(listOf(1, 2), after.rows[0].pages[0])
        assertEquals(listOf(3, 4), after.rows[1].pages[0])
        assertEquals(listOf(7, 8), after.rows[2].pages[0])
        assertEquals(listOf(19, 20), after.rows.last().pages[0])

        // Unticking puts everything back.
        val back = ticked.unmarkDone(d("2027-03-08"))
        assertEquals(g, back)
        assertEquals(r.rows, GroupReport(p.copy(groups = listOf(back)), back).rows)
    }

    @Test fun unmarkingFromADateKeepsEarlierLessons() {
        val p = plan()
        val r = GroupReport(p, p.groups[0])
        val g = r.markDoneUpTo(r.rows[3].date)
        assertEquals(4, g.done.size)
        assertEquals(listOf(d("2027-03-01"), d("2027-03-03")), g.unmarkDoneFrom(d("2027-03-08")).done.map { it.date })
        // Marking an already-done date or a non-lesson day changes nothing.
        assertEquals(g, GroupReport(p.copy(groups = listOf(g)), g).markDone(setOf(d("2027-03-01"), d("2027-03-02"))))
    }

    /** The teacher covers more of two books than planned and none of the third. */
    @Test fun recordingOtherPagesRespreadsTheRest() {
        val p = plan(books = listOf(Book("s", "Student's", 1, 20), Book("gr", "Grammar", 2, 21), Book("w", "Workbook", 6, 15)))
        val g = p.groups[0]
        val r = GroupReport(p, g)
        val third = r.rows[2]
        assertEquals(listOf(listOf(5, 6), listOf(6, 7), listOf(8)), third.pages)

        // Lessons 1 and 2 as planned; lesson 3 teaches Student's 5-8, Grammar 6-8 and no Workbook.
        val two = r.markDoneUpTo(r.rows[1].date)
        val r2 = GroupReport(p.copy(groups = listOf(two)), two)
        assertEquals(mapOf("s" to listOf(5, 6), "gr" to listOf(6, 7), "w" to listOf(8)), r2.plannedPages(third.date))
        val taught = mapOf("s" to listOf(5, 6, 7, 8), "gr" to listOf(6, 7, 8), "w" to emptyList())
        val ticked = r2.recordLesson(third.date, taught)!!
        assertEquals(DoneLesson(third.date, mapOf("s" to listOf(5, 6, 7, 8), "gr" to listOf(6, 7, 8)), edited = true), ticked.done.last())

        // The pages still to teach re-spread over the seven lessons left; Workbook 8 comes first.
        val after = GroupReport(p.copy(groups = listOf(ticked)), ticked)
        assertEquals(7, after.lessons.size)
        assertEquals((9..20).toList(), after.rows.flatMap { it.pages[0] })
        assertEquals((9..21).toList(), after.rows.flatMap { it.pages[1] })
        assertEquals((8..15).toList(), after.rows.flatMap { it.pages[2] })
        assertEquals(listOf(8), after.rows[0].pages[2])

        // The same schedule as moving the positions by hand to where the lesson left off.
        val byHand = g.copy(pos = mapOf("s" to 9, "gr" to 9, "w" to 8), from = r.rows[3].date)
        assertEquals(GroupReport(p.copy(groups = listOf(byHand)), byHand).rows, after.rows)
    }

    @Test fun recordingThePlannedPagesIsATick() {
        val p = plan()
        val r = GroupReport(p, p.groups[0])
        val date = r.rows[2].date
        val ticked = r.markDone(setOf(date))
        assertEquals(ticked, r.recordLesson(date, mapOf("b" to listOf(6, 5))))

        // Editing a done lesson, then putting the planned pages back, clears the flag again.
        val edited = r.recordLesson(date, mapOf("b" to listOf(5, 6, 7)))!!
        val re = GroupReport(p.copy(groups = listOf(edited)), edited)
        assertEquals(mapOf("b" to listOf(5, 6)), re.plannedPages(date))
        assertEquals(ticked, re.recordLesson(date, mapOf("b" to listOf(5, 6))))
        // Saving a done lesson unchanged keeps it as it is.
        assertEquals(edited, re.recordLesson(date, mapOf("b" to listOf(5, 6, 7))))

        // Unknown books are dropped, and a day with no lesson can't be recorded.
        assertEquals(ticked, r.recordLesson(date, mapOf("b" to listOf(5, 6), "gone" to listOf(1))))
        assertEquals(null, r.recordLesson(d("2027-03-02"), mapOf("b" to listOf(1))))
    }

    @Test fun recordingPagesOnATestDayTeachesThem() {
        val p = plan(events = listOf(TestEvent("t", "Test", d("2027-03-03"), ALL_GROUPS, consumes = true)))
        val r = GroupReport(p, p.groups[0])
        assertEquals(emptyMap<String, List<Int>>(), r.plannedPages(d("2027-03-03")))
        val g = r.recordLesson(d("2027-03-03"), mapOf("b" to listOf(1)))!!
        assertEquals(DoneLesson(d("2027-03-03"), mapOf("b" to listOf(1)), edited = true), g.done.single())
        val after = GroupReport(p.copy(groups = listOf(g)), g)
        assertEquals(19, after.pages)
        assertEquals((2..20).toList(), after.rows.flatMap { it.pages[0] })
    }

    @Test fun lessonPagesReadsPagesInTheBook() {
        val b = Book("b", "Book", 5, 20)
        assertEquals(listOf(5, 6, 7, 8, 10), lessonPages("8-5, 10; 6", b))
        assertEquals(listOf(5, 6, 7), lessonPages(" 5–7 ", b))
        assertEquals(emptyList<Int>(), lessonPages("", b))
        assertEquals(emptyList<Int>(), lessonPages(" , ", b))
        assertEquals(null, lessonPages("4", b))
        assertEquals(null, lessonPages("19-21", b))
        assertEquals(null, lessonPages("6, p7", b))
        assertEquals(null, lessonPages("6-", b))
    }

    @Test fun verdictMeasuresAgainstSustainablePace() {
        val fits = GroupReport(plan(), plan().groups[0]).verdict()
        assertEquals(Verdict.Fits(0.0, 2.0), fits)
        val p = plan(books = listOf(Book("b", "Book", 1, 25)))
        val short = GroupReport(p, p.groups[0]).verdict() as Verdict.Short
        assertEquals(2.5, short.by, 1e-9)
        assertEquals(5, short.cutPages)
    }

    /** 21 pages at 1.4 a lesson is exactly 15 lessons, though floating point makes it 15.000000000000002. */
    @Test fun aPlanThatFitsExactlyIsNotShort() {
        val p = plan(books = listOf(Book("b", "Book", 1, 21))).copy(end = d("2027-04-19"), pace = 1.4)
        val r = GroupReport(p, p.groups[0])
        assertEquals(15, r.teaching)
        val v = r.verdict()
        assertTrue("$v", v is Verdict.Fits)
        // Not "-0.00" either.
        assertEquals(0.0, (v as Verdict.Fits).spare, 0.0)
        // One page more is short by a real amount.
        val more = plan(books = listOf(Book("b", "Book", 1, 22))).copy(end = d("2027-04-19"), pace = 1.4)
        val short = GroupReport(more, more.groups[0]).verdict()
        assertTrue("$short", short is Verdict.Short)
        assertEquals(1, (short as Verdict.Short).cutPages)
    }

    @Test fun orthodoxEasterKnownDates() {
        assertEquals(d("2024-05-05"), orthodoxEaster(2024))
        assertEquals(d("2025-04-20"), orthodoxEaster(2025))
        assertEquals(d("2026-04-12"), orthodoxEaster(2026))
        assertEquals(d("2027-05-02"), orthodoxEaster(2027))
    }

    @Test fun greekHolidaysWithinTerm() {
        val h = greekHolidays(d("2026-09-09"), d("2027-05-31")).map { it.second }
        assertEquals(
            listOf(
                "Ohi Day", "Polytechnic Day", "Christmas Day", "New Year's Day", "Epiphany", "Clean Monday",
                "Independence Day", "Good Friday", "Labour Day", "Easter Monday",
            ),
            h,
        )
    }

    @Test fun seedPlanHasNoProblems() {
        assertEquals(emptyList<String>(), seedPlan().problems())
    }
}
