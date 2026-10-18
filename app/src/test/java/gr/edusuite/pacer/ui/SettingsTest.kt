package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.ThemeMode
import gr.edusuite.pacer.model.seedPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {
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
}
