package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.*
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class GroupOverridesTest {
    private fun d(v: String) = LocalDate.parse(v)
    private val g = Group("g", "Group", setOf(MONDAY), null)
    private val b = Book("b", "Book", 1, 20)
    private val plan = Plan("p", "Plan", d("2027-03-01"), d("2027-03-31"), 60, 4.0, listOf(g), emptyList(), listOf(b), emptyList(), emptyList())

    @Test fun groupEndStopsLessonsAndCannotExtendTerm() {
        assertEquals(listOf(d("2027-03-01"), d("2027-03-08")), plan.lessonsFor(g.copy(end = d("2027-03-10"))))
        assertEquals(plan.lessonsFor(g), plan.lessonsFor(g.copy(end = d("2027-12-31"))))
    }

    @Test fun groupSkipAddsToBookSkip() {
        val book = b.copy(skip = "2-3")
        assertEquals(listOf(1) + (6..20).toList(), plan.remainingPages(g.copy(skip = mapOf("b" to "3-5")), book))
        assertEquals(listOf(1) + (4..20).toList(), plan.remainingPages(g, book))
    }

    @Test fun extraBeyondGroupEndReturnsPinnedPagesToPool() {
        val group = g.copy(end = d("2027-03-10"), extras = listOf(ExtraLesson("e", d("2027-03-29"), mapOf("b" to listOf(20)))))
        assertTrue(plan.activeExtras(group).isEmpty())
        assertTrue(20 in plan.remainingPages(group, b))
    }

    @Test fun optionalBooksOnlyAffectSelectedGroupsAndKeepRowIndicesAligned() {
        val optional = Book("z", "Optional", 1, 5, groupOnly = true)
        val p = plan.copy(books = listOf(optional, b), lead = optional.id)
        val normal = GroupReport(p, g)
        assertEquals(listOf("b"), normal.plan.books.map { it.id })
        assertNull(normal.plan.lead)
        assertTrue(normal.rows.all { it.pages.size == 1 })
        assertEquals(20, normal.pages)
        assertFalse(normal.asTsv().lineSequence().first().contains("Optional"))
        assertTrue(p.remainingPages(g, optional).isEmpty())
        val selected = g.copy(extraBooks = setOf(optional.id))
        val report = GroupReport(p, selected)
        val baseline = GroupReport(p.copy(books = listOf(optional.copy(groupOnly = false), b)), g)
        assertEquals(25, report.pages)
        assertEquals(baseline.rows, report.rows)
        assertTrue(report.asTsv().lineSequence().first().contains("Optional"))
    }

    @Test fun copyingPlanDropsOptionalSupplementaryPagesForUnselectedTarget() {
        val optional = Book("s", "Reader", 1, 5, supplementary = true, include = "1-5", groupOnly = true)
        val p = plan.copy(books = listOf(b, optional))
        val custom = CustomPlan(true, listOf(PlannedLesson("l", mapOf("b" to listOf(1), "s" to listOf(2)))))
        assertEquals(mapOf("b" to listOf(1)), p.copyCustomPlan(custom, g).lessons.single().pages)
    }

    @Test fun noGroupOverridesChangesNothing() {
        val report = GroupReport(plan, g)
        val absent = GroupReport(plan, g.copy(end = null, skip = emptyMap(), extraBooks = emptySet()))
        assertEquals(report.rows, absent.rows)
        assertEquals(report.verdict(), absent.verdict())
        assertEquals(report.asTsv(), absent.asTsv())
    }

    // ---- removed books and a group's own finish date -------------------------------------------------

    private val b2 = Book("b2", "Workbook", 1, 12)
    private val two = plan.copy(books = listOf(b, b2))

    @Test fun removedBookLeavesPlan() {
        val removed = g.copy(removedBooks = setOf("b2"))
        val report = GroupReport(two, removed)
        assertEquals(listOf("b"), report.plan.books.map { it.id })
        assertTrue(report.rows.all { it.pages.size == 1 })
        assertTrue(two.remainingPages(removed, b2).isEmpty())
        assertEquals(0, two.bankedPages(removed, b2))
        // The other group (and the same group before) still has it.
        assertEquals(listOf("b", "b2"), GroupReport(two, g).plan.books.map { it.id })
        // What is left is paced exactly as a class that never had the book.
        assertEquals(GroupReport(plan, g).rows, report.rows)
    }

    @Test fun removedBookKeepsDoneHistory() {
        val taught = DoneLesson(d("2027-03-01"), mapOf("b" to listOf(1, 2), "b2" to listOf(1)))
        val removed = g.copy(done = listOf(taught), pos = mapOf("b2" to 2), removedBooks = setOf("b2"))
        val report = GroupReport(two, removed)
        assertEquals(listOf(taught), report.group.done)
        assertEquals(mapOf("b2" to 2), report.group.pos)
        // The lesson already taught is still not a lesson to teach, and its pages in the kept book stay taught.
        assertFalse(d("2027-03-01") in report.lessons)
        assertEquals((3..20).toList(), two.remainingPages(removed, b))
        // Added back, the book resumes where the group was.
        val back = removed.copy(removedBooks = emptySet())
        assertEquals((2..12).toList(), two.remainingPages(back, b2))
    }

    @Test fun removedLeadFallsBackToNoLead() {
        val led = two.copy(lead = "b2")
        assertEquals("b2", GroupReport(led, g).plan.lead)
        assertNull(GroupReport(led, g.copy(removedBooks = setOf("b2"))).plan.lead)
        assertEquals("b", GroupReport(led.copy(lead = "b"), g.copy(removedBooks = setOf("b2"))).plan.lead)
    }

    @Test fun handPlacedPagesOfARemovedBookAreFlaggedNotRemoved() {
        val lessons = listOf(PlannedLesson("l1", mapOf("b" to listOf(1), "b2" to listOf(1, 2)), touched = true))
        val removed = g.copy(custom = CustomPlan(true, lessons), removedBooks = setOf("b2"))
        val report = GroupReport(two, removed)
        assertEquals(mapOf("b2" to listOf(1, 2)), report.coverage()!!.stale.filterKeys { it == "b2" })
        assertEquals(lessons, report.group.custom!!.lessons)
        // Unchanged for a group that still uses it.
        assertNull(GroupReport(two, g.copy(custom = CustomPlan(true, lessons))).coverage()!!.stale["b2"])
    }

    @Test fun groupBookDueOverridesClassDue() {
        val classDue = two.copy(books = listOf(b.copy(due = d("2027-03-15")), b2))
        val earlier = GroupReport(classDue, g.copy(bookDue = mapOf("b" to d("2027-03-08"))))
        assertEquals(d("2027-03-08"), earlier.plan.books.first { it.id == "b" }.due)
        assertEquals(2, earlier.bookStats[0].lessons)
        // Later than the class's date is allowed, and an untouched book keeps its own.
        val later = GroupReport(classDue, g.copy(bookDue = mapOf("b2" to d("2027-03-15"), "b" to d("2027-03-22"))))
        assertEquals(d("2027-03-22"), later.plan.books.first { it.id == "b" }.due)
        assertEquals(d("2027-03-15"), later.plan.books.first { it.id == "b2" }.due)
        // Without a class date a group can still set one.
        assertEquals(d("2027-03-08"), GroupReport(two, g.copy(bookDue = mapOf("b" to d("2027-03-08")))).plan.books.first().due)
    }

    @Test fun groupBookDueIsClampedToGroupEnd() {
        val ends = g.copy(end = d("2027-03-10"), bookDue = mapOf("b" to d("2027-06-30")))
        val report = GroupReport(two, ends)
        assertEquals(d("2027-03-10"), report.plan.books.first { it.id == "b" }.due)
        assertEquals(report.lessons.size, report.bookStats[0].lessons)
        // The year's end clamps it too.
        assertEquals(d("2027-03-31"), GroupReport(two, g.copy(bookDue = mapOf("b" to d("2028-01-01")))).plan.books.first().due)
    }

    @Test fun noOverridesAreIdentical() {
        val plain = GroupReport(two, g)
        val empty = GroupReport(two, g.copy(removedBooks = emptySet(), bookDue = emptyMap()))
        assertSame(two, two.forGroup(g))
        assertEquals(plain.rows, empty.rows)
        assertEquals(plain.asTsv(), empty.asTsv())
        assertEquals(plain.verdict(), empty.verdict())
    }
}
