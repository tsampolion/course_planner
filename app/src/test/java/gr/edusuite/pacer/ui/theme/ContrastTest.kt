package gr.edusuite.pacer.ui.theme

import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The style guide's contrast table (docs/EDUSUITE_STYLE_GUIDE.md §2.5) and Course Pacer's accent pairs
 * (docs/PACER_ACCENT_CONTRAST.md), computed from [Tokens]. A token edit that breaks a listed ratio fails here.
 */
class ContrastTest {
    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun ratio(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** One row of a table: a foreground and background picked from the tokens, and the ratio listed for each mode. */
    private class Row(val name: String, val fg: (Tokens) -> Long, val bg: (Tokens) -> Long, val light: Double, val dark: Double)

    private fun check(rows: List<Row>) {
        for (r in rows) {
            assertEquals("${r.name} (light)", r.light, ratio(r.fg(Tokens.Light), r.bg(Tokens.Light)), 0.01)
            assertEquals("${r.name} (dark)", r.dark, ratio(r.fg(Tokens.Dark), r.bg(Tokens.Dark)), 0.01)
        }
    }

    private val tiles: List<Pair<String, (Tokens) -> Long>> = listOf(
        "lilac" to { t -> t.tileLilac }, "mint" to { t -> t.tileMint }, "sky" to { t -> t.tileSky },
        "apricot" to { t -> t.tileApricot }, "pink" to { t -> t.tilePink },
    )

    @Test fun neutralsMatchTheGuide() = check(listOf(
        Row("ink on paper", { it.ink }, { it.paper }, 15.90, 15.74),
        Row("ink on surface", { it.ink }, { it.surface }, 17.25, 14.33),
        Row("ink on surfaceContainer", { it.ink }, { it.surfaceContainer }, 13.96, 12.17),
        Row("paper on ink (selected)", { it.paper }, { it.ink }, 15.90, 15.74),
        Row("muted on paper", { it.muted }, { it.paper }, 6.14, 7.81),
        Row("muted on surface", { it.muted }, { it.surface }, 6.67, 7.11),
        Row("muted on surfaceContainer", { it.muted }, { it.surfaceContainer }, 5.39, 6.03),
        Row("outline on paper", { it.outline }, { it.paper }, 4.06, 5.45),
        Row("outline on surface", { it.outline }, { it.surface }, 4.41, 4.97),
    ))

    @Test fun tilesMatchTheGuide() {
        val onTile = listOf(11.42 to 10.87, 12.81 to 8.99, 12.93 to 9.54, 12.35 to 9.06, 11.55 to 10.64)
        val onTileMuted = listOf(6.46 to 8.20, 7.24 to 6.79, 7.31 to 7.20, 6.99 to 6.84, 6.53 to 8.03)
        check(tiles.mapIndexed { i, (name, tile) -> Row("onTile on $name", { it.onTile }, tile, onTile[i].first, onTile[i].second) })
        check(tiles.mapIndexed { i, (name, tile) ->
            Row("onTileMuted on $name", { it.onTileMuted }, tile, onTileMuted[i].first, onTileMuted[i].second)
        })
    }

    @Test fun semanticColoursMatchTheGuide() = check(listOf(
        Row("warn on paper", { it.warn }, { it.paper }, 5.60, 8.63),
        Row("ok on paper", { it.ok }, { it.paper }, 5.49, 9.65),
        Row("warn on warnSoft", { it.warn }, { it.warnSoft }, 4.93, 6.60),
        Row("ok on okSoft", { it.ok }, { it.okSoft }, 4.82, 6.89),
        Row("on warnSoft on warnSoft", { it.onWarnSoft }, { it.warnSoft }, 12.58, 11.65),
        Row("on okSoft on okSoft", { it.onOkSoft }, { it.okSoft }, 11.29, 10.83),
    ))

    @Test fun pacerAccentMatchesItsTable() = check(listOf(
        Row("accent on paper", { it.accent }, { it.paper }, 6.80, 7.81),
        Row("accent on surface", { it.accent }, { it.surface }, 7.38, 7.11),
        Row("accent on accentSoft", { it.accent }, { it.accentSoft }, 5.89, 5.00),
        Row("on accent on accent", { it.onAccent }, { it.accent }, 7.38, 6.02),
        Row("on accentSoft on accentSoft", { it.onAccentSoft }, { it.accentSoft }, 11.54, 9.60),
        // The launcher tile is the light accent in both modes, under the light paper.
        Row("icon paper on tile", { Tokens.Light.paper }, { Tokens.Light.accent }, 6.80, 6.80),
    ))

    /** Text pairs reach 4.5:1 and field outlines 3:1 (§2.5, §7), in both modes. */
    @Test fun everyPairMeetsItsTarget() {
        for (t in listOf(Tokens.Light, Tokens.Dark)) {
            val text = listOf(
                t.ink to t.paper, t.ink to t.surface, t.ink to t.surfaceContainer, t.muted to t.paper,
                t.muted to t.surface, t.muted to t.surfaceContainer, t.warn to t.paper, t.warn to t.surface,
                t.warn to t.warnSoft, t.onWarn to t.warn, t.onWarnSoft to t.warnSoft, t.ok to t.paper, t.ok to t.surface,
                t.ok to t.okSoft, t.onOk to t.ok, t.onOkSoft to t.okSoft, t.accent to t.paper, t.accent to t.surface,
                t.accent to t.accentSoft, t.onAccent to t.accent, t.onAccentSoft to t.accentSoft,
            ) + listOf(t.tileLilac, t.tileMint, t.tileSky, t.tileApricot, t.tilePink).flatMap { listOf(t.onTile to it, t.onTileMuted to it) }
            for ((fg, bg) in text) assertTrue("%08X on %08X".format(fg, bg), ratio(fg, bg) >= 4.5)
            assertTrue(ratio(t.outline, t.paper) >= 3.0)
            assertTrue(ratio(t.outline, t.surface) >= 3.0)
        }
    }
}
