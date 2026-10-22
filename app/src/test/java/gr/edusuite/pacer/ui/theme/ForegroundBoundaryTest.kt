package gr.edusuite.pacer.ui.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prevent Material's fill-primary default from becoming saffron text on pale surfaces. */
class ForegroundBoundaryTest {
    @Test fun textButtonsAndExplicitTextColoursUseReadableForegrounds() {
        val root = listOf(File("src/main/java/gr/edusuite/pacer/ui"), File("app/src/main/java/gr/edusuite/pacer/ui")).first { it.isDirectory }
        val failures = root.listFiles()!!.filter { it.extension == "kt" }.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, s ->
                if (s.trim() == "import androidx.compose.material3.TextButton" || Regex("""\bcolor\s*=\s*(?:Pacer.colors|c)\.accent\b""").containsMatchIn(s)) "${f.name}:${i+1}: $s" else null
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
