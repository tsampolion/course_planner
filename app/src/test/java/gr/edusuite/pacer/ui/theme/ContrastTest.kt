package gr.edusuite.pacer.ui.theme

import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The style guide's contrast table (docs/EDUSUITE_STYLE_GUIDE.md §2.5) for the shared light values and tiles, and the
 * Course Pacer contrast table (docs/PACER_ACCENT_CONTRAST.md) for the accent, the Pacer dark set and the lesson-kind
 * edges, computed from [Tokens]. A token edit that breaks a listed ratio fails here.
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

    /**
     * One row of a table: a foreground and background picked from the tokens, and the ratio listed for each mode. A
     * null ratio means that mode is listed in the other table.
     */
    private class Row(val name: String, val fg: (Tokens) -> Long, val bg: (Tokens) -> Long, val light: Double?, val dark: Double?)

    private fun check(rows: List<Row>) {
        for (r in rows) {
            r.light?.let { assertEquals("${r.name} (light)", it, ratio(r.fg(Tokens.Light), r.bg(Tokens.Light)), 0.01) }
            r.dark?.let { assertEquals("${r.name} (dark)", it, ratio(r.fg(Tokens.Dark), r.bg(Tokens.Dark)), 0.01) }
        }
    }

    private val tiles: List<Pair<String, (Tokens) -> Long>> = listOf(
        "lilac" to { t -> t.tileLilac }, "mint" to { t -> t.tileMint }, "sky" to { t -> t.tileSky },
        "apricot" to { t -> t.tileApricot }, "pink" to { t -> t.tilePink },
    )

    private val edges: List<Pair<String, (Tokens) -> Long>> = listOf(
        "edgeTest" to { t -> t.edgeTest }, "edgeDayOff" to { t -> t.edgeDayOff }, "edgeEdited" to { t -> t.edgeEdited },
        "edgeHand" to { t -> t.edgeHand }, "edgeFree" to { t -> t.edgeFree }, "edgeSupp" to { t -> t.edgeSupp },
    )

    // The guide's §2.5, light mode. Course Pacer's dark set has its own table (PACER_DESIGN.md D1).
    @Test fun neutralsMatchTheGuide() = check(listOf(
        Row("ink on paper", { it.ink }, { it.paper }, 15.90, null),
        Row("ink on surface", { it.ink }, { it.surface }, 17.25, null),
        Row("ink on surfaceContainer", { it.ink }, { it.surfaceContainer }, 13.96, null),
        Row("paper on ink (selected)", { it.paper }, { it.ink }, 15.90, null),
        Row("muted on paper", { it.muted }, { it.paper }, 6.14, null),
        Row("muted on surface", { it.muted }, { it.surface }, 6.67, null),
        Row("muted on surfaceContainer", { it.muted }, { it.surfaceContainer }, 5.39, null),
        Row("outline on paper", { it.outline }, { it.paper }, 4.06, null),
        Row("outline on surface", { it.outline }, { it.surface }, 4.41, null),
    ))

    // The tiles keep the guide's values in both modes.
    @Test fun tilesMatchTheGuide() {
        val onTile = listOf(11.42 to 10.87, 12.81 to 8.99, 12.93 to 9.54, 12.35 to 9.06, 11.55 to 10.64)
        val onTileMuted = listOf(6.46 to 8.20, 7.24 to 6.79, 7.31 to 7.20, 6.99 to 6.84, 6.53 to 8.03)
        check(tiles.mapIndexed { i, (name, tile) -> Row("onTile on $name", { it.onTile }, tile, onTile[i].first, onTile[i].second) })
        check(tiles.mapIndexed { i, (name, tile) ->
            Row("onTileMuted on $name", { it.onTileMuted }, tile, onTileMuted[i].first, onTileMuted[i].second)
        })
    }

    @Test fun semanticColoursMatchTheGuide() = check(listOf(
        Row("warn on paper", { it.warn }, { it.paper }, 5.60, null),
        Row("ok on paper", { it.ok }, { it.paper }, 5.49, null),
        Row("warn on warnSoft", { it.warn }, { it.warnSoft }, 4.93, null),
        Row("ok on okSoft", { it.ok }, { it.okSoft }, 4.82, null),
        Row("on warnSoft on warnSoft", { it.onWarnSoft }, { it.warnSoft }, 12.58, null),
        Row("on okSoft on okSoft", { it.onOkSoft }, { it.okSoft }, 11.29, null),
    ))

    @Test fun pacerAccentMatchesItsTable() = check(listOf(
        Row("accent on paper", { it.accent }, { it.paper }, 6.80, 10.33),
        Row("accent on surface", { it.accent }, { it.surface }, 7.38, 9.28),
        Row("accent on accentSoft", { it.accent }, { it.accentSoft }, 5.89, 6.45),
        Row("on accent on accent", { it.onAccent }, { it.accent }, 7.38, 8.23),
        Row("on accentSoft on accentSoft", { it.onAccentSoft }, { it.accentSoft }, 11.54, 10.40),
        // The launcher tile is the light accent in both modes, under the light paper.
        Row("icon paper on tile", { Tokens.Light.paper }, { Tokens.Light.accent }, 6.80, 6.80),
    ))

    @Test fun pacerDarkSetMatchesItsTable() = check(listOf(
        Row("ink on paper", { it.ink }, { it.paper }, null, 17.45),
        Row("ink on surface", { it.ink }, { it.surface }, null, 15.67),
        Row("ink on surfaceContainer", { it.ink }, { it.surfaceContainer }, null, 13.72),
        Row("paper on ink (selected)", { it.paper }, { it.ink }, null, 17.45),
        Row("muted on paper", { it.muted }, { it.paper }, null, 9.68),
        Row("muted on surface", { it.muted }, { it.surface }, null, 8.69),
        Row("muted on surfaceContainer", { it.muted }, { it.surfaceContainer }, null, 7.61),
        Row("outline on paper", { it.outline }, { it.paper }, null, 5.87),
        Row("outline on surface", { it.outline }, { it.surface }, null, 5.27),
        Row("warn on paper", { it.warn }, { it.paper }, null, 8.62),
        Row("warn on surface", { it.warn }, { it.surface }, null, 7.74),
        Row("ok on paper", { it.ok }, { it.paper }, null, 12.56),
        Row("warn on warnSoft", { it.warn }, { it.warnSoft }, null, 6.71),
        Row("ok on okSoft", { it.ok }, { it.okSoft }, null, 8.73),
        Row("on warn on warn", { it.onWarn }, { it.warn }, null, 7.40),
        Row("on ok on ok", { it.onOk }, { it.ok }, null, 9.48),
        Row("on warnSoft on warnSoft", { it.onWarnSoft }, { it.warnSoft }, null, 10.89),
        Row("on okSoft on okSoft", { it.onOkSoft }, { it.okSoft }, null, 10.83),
    ))

    @Test fun pacerEdgesMatchTheirTable() {
        val onSurface = listOf(6.06 to 9.71, 4.41 to 5.27, 7.38 to 9.28, 5.67 to 10.36, 5.22 to 10.84, 5.55 to 8.93)
        check(edges.mapIndexed { i, (name, edge) -> Row("$name on surface", edge, { it.surface }, onSurface[i].first, onSurface[i].second) })
    }

    /** Text pairs reach 4.5:1, and field outlines and lesson-kind edges 3:1 (§2.5, §7, PACER_DESIGN.md D8), in both modes. */
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
            for ((name, edge) in edges) assertTrue("$name on surface", ratio(edge(t), t.surface) >= 3.0)
        }
    }
}
