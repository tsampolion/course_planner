package gr.edusuite.pacer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetTest {
    private fun features(p: Preset) = Features(p.modules)

    @Test fun justPacingIsTheCoreOnly() {
        val f = features(Preset.JUST_PACING)
        assertEquals(emptySet<Module>(), Preset.JUST_PACING.modules)
        assertTrue(f.has(Brick.PACING) && f.has(Brick.CATCH_UP))
        assertFalse(f.has(Brick.HOMEWORK) || f.has(Brick.TESTS) || f.has(Brick.PLAN_EDITOR) || f.has(Brick.PRINT))
    }

    @Test fun pacingAndWorkAddsWorkAndNothingElse() {
        val f = features(Preset.PACING_AND_WORK)
        assertTrue(f.has(Brick.HOMEWORK) && f.has(Brick.TESTS))
        assertFalse(f.has(Brick.PLAN_EDITOR) || f.has(Brick.PRINT) || f.has(Brick.LEAD_BOOK))
        // Unit revisions need Units, which belong to Book tools, so they stay hidden here.
        assertFalse(f.has(Brick.UNIT_REVISIONS))
    }

    @Test fun everythingHasEveryBrick() {
        assertTrue(Brick.entries.all { features(Preset.EVERYTHING).has(it) })
    }

    @Test fun aSetThatMatchesAPresetIsNamedByIt() {
        assertEquals(Preset.JUST_PACING, Preset.of(emptySet()))
        assertEquals(Preset.PACING_AND_WORK, Preset.of(setOf(Module.WORK)))
        assertEquals(Preset.EVERYTHING, Preset.of(Module.entries.toSet()))
        assertEquals(null, Preset.of(setOf(Module.OUTPUT)))
    }

    @Test fun theChecklistOrderIsWorkBookToolsPlanningOutput() {
        assertEquals(listOf(Module.WORK, Module.BOOK_TOOLS, Module.PLANNING, Module.OUTPUT), Module.entries)
    }
}
