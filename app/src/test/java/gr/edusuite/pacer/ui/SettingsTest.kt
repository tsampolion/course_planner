package gr.edusuite.pacer.ui

import gr.edusuite.pacer.update.Update
import gr.edusuite.pacer.model.PaletteChoice
import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.ThemeMode
import gr.edusuite.pacer.model.seedPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {
    @org.junit.Test fun planGroupingDefaultsAndParses() {
        org.junit.Assert.assertEquals(gr.edusuite.pacer.model.PlanGrouping.WEEKS, gr.edusuite.pacer.model.Settings().planGrouping)
        org.junit.Assert.assertEquals(gr.edusuite.pacer.model.PlanGrouping.MONTHS, gr.edusuite.pacer.model.PlanGrouping.parse("MONTHS"))
        org.junit.Assert.assertEquals(gr.edusuite.pacer.model.PlanGrouping.WEEKS, gr.edusuite.pacer.model.PlanGrouping.parse("unknown"))
        org.junit.Assert.assertEquals(gr.edusuite.pacer.model.PlanGrouping.MONTHS, gr.edusuite.pacer.model.Settings(planGrouping = gr.edusuite.pacer.model.PlanGrouping.MONTHS).sanitised().planGrouping)
    }

    @Test fun themeParsesWithFallback() {
        assertEquals(ThemeMode.DARK, ThemeMode.parse("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse("purple"))
    }

    @Test fun defaultsMatchTheSamplePlan() {
        val seed = seedPlan()
        assertEquals(seed.mins, Settings().lessonMins)
        assertEquals(seed.pace, Settings().pace, 0.0)
    }

    @Test fun paceReadsCommasAndRefusesWhatTheFileCantStore() {
        assertEquals(3.5, Settings.parsePace(" 3,5 ")!!, 0.0)
        assertEquals(1.0, Settings.parsePace("1.")!!, 0.0)
        for (bad in listOf("", "abc", "-1", "NaN", "Infinity", "1e400")) assertNull(bad, Settings.parsePace(bad))
    }

    @Test fun sanitisingRestoresUnusableValues() {
        assertEquals(Settings(), Settings(lessonMins = 0, pace = Double.NaN).sanitised())
        assertEquals(Settings(), Settings(lessonMins = 10_000, pace = -1.0).sanitised())
        val ok = Settings(ThemeMode.LIGHT, 45, 3.5)
        assertEquals(ok, ok.sanitised())
    }

    @Test fun welcomeShowsUntilSeen() {
        assertFalse(Settings().welcomeSeen)
        assertTrue(Settings(welcomeSeen = true).sanitised().welcomeSeen)
    }

    @Test fun nameIsOneTrimmedLineOfBoundedLength() {
        assertEquals("", Settings(name = "   ").sanitised().name)
        assertEquals("Anna Maria", Settings(name = "  Anna \n  Maria ").sanitised().name)
        assertEquals(Settings.MAX_NAME, Settings(name = "x".repeat(100)).sanitised().name.length)
    }

    @Test fun hintsAreSeenOnceUntilShownAgain() {
        val start = Settings()
        assertFalse(Hints.seen(start, "a"))
        val seen = Hints.markSeen(start, "a")
        assertTrue(Hints.seen(seen, "a"))
        assertFalse(Hints.seen(seen, "b"))
        assertSame(seen, Hints.markSeen(seen, "a"))
        assertTrue(Hints.seen(Hints.markSeen(seen, "b"), "a"))
        assertFalse(Hints.seen(Hints.reset(seen), "a"))
        assertEquals(start, Hints.markSeen(start, " "))
    }

    @Test fun sanitisingKeepsSeenTipsButDropsBlankIds() {
        assertEquals(setOf("a"), Settings(seenTips = setOf("a", "", " ")).sanitised().seenTips)
    }
    @Test fun paletteDefaultsToBone() { assertEquals(PaletteChoice.BONE, Settings().palette) }
    @Test fun unknownPaletteNameFallsBackToBone() {
        for (name in listOf("neon", null, "", "123")) assertEquals(PaletteChoice.BONE, PaletteChoice.parse(name))
        assertEquals(PaletteChoice.SAFFRON, PaletteChoice.parse("SAFFRON"))
    }
    @Test fun sanitisedKeepsAValidPalette() {
        for (palette in PaletteChoice.entries) assertEquals(palette, Settings(palette = palette).sanitised().palette)
    }

    @org.junit.Test fun classOrderDefaultsAndParsesStoredValues() {
        org.junit.Assert.assertEquals(gr.edusuite.pacer.model.ClassOrder.YOUR_ORDER, gr.edusuite.pacer.model.Settings().classOrder)
        org.junit.Assert.assertEquals(gr.edusuite.pacer.model.ClassOrder.NAME, gr.edusuite.pacer.model.ClassOrder.parse("NAME"))
        org.junit.Assert.assertEquals(gr.edusuite.pacer.model.ClassOrder.YOUR_ORDER, gr.edusuite.pacer.model.ClassOrder.parse("future"))
        org.junit.Assert.assertEquals(gr.edusuite.pacer.model.ClassOrder.YOUR_ORDER, gr.edusuite.pacer.model.ClassOrder.parse(null))
    }

    @Test fun updateSettingsDefaults() {
        val s = Settings()
        assertTrue(s.autoUpdateCheck)
        assertEquals(0L, s.lastUpdateCheck)
        assertNull(s.availableUpdate)
    }

    @Test fun sanitisedCleansUpdateFields() {
        val good = Update("3.11.0", "n", "https://github.com/tsampolion/course_planner/releases/download/v3.11.0/course-pacer-3.11.0.apk", "https://github.com/tsampolion/course_planner/releases/tag/v3.11.0")
        assertEquals(good, Settings(availableUpdate = good).sanitised().availableUpdate)
        assertNull(Settings(availableUpdate = good.copy(apkUrl = "https://example.com/x.apk")).sanitised().availableUpdate)
        assertNull(Settings(availableUpdate = good.copy(version = "3.11")).sanitised().availableUpdate)
        assertEquals(0L, Settings(lastUpdateCheck = -5).sanitised().lastUpdateCheck)
    }
}
