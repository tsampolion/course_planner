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
    )
    private fun rgb(c: Long) = listOf(16, 8, 0).map { ((c shr it) and 255).toDouble() }
    private fun luminance(c: Long): Double = rgb(c).map { it / 255 }.map { if (it <= .04045) it / 12.92 else ((it + .055) / 1.055).pow(2.4) }.zip(listOf(.2126, .7152, .0722)).sumOf { it.first * it.second }
    private fun ratio(a: Long, b: Long): Double = (max(luminance(a), luminance(b)) + .05) / (min(luminance(a), luminance(b)) + .05)
    private fun hsv(c: Long): Pair<Double, Double> {
        val (r,g,b) = rgb(c).map { it / 255 }; val hi = maxOf(r,g,b); val lo = minOf(r,g,b); val d = hi - lo
        val h = if (d == 0.0) 0.0 else when(hi) { r -> 60 * (((g-b)/d + 6) % 6); g -> 60 * ((b-r)/d + 2); else -> 60 * ((r-g)/d + 4) }
        return h to if (hi == 0.0) 0.0 else d/hi
    }
    private fun edges(t: Tokens) = listOf(t.edgeTest,t.edgeDayOff,t.edgeEdited,t.edgeHand,t.edgeFree,t.edgeSupp)
    @Test fun everyTextPairMeetsAA() {
        for ((name,t,_) in allSets()) {
            val fg = listOf(t.ink,t.muted,t.warn,t.ok,t.accentText) + if (name == "SaffronLight") emptyList() else listOf(t.accent)
            val pairs = fg.flatMap { f -> listOf(t.paper,t.surface,t.surfaceContainer).map { f to it } } + listOf(
                t.onAccent to t.accent,t.onAccentSoft to t.accentSoft,t.onWarn to t.warn,t.onWarnSoft to t.warnSoft,t.onOk to t.ok,t.onOkSoft to t.okSoft,
                t.warn to t.warnSoft,t.ok to t.okSoft,
            ) + listOf(t.tileLilac,t.tileMint,t.tileSky,t.tileApricot,t.tilePink).flatMap { listOf(t.onTile to it,t.onTileMuted to it) }
            for ((a,b) in pairs) assertTrue("$name: ${a.toString(16)} / ${b.toString(16)} = ${ratio(a,b)}",ratio(a,b)>=4.5)
        }
    }
    @Test fun outlineAndEdgesMeetNonTextContrast() { for ((name,t,_) in allSets()) {
        assertTrue(name,ratio(t.outline,t.paper)>=3); assertTrue(name,ratio(t.outline,t.surface)>=3)
        for (e in edges(t)) assertTrue(name,ratio(e,t.surface)>=3)
    } }
    @Test fun saffronIsNeverTextOnLightPaper() {
        val t=Tokens.SaffronLight; assertTrue(ratio(t.accent,t.paper)<3)
        for (text in listOf(t.ink,t.muted,t.warn,t.ok,t.accentText)) assertNotEquals(t.accent,text)
    }
    @Test fun edgesAreDistinctWithinASet() { for ((name,t,_) in allSets()) {
        val es=edges(t); assertEquals(6,es.toSet().size)
        for(i in es.indices) for(j in 0 until i) assertTrue("$name edges $i/$j",sqrt(rgb(es[i]).zip(rgb(es[j])).sumOf { (a,b)->(a-b).pow(2) })>=60)
    } }
    @Test fun warnIsADifferentHueFromTheAccent() { for ((name,t,_) in allSets()) {
        val d=abs(hsv(t.warn).first-hsv(t.accent).first);assertTrue(name,min(d,360-d)>=40)
    } }
    @Test fun noVioletAnywhere() { for ((name,t,_) in allSets()) for(f in Tokens::class.java.declaredFields.filter { it.type==Long::class.javaPrimitiveType }) {
        f.isAccessible=true;val(h,s)=hsv(f.getLong(t)); assertFalse("$name ${f.name}",h in 255.0..300.0 && s>.25)
    } }
    @Test fun darkGroundsAreNotPureBlackOrNavy() { for((name,t,dark) in allSets()) if(dark) {
        val(h,s)=hsv(t.paper);assertTrue(name,luminance(t.paper)>0);assertFalse(name,h in 200.0..250.0 && s>.3)
    } }
    @Test fun everyPaletteAndModeBuildsAScheme() {
        val sets = PaletteChoice.entries.flatMap { p -> listOf(false,true).map { d -> Tokens.of(p,d) } }
        assertEquals(4,sets.toSet().size)
        for(p in PaletteChoice.entries) for(dark in listOf(false,true)) {
            val t=Tokens.of(p,dark);val other=Tokens.of(p,!dark);val scheme=schemeFor(t,other,dark)
            assertEquals(Color(t.accent),scheme.primary);assertEquals(Color(t.paper),scheme.background);assertEquals(Color(t.ink),scheme.onBackground)
            assertEquals(Color(other.accentText),scheme.inversePrimary)
            assertTrue("Snackbar action",ratio(other.accentText,t.ink)>=4.5)
        }
    }
}
