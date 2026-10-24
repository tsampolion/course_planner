package gr.edusuite.pacer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateModelTest {
    private val apk = "https://github.com/tsampolion/course_planner/releases/download/v3.11.0/course-pacer-3.11.0.apk"
    private val page = "https://github.com/tsampolion/course_planner/releases/tag/v3.11.0"
    private fun release(
        tag: String? = "v3.11.0",
        body: String? = "### Added\n\n- Update check in Settings.\n- A dot on the menu.",
        pageUrl: String? = page,
        assets: List<Pair<String, String>> = listOf(
            "course-pacer-3.11.0.apk" to apk,
            "notes.txt" to "https://github.com/tsampolion/course_planner/releases/download/v3.11.0/notes.txt",
        ),
    ) = Release(tag, body, pageUrl, assets)

    @Test fun parseVersionAcceptsTagsAndBareVersions() {
        assertEquals(Version(3, 10, 0), parseVersion("v3.10.0"))
        assertEquals(Version(3, 10, 0), parseVersion("3.10.0"))
        assertEquals(Version(3, 10, 0), parseVersion(" v3.10.0 "))
    }

    @Test fun parseVersionRejectsOtherShapes() {
        listOf(null, "", "v3.10", "3.10.0-beta", "3.10.0.1", "vx.y.z", "V3.10.0", "v-1.0.0", "3..0").forEach {
            assertNull(it, parseVersion(it))
        }
    }

    @Test fun parseVersionNumericEdgeCases() {
        assertEquals(Version(3, 10, 0), parseVersion("v3.010.0"))
        assertEquals(Version(10, 0, 0), parseVersion("v10.0.0"))
        assertNull(parseVersion("v3.10.2147483648"))
    }

    @Test fun isNewerComparesNumerically() {
        assertTrue(isNewer(Version(3, 10, 0), Version(3, 9, 0)))
        assertTrue(isNewer(Version(3, 10, 1), Version(3, 10, 0)))
        assertTrue(isNewer(Version(4, 0, 0), Version(3, 99, 99)))
        assertFalse(isNewer(Version(3, 10, 0), Version(3, 10, 0)))
        assertFalse(isNewer(Version(3, 9, 9), Version(3, 10, 0)))
    }

    @Test fun pickApkFindsExactNameOnly() {
        assertEquals(apk, pickApk(release().assets, "3.11.0"))
        assertNull(pickApk(release().assets, "3.12.0"))
        assertNull(pickApk(listOf("course-pacer-3.11.0.apk.sha256" to apk), "3.11.0"))
        assertNull(pickApk(emptyList(), "3.11.0"))
    }

    @Test fun toUpdateBuildsFromAGoodRelease() {
        assertEquals(
            Update("3.11.0", "Added\nUpdate check in Settings.\nA dot on the menu.", apk, page),
            toUpdate(release()),
        )
    }

    @Test fun toUpdateRejectsIncompleteReleases() {
        assertNull(toUpdate(release(tag = null)))
        assertNull(toUpdate(release(tag = "latest")))
        assertNull(toUpdate(release(assets = listOf("course-pacer-3.10.0.apk" to apk))))
        assertNull(toUpdate(release(pageUrl = null)))
        assertEquals("", toUpdate(release(body = null))!!.notes)
    }

    @Test fun toUpdateRejectsForeignUrls() {
        assertNull(toUpdate(release(assets = listOf("course-pacer-3.11.0.apk" to "https://example.com/course-pacer-3.11.0.apk"))))
        assertNull(toUpdate(release(assets = listOf("course-pacer-3.11.0.apk" to "http://github.com/tsampolion/course_planner/releases/download/v3.11.0/course-pacer-3.11.0.apk"))))
        assertNull(toUpdate(release(pageUrl = "https://github.com/someone/else/releases/tag/v3.11.0")))
    }

    @Test fun isDueAfterADay() {
        val day = CHECK_INTERVAL_MS
        assertTrue(isDue(0, 1_000))
        assertFalse(isDue(10 * day, 10 * day + day - 1))
        assertTrue(isDue(10 * day, 11 * day))
        assertTrue(isDue(10 * day, 9 * day))
    }

    @Test fun shortNotesStripsMarkdownAndKeepsThreeLines() {
        val md = "### Added\n\n- **Updates**: check in [Settings](https://x.y/z).\n* Second\n1. Third\n- Fourth"
        assertEquals("Added\nUpdates: check in Settings.\nSecond", shortNotes(md))
        assertEquals("", shortNotes(""))
        assertEquals("", shortNotes(null))
    }

    @Test fun shortNotesHandlesCrLfAndImages() {
        val md = "### Added\r\n\r\n- ![shot](https://x.y/a.png) New *badge*\r\n- _Quiet_ `notice`\r\n"
        val out = shortNotes(md)
        assertEquals("Added\nNew badge\nQuiet notice", out)
        listOf("\r", "![", "**", "](", "`").forEach { assertFalse(it, out.contains(it)) }
    }

    @Test fun pendingUpdateKeepsOnlyNewer() {
        val u = Update("3.11.0", "", apk, page)
        assertEquals(u, pendingUpdate(u, "3.10.0"))
        assertNull(pendingUpdate(u, "3.11.0"))
        assertNull(pendingUpdate(u, "3.12.0"))
        assertNull(pendingUpdate(null, "3.10.0"))
        assertNull(pendingUpdate(u.copy(version = "junk"), "3.10.0"))
        assertNull(pendingUpdate(u, "3.11.0-debug"))
    }

    @Test fun storedUpdateNeedsEveryPart() {
        assertEquals(Update("3.11.0", "n", apk, page), storedUpdate("3.11.0", "n", apk, page))
        assertEquals(Update("3.11.0", "", apk, page), storedUpdate("3.11.0", null, apk, page))
        assertNull(storedUpdate(null, "n", apk, page))
        assertNull(storedUpdate("3.11.0", "n", null, page))
        assertNull(storedUpdate("3.11.0", "n", apk, null))
        assertNull(storedUpdate("v3.11", "n", apk, page))
    }

    @Test fun storedUpdateRejectsForeignUrls() {
        assertNull(storedUpdate("3.11.0", "n", "https://example.com/a.apk", page))
        assertNull(storedUpdate("3.11.0", "n", apk, "https://example.com/"))
    }
}
