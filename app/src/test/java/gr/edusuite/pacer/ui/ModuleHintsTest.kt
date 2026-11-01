package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Brick
import gr.edusuite.pacer.model.Features
import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.model.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModuleHintsTest {
    private val none = Features(emptySet())

    @Test fun pagesBeyondThePlanCountAsAhead() {
        assertEquals(3, pagesAhead(mapOf("b" to listOf(10, 11, 12)), mapOf("b" to listOf(10, 11, 12, 13, 14, 15))))
        assertEquals(0, pagesAhead(mapOf("b" to listOf(10, 11, 12)), mapOf("b" to listOf(10, 11))))
        assertEquals(2, pagesAhead(emptyMap(), mapOf("b" to listOf(4, 5))))
    }

    @Test fun differentPagesAreAHandEdit() {
        assertEquals(true, isHandEdit(mapOf("b" to listOf(1, 2)), mapOf("b" to listOf(1, 2, 3))))
        assertEquals(true, isHandEdit(mapOf("b" to listOf(1, 2)), mapOf("b" to listOf(2, 3))))
        assertEquals(false, isHandEdit(mapOf("b" to listOf(1, 2)), mapOf("b" to listOf(1, 2))))
    }

    @Test fun workHintAfterPagesRecordedAheadOfThePlan() {
        assertEquals(ModuleHint.WORK, dueModuleHint(none, emptySet(), aheadPages = 3, handEdits = 0))
    }

    @Test fun planningHintAfterTwoHandEdits() {
        assertNull(dueModuleHint(none, emptySet(), aheadPages = 0, handEdits = 1))
        assertEquals(ModuleHint.PLANNING, dueModuleHint(none, emptySet(), aheadPages = 0, handEdits = 2))
    }

    @Test fun workComesFirstWhenBothAreDue() {
        assertEquals(ModuleHint.WORK, dueModuleHint(none, emptySet(), aheadPages = 3, handEdits = 5))
    }

    @Test fun aSeenHintNeverComesBack() {
        assertNull(dueModuleHint(none, setOf(Hints.WORK_AHEAD), aheadPages = 3, handEdits = 0))
        assertEquals(ModuleHint.PLANNING, dueModuleHint(none, setOf(Hints.WORK_AHEAD), aheadPages = 3, handEdits = 2))
    }

    @Test fun nothingWhenTheToolIsAlreadyOn() {
        assertNull(dueModuleHint(Features(setOf(Module.WORK)), emptySet(), aheadPages = 3, handEdits = 0))
        assertNull(dueModuleHint(Features(setOf(Module.PLANNING)), emptySet(), aheadPages = 0, handEdits = 4))
    }

    @Test fun savedHomeworkMeansWorkIsAlreadyShowing() {
        assertNull(dueModuleHint(Features(emptySet(), inUse = setOf(Brick.HOMEWORK)), emptySet(), aheadPages = 3, handEdits = 0))
    }

    @Test fun eachHintTurnsOnItsOwnModule() {
        assertEquals(Module.WORK, ModuleHint.WORK.module)
        assertEquals(Module.PLANNING, ModuleHint.PLANNING.module)
    }
}
