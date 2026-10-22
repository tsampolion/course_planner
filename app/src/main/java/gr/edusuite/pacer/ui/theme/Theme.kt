package gr.edusuite.pacer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import gr.edusuite.pacer.model.PaletteChoice
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.graphics.Color
import gr.edusuite.pacer.ui.PacerMotion

/** Palette choice and mode repaint together; wallpaper colours stay off. */
val LocalPacerDark = staticCompositionLocalOf { false }

val LocalPacerColors = staticCompositionLocalOf { colorsFor(Tokens.of(PaletteChoice.BONE, false)) }

@Composable
fun PacerTheme(dark: Boolean = isSystemInDarkTheme(), palette: PaletteChoice = PaletteChoice.BONE, content: @Composable () -> Unit) {
    PaletteMaterialTheme(dark, palette) { PacerMotion(content) }
}

/** Platform-independent palette locals; motion/lifecycle integration stays in PacerTheme. */
@Composable
internal fun PaletteMaterialTheme(dark: Boolean, palette: PaletteChoice, content: @Composable () -> Unit) {
    val tokens = Tokens.of(palette, dark)
    val other = Tokens.of(palette, !dark)
    CompositionLocalProvider(
        LocalPacerColors provides colorsFor(tokens), LocalPacerDark provides dark,
    ) {
        MaterialTheme(
            colorScheme = schemeFor(tokens, other, dark),
            typography = PacerTypography,
            shapes = PacerShapes,
            content = {
                CompositionLocalProvider(
                    LocalTextSelectionColors provides TextSelectionColors(Color(tokens.accentText), Color(tokens.accentSoft)),
                    content = content,
                )
            },
        )
    }
}

object Pacer {
    val dark: Boolean @Composable get() = LocalPacerDark.current
    val colors: PacerColors @Composable get() = LocalPacerColors.current
}
