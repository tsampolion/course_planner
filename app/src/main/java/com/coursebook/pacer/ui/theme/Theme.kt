package com.coursebook.pacer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The web page's palette: deep teal ink on pale sage paper, rust for warnings, moss for "fits". */
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

private val LightExtra = PacerColors(
    muted = Color(0xFF5C6B66), line = Color(0xFFD5DEDA), accentSoft = Color(0xFFE0EBEA),
    warn = Color(0xFF9E4A24), warnSoft = Color(0xFFF6E6DE), ok = Color(0xFF3E6E47), okSoft = Color(0xFFE4EFE6),
)

private val DarkExtra = PacerColors(
    muted = Color(0xFF93A39D), line = Color(0xFF2B3634), accentSoft = Color(0xFF1E2E2D),
    warn = Color(0xFFE0906A), warnSoft = Color(0xFF2E211A), ok = Color(0xFF7FB389), okSoft = Color(0xFF1B2820),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF245E60), onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EBEA), onPrimaryContainer = Color(0xFF245E60),
    secondaryContainer = Color(0xFFE0EBEA), onSecondaryContainer = Color(0xFF161F1D),
    background = Color(0xFFF2F5F2), onBackground = Color(0xFF161F1D),
    surface = Color(0xFFF2F5F2), onSurface = Color(0xFF161F1D),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF), surfaceContainerHighest = Color(0xFFE9EEEB),
    onSurfaceVariant = Color(0xFF5C6B66), outline = Color(0xFFB9C6C1), outlineVariant = Color(0xFFD5DEDA),
    error = Color(0xFF9E4A24), errorContainer = Color(0xFFF6E6DE), onErrorContainer = Color(0xFF9E4A24),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6FB3AE), onPrimary = Color(0xFF0F1413),
    primaryContainer = Color(0xFF1E2E2D), onPrimaryContainer = Color(0xFF6FB3AE),
    secondaryContainer = Color(0xFF1E2E2D), onSecondaryContainer = Color(0xFFE6EDEA),
    background = Color(0xFF0F1413), onBackground = Color(0xFFE6EDEA),
    surface = Color(0xFF0F1413), onSurface = Color(0xFFE6EDEA),
    surfaceContainer = Color(0xFF19201E), surfaceContainerLow = Color(0xFF19201E),
    surfaceContainerHigh = Color(0xFF19201E), surfaceContainerHighest = Color(0xFF222B29),
    onSurfaceVariant = Color(0xFF93A39D), outline = Color(0xFF41504D), outlineVariant = Color(0xFF2B3634),
    error = Color(0xFFE0906A), errorContainer = Color(0xFF2E211A), onErrorContainer = Color(0xFFE0906A),
)

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
