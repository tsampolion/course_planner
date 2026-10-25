package gr.edusuite.pacer.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spacing on the list and settings screens comes from `Space` and `Rhythm`, never from a literal `NN.dp`. */
class SpacingTokensTest {
    private val comments = Regex("""(?s)//[^\n]*|/\*.*?\*/""")
    private val spacing = Regex(
        """\b(?:padding|spacedBy|PaddingValues|absolutePadding)\([^)]*\b\d+(?:\.\d+)?\.dp|Spacer\(Modifier\.(?:height|width)\(\d+(?:\.\d+)?\.dp""",
    )
    private val screens = setOf(
        "YearScreens.kt", "SettingsScreen.kt", "ClassScreens.kt", "OverviewScreen.kt", "GroupSettingsScreen.kt",
        "UnitsSettingsScreen.kt", "WorkScreen.kt", "GroupStats.kt", "GroupScreen.kt", "GroupHome.kt", "Rows.kt", "Components.kt",
    )

    /** Intentional literals, each with its reason; anything else must use a token. */
    private val allowed = mapOf(
        // Clearance under the last class row for the floating New class button; no 4dp-grid token is tall enough.
        "YearScreens.kt" to listOf("Spacer(Modifier.height(72.dp"),
        // Text buttons whose icon-sized label needs no inset beyond the 48dp target.
        "ClassScreens.kt" to listOf("PaddingValues(0.dp"),
        // A 2dp optical gap between a figure and its caption, below the 4dp grid.
        "GroupStats.kt" to listOf("spacedBy(2.dp"),
        "Rows.kt" to listOf("spacedBy(2.dp", "PaddingValues(0.dp"),
        "Components.kt" to listOf("PaddingValues(0.dp", "vertical = 2.dp"),
    )

    private fun offenders(file: String, source: String): List<String> =
        spacing.findAll(comments.replace(source, "")).map { it.value }
            .filter { hit -> allowed[file].orEmpty().none { hit.contains(it) } }.toList()

    @Test fun noScreenUsesLiteralDpSpacing() {
        val root = sequenceOf(File("src/main/java/gr/edusuite/pacer/ui"), File("app/src/main/java/gr/edusuite/pacer/ui"))
            .firstOrNull { it.isDirectory } ?: error("UI source directory not found")
        val failures = root.listFiles()!!.filter { it.name in screens }.flatMap { f -> offenders(f.name, f.readText()).map { "${f.name}: $it" } }
        assertTrue("Use Space.* or Rhythm.* instead of literal dp:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test fun catchesLiteralSpacing() {
        assertEquals(1, offenders("ClassScreens.kt", "Modifier.padding(top = 8.dp)").size)
        assertEquals(1, offenders("ClassScreens.kt", "Arrangement.spacedBy(12.dp)").size)
        assertEquals(emptyList<String>(), offenders("ClassScreens.kt", "Modifier.padding(Space.m) // 8.dp"))
        assertEquals(emptyList<String>(), offenders("ClassScreens.kt", "Modifier.heightIn(min = 48.dp)"))
    }
}
