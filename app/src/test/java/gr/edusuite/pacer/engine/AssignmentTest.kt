package gr.edusuite.pacer.engine

import gr.edusuite.pacer.data.migrateAssignments
import gr.edusuite.pacer.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AssignmentTest {
    private val date = LocalDate.of(2026, 9, 9)

    @Test fun migratedAssignmentsChangeNothing() {
        val year = seedYear()
        val base = year.classes.first()
        val c = base.copy(homework = listOf(Homework("h", "ga", base.books.first().id, 12, 15, date)),
            events = listOf(TestEvent("all", "Test", date, ALL_GROUPS, true),
                TestEvent("one", "Alongside", date.plusDays(2), "ga", false)))
        val old = year.planFor(c, TEST_TODAY)
        val migrated = year.planFor(migrateAssignments(c), TEST_TODAY)
        for (g in old.groups) {
            val before = GroupReport(old, g)
            val after = GroupReport(migrated, migrated.groups.first { it.id == g.id })
            // Class-wide targeting is intentionally replaced by group ownership. Compare every
            // schedule/display field while normalising only that obsolete routing metadata.
            fun legacyRows(rows: List<Row>) = rows.map { row -> row.copy(
                events = row.events.map { it.copy(who = g.id) }, assignments = emptyList()) }
            assertEquals(legacyRows(before.rows), legacyRows(after.rows))
            assertEquals(before.pages, after.pages)
            assertEquals(before.banked, after.banked)
            assertEquals(before.teachingDates, after.teachingDates)
            assertEquals(before.verdict(), after.verdict())
            val custom = before.toCustomPlan()
            val oldCustom = GroupReport(old, g.copy(custom = custom))
            val newCustom = GroupReport(migrated, after.group.copy(custom = custom))
            assertEquals(legacyRows(oldCustom.rows), legacyRows(newCustom.rows))
        }
    }

    @Test fun worksheetsBlockButHomeworkNeverTakesALesson() {
        val p = seedPlan()
        val g = p.groups.first().copy(assignments = listOf(
            Assignment("hw", AssignmentType.HOMEWORK, "Read", due = date, takesLesson = true,
                pages = mapOf(p.books.first().id to listOf(1, 2))),
            Assignment("ws", AssignmentType.WORKSHEET, "Practice", due = date.plusDays(2), takesLesson = true),
        ))
        val report = GroupReport(p.copy(groups = listOf(g)), g)
        assertFalse(report.rows.first().blocked)
        assertTrue(report.rows.first { it.date == date.plusDays(2) }.blocked)
        assertEquals(listOf("hw"), report.rows.first().assignments.map { it.id })
        assertEquals(listOf("Practice"), report.rows.first { it.blocked }.events.map { it.name })
        assertEquals(setOf(1, 2), report.plan.homeworkPages(g.id, p.books.first().id, report.plan.today))
    }
    @Test fun migrationKeepsOptionalBooksAndGroupCalendarOverrides() {
        val y = seedYear()
        val base = y.classes.first()
        val optional = Book("optional", "Optional", 1, 12, groupOnly = true)
        val g = base.groups.first().copy(end = date.plusDays(12), skip = mapOf(base.books.first().id to "3-4"), extraBooks = setOf(optional.id))
        val c = base.copy(books = listOf(optional) + base.books, groups = listOf(g),
            homework = listOf(Homework("h", g.id, optional.id, 1, 2, date)),
            events = listOf(TestEvent("t", "Test", date.plusDays(2), ALL_GROUPS, true)))
        val before = GroupReport(y.planFor(c, TEST_TODAY), g)
        val migrated = migrateAssignments(c)
        val after = GroupReport(y.planFor(migrated, TEST_TODAY), migrated.groups.single())
        fun comparable(rows: List<Row>) = rows.map { row -> row.copy(events = row.events.map { it.copy(who = g.id) }, assignments = emptyList()) }
        assertEquals(comparable(before.rows), comparable(after.rows))
        assertEquals(before.plan.books, after.plan.books)
        assertEquals(before.banked, after.banked)
        assertEquals(before.bookStats, after.bookStats)
        assertEquals(g.end, after.group.end)
        assertEquals(g.skip, after.group.skip)
        assertEquals(g.extraBooks, after.group.extraBooks)
        val unselected = after.group.copy(extraBooks = emptySet())
        val without = GroupReport(y.planFor(migrated, TEST_TODAY), unselected)
        assertFalse(without.plan.books.any { it.id == optional.id })
        assertTrue(without.rows.all { it.pages.size == base.books.size })
    }
}
