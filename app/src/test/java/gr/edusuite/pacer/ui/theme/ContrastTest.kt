package gr.edusuite.pacer.ui.theme

import gr.edusuite.pacer.model.PaletteChoice
import androidx.compose.ui.graphics.Color
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class ContrastTest {
    private fun allSets() = listOf(
        Triple("BoneLight", Tokens.BoneLight, false), Triple("BoneDark", Tokens.BoneDark, true),
        Triple("SaffronLight", Tokens.SaffronLight, false), Triple("SaffronDark", Tokens.SaffronDark, true),
        Triple("OxfordLight", Tokens.OxfordLight, false), Triple("OxfordDark", Tokens.OxfordDark, true),
    )
    private fun families() = listOf(
        Tokens.BoneLight to Tokens.BoneDark, Tokens.SaffronLight to Tokens.SaffronDark, Tokens.OxfordLight to Tokens.OxfordDark,
    )
    private fun rgb(c: Long) = listOf(16, 8, 0).map { ((c shr it) and 255).toDouble() }
    private fun luminance(c: Long): Double = rgb(c).map { it / 255 }.map { if (it <= .04045) it / 12.92 else ((it + .055) / 1.055).pow(2.4) }.zip(listOf(.2126, .7152, .0722)).sumOf { it.first * it.second }
    private fun ratio(a: Long, b: Long): Double = (max(luminance(a), luminance(b)) + .05) / (min(luminance(a), luminance(b)) + .05)
    private fun hsv(c: Long): Pair<Double, Double> {
        val (r,g,b) = rgb(c).map { it / 255 }; val hi = maxOf(r,g,b); val lo = minOf(r,g,b); val d = hi - lo
        val h = if (d == 0.0) 0.0 else when(hi) { r -> 60 * (((g-b)/d + 6) % 6); g -> 60 * ((b-r)/d + 2); else -> 60 * ((r-g)/d + 4) }
        return h to if (hi == 0.0) 0.0 else d/hi
    }
    private fun hueGap(a: Long, b: Long): Double { val d = abs(hsv(a).first - hsv(b).first); return min(d, 360 - d) }
    private fun distance(a: Long, b: Long) = sqrt(rgb(a).zip(rgb(b)).sumOf { (x, y) -> (x - y).pow(2) })
    /** The same mix as [kindTint]: the edge colour laid over surface at [KIND_TINT]. */
    private fun tint(edge: Long, surface: Long): Long = rgb(edge).zip(rgb(surface)).map { (e, s) -> (s + (e - s) * KIND_TINT).roundToLong() }
        .let { (r, g, b) -> (0xFFL shl 24) or (r shl 16) or (g shl 8) or b }
    private fun edges(t: Tokens) = listOf(t.edgeTest,t.edgeDayOff,t.edgeEdited,t.edgeHand,t.edgeFree,t.edgeSupp,t.edgeWorksheet)
    private fun grounds(t: Tokens) = listOf(t.paper, t.surface, t.surfaceContainer)

    @Test fun everyTextPairMeetsAA() {
        for ((name,t,_) in allSets()) {
            val fg = listOf(t.ink,t.muted,t.warn,t.ok,t.accentText,t.second,t.onSecondSoft) + if (name == "SaffronLight") emptyList() else listOf(t.accent)
            val pairs = fg.flatMap { f -> grounds(t).map { f to it } } + listOf(
                t.onAccent to t.accent,t.onAccentSoft to t.accentSoft,t.onWarn to t.warn,t.onWarnSoft to t.warnSoft,t.onOk to t.ok,t.onOkSoft to t.okSoft,
                t.warn to t.warnSoft,t.ok to t.okSoft,t.onSecond to t.second,t.onSecondSoft to t.secondSoft,t.second to t.secondSoft,
                // Home's header field and the page pills are accentSoft; today's lesson row is secondSoft.
                t.ink to t.accentSoft,t.ink to t.secondSoft,
            ) + t.classTiles.flatMap { listOf(t.onTile to it,t.onTileMuted to it,t.ink to it,t.muted to it) }
            for ((a,b) in pairs) assertTrue("$name: ${a.toString(16)} / ${b.toString(16)} = ${ratio(a,b)}",ratio(a,b)>=4.5)
        }
    }
    /** A kind tints its whole row and today's row is secondSoft; a row's words (ink, done, changed pages, homework) stay at 4.5:1 and its edge at 3:1. */
    @Test fun tintedRowsKeepTheirContrast() { for ((name,t,_) in allSets()) {
        val rows = edges(t).map { it to tint(it, t.surface) } + (t.second to t.secondSoft)
        for ((e, row) in rows) {
            for (f in listOf(t.ink, t.muted, t.warn, t.accentText)) assertTrue("$name ${f.toString(16)} on ${row.toString(16)} = ${ratio(f,row)}", ratio(f,row) >= 4.5)
            assertTrue("$name edge ${e.toString(16)} on its row", ratio(e,row) >= 3)
        }
    } }
    @Test fun outlineAndEdgesMeetNonTextContrast() { for ((name,t,_) in allSets()) {
        assertTrue(name,ratio(t.outline,t.paper)>=3); assertTrue(name,ratio(t.outline,t.surface)>=3)
        for (e in edges(t)) assertTrue(name,ratio(e,t.surface)>=3)
    } }
    @Test fun saffronIsNeverTextOnLightPaper() {
        val t=Tokens.SaffronLight; assertTrue(ratio(t.accent,t.paper)<3)
        for (text in listOf(t.ink,t.muted,t.warn,t.ok,t.accentText,t.second)) assertNotEquals(t.accent,text)
    }
    @Test fun progressMarksMeetNonTextContrast() { for ((name,t,_) in allSets()) for (ground in grounds(t) + (if (t.mark == t.second) t.secondSoft else t.accentSoft)) assertTrue("$name mark on ${ground.toString(16)}",ratio(t.mark,ground)>=3) }
    /** The update dot on ☰ sits on the button's surface pill, over the bar's paper, and the menu rows' surface; it carries meaning, so 3:1 on each (WCAG 1.4.11). */
    @Test fun theUpdateDotMeetsNonTextContrastOnTheBarAndTheMenu() {
        for ((name,t,_) in allSets()) for (ground in listOf(t.paper,t.surface)) assertTrue("$name update dot on ${ground.toString(16)} = ${ratio(t.mark,ground)}",ratio(t.mark,ground)>=3)
        // The fill accent is why the dot uses the mark colour: Saffron light's fill is under 2:1 on its pill.
        assertTrue(ratio(Tokens.SaffronLight.accent,Tokens.SaffronLight.surface)<3)
    }
    /** Today's dot sits on a class colour on the Home board, and today's ring on the week strip's cells. */
    @Test fun theTodayMarkReadsOnEveryClassColourAndCell() { for ((name,t,_) in allSets()) for (ground in t.classTiles + t.surfaceContainer) {
        assertTrue("$name today on ${ground.toString(16)} = ${ratio(t.second,ground)}", ratio(t.second,ground) >= 3)
    } }
    @Test fun edgesAreDistinctWithinASet() { for ((name,t,_) in allSets()) {
        val es=edges(t); assertEquals(7,es.toSet().size)
        for(i in es.indices) for(j in 0 until i) assertTrue("$name edges $i/$j",distance(es[i],es[j])>=60)
    } }
    @Test fun classColoursAreDistinctWithinASet() { for ((name,t,_) in allSets()) {
        val ts=t.classTiles; assertEquals(5,ts.toSet().size)
        for(i in ts.indices) for(j in 0 until i) assertTrue("$name class colours $i/$j = ${distance(ts[i],ts[j])}",distance(ts[i],ts[j])>=20)
    } }
    @Test fun aTestEdgeIsNeverTheWarningColour() { for ((name,t,_) in allSets()) assertNotEquals(name,t.warn,t.edgeTest) }
    @Test fun everyKindKeepsItsHueFamilyInLightAndDark() {
        fun family(l: Tokens, d: Tokens) = listOf(l.edgeTest to d.edgeTest, l.edgeEdited to d.edgeEdited, l.edgeHand to d.edgeHand, l.edgeFree to d.edgeFree, l.edgeSupp to d.edgeSupp, l.edgeWorksheet to d.edgeWorksheet, l.second to d.second)
        for ((light,dark) in families()) for ((a,b) in family(light,dark)) assertTrue("${a.toString(16)} / ${b.toString(16)}",hueGap(a,b)<=20)
    }
    @Test fun warnIsADifferentHueFromBothPrimaries() { for ((name,t,_) in allSets()) {
        assertTrue("$name warn/accent",hueGap(t.warn,t.accent)>=40); assertTrue("$name warn/second",hueGap(t.warn,t.second)>=40)
    } }
    /** The two primaries are two colours, and nothing else wears the second one: in Oxford, red means only the second primary. */
    @Test fun theSecondPrimaryOwnsItsHue() { for ((name,t,_) in allSets()) {
        assertTrue("$name second/accent",hueGap(t.second,t.accent)>=40)
        for (e in edges(t)) {
            assertTrue("$name edge ${e.toString(16)} within 25° of second",hueGap(e,t.second)>=25)
            assertTrue("$name edge ${e.toString(16)} too close to second",distance(e,t.second)>=60)
        }
    } }
    @Test fun noVioletAnywhere() { for ((name,t,_) in allSets()) for(f in Tokens::class.java.declaredFields.filter { it.type==Long::class.javaPrimitiveType }) {
        f.isAccessible=true;val(h,s)=hsv(f.getLong(t)); assertFalse("$name ${f.name}",h in 255.0..300.0 && s>.25)
    } }
    @Test fun darkGroundsAreNotPureBlackOrNavy() { for((name,t,dark) in allSets()) if(dark) {
        val(h,s)=hsv(t.paper);assertTrue(name,luminance(t.paper)>0);assertFalse(name,h in 200.0..250.0 && s>.3)
    } }
    /** The app's kindTint is the same sRGB mix this test measures. */
    @Test fun kindTintInTheAppMatchesTheMeasuredMix() { for ((name,t,_) in allSets()) {
        val colors = colorsFor(t)
        for (e in edges(t)) {
            val app = colors.kindTint(Color(e)); val measured = Color(tint(e, t.surface))
            for ((a, b) in listOf(app.red to measured.red, app.green to measured.green, app.blue to measured.blue)) assertEquals("$name ${e.toString(16)}", b, a, 1.5f / 255)
        }
    } }
        @Test fun classColoursCycleByPosition() {
        val t = Tokens.BoneLight
        assertEquals(t.classTiles[0], t.classTile(0)); assertEquals(t.classTiles[4], t.classTile(4)); assertEquals(t.classTiles[0], t.classTile(5))
        assertEquals(t.classTiles[0], t.classTile(-1))
    }
    @Test fun everyPaletteAndModeBuildsAScheme() {
        assertEquals(listOf("BONE","SAFFRON","OXFORD"), PaletteChoice.entries.map { it.name })
        val sets = PaletteChoice.entries.flatMap { p -> listOf(false,true).map { d -> Tokens.of(p,d) } }
        assertEquals(6,sets.toSet().size)
        for(p in PaletteChoice.entries) for(dark in listOf(false,true)) {
            val t=Tokens.of(p,dark);val other=Tokens.of(p,!dark);val scheme=schemeFor(t,other,dark)
            assertEquals(Color(t.accent),scheme.primary);assertEquals(Color(t.paper),scheme.background);assertEquals(Color(t.ink),scheme.onBackground)
            assertEquals(Color(other.accentText),scheme.inversePrimary)
            assertTrue("Snackbar action",ratio(other.accentText,t.ink)>=4.5)
        }
    }
}
