package gr.edusuite.pacer.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guard the resource boundary without pulling Android or a Kotlin parser into the JVM tests. */
class NoHardcodedTextTest {
    private val tokens = Regex("""(?s)//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*""" + "\"")
    private val interpolation = Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z_0-9]*""")
    private val letters = Regex("[A-Za-z]")
    private val uiCall = Regex("""\b(?:Text|Hint|Tag|Heading|Eyebrow|PrimaryButton|SecondaryButton|TertiaryButton|AddButton|ErrorBox|GuideCard|LinkRow|SheetFrame|AddDialogFrame|PillIconButton)\s*\(\s*$""")
    private val uiAssignment = Regex("""\b(?:text|title|label|contentDescription|stateDescription|onClickLabel|onLongClickLabel|actionLabel|placeholder|confirm)\s*=\s*$""")
    private val dataStrings = mapOf(
        "ClassScreens.kt" to setOf("add", "books", "class", "delete", "empty", "groups", "guide", "hint", "lessons", "more", "name", "past", "upcoming", "group:"),
        "YearScreens.kt" to setOf("add", "delete", "empty", "end", "foot", "greek", "hint", "name", "offhead", "start", "year", "term", "more", "classes", "days-off", "years", "print"),
        "OverviewScreen.kt" to setOf("empty", "class:", "head:", "none:"),
        "GroupScreen.kt" to setOf("a", "custom", "empty", "today", "week", "lesson", "slim", "wb", "w", "r", "d", "p", "L", "N", "S"),
        "LessonLedger.kt" to setOf("EEEE d MMMM"),
        "SupplementaryUi.kt" to setOf("replace", "answers", "asking", "k", "s"),
        "GroupStats.kt" to setOf("%.1f", "MMMM yyyy"),
        "HomeScreen.kt" to setOf("EEE d MMM", "EEEE d MMMM"),
        "PacerApp.kt" to setOf("screen", "text/plain"),
        "Motion.kt" to setOf("figure", "press"),
        "Components.kt" to setOf("swipe action", "save completion"),
        "DashboardComponents.kt" to setOf("expand", "segment", "segment ink", "selected day"),
        "SettingsScreen.kt" to setOf("about", "application/json", "backup", "defaults", "help", "name", "theme", "wt", "w", "\\uFEFF"),
        "GroupModel.kt" to setOf("[^\\\\p{L}\\\\p{N}']+", "B", "book"),
        "NavModel.kt" to setOf("\\\\p{Mn}+", "\\\\s+"),
        "SetAside.kt" to setOf("application/json", "course-pacer-unreadable-.json"),
    )

    private fun violations(file: String, original: String): List<String> {
        // These are comparison keys for Nav.problems and the deliberately non-resource printed notice.
        val source = original
            .replace(Regex("""private const val NO_(?:BOOKS|GROUPS) = "[^"]*"""), "")
            .replace(Regex("""(?s)private const val ARITHMETIC =.*?const val PRINT_NOTE = "[^"]*"""), "")
        return tokens.findAll(source).mapNotNull { match ->
            if (!match.value.startsWith('"')) return@mapNotNull null
            val literal = match.value.drop(1).dropLast(1)
            val prose = interpolation.replace(literal, "")
            if (!letters.containsMatchIn(prose)) return@mapNotNull null
            val prefix = source.substring(0, match.range.first).takeLast(200)
            val directUi = uiCall.containsMatchIn(prefix) || uiAssignment.containsMatchIn(prefix)
            // Diagnostic labels are only non-visible labels inside the animation implementation.
            val diagnostic = file in setOf("Motion.kt", "Components.kt", "DashboardComponents.kt", "PacerApp.kt") &&
                Regex("""\blabel\s*=\s*$""").containsMatchIn(prefix) && prose in dataStrings[file].orEmpty()
            if ((!directUi || diagnostic) && prose in dataStrings[file].orEmpty()) null
            else "${source.take(match.range.first).count { it == '\n' } + 1}: $literal"
        }.toList()
    }

    @Test fun noHardcodedUiCopy() {
        val root = sequenceOf(File("src/main/java/gr/edusuite/pacer/ui"), File("app/src/main/java/gr/edusuite/pacer/ui"))
            .firstOrNull { it.isDirectory } ?: error("UI source directory not found")
        val excluded = setOf("Nav.kt", "PacerViewModel.kt", "PrintSheet.kt", "AppIcons.kt")
        val failures = root.listFiles()!!.filter { it.extension == "kt" && it.name !in excluded }.flatMap { file ->
            violations(file.name, file.readText()).map { "${file.name}:$it" }
        }
        assertTrue("Move visible copy to strings.xml:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test fun catchesVisibleCopyEvenWhenItMatchesAnInternalKey() {
        assertFalse(violations("ClassScreens.kt", "Text(\"name\")").isEmpty())
        assertFalse(violations("PacerApp.kt", "contentDescription = \"Back\"").isEmpty())
        assertFalse(violations("GroupScreen.kt", "Hint(\"Total: \$n lessons\")").isEmpty())
        assertEquals(emptyList<String>(), violations("ClassScreens.kt", "item(key = \"name\") {}"))
        assertEquals(emptyList<String>(), violations("GroupScreen.kt", "Text(\"\$n\")"))
        assertEquals(emptyList<String>(), violations("GroupScreen.kt", "// Text(\"Comment\")\nText(stringResource(R.string.example))"))
    }
}
