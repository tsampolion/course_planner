package gr.edusuite.pacer.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The style guide's mechanical rules for the UI sources (`docs/EDUSUITE_STYLE_GUIDE.md` §1, §8; `CLAUDE.md` UI copy and
 * icons): no labels set in capitals, no shadows (the app is flat; a lifted row steps up a tone), and every icon is a
 * Material Symbol in `AppIcons.kt`.
 */
class StyleGuardTest {
    private val root = sequenceOf(File("src/main/java/gr/edusuite/pacer/ui"), File("app/src/main/java/gr/edusuite/pacer/ui"))
        .firstOrNull { it.isDirectory } ?: error("UI source directory not found")

    private fun offending(pattern: Regex, except: Set<String> = emptySet()): List<String> =
        root.walkTopDown().filter { it.extension == "kt" && it.name !in except }.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line -> if (pattern.containsMatchIn(line)) "${f.name}:${i + 1}: ${line.trim()}" else null }
        }.toList()

    @Test fun noTextIsSetInCapitals() {
        val found = offending(Regex("""\.uppercase\(\)"""))
        assertTrue("Sentence case, never capitals (§8):\n${found.joinToString("\n")}", found.isEmpty())
    }

    @Test fun nothingCastsAShadow() {
        val found = offending(Regex("""shadowElevation|\.shadow\("""))
        assertTrue("Flat: use a tone step, not a shadow (§1):\n${found.joinToString("\n")}", found.isEmpty())
    }

    @Test fun iconsComeOnlyFromAppIcons() {
        val found = offending(Regex("""PathParser|ImageVector\.Builder|materialIcon|Icons\.(Filled|Outlined|Rounded|Default)"""), except = setOf("AppIcons.kt"))
        assertTrue("Icons are Material Symbols path data in AppIcons.kt:\n${found.joinToString("\n")}", found.isEmpty())
    }

    @Test fun everyIconPathParses() {
        val icons = AppIcons::class.java.declaredMethods.filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType.simpleName == "ImageVector" }
        assertTrue(icons.size >= 30)
        icons.forEach { it.invoke(AppIcons) }
    }
}
