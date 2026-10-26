package gr.edusuite.pacer.ui.theme

import gr.edusuite.pacer.model.PaletteChoice

/** Pacer-owned Bone and Saffron palettes (D13). All ARGB literals live here; contrast is verified on the JVM. */
data class Tokens(
    // §2.1 Neutrals.
    val paper: Long,
    val surface: Long,
    val surfaceContainer: Long,
    val line: Long,
    val outline: Long,
    val muted: Long,
    val ink: Long,
    // Tile field names are legacy; their values now follow the selected palette, never lilac or pink.
    val tileLilac: Long,
    val tileMint: Long,
    val tileSky: Long,
    val tileApricot: Long,
    val tilePink: Long,
    val onTile: Long,
    val onTileMuted: Long,
    // §2.3 Semantic colours.
    val warn: Long,
    val onWarn: Long,
    val warnSoft: Long,
    val onWarnSoft: Long,
    val ok: Long,
    val onOk: Long,
    val okSoft: Long,
    val onOkSoft: Long,
    // §2.4 Course Pacer's accent.
    val accent: Long,
    val onAccent: Long,
    val accentSoft: Long,
    val onAccentSoft: Long,
    // Progress fills and ticks: the accent where it reaches 3:1 on surfaceContainer, accentText on Saffron light.
    val mark: Long,
    // Course Pacer's lesson-kind edges (PACER_DESIGN.md D8): at least 3:1 on surface.
    val edgeTest: Long,
    val edgeDayOff: Long,
    val edgeEdited: Long,
    val edgeHand: Long,
    val edgeFree: Long,
    val edgeSupp: Long,
    // A worksheet lesson has an edge of its own; every kind keeps its hue family in light and dark (ContrastTest).
    val edgeWorksheet: Long,
) {
    /** A readable foreground for links, focus and controls; saffron itself is a fill. */
    val accentText: Long get() = onAccentSoft

    companion object {
        val BoneLight = Tokens(
            paper = 0xFFE9E6DD, surface = 0xFFF4F2EC, surfaceContainer = 0xFFE1DED4,
            line = 0xFFCDCABF, outline = 0xFF7D766C, muted = 0xFF6B5B51,
            ink = 0xFF2A1D17, tileLilac = 0xFFD7DFCA, tileMint = 0xFFE0DCD1,
            tileSky = 0xFFD1DEE0, tileApricot = 0xFFE5D9CB, tilePink = 0xFFDDE3BF,
            onTile = 0xFF2A1D17, onTileMuted = 0xFF574D42, warn = 0xFF91421F,
            onWarn = 0xFFF4F2EC, warnSoft = 0xFFE9DCD0, onWarnSoft = 0xFF542811,
            ok = 0xFF37622F, onOk = 0xFFF4F2EC, okSoft = 0xFFD9E1CF,
            onOkSoft = 0xFF233C1E, accent = 0xFF3F6B34, onAccent = 0xFFF4F2EC,
            accentSoft = 0xFFD9E3D0, onAccentSoft = 0xFF253F1F, mark = 0xFF3F6B34, edgeTest = 0xFF994B39,
            edgeDayOff = 0xFF7D766C, edgeEdited = 0xFF3F6B34, edgeHand = 0xFF2D6C84,
            edgeFree = 0xFF896900, edgeSupp = 0xFF414700, edgeWorksheet = 0xFFBD7728,
        )

        val BoneDark = Tokens(
            paper = 0xFF161C1A, surface = 0xFF1F2725, surfaceContainer = 0xFF2A3330,
            line = 0xFF3B4540, outline = 0xFF89908D, muted = 0xFF9AA097,
            ink = 0xFFE3E2DA, tileLilac = 0xFF303E31, tileMint = 0xFF3A3C34,
            tileSky = 0xFF2C3E42, tileApricot = 0xFF45382D, tilePink = 0xFF3B4128,
            onTile = 0xFFE3E2DA, onTileMuted = 0xFFC6C9BE, warn = 0xFFFF8F6B,
            onWarn = 0xFF3A1206, warnSoft = 0xFF432B21, onWarnSoft = 0xFFF8CBB6,
            ok = 0xFF9FC98E, onOk = 0xFF0F1A0C, okSoft = 0xFF2C3F29,
            onOkSoft = 0xFFCEE7C0, accent = 0xFF9FC98E, onAccent = 0xFF0F1A0C,
            accentSoft = 0xFF2D4129, onAccentSoft = 0xFFD4E8C8, mark = 0xFF9FC98E, edgeTest = 0xFFD77D60,
            edgeDayOff = 0xFF89908D, edgeEdited = 0xFF9FC98E, edgeHand = 0xFF77B2C2,
            edgeFree = 0xFFD8C26A, edgeSupp = 0xFF7CA823, edgeWorksheet = 0xFFA86623,
        )

        val SaffronLight = Tokens(
            paper = 0xFFF6F4EC, surface = 0xFFFFFFFF, surfaceContainer = 0xFFE9E9DF,
            line = 0xFFD5D9CE, outline = 0xFF738078, muted = 0xFF5A6A60,
            ink = 0xFF0D2A1C, tileLilac = 0xFFF4E7AA, tileMint = 0xFFDCE7D9,
            tileSky = 0xFFDCE7E5, tileApricot = 0xFFEBE3CE, tilePink = 0xFFE1E2D9,
            onTile = 0xFF0D2A1C, onTileMuted = 0xFF42574A, warn = 0xFF9A2F24,
            onWarn = 0xFFFFFFFF, warnSoft = 0xFFF0DED4, onWarnSoft = 0xFF5F2416,
            ok = 0xFF315F40, onOk = 0xFFFFFFFF, okSoft = 0xFFDEE8D9,
            onOkSoft = 0xFF1D4028, accent = 0xFFF0BF1A, onAccent = 0xFF0D2A1C,
            accentSoft = 0xFFF4E7AE, onAccentSoft = 0xFF344522, mark = 0xFF344522, edgeTest = 0xFFB04A2A,
            edgeDayOff = 0xFF738078, edgeEdited = 0xFF315F40, edgeHand = 0xFF2D6C84,
            edgeFree = 0xFF896900, edgeSupp = 0xFF414700, edgeWorksheet = 0xFF6E412A,
        )

        val SaffronDark = Tokens(
            paper = 0xFF0E1A14, surface = 0xFF16251D, surfaceContainer = 0xFF24352B,
            line = 0xFF35493B, outline = 0xFF828A85, muted = 0xFF9DB0A4,
            ink = 0xFFEEF0E8, tileLilac = 0xFF454025, tileMint = 0xFF2D3F30,
            tileSky = 0xFF293D39, tileApricot = 0xFF413B2A, tilePink = 0xFF363C32,
            onTile = 0xFFEEF0E8, onTileMuted = 0xFFC8D4C8, warn = 0xFFFF7C70,
            onWarn = 0xFF3A1206, warnSoft = 0xFF43291D, onWarnSoft = 0xFFF8CBB6,
            ok = 0xFFA1CAA3, onOk = 0xFF102719, okSoft = 0xFF283F2D,
            onOkSoft = 0xFFD7EACF, accent = 0xFFF2C94C, onAccent = 0xFF0E1A14,
            accentSoft = 0xFF423C1E, onAccentSoft = 0xFFF1E2A8, mark = 0xFFF2C94C, edgeTest = 0xFFD77D60,
            edgeDayOff = 0xFF828A85, edgeEdited = 0xFFA1CAA3, edgeHand = 0xFF77B2C2,
            edgeFree = 0xFFD8C26A, edgeSupp = 0xFF7CA823, edgeWorksheet = 0xFFA85D23,
        )

        fun of(palette: PaletteChoice, dark: Boolean): Tokens = when (palette) {
            PaletteChoice.BONE -> if (dark) BoneDark else BoneLight
            PaletteChoice.SAFFRON -> if (dark) SaffronDark else SaffronLight
        }
    }
}
