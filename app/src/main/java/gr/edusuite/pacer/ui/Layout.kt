package gr.edusuite.pacer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity

/*
 * How much room the screen has (docs/PACER_DESIGN.md: large text from font scale 1.5, tablets from 600dp wide). Plain
 * Kotlin so the thresholds are unit-testable; the composables only read the configuration.
 */

/** From this font scale, layouts reflow and screens show less with the rest one tap away. */
const val LARGE_TEXT_SCALE = 1.5f

/** Large-text mode: [fontScale] is the system's, already non-linear on Android 14+. */
fun largeText(fontScale: Float): Boolean = fontScale >= LARGE_TEXT_SCALE

/** How wide the window is: a phone, a small tablet with a modal menu, or a tablet with the menu always open. */
enum class WindowLayout { COMPACT, MEDIUM, EXPANDED }

/** [COMPACT] under 600dp, [MEDIUM] from 600dp, [EXPANDED] from 840dp. The width is the window's own, in dp, so no window-size library is needed. */
fun windowLayout(widthDp: Int): WindowLayout = when {
    widthDp >= 840 -> WindowLayout.EXPANDED
    widthDp >= 600 -> WindowLayout.MEDIUM
    else -> WindowLayout.COMPACT
}

@Composable
fun rememberLargeText(): Boolean = largeText(LocalDensity.current.fontScale)

@Composable
fun rememberWindowLayout(): WindowLayout {
    val density = LocalDensity.current
    return windowLayout((LocalWindowInfo.current.containerSize.width / density.density).toInt())
}

/** The group frame owns the top inset; child pages retain only the bottom inset. */
data class GroupInsets(val top: androidx.compose.ui.unit.Dp, val body: androidx.compose.foundation.layout.PaddingValues)
fun groupInsets(padding: androidx.compose.foundation.layout.PaddingValues): GroupInsets = GroupInsets(
    padding.calculateTopPadding(), androidx.compose.foundation.layout.PaddingValues(bottom = padding.calculateBottomPadding()),
)
