package gr.edusuite.pacer.ui.theme

import gr.edusuite.pacer.model.PaletteChoice

/** Which of [count] class colours the class at [position] takes: they repeat, and an unknown position (-1) takes the first. */
internal fun classIndex(position: Int, count: Int): Int = if (position < 0) 0 else position % count

/** How strongly a lesson kind tints its row: the edge colour laid over surface at this share (ContrastTest checks the result). */
const val KIND_TINT = 0.12

/**
 * Pacer-owned palettes (D13): Freetown (stored as BONE), Arusha (SAFFRON) and Oxford, each light and dark.
 * Every palette has two primaries, five class colours and seven lesson-kind colours. All ARGB literals live here;
 * contrast is verified on the JVM.
 */
data class Tokens(
    // Neutrals, tinted toward each palette's hue.
    val paper: Long,
    val surface: Long,
    val surfaceContainer: Long,
    val line: Long,
    val outline: Long,
    val muted: Long,
    val ink: Long,
    // Five class colours: a class takes one by its position in the class list (classTile).
    val tile1: Long,
    val tile2: Long,
    val tile3: Long,
    val tile4: Long,
    val tile5: Long,
    val onTile: Long,
    val onTileMuted: Long,
    // Semantic colours. Warnings sit at least 40° of hue from both primaries.
    val warn: Long,
    val onWarn: Long,
    val warnSoft: Long,
    val onWarnSoft: Long,
    val ok: Long,
    val onOk: Long,
    val okSoft: Long,
    val onOkSoft: Long,
    // The first primary: the lesson hero, Done, selected controls, the logo.
    val accent: Long,
    val onAccent: Long,
    val accentSoft: Long,
    val onAccentSoft: Long,
    // The second primary: today, Add actions and (in Freetown and Arusha) progress. Nothing else wears its hue.
    val second: Long,
    val onSecond: Long,
    val secondSoft: Long,
    val onSecondSoft: Long,
    // Progress fills and ticks: at least 3:1 on every ground.
    val mark: Long,
    // Course Pacer's lesson-kind edges (PACER_DESIGN.md D8): at least 3:1 on surface; each kind also tints its row.
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

    /** The five class colours in order. */
    val classTiles: List<Long> get() = listOf(tile1, tile2, tile3, tile4, tile5)

    /** The colour of the class at [position] in the class list; the five colours repeat, and an unknown position (-1) takes the first. */
    fun classTile(position: Int): Long = classTiles[classIndex(position, classTiles.size)]

    companion object {
        val BoneLight = Tokens(
            paper = 0xFFE9EBE4, surface = 0xFFF7F8F3, surfaceContainer = 0xFFDCE3D9, line = 0xFFC8D0C4,
            outline = 0xFF6F786E, muted = 0xFF535C54, ink = 0xFF17211C, tile1 = 0xFFC9E4CF,
            tile2 = 0xFFD2DDF7, tile3 = 0xFFF1DCA8, tile4 = 0xFFF3D0DF, tile5 = 0xFFBDE3EE,
            onTile = 0xFF17211C, onTileMuted = 0xFF46514A, warn = 0xFF9E2A2B, onWarn = 0xFFF7F8F3,
            warnSoft = 0xFFF2DCD8, onWarnSoft = 0xFF5A1515, ok = 0xFF1E5C43, onOk = 0xFFF7F8F3,
            okSoft = 0xFFCFE3D6, onOkSoft = 0xFF123B2A, accent = 0xFF1E5C43, onAccent = 0xFFF7F8F3,
            accentSoft = 0xFFCFE3D6, onAccentSoft = 0xFF123B2A, second = 0xFF2550C4, onSecond = 0xFFF7F8F3,
            secondSoft = 0xFFDCE4F8, onSecondSoft = 0xFF142F78, mark = 0xFF2550C4, edgeTest = 0xFFA8336A,
            edgeDayOff = 0xFF6F786E, edgeEdited = 0xFF1E5C43, edgeHand = 0xFF1C6E8A, edgeFree = 0xFF8A6A00,
            edgeSupp = 0xFF4C5600, edgeWorksheet = 0xFFC25E16,
        )

        val BoneDark = Tokens(
            paper = 0xFF161A17, surface = 0xFF1F2420, surfaceContainer = 0xFF283029, line = 0xFF374038,
            outline = 0xFF8A928A, muted = 0xFFA6AEA5, ink = 0xFFE6E9E1, tile1 = 0xFF21402E,
            tile2 = 0xFF22305A, tile3 = 0xFF46391A, tile4 = 0xFF4A2440, tile5 = 0xFF163E4A,
            onTile = 0xFFE6E9E1, onTileMuted = 0xFFC3CBC1, warn = 0xFFEF8E86, onWarn = 0xFF2A0B0A,
            warnSoft = 0xFF402322, onWarnSoft = 0xFFF6CFCB, ok = 0xFF86C3A3, onOk = 0xFF0F1E17,
            okSoft = 0xFF21392D, onOkSoft = 0xFFCDE6D7, accent = 0xFF86C3A3, onAccent = 0xFF0F1E17,
            accentSoft = 0xFF21392D, onAccentSoft = 0xFFCDE6D7, second = 0xFF93B0FF, onSecond = 0xFF0E1A3D,
            secondSoft = 0xFF1F2C52, onSecondSoft = 0xFFD6E0FF, mark = 0xFF93B0FF, edgeTest = 0xFFE17DAE,
            edgeDayOff = 0xFF8E8A84, edgeEdited = 0xFF86C3A3, edgeHand = 0xFF4FB8D0, edgeFree = 0xFFD8BC4E,
            edgeSupp = 0xFF8FB33A, edgeWorksheet = 0xFFEC8034,
        )

        val SaffronLight = Tokens(
            paper = 0xFFF1F1EE, surface = 0xFFFFFFFF, surfaceContainer = 0xFFE6E6E1, line = 0xFFD4D4CF,
            outline = 0xFF787874, muted = 0xFF565652, ink = 0xFF161615, tile1 = 0xFFFCE9AE,
            tile2 = 0xFFCFEAE6, tile3 = 0xFFF8DCC4, tile4 = 0xFFD7E3F7, tile5 = 0xFFDDE8C8,
            onTile = 0xFF161615, onTileMuted = 0xFF4A4A46, warn = 0xFFB0213A, onWarn = 0xFFFFFFFF,
            warnSoft = 0xFFF8DEE2, onWarnSoft = 0xFF5E0F1C, ok = 0xFF2B6A3D, onOk = 0xFFFFFFFF,
            okSoft = 0xFFDCEBDF, onOkSoft = 0xFF163C21, accent = 0xFFF5B800, onAccent = 0xFF161615,
            accentSoft = 0xFFFBEAB0, onAccentSoft = 0xFF2A2822, second = 0xFF006B61, onSecond = 0xFFFFFFFF,
            secondSoft = 0xFFCFEAE6, onSecondSoft = 0xFF00403A, mark = 0xFF006B61, edgeTest = 0xFFA8336A,
            edgeDayOff = 0xFF787874, edgeEdited = 0xFF7A5C00, edgeHand = 0xFF2D5DB8, edgeFree = 0xFF3A3A36,
            edgeSupp = 0xFF3C7A2A, edgeWorksheet = 0xFFC25E16,
        )

        val SaffronDark = Tokens(
            paper = 0xFF161615, surface = 0xFF1F1F1D, surfaceContainer = 0xFF2A2A27, line = 0xFF3B3B37,
            outline = 0xFF8D8D88, muted = 0xFFA9A9A3, ink = 0xFFEDEDE8, tile1 = 0xFF3D3416,
            tile2 = 0xFF173A35, tile3 = 0xFF52281A, tile4 = 0xFF1E2B45, tile5 = 0xFF2B3420,
            onTile = 0xFFEDEDE8, onTileMuted = 0xFFC9C9C3, warn = 0xFFF2828F, onWarn = 0xFF2C0A10,
            warnSoft = 0xFF42222A, onWarnSoft = 0xFFF8CFD5, ok = 0xFF8CC69C, onOk = 0xFF0F2015,
            okSoft = 0xFF22382A, onOkSoft = 0xFFCFE8D6, accent = 0xFFF7C531, onAccent = 0xFF161615,
            accentSoft = 0xFF3D3416, onAccentSoft = 0xFFF6E3A6, second = 0xFF4CC7B6, onSecond = 0xFF0B2622,
            secondSoft = 0xFF173A35, onSecondSoft = 0xFFC7EEE8, mark = 0xFF4CC7B6, edgeTest = 0xFFE17DAE,
            edgeDayOff = 0xFF8D8D88, edgeEdited = 0xFFE0C04A, edgeHand = 0xFF86A8F0, edgeFree = 0xFFC9C9C3,
            edgeSupp = 0xFF9CCB7A, edgeWorksheet = 0xFFEC8034,
        )

        val OxfordLight = Tokens(
            paper = 0xFFF4F5F8, surface = 0xFFFFFFFF, surfaceContainer = 0xFFE4E8F0, line = 0xFFCFD6E2,
            outline = 0xFF6E7787, muted = 0xFF4F596A, ink = 0xFF121A2C, tile1 = 0xFFDCE3F4,
            tile2 = 0xFFF9DCE0, tile3 = 0xFFC6E6F2, tile4 = 0xFFE6E6E0, tile5 = 0xFFD2EBD9,
            onTile = 0xFF121A2C, onTileMuted = 0xFF465065, warn = 0xFF8A5600, onWarn = 0xFFFFFFFF,
            warnSoft = 0xFFF6E7C8, onWarnSoft = 0xFF4A2E00, ok = 0xFF1E6B4A, onOk = 0xFFFFFFFF,
            okSoft = 0xFFD8EBE1, onOkSoft = 0xFF0F3D29, accent = 0xFF012169, onAccent = 0xFFFFFFFF,
            accentSoft = 0xFFDCE3F4, onAccentSoft = 0xFF12296B, second = 0xFFC8102E, onSecond = 0xFFFFFFFF,
            secondSoft = 0xFFF9DCE0, onSecondSoft = 0xFF7A0A1C, mark = 0xFF012169, edgeTest = 0xFF0E6E78,
            edgeDayOff = 0xFF6E7787, edgeEdited = 0xFF012169, edgeHand = 0xFF2F6FC4, edgeFree = 0xFF8A6A00,
            edgeSupp = 0xFF4C6A00, edgeWorksheet = 0xFFC25E16,
        )

        val OxfordDark = Tokens(
            paper = 0xFF141619, surface = 0xFF1C1F24, surfaceContainer = 0xFF252A33, line = 0xFF363D49,
            outline = 0xFF8A93A3, muted = 0xFFA7AFBD, ink = 0xFFE9ECF2, tile1 = 0xFF1E2C4A,
            tile2 = 0xFF4A1C24, tile3 = 0xFF133D45, tile4 = 0xFF2E2E2A, tile5 = 0xFF1C3828,
            onTile = 0xFFE9ECF2, onTileMuted = 0xFFC3C9D4, warn = 0xFFE6B04E, onWarn = 0xFF2B1D00,
            warnSoft = 0xFF3A2E14, onWarnSoft = 0xFFF5DFA8, ok = 0xFF7FC4A2, onOk = 0xFF0C2219,
            okSoft = 0xFF1E352B, onOkSoft = 0xFFCBE8DA, accent = 0xFF8EAEF2, onAccent = 0xFF0A1430,
            accentSoft = 0xFF1E2C4A, onAccentSoft = 0xFFD3DFFA, second = 0xFFFF6F7F, onSecond = 0xFF330810,
            secondSoft = 0xFF4A1C24, onSecondSoft = 0xFFFFD3D8, mark = 0xFF8EAEF2, edgeTest = 0xFF4FC0C9,
            edgeDayOff = 0xFF8A93A3, edgeEdited = 0xFF8EAEF2, edgeHand = 0xFF4F86E0, edgeFree = 0xFFD8BC4E,
            edgeSupp = 0xFF8FB33A, edgeWorksheet = 0xFFEC8034,
        )

        fun of(palette: PaletteChoice, dark: Boolean): Tokens = when (palette) {
            PaletteChoice.BONE -> if (dark) BoneDark else BoneLight
            PaletteChoice.SAFFRON -> if (dark) SaffronDark else SaffronLight
            PaletteChoice.OXFORD -> if (dark) OxfordDark else OxfordLight
        }
    }
}
