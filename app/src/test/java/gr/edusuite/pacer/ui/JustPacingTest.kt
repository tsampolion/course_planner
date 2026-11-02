package gr.edusuite.pacer.ui

import gr.edusuite.pacer.R
import gr.edusuite.pacer.model.Brick
import gr.edusuite.pacer.model.Features
import gr.edusuite.pacer.model.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JustPacingTest {
    private val none = Features(emptySet())

    @Test fun theGroupMenuIsAbsentWhenOnlyTipsWouldBeInIt() {
        val items = groupMenuItems(true, false, 1, false, features = none)
        assertEquals(listOf(GroupMenuItem.TIPS), items)
        assertFalse(groupMenuShown(items))
    }

    @Test fun aWarningFixKeepsTheGroupMenu() {
        assertTrue(groupMenuShown(groupMenuItems(true, false, 1, false, catchUp = true, features = none)))
    }

    @Test fun aToolThatAddsToTheMenuBringsItBack() {
        listOf(Module.OUTPUT, Module.PLANNING).forEach { m ->
            assertTrue(m.name, groupMenuShown(groupMenuItems(true, false, 1, false, features = Features(setOf(m)))))
        }
    }

    @Test fun everyToolsOnShowsTheMenuAsBefore() {
        assertTrue(groupMenuShown(groupMenuItems(true, false, 1, false, features = Features.ALL)))
    }

    @Test fun onWorkTheMenuIsAbsentWhenOnlyTipsWouldBeOnThatPage() {
        val items = groupMenuItems(true, false, 1, false, catchUp = true, features = Features(setOf(Module.WORK)))
        assertTrue(groupMenuShown(items, GroupPage.PLAN))
        assertFalse(groupMenuShown(items, GroupPage.WORK))
    }

    @Test fun dueSoonFollowsWorkOrSavedWork() {
        assertFalse(dueSoonShown(none))
        assertTrue(dueSoonShown(Features(setOf(Module.WORK))))
        assertTrue(dueSoonShown(Features(emptySet(), inUse = setOf(Brick.TESTS))))
    }

    @Test fun printRowsFollowOutput() {
        assertFalse(printShown(none))
        assertTrue(printShown(Features(setOf(Module.OUTPUT))))
    }

    @Test fun paceNeededOpensOwnPaceOnlyWithPlanningTools() {
        assertFalse(ownPaceShown(none))
        assertTrue(ownPaceShown(Features(setOf(Module.PLANNING))))
    }

    @Test fun aFixOnlyPlanHasNoDragHandlesWithoutPlanningTools() {
        assertFalse(dragShown(PlanBarMode.CATCH_UP_ONLY))
        assertFalse(dragShown(PlanBarMode.NONE))
        assertTrue(dragShown(PlanBarMode.EDITOR))
    }

    @Test fun hintsNeverNameAToolThatIsOff() {
        assertEquals(R.string.hint_lesson_menu_basic, lessonMenuHint(none))
        assertEquals(R.string.hint_lesson_menu, lessonMenuHint(Features(setOf(Module.PLANNING))))
        assertEquals(R.string.group_settings_books_hint_basic, groupBooksHint(none))
        assertEquals(R.string.group_settings_books_hint, groupBooksHint(Features(setOf(Module.BOOK_TOOLS))))
        assertEquals(R.string.warn_infeasible_basic, infeasibleWarning(none))
        assertEquals(R.string.warn_infeasible, infeasibleWarning(Features(setOf(Module.WORK))))
    }

    @Test fun tipsTeachThePlanEditorOnlyWhereItShows() {
        assertFalse(tipsShowPlanEditor(PlanBarMode.CATCH_UP_ONLY))
        assertTrue(tipsShowPlanEditor(PlanBarMode.EDITOR))
    }
}
