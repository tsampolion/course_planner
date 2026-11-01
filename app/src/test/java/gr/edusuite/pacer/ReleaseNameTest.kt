package gr.edusuite.pacer

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The name the What's new deck shows is the one in the changelog heading of the version being built. Gradle runs unit tests in `app/`. */
class ReleaseNameTest {
    private val version = Regex("""versionName\s*=\s*"([^"]+)"""").find(File("build.gradle.kts").readText())!!.groupValues[1]
    private val heading = Regex("""^## \[${Regex.escape(version)}] - \S+ - (.+)$""", RegexOption.MULTILINE).find(File("../CHANGELOG.md").readText())
    private val resource = Regex("""<string name="release_name"[^>]*>([^<]*)</string>""").find(File("src/main/res/values/strings.xml").readText())!!.groupValues[1]

    @Test fun aFixKeepsTheNameOfTheReleaseItFixesAndAddsNone() {
        if (version.substringAfterLast('.') != "0") assertEquals("a fix heading carries no name", null, heading)
    }

    @Test fun aFeatureReleaseNamesItselfAndTheDeckShowsThatName() {
        if (version.substringAfterLast('.') == "0") {
            assertTrue("0.A.B.0 needs a name in its CHANGELOG heading", heading != null)
            assertEquals(heading!!.groupValues[1].trim(), resource)
        }
    }
}
