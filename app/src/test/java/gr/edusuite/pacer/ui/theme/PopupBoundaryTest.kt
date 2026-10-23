package gr.edusuite.pacer.ui.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** All popups must opt out of Material's percentage-rounded menu default. */
class PopupBoundaryTest {
    @Test fun menusUseTheSharedNonClippingPopup() {
        val root = listOf(File("src/main/java/gr/edusuite/pacer/ui"), File("app/src/main/java/gr/edusuite/pacer/ui")).first { it.isDirectory }
        val bypasses = root.walkTopDown().filter { it.extension == "kt" && it.name != "PacerDropdownMenu.kt" }
            .filter { it.readText().contains("import androidx.compose.material3.DropdownMenu\n") }.map { it.name }.toList()
        assertTrue("Menus bypassing the safe popup: $bypasses", bypasses.isEmpty())
    }
}
