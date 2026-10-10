package com.coursebook.pacer.engine

import com.coursebook.pacer.model.ALL_GROUPS
import com.coursebook.pacer.model.Book
import com.coursebook.pacer.model.Closure
import com.coursebook.pacer.model.Group
import com.coursebook.pacer.model.Homework
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.model.TestEvent
import com.coursebook.pacer.model.seedPlan
import java.time.DayOfWeek.MONDAY
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

    @Test fun verdictMeasuresAgainstSustainablePace() {
        val fits = GroupReport(plan(), plan().groups[0]).verdict()
        assertEquals(Verdict.Fits(0.0, 2.0), fits)
        val p = plan(books = listOf(Book("b", "Book", 1, 25)))
        val short = GroupReport(p, p.groups[0]).verdict() as Verdict.Short
        assertEquals(2.5, short.by, 1e-9)
        assertEquals(5, short.cutPages)
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
