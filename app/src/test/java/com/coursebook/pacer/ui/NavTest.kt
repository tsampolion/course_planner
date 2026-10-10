package com.coursebook.pacer.ui

import com.coursebook.pacer.model.AppData
import com.coursebook.pacer.model.Book
import com.coursebook.pacer.model.Group
import com.coursebook.pacer.model.Plan
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class NavTest {
    private val home = listOf(Screen.Home)

    @Test fun savedViewMapsToStack() {
        assertEquals(home, Nav.fromView(AppData.VIEW_SETUP))
        assertEquals(home, Nav.fromView(""))
        assertEquals(listOf(Screen.Home, Screen.Schedules, Screen.Group("ga")), Nav.fromView("ga"))
    }

    @Test fun stackMapsToSavedView() {
        assertEquals(AppData.VIEW_SETUP, Nav.viewOf(home))
        assertEquals(AppData.VIEW_SETUP, Nav.viewOf(listOf(Screen.Home, Screen.Books)))
        assertEquals("ga", Nav.viewOf(listOf(Screen.Home, Screen.Group("ga"))))
        // The round trip keeps an open group.
        assertEquals("ga", Nav.viewOf(Nav.fromView("ga")))
    }

    @Test fun pushAndPop() {
        val books = Nav.push(home, Screen.Books)
        assertEquals(listOf(Screen.Home, Screen.Books), books)
        assertEquals(books, Nav.push(books, Screen.Books))
        assertEquals(home, Nav.pop(books))
        assertEquals(home, Nav.pop(home))
        assertEquals(home, Nav.push(books, Screen.Home))
    }

    @Test fun switchingGroupsSwapsInsteadOfStacking() {
        val a = Nav.push(home, Screen.Group("a"))
        assertEquals(listOf(Screen.Home, Screen.Group("b")), Nav.push(a, Screen.Group("b")))
        // From Groups, a group opens on top so back returns to Groups.
        val groups = Nav.push(home, Screen.Groups)
        assertEquals(listOf(Screen.Home, Screen.Groups, Screen.Group("a")), Nav.push(groups, Screen.Group("a")))
    }

    @Test fun pruneDropsMissingGroups() {
        val stack = listOf(Screen.Home, Screen.Groups, Screen.Group("gone"))
        assertEquals(listOf(Screen.Home, Screen.Groups), Nav.prune(stack, setOf("ga")))
        assertEquals(stack, Nav.prune(stack, setOf("gone")))
        assertEquals(home, Nav.prune(emptyList(), emptySet()))
        assertEquals(listOf(Screen.Home, Screen.Books), Nav.prune(listOf(Screen.Books), emptySet()))
    }

    @Test fun planSwitchKeepsScreensButDropsGroups() {
        assertEquals(listOf(Screen.Home, Screen.Plans), Nav.afterPlanSwitch(listOf(Screen.Home, Screen.Plans)))
        assertEquals(listOf(Screen.Home, Screen.Schedules), Nav.afterPlanSwitch(listOf(Screen.Home, Screen.Schedules, Screen.Group("ga"))))
    }

    private fun plan(
        start: LocalDate? = LocalDate.parse("2027-03-01"),
        end: LocalDate? = LocalDate.parse("2027-03-31"),
        groups: List<Group> = listOf(Group("g", "Group A", setOf(MONDAY), null)),
        books: List<Book> = listOf(Book("b", "Workbook", 1, 20)),
    ) = Plan("p", "Test", start, end, 60, 2.0, groups, emptyList(), books, emptyList(), emptyList())

    @Test fun problemsLinkToTheScreenThatFixesThem() {
        assertEquals(emptyList<Problem>(), Nav.problems(plan()))
        assertEquals(listOf(Screen.Term), Nav.problems(plan(end = null)).map { it.fix })
        assertEquals(listOf(Screen.Term), Nav.problems(plan(end = LocalDate.parse("2027-02-01"))).map { it.fix })
        assertEquals(listOf(Screen.Groups), Nav.problems(plan(groups = emptyList())).map { it.fix })
        assertEquals(listOf(Screen.Books), Nav.problems(plan(books = emptyList())).map { it.fix })
        assertEquals(
            listOf(Screen.Groups),
            Nav.problems(plan(groups = listOf(Group("g", "Group A", emptySet(), null)))).map { it.fix },
        )
        assertEquals(listOf(Screen.Books), Nav.problems(plan(books = listOf(Book("b", "Workbook", 9, 3)))).map { it.fix })
        assertEquals(
            listOf(Screen.Books),
            Nav.problems(plan(books = listOf(Book("b", "Workbook", 1, 20, due = LocalDate.parse("2027-06-01"))))).map { it.fix },
        )
    }
}
