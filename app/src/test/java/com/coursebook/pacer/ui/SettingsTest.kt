package com.coursebook.pacer.ui

import com.coursebook.pacer.model.Settings
import com.coursebook.pacer.model.ThemeMode
import com.coursebook.pacer.model.seedPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
