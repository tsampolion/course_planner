package gr.edusuite.pacer.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupShapeTest {
    @Test fun popupCornersStayInsideContentPaddingAsMenusGrow() {
        // Menu rows have 12dp horizontal padding and the menu has 8dp vertical padding.
        // At y=8dp, a 12dp corner has already receded to less than that text inset.
        for (density in listOf(1f, 3f)) for (height in listOf(64f, 240f, 560f)) {
            val outline = PacerMenuShape.createOutline(
                Size(280f * density, height * density), LayoutDirection.Ltr, Density(density),
            ) as Outline.Rounded
            val rect = outline.roundRect
            assertEquals(12f * density, rect.topLeftCornerRadius.x, 0.01f)
            assertEquals(12f * density, rect.bottomRightCornerRadius.y, 0.01f)
            val radius = rect.topLeftCornerRadius.x
            val y = 8f * density
            val inset = radius - kotlin.math.sqrt(radius * radius - (radius - y) * (radius - y))
            assertTrue("Text inset must be inside the popup outline", inset < 12f * density)
        }
    }
}
