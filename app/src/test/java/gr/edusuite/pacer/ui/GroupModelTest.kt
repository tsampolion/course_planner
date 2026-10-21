package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Verdict
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.Plan
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupModelTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun bookInitialsShortenNames() {
        assertEquals("SB", bookInitials("Student's Book"))
        assertEquals("WB", bookInitials("Workbook"))
        assertEquals("WB", bookInitials("WorkBook"))
        assertEquals("B2", bookInitials("Book 2"))
        assertEquals("GIU", bookInitials("Grammar in Use"))
        assertEquals("Reader", bookInitials("Reader"))
        assertEquals("Voc", bookInitials("vocabulary"))
        assertEquals("ΒΜ", bookInitials("βιβλίο μαθητή"))
        assertEquals("", bookInitials("  "))
    }

    @Test fun clashingInitialsKeepFullNames() {
        assertEquals(listOf("SB", "WB", "Reader"), shortBookNames(listOf("Student's Book", "Workbook", "Reader")))
        assertEquals(listOf("Student's Book", "Speaking Book", "WB"), shortBookNames(listOf("Student's Book", "Speaking Book", "Workbook")))
    }

    @Test fun weeksAreThisWeekNextWeekThenDates() {
        // Wednesday 21 October 2026.
        val today = d("2026-10-21")
        val dates = listOf("2026-10-14", "2026-10-23", "2026-10-21", "2026-10-28", "2026-10-30", "2026-11-04", "2026-11-07").map(::d)
        val weeks = weekSections(dates, today) { it }
        assertEquals(listOf(WeekLabel.DATES, WeekLabel.THIS, WeekLabel.NEXT, WeekLabel.DATES), weeks.map { it.label })
        assertEquals(listOf(d("2026-10-12"), d("2026-10-19"), d("2026-10-26"), d("2026-11-02")), weeks.map { it.start })
        // In date order inside each week.
        assertEquals(listOf(d("2026-10-21"), d("2026-10-23")), weeks[1].items)
        // A week runs to Friday, or to Sunday when something falls on the weekend.
        assertEquals(d("2026-10-23"), weeks[1].end)
        assertEquals(d("2026-11-08"), weeks[3].end)
        assertEquals(emptyList<WeekSection<LocalDate>>(), weekSections(emptyList<LocalDate>(), today) { it })
    }

    @Test fun weekRangesReadShort() {
        assertEquals("19–23 Oct", weekRange(d("2026-10-19"), d("2026-10-23")))
        assertEquals("30 Nov – 4 Dec", weekRange(d("2026-11-30"), d("2026-12-04")))
    }

    /** Mondays and Wednesdays in March 2027: 10 lessons. */
    private fun plan(books: List<Book>, lead: String?, pos: Map<String, Int> = emptyMap()) = Plan(
        id = "p", name = "Test", start = d("2027-03-01"), end = d("2027-03-31"), mins = 60, pace = 2.0,
        groups = listOf(Group("g", "G", setOf(MONDAY, WEDNESDAY), d("2027-03-01"), pos = pos)),
        closures = emptyList(), books = books, homework = emptyList(), events = emptyList(), lead = lead,
    )

    @Test fun inStepFollowsTheLeadRule() {
        val books = listOf(Book("sb", "Student's Book", 1, 100), Book("wb", "Workbook", 1, 50))
        // 40 of the lead's 100 pages taught: in step, the workbook has taught round(40 × 50 / 100) = 20 pages.
        val p = plan(books, "sb", mapOf("sb" to 41, "wb" to 11))
        assertEquals(mapOf("wb" to 21), GroupReport(p, p.groups.first()).inStepPages())
        // No lead book, no target.
        assertEquals(emptyMap<String, Int>(), plan(books, null).let { GroupReport(it, it.groups.first()).inStepPages() })
        // A book with a finish-by date keeps its own pace, as followLead leaves it alone.
        val due = plan(listOf(books[0], books[1].copy(due = d("2027-03-20"))), "sb", mapOf("sb" to 41))
        assertEquals(emptyMap<String, Int>(), GroupReport(due, due.groups.first()).inStepPages())
    }

    @Test fun inStepKeepsUnitsInStep() {
        val books = listOf(Book("sb", "Student's Book", 1, 100, units = "1, 51"), Book("wb", "Workbook", 1, 60, units = "1, 21"))
        // The lead starts its second unit, so the workbook would start its second unit too.
        val p = plan(books, "sb", mapOf("sb" to 51))
        assertEquals(mapOf("wb" to 21), GroupReport(p, p.groups.first()).inStepPages())
    }

    @Test fun pagesMoreRoundsUp() {
        assertEquals(1, Verdict.Short(2.0, 3.0, 3.19, 10).pagesMore())
        assertEquals(2, Verdict.Short(2.0, 3.0, 4.5, 10).pagesMore())
    }

    @Test fun groupHintsShowOnce() {
        val ids = listOf(Hints.LESSON_MENU, Hints.PACE_TRACK)
        assertEquals(ids.size, ids.toSet().size)
        val seen = Hints.markSeen(Settings(), Hints.LESSON_MENU)
        assertEquals(true, Hints.seen(seen, Hints.LESSON_MENU))
        assertEquals(false, Hints.seen(seen, Hints.PACE_TRACK))
    }
}
