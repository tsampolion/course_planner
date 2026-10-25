package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.SchoolClass
import gr.edusuite.pacer.model.SchoolYear
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavModelTest {
    // March 2027 has five Mondays, so a group on Mondays has five lessons.
    private fun group(id: String, name: String, custom: CustomPlan? = null) = Group(id, name, setOf(MONDAY), null, custom = custom)

    private fun schoolClass(id: String, name: String, pace: Double, groups: List<Group>, books: List<Book> = listOf(Book("b$id", "Workbook", 1, 20))) =
        SchoolClass(id, name, 60, pace, groups, books, emptyList(), emptyList())

    private fun year(vararg classes: SchoolClass) =
        SchoolYear("y", "2026–27", LocalDate.parse("2027-03-01"), LocalDate.parse("2027-03-31"), emptyList(), classes.toList())

    private fun report(c: SchoolClass): GroupReport = year(c).let { y -> GroupReport(y.planFor(c), c.groups.first()) }

    @Test fun wholeLessonsRoundUpAndShowAtLeastOne() {
        assertEquals(4, wholeLessons(3.33))
        assertEquals(1, wholeLessons(0.3))
        assertEquals(3, wholeLessons(3.0))
        assertEquals(3, wholeLessons(2.01))
        // Floating-point dust doesn't add a lesson.
        assertEquals(3, wholeLessons(3.0000000000004))
        assertEquals(0, wholeLessons(0.0))
        assertEquals(0, wholeLessons(-1.0))
    }

    @Test fun statusComesFromTheVerdict() {
        // 20 pages over 5 lessons: at 2 a lesson that's 10 lessons, 5 short; at 3, 6.67 lessons, 2 short; at 4, on pace.
        assertEquals(5, report(schoolClass("a", "B1", 2.0, listOf(group("g", "Mon")))).lessonsShort())
        assertEquals(2, report(schoolClass("a", "B1", 3.0, listOf(group("g", "Mon")))).lessonsShort())
        assertEquals(0, report(schoolClass("a", "B1", 4.0, listOf(group("g", "Mon")))).lessonsShort())
        // No pace, nothing to measure.
        assertNull(report(schoolClass("a", "B1", 0.0, listOf(group("g", "Mon")))).lessonsShort())
    }

    @Test fun yearStatusCountsGroupsBehind() {
        val y = year(
            schoolClass("a", "B1", 2.0, listOf(group("a1", "Mon"), group("a2", "Mon too"))),
            schoolClass("b", "C2", 4.0, listOf(group("b1", "Mon"))),
            schoolClass("c", "FCE", 0.0, listOf(group("c1", "Mon"))),
        )
        assertEquals(mapOf("a1" to 5, "a2" to 5, "b1" to 0, "c1" to null), yearStatus(y))
        assertEquals(2, groupsBehind(y))
    }

    @Test fun searchFindsGroupsByClassAndGroupName() {
        val y = year(
            schoolClass("a", "B1 Pre-Lower", 2.0, listOf(group("a1", "Mon–Wed"), group("a2", "Tue–Thu"))),
            schoolClass("b", "Ελληνικά", 2.0, listOf(Group("b1", "Δευτέρα", setOf(WEDNESDAY), null))),
        )
        val all = listOf(GroupHit("a", "B1 Pre-Lower", "a1", "Mon–Wed"), GroupHit("a", "B1 Pre-Lower", "a2", "Tue–Thu"))
        // A class's name finds all its groups, in any case.
        assertEquals(all, searchGroups(y, "pre-lower"))
        // Words can come from the class and the group, in any order.
        assertEquals(listOf(all[1]), searchGroups(y, " tue  b1 "))
        // Accents don't matter.
        assertEquals(listOf("b1"), searchGroups(y, "ελληνικα δευτερα").map { it.groupId })
        assertEquals(emptyList<GroupHit>(), searchGroups(y, "fce"))
        assertEquals(emptyList<GroupHit>(), searchGroups(y, "   "))
    }

    @Test fun warningsListEachProblemWithItsFix() {
        // On pace with no custom plan: nothing to warn about.
        assertEquals(emptyList<GroupWarning>(), groupWarnings(report(schoolClass("a", "B1", 4.0, listOf(group("g", "Mon"))))))
        // Short by 5 lessons: 20 pages, 10 more than 2 a lesson fit.
        assertEquals(listOf(GroupWarning.Behind(5, 10)), groupWarnings(report(schoolClass("a", "B1", 2.0, listOf(group("g", "Mon"))))))
        // An empty custom plan leaves every page out, so it offers Re-pace too.
        val empty = groupWarnings(report(schoolClass("a", "B1", 4.0, listOf(group("g", "Mon", CustomPlan(true, emptyList()))))))
        assertEquals(listOf(GroupWarning.Missing(20)), empty)
    }

    @Test fun aBookLeftBehindIsAWarning() {
        // Two books, one taught to page 15 of 20 and the other not started: the second is lessons behind.
        val books = listOf(Book("sb", "Student's Book", 1, 20), Book("wb", "Workbook", 1, 20))
        val g = Group("g", "Mon", setOf(MONDAY), null, pos = mapOf("sb" to 16))
        val w = groupWarnings(report(schoolClass("a", "B1", 20.0, listOf(g), books)))
        val lag = w.filterIsInstance<GroupWarning.BookBehind>().single()
        assertEquals("wb", lag.lag.bookId)
        assertTrue(lag.lessons >= 1)
        assertEquals(wholeLessons(lag.lag.lessonsBehind), lag.lessons)
    }

    @Test fun homeListsOtherGroups() {
        val groups = listOf(group("a", "A"), group("b", "B"), group("c", "C"))
        val entries = switcherEntries(Screen.Group("b"), groups) as SwitcherEntries.Groups
        assertEquals(listOf("a", "c"), entries.others.map { it.id })
    }
    @Test fun nestedListsOtherThreePages() {
        val groups = listOf(group("a", "A"), group("b", "B"))
        val plan = switcherEntries(Screen.Group("a", GroupPage.PLAN), groups) as SwitcherEntries.Pages
        assertEquals("a", plan.groupId)
        assertEquals(listOf(GroupDestination.WORK, GroupDestination.STATS, GroupDestination.SETTINGS), plan.pages)
        val settings = switcherEntries(Screen.GroupSettings("a"), groups) as SwitcherEntries.Pages
        assertEquals(listOf(GroupDestination.PLAN, GroupDestination.WORK, GroupDestination.STATS), settings.pages)
    }
    @Test fun singleGroupHasNoOtherGroups() {
        val entries = switcherEntries(Screen.Group("a"), listOf(group("a", "A"))) as SwitcherEntries.Groups
        assertTrue(entries.others.isEmpty())
    }
}
