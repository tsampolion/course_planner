package gr.edusuite.pacer.ui

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

    @Test fun workOffersToTurnOnWorkOnlyWhenNothingCouldBeAdded() {
        assertTrue(workOffOffer(none, hasAssignments = false))
        assertFalse(workOffOffer(Features(setOf(Module.WORK)), hasAssignments = false))
    }

    @Test fun savedWorkAlwaysShowsAndNeverShowsTheOffer() {
        assertFalse(workOffOffer(none, hasAssignments = true))
        assertFalse(workOffOffer(Features(emptySet(), inUse = setOf(Brick.HOMEWORK)), hasAssignments = false))
    }
}
