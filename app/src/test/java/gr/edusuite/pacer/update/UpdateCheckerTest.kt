package gr.edusuite.pacer.update

import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    private val day = CHECK_INTERVAL_MS
    private var clock = 100 * day
    private var calls = 0
    private fun rel(v: String) = Release(
        "v$v", "- Notes", "https://github.com/tsampolion/course_planner/releases/tag/v$v",
        listOf("course-pacer-$v.apk" to "https://github.com/tsampolion/course_planner/releases/download/v$v/course-pacer-$v.apk"),
    )
    private fun checker(result: Release?, installed: String = "0.4.10.0", enabled: Boolean = true) =
        UpdateChecker({ calls++; result?.let(::listOf) }, installed, enabled) { clock }
    private val found = toUpdate(rel("0.4.11.0"))!!

    @Test fun fetchFindsNewer() = assertEquals(Fetched.Found(found.copy(notes = "")), checker(rel("0.4.11.0")).fetch())  // too old for teacher notes
    @Test fun fetchSameOrOlderIsUpToDate() {
        assertEquals(Fetched.UpToDate, checker(rel("0.4.10.0")).fetch())
        assertEquals(Fetched.UpToDate, checker(rel("0.4.9.0")).fetch())
    }
    @Test fun fetchWithoutApkIsUpToDate() = assertEquals(Fetched.UpToDate, checker(rel("0.4.11.0").copy(assets = emptyList())).fetch())
    @Test fun fetchFailureIsFailed() = assertEquals(Fetched.Failed, checker(null).fetch())
    @Test fun fetchSwallowsExceptions() =
        assertEquals(Fetched.Failed, UpdateChecker({ error("boom") }, "0.4.10.0", true) { clock }.fetch())
    @Test fun disabledNeverFetches() {
        assertEquals(Fetched.Failed, checker(rel("0.4.11.0"), enabled = false).fetch())
        assertEquals(0, calls)
    }

    @Test fun shouldAutoCheckRules() {
        val c = checker(null)
        assertTrue(c.shouldAutoCheck(Settings()))
        assertFalse(c.shouldAutoCheck(Settings(autoUpdateCheck = false)))
        assertFalse(c.shouldAutoCheck(Settings(lastUpdateCheck = clock - day + 1)))
        assertTrue(c.shouldAutoCheck(Settings(lastUpdateCheck = clock - day)))
        assertFalse(checker(null, enabled = false).shouldAutoCheck(Settings()))
    }

    @Test fun appliedFoundStoresAndStamps() {
        val s = checker(null).applied(Settings(), Fetched.Found(found))
        assertEquals(found, s.availableUpdate)
        assertEquals(clock, s.lastUpdateCheck)
    }
    @Test fun appliedUpToDateClearsAndStamps() {
        val s = checker(null).applied(Settings(availableUpdate = found), Fetched.UpToDate)
        assertNull(s.availableUpdate)
        assertEquals(clock, s.lastUpdateCheck)
    }
    @Test fun appliedFailedChangesNothing() {
        val before = Settings(availableUpdate = found, lastUpdateCheck = 7)
        assertSame(before, checker(null).applied(before, Fetched.Failed))
    }
    @Test fun appliedKeepsOtherSettings() {
        // The teacher changed the theme and turned auto-check off while the request was in flight.
        val current = Settings(theme = ThemeMode.DARK, autoUpdateCheck = false, name = "Anna")
        val s = checker(null).applied(current, Fetched.Found(found))
        assertEquals(ThemeMode.DARK, s.theme)
        assertFalse(s.autoUpdateCheck)
        assertEquals("Anna", s.name)
    }

    @Test fun launchClearsCaughtUpUpdate() {
        assertNull(checker(null, installed = "0.4.11.0").atLaunch(Settings(availableUpdate = found)).availableUpdate)
        assertNull(checker(null, installed = "0.4.12.0").atLaunch(Settings(availableUpdate = found)).availableUpdate)
        val keep = Settings(availableUpdate = found)
        assertSame(keep, checker(null, installed = "0.4.10.0").atLaunch(keep))
        assertNull(checker(null, enabled = false).atLaunch(keep).availableUpdate)
    }

    @Test fun pendingHonoursEnabled() {
        val s = Settings(availableUpdate = found)
        assertEquals(found, checker(null).pending(s))
        assertNull(checker(null, enabled = false).pending(s))
        assertNull(checker(null, installed = "0.4.11.0").pending(s))
    }
}
