package gr.edusuite.pacer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.position
import gr.edusuite.pacer.ui.theme.Pacer

/**
 * One core book's line: how far through the book the group is, and where the lead book says it should be ([tick], null with no lead).
 * Supplementary books have no line; they are placed, not paced.
 */
internal data class PacingLine(val bookId: String, val fraction: Float, val tick: Float?, val lead: Boolean)

internal fun pacingLines(report: GroupReport): List<PacingLine> {
    val inStep = report.inStepPages()
    return report.plan.books.filter { !it.supplementary }.map { b ->
        val all = b.allPages()
        val pos = position(report.group, b)
        fun at(page: Int) = if (all.isEmpty()) 0f else all.count { it < page }.toFloat() / all.size
        PacingLine(b.id, at(pos), inStep[b.id]?.let { at(it) }, b.id == report.plan.lead)
    }
}

/**
 * The progress strip's mark (bolder direction): a thin line per book ending where the group is, with the lead book's tick.
 * It is the only caller of [pacingLines]; to drop the idea, put a LinearProgressIndicator back where [ProgressStrip] calls this.
 */
@Composable
internal fun PacingLines(report: GroupReport, modifier: Modifier = Modifier) {
    val c = Pacer.colors
    val lines = remember(report) { pacingLines(report) }
    Column(modifier.fillMaxWidth().clearAndSetSemantics {}, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        lines.forEach { line ->
            BoxWithConstraints(Modifier.fillMaxWidth().height(6.dp), contentAlignment = Alignment.CenterStart) {
                Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(c.surfaceContainer)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(line.fraction.coerceIn(0f, 1f)).clip(CircleShape).background(c.mark))
                }
                line.tick?.let { Box(Modifier.offset(x = maxWidth * it.coerceIn(0f, 1f) - 1.dp).width(2.dp).fillMaxHeight().background(c.ink, CircleShape)) }
            }
        }
    }
}
