package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Brick
import gr.edusuite.pacer.model.Features
import gr.edusuite.pacer.model.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturesSheetModelTest {
    @Test fun everyModuleHasARowInOrder() {
        assertEquals(Module.entries.toList(), moduleRows(Features(emptySet())).map { it.module })
    }

    @Test fun rowsThatAreOnReadOnAndOthersOfferTurnOn() {
        val rows = moduleRows(Features(setOf(Module.WORK))).associate { it.module to it.on }
        assertTrue(rows.getValue(Module.WORK))
        assertFalse(rows.getValue(Module.PLANNING))
    }

    @Test fun savedDataDoesNotCountAsTheModuleBeingOn() {
        val rows = moduleRows(Features(emptySet(), inUse = setOf(Brick.HOMEWORK))).associate { it.module to it.on }
        assertFalse(rows.getValue(Module.WORK))
    }

    @Test fun theSummaryCountsTheToolsThatAreOn() {
        assertEquals(1, onCount(Features(setOf(Module.WORK))))
        assertEquals(4, onCount(Features.ALL))
        assertEquals(0, onCount(Features(emptySet())))
    }

    @Test fun onlyMenuModulesPromiseAMenu() {
        assertEquals(setOf(Module.PLANNING, Module.OUTPUT), Module.entries.filter { it.addsToGroupMenu() }.toSet())
    }
}
