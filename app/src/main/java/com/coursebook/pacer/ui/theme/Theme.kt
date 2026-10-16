package com.coursebook.pacer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * EduInvoiceApp's palette (tsampolion/EduInvoiceApp, ui/theme/Theme.kt), so the two apps look alike: navy-blue
 * primary, slate secondary, green tertiary and Material's red error. Those roles are its exact values.
 *
 * EduInvoiceApp leaves every other role to Material 3's baseline, which would bring in purple surfaces and pink
 * tertiary containers. Here those roles come from the same tonal palettes instead: the neutrals and secondary from its
 * blue seed (hue 257, the Theme Builder palettes its own values sit on), the tertiary containers from its green's hue at
 * Theme Builder's tertiary chroma (24; the green's own 53 turns them neon), the error roles from Material's baseline as in
 * EduInvoiceApp. Every role is set, so a Compose update can't change the look.
 *
 * No dynamic colour (EduInvoiceApp uses the wallpaper on Android 12 and later): the palette is the same on every phone.
 */

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2E5A88), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF), onPrimaryContainer = Color(0xFF001C38), inversePrimary = Color(0xFFA2C9FF),
    secondary = Color(0xFF545F71), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E3F9), onSecondaryContainer = Color(0xFF111C2B),
    tertiary = Color(0xFF2E7D32), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCEBC4), onTertiaryContainer = Color(0xFF082008),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFAF9FD), onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFAF9FD), onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB), onSurfaceVariant = Color(0xFF43474E), surfaceTint = Color(0xFF2E5A88),
    inverseSurface = Color(0xFF2F3033), inverseOnSurface = Color(0xFFF1F0F4),
    outline = Color(0xFF73777F), outlineVariant = Color(0xFFC3C6CF), scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAF9FD), surfaceDim = Color(0xFFDAD9DD),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF4F3F7),
    surfaceContainer = Color(0xFFEEEDF1), surfaceContainerHigh = Color(0xFFE8E8EB),
    surfaceContainerHighest = Color(0xFFE3E2E6),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA2C9FF), onPrimary = Color(0xFF00315B),
    primaryContainer = Color(0xFF0F4880), onPrimaryContainer = Color(0xFFD3E4FF), inversePrimary = Color(0xFF2E5A88),
    secondary = Color(0xFFBCC7DC), onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3C4758), onSecondaryContainer = Color(0xFFD8E3F9),
    tertiary = Color(0xFF8CD48F), onTertiary = Color(0xFF003909),
    tertiaryContainer = Color(0xFF334D30), onTertiaryContainer = Color(0xFFCCEBC4),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF121316), onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF121316), onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF43474E), onSurfaceVariant = Color(0xFFC3C6CF), surfaceTint = Color(0xFFA2C9FF),
    inverseSurface = Color(0xFFE3E2E6), inverseOnSurface = Color(0xFF2F3033),
    outline = Color(0xFF8D9199), outlineVariant = Color(0xFF43474E), scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF38393C), surfaceDim = Color(0xFF121316),
    surfaceContainerLowest = Color(0xFF0D0E11), surfaceContainerLow = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFF1E2022), surfaceContainerHigh = Color(0xFF292A2D),
    surfaceContainerHighest = Color(0xFF333538),
)

/** The app's own tokens, on top of the Material roles: muted text, rules, a soft accent, warnings and "fits". */
@Immutable
data class PacerColors(
    val muted: Color,
    val line: Color,
    val accentSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val ok: Color,
    val okSoft: Color,
)

/** The tokens as Material roles, the way EduInvoiceApp's status chips use them: green is good, error is a warning. */
private fun extrasFor(scheme: ColorScheme, ok: Color) = PacerColors(
    muted = scheme.onSurfaceVariant, line = scheme.outlineVariant, accentSoft = scheme.secondaryContainer,
    warn = scheme.error, warnSoft = scheme.errorContainer, ok = ok, okSoft = scheme.tertiaryContainer,
)

// "Fits" is body text on panels, where the light tertiary (tone 46) falls just short of 4.5:1, so it takes tone 40
// of the same green.
private val LightExtra = extrasFor(LightScheme, ok = Color(0xFF1B6D24))
private val DarkExtra = extrasFor(DarkScheme, ok = DarkScheme.tertiary)

val LocalPacerColors = staticCompositionLocalOf { LightExtra }

@Composable
fun PacerTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalPacerColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
    }
}

object Pacer {
    val colors: PacerColors @Composable get() = LocalPacerColors.current
}
