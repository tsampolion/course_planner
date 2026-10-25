package gr.edusuite.pacer

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mechanical checks for the docs rules in CLAUDE.md. Gradle runs unit tests in `app/`, so the repository root is `..`. */
class DocsTest {
    private val root = File("..")
    private val docs = File(root, "docs")
    private val link = Regex("""\]\(([^)\s]+)\)""")

    private fun docFiles() = docs.listFiles { f -> f.isFile && f.name.endsWith(".md") }!!.sortedBy { it.name }

    @Test
    fun everyDocIsInTheIndex() {
        val index = File(docs, "README.md").readText()
        val missing = docFiles().map { it.name }.filter { it != "README.md" && !index.contains("($it)") }
        assertTrue("Not linked from docs/README.md: $missing", missing.isEmpty())
    }

    @Test
    fun relativeLinksResolve() {
        val files = listOf("README.md", "CLAUDE.md", "PRODUCT.md").map { File(root, it) } + docFiles()
        val broken = mutableListOf<String>()
        for (file in files) {
            for (m in link.findAll(file.readText())) {
                val target = m.groupValues[1].substringBefore('#')
                if (target.isEmpty() || target.contains("://") || target.startsWith("mailto:")) continue
                if (!File(file.parentFile, target).exists()) broken += "${file.name} -> $target"
            }
        }
        assertTrue("Broken links: $broken", broken.isEmpty())
    }

    @Test
    fun everyBriefHasAStatusLine() {
        val bad = docFiles().filter { it.name.endsWith("_BRIEF.md") }.filter { f ->
            val first = f.readLines().firstOrNull { it.isNotBlank() && !it.startsWith("#") }
            first == null || !first.startsWith("Status:")
        }.map { it.name }
        assertTrue("Briefs without a Status: line: $bad", bad.isEmpty())
    }

    @Test
    fun noWorkingLogsAreCommitted() {
        assertFalse("docs/work must not exist", File(docs, "work").exists())
    }

    @Test
    fun claudeMdStaysShort() {
        val size = File(root, "CLAUDE.md").length()
        assertTrue("CLAUDE.md is $size bytes; keep it under 12 KB", size < 12 * 1024)
    }

    @Test
    fun topChangelogSectionMatchesVersionName() {
        val name = Regex("""versionName\s*=\s*"([^"]+)"""")
            .find(File("build.gradle.kts").readText())!!.groupValues[1]
        val top = Regex("""(?m)^## \[(\d[^\]]*)]""").find(File(root, "CHANGELOG.md").readText())!!.groupValues[1]
        assertEquals(name, top)
    }

    @Test
    fun plansAndSpecsAreTemporary() {
        val dir = File(docs, "superpowers")
        val count = if (dir.exists()) dir.walkTopDown().count { it.isFile } else 0
        assertTrue("docs/superpowers holds $count files; delete finished plans and specs", count <= 4)
    }
}
