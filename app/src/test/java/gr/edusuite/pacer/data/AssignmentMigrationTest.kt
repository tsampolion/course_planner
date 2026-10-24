package gr.edusuite.pacer.data

import gr.edusuite.pacer.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AssignmentMigrationTest {
    private val date = LocalDate.of(2026, 10, 23)

    @Test fun legacyWorkReachesOnlyItsGroupsAndMigrationIsIdempotent() {
        val c = seedYear().classes.first().copy(
            books = listOf(Book("b", "Book", 1, 20)),
            homework = listOf(Homework("h", "ga", "b", 3, 5, date)),
            events = listOf(
                TestEvent("all", "Everyone", date, ALL_GROUPS, true),
                TestEvent("one", "Only B", date.plusDays(1), "gb", false),
                TestEvent("orphan", "Missing", date, "missing", true),
            ),
        )
        val out = migrateAssignments(c)
        val a = out.groups.first { it.id == "ga" }.assignments
        val b = out.groups.first { it.id == "gb" }.assignments
        assertEquals(listOf(AssignmentType.HOMEWORK, AssignmentType.TEST), a.map { it.type })
        assertEquals(mapOf("b" to listOf(3, 4, 5)), a[0].pages)
        assertEquals(date, a[0].setOn)
        assertNull(a[0].due)
        assertFalse(a[0].takesLesson)
        assertEquals("Everyone", a[1].title)
        assertEquals(date, a[1].due)
        assertTrue(a[1].takesLesson)
        assertEquals(listOf("Everyone", "Only B"), b.map { it.title })
        assertFalse(b.last().takesLesson)
        assertTrue(out.homework.isEmpty())
        assertTrue(out.events.isEmpty())
        assertEquals(out, migrateAssignments(out))
    }

    @Test fun migrationPreservesExistingWorkAndRepairsCollidingIds() {
        val y = seedYear()
        val c = y.classes.first()
        val existing = Assignment("same", AssignmentType.WORKSHEET, "Existing")
        val group = c.groups.first().copy(assignments = listOf(existing))
        val out = migrateAssignments(c.copy(groups = listOf(group), events = listOf(
            TestEvent("same", "Test", date, ALL_GROUPS, true),
        )))
        val assignments = out.groups.single().assignments
        assertEquals(existing, assignments.first())
        assertEquals(2, assignments.map { it.id }.toSet().size)
    }

    @Test fun migrationBoundsHugeRangesAndNormalisesReversedRanges() {
        val c = seedYear().classes.first()
        val out = migrateAssignments(c.copy(homework = listOf(
            Homework("huge", "ga", "b", MAX_PAGE - 1, Int.MAX_VALUE, null),
            Homework("reversed", "ga", "b", 5, 3, null),
        )))
        val work = out.groups.first().assignments
        assertEquals(mapOf("b" to listOf(MAX_PAGE - 1, MAX_PAGE)), work[0].pages)
        assertEquals(mapOf("b" to listOf(3, 4, 5)), work[1].pages)
    }

    @Test fun assignmentSanitisingKeepsKnownBooksAndClampsPages() {
        val y = seedYear()
        val c = y.classes.first()
        val a = Assignment("a", AssignmentType.HOMEWORK, "Read", pages = mapOf(
            "b" to listOf(-1, 2, 2, Int.MAX_VALUE), "missing" to listOf(1),
        ))
        val input = y.copy(classes = listOf(c.copy(books = listOf(Book("b", "Book", 1, 20)),
            groups = listOf(c.groups.first().copy(assignments = listOf(a, a))))))
        val out = sanitiseYears(listOf(input)).single().classes.single().groups.single().assignments
        assertEquals(mapOf("b" to listOf(0, 2, MAX_PAGE)), out.first().pages)
        assertEquals(2, out.map { it.id }.toSet().size)
        assertEquals(out.first().copy(id = out.last().id), out.last())
        val clean = sanitiseYears(listOf(input))
        assertEquals(clean, sanitiseYears(clean))
    }
}
