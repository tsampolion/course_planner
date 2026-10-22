package gr.edusuite.pacer.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.graphics.Color
import gr.edusuite.pacer.model.PaletteChoice
import gr.edusuite.pacer.ui.searchFieldColors
import gr.edusuite.pacer.ui.paletteFieldColors
import kotlin.coroutines.EmptyCoroutineContext
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeSelectionTest {
    private class NoNodes : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
    @Test fun actualThemeKeepsReadableSelectionAndSearchCursor() {
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(NoNodes(), recomposer)
        val seen = mutableSetOf<Pair<PaletteChoice, Boolean>>()
        try {
            for (palette in PaletteChoice.entries) for (dark in listOf(false,true)) {
                val t = Tokens.of(palette,dark)
                composition.setContent {
                    PaletteMaterialTheme(dark,palette) {
                        seen += palette to dark
                        assertEquals(Color(t.accent), MaterialTheme.colorScheme.primary)
                        assertEquals(Color(t.accentText), LocalTextSelectionColors.current.handleColor)
                        assertEquals(Color(t.accentSoft), LocalTextSelectionColors.current.backgroundColor)
                        assertEquals(Color(t.accentText), searchFieldColors().cursorColor)
                        assertEquals(Color(t.accentText), paletteFieldColors().cursorColor)
                    }
                }
            }
            assertEquals(4, seen.size)
        } finally { composition.dispose(); recomposer.cancel() }
    }
}
