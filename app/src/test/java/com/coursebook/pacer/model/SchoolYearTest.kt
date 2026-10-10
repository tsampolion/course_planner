package com.coursebook.pacer.model

import com.coursebook.pacer.data.MIGRATED_CLASS_NAME
import com.coursebook.pacer.data.migratePlans
import com.coursebook.pacer.engine.GroupReport
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SchoolYearTest {
    private fun d(s: String) = LocalDate.parse(s)

    private fun klass(id: String, vararg groupIds: String) = SchoolClass(
        id, "Class $id", 60, 3.0,
        groupIds.map { Group(it, "Group $it", setOf(MONDAY, THURSDAY), null) },
        listOf(Book("b$id", "Book", 1, 50)), emptyList(), emptyList(),
    )

    @Test fun planForKeepsOnlyTheDaysOffThatReachTheClass() {
        val a = klass("a", "a1", "a2")
        val b = klass("b", "b1")
        val off = listOf(
            Closure("1", "Holiday", d("2026-10-28"), d("2026-10-28"), ALL_GROUPS),
            Closure("2", "Trip", d("2026-11-02"), d("2026-11-02"), "a"),
            Closure("3", "Report cards", d("2026-11-05"), d("2026-11-05"), "a2"),
            Closure("4", "Other class", d("2026-11-09"), d("2026-11-09"), "b1"),
        )
        val year = SchoolYear("y", "2026–27", d("2026-09-01"), d("2027-06-01"), off, listOf(a, b))

        val plan = year.planFor(a)
        assertEquals(listOf("1" to ALL_GROUPS, "2" to ALL_GROUPS, "3" to "a2"), plan.closures.map { it.id to it.who })
        assertEquals("a", plan.id)
        assertEquals("Class a", plan.name)
        assertEquals(year.start, plan.start)
        assertEquals(a.groups, plan.groups)
        assertEquals(listOf("1" to ALL_GROUPS, "4" to "b1"), year.planFor(b).closures.map { it.id to it.who })
    }

    @Test fun seedYearPacesLikeTheSamplePlan() {
        val year = seedYear()
        val plan = seedPlan()
        assertEquals(year.closures.size, plan.closures.size)
        assertEquals(listOf("Group A", "Group B"), plan.groups.map { it.name })
        assertEquals(Settings().lessonMins, year.classes.first().mins)
    }

    /** Parity: a plan from an older file schedules row for row as it did before migrating. */
    @Test fun migrationKeepsEverySchedule() {
        val old = seedPlan().copy(id = "p1", name = "2026–27 main")
        val other = seedPlan().copy(id = "p2", name = "Summer", groups = old.groups.take(1))
        val data = migratePlans(listOf(old, other), activePlan = "p1", view = "gb")

        assertEquals(listOf("2026–27 main", "Summer"), data.years.map { it.name })
        assertEquals("p1", data.activeYear)
        assertEquals(data.years.first().classes.single().id, data.activeClass)
        assertEquals(MIGRATED_CLASS_NAME, data.schoolClass?.name)
        assertEquals("gb", data.view)

        val migrated = data.plan!!
        for (g in old.groups) {
            assertEquals(GroupReport(old, g).rows, GroupReport(migrated, g).rows)
        }
    }

    @Test fun migrationClosesAGroupFromAnotherPlan() {
        val p = seedPlan().copy(id = "p1")
        assertEquals(AppData.VIEW_SETUP, migratePlans(listOf(p), "p1", "nope").view)
        assertEquals(AppData.VIEW_SETUP, migratePlans(listOf(p), "p1", AppData.VIEW_SETUP).view)
        // An unknown active plan opens nothing.
        val none = migratePlans(listOf(p), "gone", "ga")
        assertEquals("", none.activeYear)
        assertEquals(null, none.plan)
        assertEquals(AppData.VIEW_SETUP, none.view)
    }

    @Test fun suggestsTheNextYear() {
        assertEquals(YearDraft("2026–27", d("2026-09-01"), d("2027-05-31")), suggestYear(emptyList(), d("2026-10-10")))
        assertEquals("2025–26", suggestYear(emptyList(), d("2026-03-01")).name)
        assertEquals("2027–28", suggestYear(listOf(seedYear()), d("2026-10-10")).name)
        // Old years only: the one under way wins.
        val old = seedYear().copy(start = d("2019-09-01"))
        assertEquals("2026–27", suggestYear(listOf(old), d("2026-10-10")).name)
        assertEquals("2099–00", suggestYear(emptyList(), d("2099-09-01")).name)
    }
}
