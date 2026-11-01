package gr.edusuite.pacer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

private fun c(argb: Long) = Color(argb)

/**
 * Pacer's palette tokens on Material 3's roles (D13). Every role is set, so a Compose update can't change
 * the look, and wallpaper (dynamic) colour is never used (§11): the accent is what identifies the app.
 */
internal fun schemeFor(t: Tokens, other: Tokens, dark: Boolean): ColorScheme {
    val args = SchemeArgs(t, other)
    return if (dark) darkColorScheme(
        primary = args.primary, onPrimary = args.onPrimary, primaryContainer = args.soft, onPrimaryContainer = args.onSoft,
        inversePrimary = args.inversePrimary, secondary = args.muted, onSecondary = args.surface,
        secondaryContainer = args.soft, onSecondaryContainer = args.onSoft,
        tertiary = c(t.ok), onTertiary = c(t.onOk), tertiaryContainer = c(t.okSoft), onTertiaryContainer = c(t.onOkSoft),
        error = c(t.warn), onError = c(t.onWarn), errorContainer = c(t.warnSoft), onErrorContainer = c(t.onWarnSoft),
        background = args.paper, onBackground = args.ink, surface = args.paper, onSurface = args.ink,
        surfaceVariant = args.container, onSurfaceVariant = args.muted, surfaceTint = Color.Transparent,
        inverseSurface = args.ink, inverseOnSurface = args.paper,
        outline = c(t.outline), outlineVariant = c(t.line), scrim = Color.Black,
        surfaceBright = args.surface, surfaceDim = args.paper,
        surfaceContainerLowest = args.surface, surfaceContainerLow = args.surface, surfaceContainer = args.container,
        surfaceContainerHigh = args.container, surfaceContainerHighest = args.container,
    ) else lightColorScheme(
        primary = args.primary, onPrimary = args.onPrimary, primaryContainer = args.soft, onPrimaryContainer = args.onSoft,
        inversePrimary = args.inversePrimary, secondary = args.muted, onSecondary = args.surface,
        secondaryContainer = args.soft, onSecondaryContainer = args.onSoft,
        tertiary = c(t.ok), onTertiary = c(t.onOk), tertiaryContainer = c(t.okSoft), onTertiaryContainer = c(t.onOkSoft),
        error = c(t.warn), onError = c(t.onWarn), errorContainer = c(t.warnSoft), onErrorContainer = c(t.onWarnSoft),
        background = args.paper, onBackground = args.ink, surface = args.paper, onSurface = args.ink,
        surfaceVariant = args.container, onSurfaceVariant = args.muted, surfaceTint = Color.Transparent,
        inverseSurface = args.ink, inverseOnSurface = args.paper,
        outline = c(t.outline), outlineVariant = c(t.line), scrim = Color.Black,
        surfaceBright = args.surface, surfaceDim = args.container,
        surfaceContainerLowest = args.surface, surfaceContainerLow = args.surface, surfaceContainer = args.container,
        surfaceContainerHigh = args.container, surfaceContainerHighest = args.container,
    )
}

private class SchemeArgs(t: Tokens, other: Tokens) {
    val primary = c(t.accent)
    val onPrimary = c(t.onAccent)
    val soft = c(t.accentSoft)
    val onSoft = c(t.onAccentSoft)
    val inversePrimary = c(other.accentText)
    val paper = c(t.paper)
    val surface = c(t.surface)
    val container = c(t.surfaceContainer)
    val muted = c(t.muted)
    val ink = c(t.ink)
}

/** The palette tokens and Course Pacer's edges that Material 3 has no role for, or that read better by name. */
@Immutable
data class PacerColors(
    val paper: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val line: Color,
    val outline: Color,
    val muted: Color,
    val ink: Color,
    val tile1: Color,
    val tile2: Color,
    val tile3: Color,
    val tile4: Color,
    val tile5: Color,
    val onTile: Color,
    val onTileMuted: Color,
    val warn: Color,
    val onWarn: Color,
    val warnSoft: Color,
    val onWarnSoft: Color,
    val ok: Color,
    val onOk: Color,
    val okSoft: Color,
    val onOkSoft: Color,
    val accent: Color,
    val accentText: Color,
    val mark: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    val second: Color,
    val onSecond: Color,
    val secondSoft: Color,
    val onSecondSoft: Color,
    val edgeTest: Color,
    val edgeDayOff: Color,
    val edgeEdited: Color,
    val edgeHand: Color,
    val edgeFree: Color,
    val edgeSupp: Color,
    val edgeWorksheet: Color,
    private val classTiles: List<Color>,
) {
    /** The colour of the class at [position] in the class list (Tokens.classTile). */
    fun classTile(position: Int): Color = classTiles[if (position < 0) 0 else position % classTiles.size]

    /** The track under a progress fill: the soft colour of the primary the fill is drawn in. */
    val progressTrack: Color get() = if (mark == second) secondSoft else accentSoft

    /** A lesson kind's row ground: its edge colour over surface at [KIND_TINT], mixed in sRGB as ContrastTest measures it. */
    fun kindTint(edge: Color): Color {
        val f = KIND_TINT.toFloat()
        return Color(surface.red + (edge.red - surface.red) * f, surface.green + (edge.green - surface.green) * f, surface.blue + (edge.blue - surface.blue) * f)
    }
}

internal fun colorsFor(t: Tokens) = PacerColors(
    paper = c(t.paper), surface = c(t.surface), surfaceContainer = c(t.surfaceContainer), line = c(t.line),
    outline = c(t.outline), muted = c(t.muted), ink = c(t.ink),
    tile1 = c(t.tile1), tile2 = c(t.tile2), tile3 = c(t.tile3), tile4 = c(t.tile4),
    tile5 = c(t.tile5), onTile = c(t.onTile), onTileMuted = c(t.onTileMuted),
    warn = c(t.warn), onWarn = c(t.onWarn), warnSoft = c(t.warnSoft), onWarnSoft = c(t.onWarnSoft),
    ok = c(t.ok), onOk = c(t.onOk), okSoft = c(t.okSoft), onOkSoft = c(t.onOkSoft),
    accent = c(t.accent), accentText = c(t.accentText), mark = c(t.mark), onAccent = c(t.onAccent), accentSoft = c(t.accentSoft), onAccentSoft = c(t.onAccentSoft),
    second = c(t.second), onSecond = c(t.onSecond), secondSoft = c(t.secondSoft), onSecondSoft = c(t.onSecondSoft),
    edgeTest = c(t.edgeTest), edgeDayOff = c(t.edgeDayOff), edgeEdited = c(t.edgeEdited), edgeHand = c(t.edgeHand),
    edgeFree = c(t.edgeFree), edgeSupp = c(t.edgeSupp), edgeWorksheet = c(t.edgeWorksheet),
    classTiles = t.classTiles.map(::c),
)
