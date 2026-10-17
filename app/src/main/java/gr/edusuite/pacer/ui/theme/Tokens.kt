package gr.edusuite.pacer.ui.theme

/**
 * Every colour in the EduSuite style guide (docs/EDUSUITE_STYLE_GUIDE.md §2), as ARGB values, with Course Pacer's
 * accent (§2.4, docs/PACER_ACCENT_CONTRAST.md). Plain Kotlin, so ContrastTest can check the guide's ratios on the JVM.
 * A hex value appears nowhere else in the app; the theme maps these onto Material 3 roles in Color.kt.
 */
data class Tokens(
    // §2.1 Neutrals.
    val paper: Long,
    val surface: Long,
    val surfaceContainer: Long,
    val line: Long,
    val outline: Long,
    val muted: Long,
    val ink: Long,
    // §2.2 Tiles.
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
) {
    companion object {
        val Light = Tokens(
            paper = 0xFFF6F5FA, surface = 0xFFFFFFFF, surfaceContainer = 0xFFE8E6EF,
            line = 0xFFE4E2EA, outline = 0xFF7A7686, muted = 0xFF5E5A6B, ink = 0xFF1B1A22,
            tileLilac = 0xFFD9CCF5, tileMint = 0xFFBFE8C8, tileSky = 0xFFBDE6F0, tileApricot = 0xFFFFD39A,
            tilePink = 0xFFF4C6E6, onTile = 0xFF1B1A22, onTileMuted = 0xFF46424F,
            warn = 0xFF9E4A24, onWarn = 0xFFFFFFFF, warnSoft = 0xFFF7E4DA, onWarnSoft = 0xFF3E1A0A,
            ok = 0xFF3E6E47, onOk = 0xFFFFFFFF, okSoft = 0xFFDCEBDF, onOkSoft = 0xFF15321C,
            accent = 0xFF245E60, onAccent = 0xFFFFFFFF, accentSoft = 0xFFD5EAE8, onAccentSoft = 0xFF0F2E2F,
        )

        val Dark = Tokens(
            paper = 0xFF121118, surface = 0xFF1C1B22, surfaceContainer = 0xFF2A2832,
            line = 0xFF2E2C37, outline = 0xFF8C889A, muted = 0xFFA9A5B5, ink = 0xFFECEAF2,
            tileLilac = 0xFF3A2E5C, tileMint = 0xFF1F4A33, tileSky = 0xFF1B4350, tileApricot = 0xFF5A3B12,
            tilePink = 0xFF55264A, onTile = 0xFFF3F1F8, onTileMuted = 0xFFD6D2E0,
            warn = 0xFFEE9C78, onWarn = 0xFF3E1A0A, warnSoft = 0xFF3D2419, onWarnSoft = 0xFFF7E4DA,
            ok = 0xFF8FC79A, onOk = 0xFF0F2A17, okSoft = 0xFF1D3424, onOkSoft = 0xFFDCEBDF,
            accent = 0xFF6FB3AE, onAccent = 0xFF0F2E2F, accentSoft = 0xFF163C3C, onAccentSoft = 0xFFD5EAE8,
        )
    }
}
