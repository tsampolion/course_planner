package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.Closure
import gr.edusuite.pacer.model.seedYear
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreensModelTest {
    private fun g(name: String, short: Int?, next: String?, c: String = "c") =
        OverviewGroup(c, name, name, short, next?.let(LocalDate::parse), 10)

    private val classes = listOf(
        OverviewClass("c1", "B Junior", listOf(g("Wed–Fri", 0, "2026-10-23", "c1"), g("mon–Wed", null, null, "c1"), g("Tue–Thu", 2, "2026-10-22", "c1"))),
        OverviewClass("c2", "A Junior", listOf(g("Thu–Sat", 1, "2026-10-22", "c2"), g("Mon–Wed", 3, "2026-10-26", "c2"))),
    )

    private fun names(cs: List<OverviewClass>) = cs.map { c -> c.groups.map { it.name } }

    @Test fun mostBehindFirstInsideEachClassOnly() {
        val sorted = sortGroups(classes, GroupOrder.MOST_BEHIND)
        assertEquals(listOf("c1", "c2"), sorted.map { it.classId })
        assertEquals(listOf(listOf("Tue–Thu", "Wed–Fri", "mon–Wed"), listOf("Mon–Wed", "Thu–Sat")), names(sorted))
    }

    @Test fun byNameIgnoresCase() {
        assertEquals(listOf(listOf("mon–Wed", "Tue–Thu", "Wed–Fri"), listOf("Mon–Wed", "Thu–Sat")), names(sortGroups(classes, GroupOrder.NAME)))
    }

    @Test fun byNextLessonPutsGroupsWithNoneLeftLast() {
        assertEquals(listOf(listOf("Tue–Thu", "Wed–Fri", "mon–Wed"), listOf("Thu–Sat", "Mon–Wed")), names(sortGroups(classes, GroupOrder.NEXT_LESSON)))
    }

    @Test fun tiesKeepTheClassOrder() {
        val tied = listOf(OverviewClass("c", "C", listOf(g("B", 0, "2026-10-22"), g("A", 0, "2026-10-22"))))
        assertEquals(listOf(listOf("B", "A")), names(sortGroups(tied, GroupOrder.MOST_BEHIND)))
        assertEquals(listOf(listOf("B", "A")), names(sortGroups(tied, GroupOrder.NEXT_LESSON)))
    }

    @Test fun overviewFollowsTheYearAndItsVerdicts() {
        val year = seedYear()
        val o = pacingOverview(year).single()
        assertEquals(listOf("ga", "gb"), o.groups.map { it.groupId })
        assertEquals(yearStatus(year)["ga"], o.groups.first().short)
        assertEquals(LocalDate.parse("2026-09-09"), o.groups.first().next)
        assertEquals(groupsBehind(year), o.behind)
    }

    @Test fun classSummaryCountsGroupsBooksAndBehind() {
        val year = seedYear()
        val s = classSummaries(year).single()
        assertEquals(listOf("Group A", "Group B"), s.groupNames)
        assertEquals(3, s.books)
        assertEquals(groupsBehind(year), s.behind)
    }

    @Test fun termSummaryCountsWeeksAndEveryoneDaysOffWithoutSundays() {
        val year = seedYear().copy(
            start = LocalDate.parse("2026-09-14"), end = LocalDate.parse("2026-09-27"),
            closures = listOf(
                // Sat 19 to Mon 21: Sunday 20 leaves out.
                Closure("a", "Break", LocalDate.parse("2026-09-19"), LocalDate.parse("2026-09-21"), ALL_GROUPS),
                // Overlaps the break and runs past the term end.
                Closure("b", "Again", LocalDate.parse("2026-09-21"), LocalDate.parse("2026-10-02"), ALL_GROUPS),
                Closure("c", "One group", LocalDate.parse("2026-09-15"), LocalDate.parse("2026-09-15"), "ga"),
            ),
        )
        // 21–26 (six days) plus 19: Sunday 20 and 27 leave out.
        assertEquals(TermSummary(weeks = 2, daysOff = 7), termSummary(year))
        assertEquals(3, termSummary(year.copy(end = LocalDate.parse("2026-09-28")))!!.weeks)
    }

    @Test fun noTermNoSummary() {
        assertNull(termSummary(seedYear().copy(end = null)))
        assertNull(termSummary(seedYear().copy(end = LocalDate.parse("2026-01-01"))))
    }

    @Test fun largeOverviewKeepsOnlyGroupsBehindAndSetsOnPaceClassesApart() {
        val l = largeOverview(classes + OverviewClass("c3", "C", listOf(g("X", 0, "2026-10-22", "c3"))) + OverviewClass("c4", "Empty", emptyList()))
        assertEquals(listOf("c1", "c2"), l.classes.map { it.shown.classId })
        assertEquals(listOf(listOf("Tue–Thu"), listOf("Thu–Sat", "Mon–Wed")), l.classes.map { c -> c.shown.groups.map { it.name } })
        // B Junior: Wed–Fri on pace and mon–Wed unmeasured sit behind its "on pace" link.
        assertEquals(listOf("Wed–Fri", "mon–Wed"), l.classes.first().onPace.map { it.name })
        assertEquals(emptyList<OverviewGroup>(), l.classes.last().onPace)
        assertEquals(listOf("c3"), l.allOnPace.map { it.classId })
    }
}
