package gr.edusuite.pacer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/*
 * The EduSuite theme (docs/EDUSUITE_STYLE_GUIDE.md): shared neutrals and tiles, Course Pacer's teal accent, Outfit and
 * the guide's corners. Colours come only from Tokens.kt; wallpaper colour is off on every Android version (§11), so the
 * app looks the same on every phone and the accent tells it apart from Course Invoicer.
 */

val LocalPacerColors = staticCompositionLocalOf { colorsFor(Tokens.Light) }

@Composable
fun PacerTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val tokens = if (dark) Tokens.Dark else Tokens.Light
    val other = if (dark) Tokens.Light else Tokens.Dark
    CompositionLocalProvider(LocalPacerColors provides colorsFor(tokens)) {
        MaterialTheme(
            colorScheme = schemeFor(tokens, other, dark),
            typography = PacerTypography,
            shapes = PacerShapes,
            content = content,
        )
    }
}

object Pacer {
    val colors: PacerColors @Composable get() = LocalPacerColors.current
}
