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
        assertEquals(Version(listOf(3, 10, 0)), parseVersion("v3.10.0"))
        assertEquals(Version(listOf(3, 10, 0)), parseVersion("3.10.0"))
        assertEquals(Version(listOf(3, 10, 0)), parseVersion(" v3.10.0 "))
    }

    @Test fun parseVersionRejectsOtherShapes() {
        listOf(null, "", "v3.10", "3.10.0-beta", "vx.y.z", "V3.10.0", "v-1.0.0", "3..0").forEach {
            assertNull(it, parseVersion(it))
        }
    }

    @Test fun parseVersionNumericEdgeCases() {
        assertEquals(Version(listOf(3, 10, 0)), parseVersion("v3.010.0"))
        assertEquals(Version(listOf(10, 0, 0)), parseVersion("v10.0.0"))
        assertNull(parseVersion("v3.10.2147483648"))
    }

    @Test fun parsesFourParts() {
        assertEquals("0.4.1.1", parseVersion("v0.4.1.1")!!.text())
        assertEquals("0.4.1.1", parseVersion("0.4.1.1")!!.text())
    }

    @Test fun parsesOldThreeParts() {
        assertEquals("4.1.0", parseVersion("4.1.0")!!.text())
    }

    @Test fun rejectsFiveParts() {
        assertNull(parseVersion("0.4.1.1.1"))
        assertNull(parseVersion("v0.4.1."))
    }

    @Test fun rejectsOverflow() {
        assertNull(parseVersion("v0.4.1.2147483648"))
    }

    @Test fun fourPartOrdering() {
        fun v(s: String) = parseVersion(s)!!
        assertTrue(isNewer(v("0.4.2.0"), v("0.4.1.1")))
        assertTrue(isNewer(v("0.4.1.1"), v("0.4.1.0")))
        assertFalse(isNewer(v("0.4.1.1"), v("4.1.0")))
        assertFalse(isNewer(v("0.4.1.0"), v("4.1.0")))
        assertTrue(isNewer(v("4.1.0"), v("0.4.1.0")))
        assertFalse(isNewer(v("0.4.1.1"), v("0.4.1.1")))
    }

    @Test fun pickApkFindsFourPartAsset() {
        val url = "https://github.com/tsampolion/course_planner/releases/download/v0.4.2.0/course-pacer-0.4.2.0.apk"
        assertEquals(url, pickApk(listOf("course-pacer-0.4.2.0.apk" to url), "0.4.2.0"))
    }

    @Test fun toUpdateKeepsFourPartVersion() {
        val url = "https://github.com/tsampolion/course_planner/releases/download/v0.4.2.0/course-pacer-0.4.2.0.apk"
        val pg = "https://github.com/tsampolion/course_planner/releases/tag/v0.4.2.0"
        val u = toUpdate(Release("v0.4.2.0", "- New", pg, listOf("course-pacer-0.4.2.0.apk" to url)))
        assertEquals(Update("0.4.2.0", "New", url, pg), u)
        assertEquals(u, pendingUpdate(u, "0.4.1.1"))
        assertNull(pendingUpdate(u, "0.4.2.0"))
    }

    @Test fun isNewerComparesNumerically() {
        assertTrue(isNewer(Version(listOf(3, 10, 0)), Version(listOf(3, 9, 0))))
        assertTrue(isNewer(Version(listOf(3, 10, 1)), Version(listOf(3, 10, 0))))
        assertTrue(isNewer(Version(listOf(4, 0, 0)), Version(listOf(3, 99, 99))))
        assertFalse(isNewer(Version(listOf(3, 10, 0)), Version(listOf(3, 10, 0))))
        assertFalse(isNewer(Version(listOf(3, 9, 9)), Version(listOf(3, 10, 0))))
    }

    @Test fun pickApkFindsExactNameOnly() {
        assertEquals(apk, pickApk(release().assets, "3.11.0"))
        assertNull(pickApk(release().assets, "3.12.0"))
        assertNull(pickApk(listOf("course-pacer-3.11.0.apk.sha256" to apk), "3.11.0"))
        assertNull(pickApk(emptyList(), "3.11.0"))
    }

    @Test fun toUpdateBuildsFromAGoodRelease() {
        assertEquals(
            Update("3.11.0", "Update check in Settings.\nA dot on the menu.", apk, page),
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

    @Test fun shortNotesKeepsOnlyBulletsAndCapsThem() {
        val md = "### Added\n\n- **Updates**: check in [Settings](https://x.y/z).\n* Second\n- Third\n- Fourth\n- Fifth\nPlain line"
        assertEquals("Updates: check in Settings.\nSecond\nThird\nFourth", shortNotes(md))
        assertEquals("", shortNotes(""))
        assertEquals("", shortNotes(null))
    }

    @Test fun shortNotesHandlesCrLfAndImagesAndDropsCodeBullets() {
        val md = "### Added\r\n\r\n- ![shot](https://x.y/a.png) New *badge*\r\n- _Quiet_ `notice`\r\n- Calm"
        val out = shortNotes(md)
        assertEquals("New badge\nCalm", out)
        listOf("\r", "![", "**", "](", "`").forEach { assertFalse(it, out.contains(it)) }
    }

    @Test fun shortNotesNeverShowsTheDeveloperText() {
        val body = "### Fixed\nThe saved-file version is now 16. 0.4.12.0 began saving whether homework is done.\n" +
            "- Homework you tick as done is now remembered.\n" +
            "- The saved-file version is now 16 (`JsonCodec.VERSION`).\n\n" +
            "Signed with certificate SHA-256 `f8cb5004`."
        assertEquals("Homework you tick as done is now remembered.", shortNotes(body))
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

    private fun r(v: String, vararg bullets: String, pre: Boolean = false) = Release(
        "v$v", bullets.joinToString("\n") { "- $it" }, "https://github.com/tsampolion/course_planner/releases/tag/v$v",
        listOf("course-pacer-$v.apk" to "https://github.com/tsampolion/course_planner/releases/download/v$v/course-pacer-$v.apk"),
        prerelease = pre,
    )

    @Test fun noticeLeadsWithTheBiggestReleaseThenFillsToFour() {
        val list = listOf(
            r("0.4.15.0", "Choose features.", "Release names."), r("0.4.14.1", "Class colours."),
            r("0.4.14.0", "Third palette."), r("0.4.13.2", "Old homework."),
        )
        assertEquals("Choose features.\nThird palette.\nClass colours.\nRelease names.", toUpdate(list, "0.4.13.2")!!.notes)
        assertEquals("0.4.15.0", toUpdate(list, "0.4.13.2")!!.version)
    }

    @Test fun anAReleaseOutranksB() {
        val list = listOf(r("0.5.0.0", "Files change."), r("0.4.15.0", "New feature."), r("0.4.14.1", "Fix."))
        assertEquals("Files change.\nNew feature.\nFix.", toUpdate(list, "0.4.14.0")!!.notes)
    }

    @Test fun noticeIgnoresNotesWrittenBeforeThePlainEnglishRule() {
        val list = listOf(r("0.4.14.1", "Class colours."), r("0.4.13.2", "Feature bricks follow modules."))
        assertEquals("Class colours.", toUpdate(list, "0.4.10.0")!!.notes)
        assertEquals("", toUpdate(listOf(r("0.4.13.2", "Feature bricks.")), "0.4.10.0")!!.notes)
    }

    @Test fun noticeSkipsOldTagsPrereleasesAndOlderReleases() {
        val list = listOf(r("0.4.16.0", "Beta.", pre = true), r("0.4.15.0", "Real."), r("4.1.0", "Ancient."), r("0.4.14.0", "Seen."))
        val u = toUpdate(list, "0.4.14.0")!!
        assertEquals("0.4.15.0", u.version)
        assertEquals("Real.", u.notes)
        assertNull(toUpdate(listOf(r("4.1.0", "Ancient.")), "0.4.14.0"))
    }
}
