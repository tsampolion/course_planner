package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.model.Brick
import gr.edusuite.pacer.model.Features
import gr.edusuite.pacer.model.Module
import gr.edusuite.pacer.model.TEST_TODAY
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

    private fun report(c: SchoolClass): GroupReport = year(c).let { y -> GroupReport(y.planFor(c, TEST_TODAY), c.groups.first()) }

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
        assertEquals(mapOf("a1" to 5, "a2" to 5, "b1" to 0, "c1" to null), yearStatus(y, TEST_TODAY))
        assertEquals(2, groupsBehind(y, TEST_TODAY))
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

    // ---- the title switcher: one sheet for every group page -------------------------------

    @Test fun switcherListsThePlacesInFixedOrderWithTheCurrentOneMarked() {
        val order = listOf(GroupPlace.HOME, GroupPlace.PLAN, GroupPlace.WORK, GroupPlace.STATS, GroupPlace.SETTINGS)
        val cases = mapOf(
            Screen.Group("a") to GroupPlace.HOME,
            Screen.Group("a", GroupPage.PLAN) to GroupPlace.PLAN,
            Screen.Group("a", GroupPage.WORK) to GroupPlace.WORK,
            Screen.Group("a", GroupPage.STATS) to GroupPlace.STATS,
            Screen.GroupSettings("a") to GroupPlace.SETTINGS,
        )
        for ((screen, here) in cases) {
            val pages = switcherPages(screen)
            assertEquals("$screen", order, pages.map { it.place })
            assertEquals("$screen", listOf(here), pages.filter { it.current }.map { it.place })
        }
    }

    @Test fun onATabletGroupHomeCountsAsPlanAndIsNotListedTwice() {
        val tablet = switcherPages(Screen.Group("a"), plansStandIn = true)
        assertEquals(GroupPlace.PLAN, tablet.single { it.current }.place)
        assertEquals(listOf(GroupPlace.PLAN, GroupPlace.WORK, GroupPlace.STATS, GroupPlace.SETTINGS), tablet.map { it.place })
        assertEquals(GroupPlace.HOME, switcherPages(Screen.Group("a"), plansStandIn = false).single { it.current }.place)
    }

    // ---- Work follows its bricks everywhere, like group home's pills ---------------------

    private val justPacing = Features(emptySet())
    private val workOn = Features(setOf(Module.WORK))
    private val withoutWork = listOf(GroupPlace.HOME, GroupPlace.PLAN, GroupPlace.STATS, GroupPlace.SETTINGS)

    @Test fun atJustPacingNeitherTheSwitcherNorTheMenuListsWork() {
        assertEquals(withoutWork, switcherPages(Screen.Group("ga"), features = justPacing).map { it.place })
        val menu = menuRows(twoClasses, MenuLevel.Group("c1", "ga"), Screen.Group("ga"), features = justPacing)
        assertEquals(withoutWork, menu.filterIsInstance<MenuRow.PlaceItem>().map { it.place })
        assertEquals(withoutWork.drop(1), groupPlaces(justPacing, plansStandIn = true))
    }

    @Test fun withWorkOnBothListIt() {
        assertEquals(GroupPlace.entries.toList(), switcherPages(Screen.Group("ga"), features = workOn).map { it.place })
        assertEquals(GroupPlace.entries.toList(), menuRows(twoClasses, MenuLevel.Group("c1", "ga"), Screen.Group("ga"), features = workOn).filterIsInstance<MenuRow.PlaceItem>().map { it.place })
    }

    @Test fun withWorkOffButAnAssignmentSavedWorkShows() {
        val saved = Features(emptySet(), setOf(Brick.HOMEWORK))
        assertTrue(GroupPlace.WORK in groupPlaces(saved))
        assertTrue(GroupPlace.WORK in switcherPages(Screen.Group("ga"), features = saved).map { it.place })
    }

    @Test fun aWorkScreenWhoseThePlaceIsGoneFallsBackToGroupHome() {
        assertEquals(Screen.Group("ga"), Screen.Group("ga", GroupPage.WORK).withoutGonePlace(justPacing))
        assertEquals(Screen.Group("ga", GroupPage.WORK), Screen.Group("ga", GroupPage.WORK).withoutGonePlace(workOn))
        assertEquals(Screen.Group("ga", GroupPage.STATS), Screen.Group("ga", GroupPage.STATS).withoutGonePlace(justPacing))
        assertEquals(Screen.Class, Screen.Class.withoutGonePlace(justPacing))
    }

    @Test fun switcherOffersTheOpenClassesOtherGroupsAfterThePlaces() {
        val groups = listOf(group("a", "A"), group("b", "B"), group("c", "C"))
        assertEquals(listOf("a", "c"), switcherGroups(groups, "b").map { it.id })
        assertTrue(switcherGroups(listOf(group("a", "A")), "a").isEmpty())
    }

    @Test fun everyPlaceOpensItsOwnScreen() {
        assertEquals(Screen.Group("g"), GroupPlace.HOME.screen("g"))
        assertEquals(Screen.Group("g", GroupPage.PLAN), GroupPlace.PLAN.screen("g"))
        assertEquals(Screen.Group("g", GroupPage.WORK), GroupPlace.WORK.screen("g"))
        assertEquals(Screen.Group("g", GroupPage.STATS), GroupPlace.STATS.screen("g"))
        assertEquals(Screen.GroupSettings("g"), GroupPlace.SETTINGS.screen("g"))
    }

    // ---- the ☰ menu: Classes & Groups opens where the teacher is --------------------------

    private val fce = schoolClass("c1", "FCE", 4.0, listOf(group("ga", "Group A"), group("gb", "Group B")))
    private val b1 = schoolClass("c2", "B1", 4.0, listOf(group("gc", "Group C")))
    private val twoClasses = year(fce, b1)

    @Test fun theMenuOpensAtTheScreenTheTeacherIsOn() {
        assertEquals(MenuLevel.Group("c1", "ga"), menuLevelFor(Screen.Group("ga", GroupPage.PLAN), "c1"))
        assertEquals(MenuLevel.Group("c1", "ga"), menuLevelFor(Screen.GroupSettings("ga"), "c1"))
        assertEquals(MenuLevel.Class("c1"), menuLevelFor(Screen.Class, "c1"))
        assertEquals(MenuLevel.Class("c1"), menuLevelFor(Screen.Units, "c1"))
        assertEquals(MenuLevel.Classes, menuLevelFor(Screen.Classes, ""))
        for (s in listOf(Screen.Home, Screen.Overview, Screen.YearSettings, Screen.Settings)) assertNull("$s", menuLevelFor(s, "c1"))
    }

    @Test fun theClassesLevelListsEveryClassThenAllClasses() {
        val rows = menuRows(twoClasses, MenuLevel.Classes, Screen.Classes)
        assertEquals(
            listOf(MenuRow.ClassItem("c1", "FCE", 2), MenuRow.ClassItem("c2", "B1", 1), MenuRow.AllClasses(2, current = true)),
            rows,
        )
        assertTrue(menuRows(year(), MenuLevel.Classes, Screen.Classes).isEmpty())
    }

    @Test fun aClassLevelHidesTheOtherClassesAndListsItsSettingsAndGroups() {
        val rows = menuRows(twoClasses, MenuLevel.Class("c1"), Screen.Class)
        assertEquals(
            listOf(
                MenuRow.Up(MenuLevel.Classes, null), MenuRow.Heading("FCE", isClass = true, groups = 2), MenuRow.ClassSettings("c1", current = true),
                MenuRow.GroupItem("c1", "ga", "Group A", false), MenuRow.GroupItem("c1", "gb", "Group B", false),
            ),
            rows,
        )
        assertTrue(rows.none { it is MenuRow.ClassItem })
    }

    @Test fun aGroupLevelListsItsFivePlacesAndMarksTheOpenOne() {
        val rows = menuRows(twoClasses, MenuLevel.Group("c1", "ga"), Screen.Group("ga", GroupPage.STATS))
        assertEquals(MenuRow.Up(MenuLevel.Class("c1"), "FCE"), rows[0])
        assertEquals(MenuRow.Heading("Group A", isClass = false), rows[1])
        val places = rows.filterIsInstance<MenuRow.PlaceItem>()
        assertEquals(GroupPlace.entries.toList(), places.map { it.place })
        assertEquals(listOf(GroupPlace.STATS), places.filter { it.current }.map { it.place })
        // Another group's page is not "here" for this group.
        assertTrue(menuRows(twoClasses, MenuLevel.Group("c1", "gb"), Screen.Group("ga", GroupPage.STATS)).filterIsInstance<MenuRow.PlaceItem>().none { it.current })
    }

    @Test fun theWayUpFromEachLevelGoesOneStepUp() {
        val group = menuRows(twoClasses, MenuLevel.Group("c1", "ga"), Screen.Group("ga")).first() as MenuRow.Up
        assertEquals(MenuLevel.Class("c1"), group.to)
        assertEquals("FCE", group.name)
        val cls = menuRows(twoClasses, MenuLevel.Class("c1"), Screen.Class).first() as MenuRow.Up
        assertEquals(MenuLevel.Classes, cls.to)
        assertNull(cls.name)
    }

    @Test fun aDeletedClassOrGroupFallsBackToTheLevelAbove() {
        assertEquals(menuRows(twoClasses, MenuLevel.Classes, Screen.Home), menuRows(twoClasses, MenuLevel.Class("gone"), Screen.Home))
        assertEquals(menuRows(twoClasses, MenuLevel.Class("c1"), Screen.Home), menuRows(twoClasses, MenuLevel.Group("c1", "gone"), Screen.Home))
        assertEquals(menuRows(twoClasses, MenuLevel.Classes, Screen.Home), menuRows(twoClasses, MenuLevel.Group("gone", "ga"), Screen.Home))
    }

    @Test fun onATabletTheGroupLevelMarksPlanForGroupHome() {
        val places = menuRows(twoClasses, MenuLevel.Group("c1", "ga"), Screen.Group("ga"), plansStandIn = true).filterIsInstance<MenuRow.PlaceItem>()
        assertEquals(listOf(GroupPlace.PLAN), places.filter { it.current }.map { it.place })
        // Group home is shown as Plan there, so it is not offered as a second row.
        assertTrue(places.none { it.place == GroupPlace.HOME })
    }

    @Test fun theListFollowsTheTeacherInsideItAndStaysPutOutsideIt() {
        val open = MenuState(MenuLevel.Class("c1"), expanded = true)
        // Moving to another group opens that group's places.
        assertEquals(MenuState(MenuLevel.Group("c1", "gb"), true), open.followScreen(MenuLevel.Group("c1", "gb")))
        // Opening Pacing overview or Settings leaves a pinned menu as it was, open or folded.
        assertEquals(open, open.followScreen(null))
        assertEquals(MenuState(MenuLevel.Classes, false), MenuState(MenuLevel.Classes, false).followScreen(null))
    }

    @Test fun eachTimeTheMenuOpensItStartsAtThePlaceOrFolded() {
        assertEquals(MenuState(MenuLevel.Group("c1", "ga"), true), menuStateOnOpen(MenuLevel.Group("c1", "ga")))
        assertEquals(MenuState(MenuLevel.Classes, false), menuStateOnOpen(null))
    }

    @Test fun longYearNamesAreShortenedForTheMenu() {
        assertEquals("2026–27", yearShort("2026–2027"))
        assertEquals("2026–27", yearShort("2026-2027"))
        assertEquals("2026–27", yearShort("2026–27"))
        assertEquals("Year 1", yearShort(" Year 1 "))
        assertEquals("2026–27 Aut…", yearShort("2026–2027 Autumn term"))
        assertEquals("A very long…", yearShort("A very long year name"))
    }

    // ---- the top bar keeps the title's room ------------------------------------------------

    @Test fun aShortTitleKeepsAllTheButtonsAndALongOneFoldsThem() {
        // 360dp phone, ☰ plus warnings, Undo and ⋮.
        assertTrue(!foldBarActions(360f, 1f, "Group A".length, 3, hasMenu = true, hasSwitcher = true))
        assertTrue(foldBarActions(360f, 1f, "Advanced Conversation Thursday".length, 3, hasMenu = true, hasSwitcher = true))
    }

    @Test fun largeTextFoldsTitlesThatFitAtTheNormalSize() {
        val title = "Group A".length
        assertTrue(!foldBarActions(360f, 1f, title, 3, hasMenu = true, hasSwitcher = true))
        assertTrue(foldBarActions(360f, 2f, title, 3, hasMenu = true, hasSwitcher = true))
    }

    @Test fun largeTextGivesALongTitleASecondLineAfterTheButtonsFold() {
        val title = "Advanced Conversation Thursday".length
        // 360dp at 200%: even with only ⋮ left the title doesn't fit on one line.
        assertEquals(2, titleLines(360f, 2f, title, 1, hasMenu = true, hasSwitcher = true))
        // At the normal size the same title is cut, never wrapped.
        assertEquals(1, titleLines(360f, 1f, title, 1, hasMenu = true, hasSwitcher = true))
        // A title that fits keeps one line at any size.
        assertEquals(1, titleLines(360f, 2f, "A".length, 1, hasMenu = true, hasSwitcher = true))
    }

    @Test fun aWideBarNeverFolds() {
        assertTrue(!foldBarActions(900f, 1f, "Advanced Conversation Thursday".length, 3, hasMenu = false, hasSwitcher = true))
    }
}
