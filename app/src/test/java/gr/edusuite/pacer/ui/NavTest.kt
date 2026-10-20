package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.SchoolClass
import gr.edusuite.pacer.model.SchoolYear
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavTest {
    private val home = listOf(Screen.Home)
    private val classStack = listOf(Screen.Home, Screen.Classes, Screen.Class)

    @Test fun savedViewMapsToStack() {
        assertEquals(home, Nav.fromView(AppData.VIEW_SETUP))
        assertEquals(home, Nav.fromView(""))
        assertEquals(classStack + Screen.Group("ga"), Nav.fromView("ga"))
    }

    @Test fun stackMapsToSavedView() {
        assertEquals(AppData.VIEW_SETUP, Nav.viewOf(home))
        assertEquals(AppData.VIEW_SETUP, Nav.viewOf(classStack))
        assertEquals("ga", Nav.viewOf(listOf(Screen.Home, Screen.Overview, Screen.Group("ga"))))
        // The round trip keeps an open group.
        assertEquals("ga", Nav.viewOf(Nav.fromView("ga")))
    }

    @Test fun pushAndPop() {
        // Home's tiles open the year's screens straight from Home.
        val year = Nav.push(home, Screen.Overview)
        assertEquals(listOf(Screen.Home, Screen.Overview), year)
        assertEquals(year, Nav.push(year, Screen.Overview))
        assertEquals(listOf(Screen.Home, Screen.YearSettings), Nav.push(home, Screen.YearSettings))
        assertEquals(home, Nav.pop(year))
        assertEquals(home, Nav.pop(home))
        assertEquals(home, Nav.push(classStack, Screen.Home))
    }

    @Test fun switchingGroupsSwapsInsteadOfStacking() {
        val a = Nav.push(classStack, Screen.Group("a"))
        assertEquals(classStack + Screen.Group("b"), Nav.push(a, Screen.Group("b")))
    }

    @Test fun screensKnowWhatTheyNeed() {
        assertFalse(Screen.Home.needsYear)
        assertFalse(Screen.Settings.needsYear)
        assertTrue(Screen.Overview.needsYear)
        assertFalse(Screen.Classes.needsClass)
        assertTrue(Screen.Tests.needsClass)
        assertTrue(Screen.Group("g").needsYear && Screen.Group("g").needsClass)
    }

    @Test fun pruneDropsWhatNoLongerExists() {
        val stack = classStack + Screen.Group("gone")
        assertEquals(classStack, Nav.prune(stack, hasYear = true, hasClass = true, groupIds = setOf("ga")))
        assertEquals(stack, Nav.prune(stack, hasYear = true, hasClass = true, groupIds = setOf("gone")))
        // A deleted class closes back to Classes; a deleted year closes back to Home.
        assertEquals(listOf(Screen.Home, Screen.Classes), Nav.prune(stack, true, false, setOf("gone")))
        assertEquals(home, Nav.prune(stack, false, false, emptySet()))
        assertEquals(listOf(Screen.Home, Screen.Settings), Nav.prune(listOf(Screen.Settings), false, false, emptySet()))
        assertEquals(home, Nav.prune(emptyList(), false, false, emptySet()))
    }

    @Test fun switchingClassOrYearClosesItsScreens() {
        val tests = classStack + Screen.Tests
        assertEquals(listOf(Screen.Home, Screen.Classes), Nav.afterClassSwitch(tests))
        assertEquals(listOf(Screen.Home, Screen.Overview), Nav.afterClassSwitch(listOf(Screen.Home, Screen.Overview, Screen.Group("g"))))
        assertEquals(home, Nav.afterYearSwitch(tests))
    }

    private fun plan(
        start: LocalDate? = LocalDate.parse("2027-03-01"),
        end: LocalDate? = LocalDate.parse("2027-03-31"),
        groups: List<Group> = listOf(Group("g", "Group A", setOf(MONDAY), null)),
        books: List<Book> = listOf(Book("b", "Workbook", 1, 20)),
    ) = Plan("p", "Test", start, end, 60, 2.0, groups, emptyList(), books, emptyList(), emptyList())

    @Test fun problemsLinkToTheScreenThatFixesThem() {
        assertEquals(emptyList<Problem>(), Nav.problems(plan()))
        assertEquals(listOf(Screen.YearSettings), Nav.problems(plan(end = null)).map { it.fix })
        assertEquals(listOf(Screen.YearSettings), Nav.problems(plan(end = LocalDate.parse("2027-02-01"))).map { it.fix })
        assertEquals(listOf(Screen.Class), Nav.problems(plan(groups = emptyList())).map { it.fix })
        assertEquals(listOf(Screen.Class), Nav.problems(plan(books = emptyList())).map { it.fix })
        assertEquals(
            listOf(Screen.Class),
            Nav.problems(plan(groups = listOf(Group("g", "Group A", emptySet(), null)))).map { it.fix },
        )
        assertEquals(listOf(Screen.Class), Nav.problems(plan(books = listOf(Book("b", "Workbook", 9, 3)))).map { it.fix })
        assertEquals(
            listOf(Screen.Class),
            Nav.problems(plan(books = listOf(Book("b", "Workbook", 1, 20, due = LocalDate.parse("2027-06-01"))))).map { it.fix },
        )
        // A custom plan that leaves pages out opens its group.
        val gap = Group("g", "Group A", setOf(MONDAY), null, custom = CustomPlan(true, emptyList()))
        assertEquals(listOf(Screen.Group("g")), Nav.problems(plan(groups = listOf(gap))).map { it.fix })
    }

    private fun schoolClass(id: String, name: String, books: List<Book> = listOf(Book("b", "Workbook", 1, 20))) =
        SchoolClass(id, name, 60, 2.0, listOf(Group("g$id", "Group A", setOf(MONDAY), null)), books, emptyList(), emptyList())

    @Test fun yearProblemsListTermOnceAndNameTheirClass() {
        val year = SchoolYear(
            "y", "2026–27", null, null, emptyList(),
            listOf(schoolClass("a", "B1"), schoolClass("b", "C2", books = emptyList())),
        )
        assertEquals(
            listOf(
                Problem("Set both a term start and a term end.", Screen.YearSettings),
                Problem("C2: Add at least one book.", Screen.Class, "b"),
            ),
            Nav.problems(year),
        )
        val empty = year.copy(start = LocalDate.parse("2026-09-01"), end = LocalDate.parse("2027-06-01"), classes = emptyList())
        assertEquals(listOf(Problem("Add at least one class.", Screen.Classes)), Nav.problems(empty))
    }
}
