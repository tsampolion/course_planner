package gr.edusuite.pacer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.BookLag
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Source
import gr.edusuite.pacer.engine.catchUp
import gr.edusuite.pacer.engine.catchUpPlan
import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.fixed2
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.foldBacklog
import gr.edusuite.pacer.engine.plain
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

/*
 * Warnings with a way out (docs/CUSTOM_PLANS_BRIEF.md decisions 8 and 9): a group behind its sustainable pace gets a
 * catch-up it can preview and apply, and a book left behind the others gets a plan to bring it back in step. Both
 * write into the custom plan; the proposed plan never changes. The numbers come from engine/Warnings.kt.
 */

/** A warning in the suite's banner style with the action that answers it. */
@Composable
fun WarningBanner(text: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pacer.colors
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(MaterialTheme.shapes.medium).background(c.warnSoft),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(c.warn))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.onWarnSoft, modifier = Modifier.weight(1f).padding(horizontal = Space.m, vertical = Space.s))
        TertiaryButton(action, onAction, color = c.onWarnSoft)
    }
}

/** The boosts the catch-up offers: one, two or three pages a lesson above the sustainable pace. */
private val BOOSTS = listOf(1, 2, 3)

/**
 * How to catch up: pick +1, +2 or +3 pages a lesson and see at once how many lessons that takes and when the group is
 * back on pace, with the first lessons it changes. Applying it writes those lessons into the custom plan.
 */
@Composable
fun CatchUpSheet(report: GroupReport, onApply: (Int) -> Unit, onDismiss: () -> Unit) {
    var choice by rememberSaveable(report.group.id) { mutableIntStateOf(0) }
    val boost = BOOSTS[choice]
    val cu = remember(report, boost) { report.catchUp(boost) }
    val preview = remember(report, boost) { previewOf(report, report.catchUpPlan(boost)?.let { report.group.copy(custom = it) }) }
    SheetFrame(stringResource(R.string.warn_catch_up_title), stringResource(R.string.warn_apply_catch_up), cu?.feasible == true, { onApply(boost) }, onDismiss) {
        if (cu == null) {
            Hint(stringResource(R.string.warn_on_pace))
            return@SheetFrame
        }
        Text(pluralStringResource(R.plurals.warn_behind, cu.behind, cu.behind, report.plan.pace.plain()), style = MaterialTheme.typography.bodyLarge)
        SegmentedToggle(BOOSTS.map { stringResource(R.string.warn_boost, it) }, choice, { choice = it })
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            AnimatedFigure("${cu.lessons}", style = MaterialTheme.typography.displaySmall.merge(Figures))
            Text(pluralStringResource(R.plurals.warn_lessons_word, cu.lessons), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Space.s))
        }
        val until = cu.until
        if (cu.feasible && until != null) {
            Text(stringResource(R.string.warn_catch_up_plan, (report.plan.pace + boost).plain(), until.dayOfWeek.short(), until.fmtShort()), style = MaterialTheme.typography.bodyMedium)
        } else {
            WarningBanner(stringResource(R.string.warn_infeasible, report.teaching), stringResource(R.string.warn_bigger_step), { choice = (choice + 1).coerceAtMost(BOOSTS.lastIndex) })
        }
        PreviewLines(preview)
        Hint(stringResource(R.string.warn_writes_custom))
    }
}

/**
 * Bringing a book back in step: over how many lessons (2, 4 or 6) its backlog goes, with a preview of that book's
 * pages in the first lessons. Applying it writes them into the custom plan.
 */
@Composable
fun FoldSheet(report: GroupReport, lag: BookLag, onApply: (Int) -> Unit, onDismiss: () -> Unit) {
    val book = report.plan.books.firstOrNull { it.id == lag.bookId } ?: return
    val spans = listOf(2, 4, 6)
    var choice by rememberSaveable(report.group.id, lag.bookId) { mutableIntStateOf(1) }
    val over = spans[choice]
    val preview = remember(report, lag.bookId, over) { previewOf(report, report.foldBacklog(lag.bookId, over)?.let { report.group.copy(custom = it) }, lag.bookId) }
    SheetFrame(stringResource(R.string.warn_fold_title, book.name), stringResource(R.string.warn_apply_fold), preview.isNotEmpty(), { onApply(over) }, onDismiss) {
        Text(stringResource(R.string.warn_book_behind, book.name, lag.lessonsBehind.fixed2()), style = MaterialTheme.typography.bodyLarge)
        SegmentedToggle(spans.map { pluralStringResource(R.plurals.warn_over, it, it) }, choice, { choice = it })
        PreviewLines(preview)
        Hint(stringResource(R.string.warn_writes_custom))
    }
}

/** The first lessons of a catch-up as "#3 Mon 8 Mar · SB 5–8 · WB 3", optionally for one book only. */
private fun previewOf(report: GroupReport, group: gr.edusuite.pacer.model.Group?, bookId: String? = null): List<String> {
    if (group == null) return emptyList()
    val plan = report.plan
    val rows = GroupReport(plan, group, Source.CUSTOM).rows.filter { !it.blocked }.take(PREVIEW_ROWS)
    return rows.map { r ->
        val cells = plan.books.zip(r.pages).filter { (b, p) -> p.isNotEmpty() && (bookId == null || b.id == bookId) }
            .joinToString(" · ") { (b, p) -> "${b.name} ${collapse(p)}" }
        "${r.date.dayOfWeek.short()} ${r.date.fmtShort()} · ${cells.ifEmpty { collapse(emptyList()) }}"
    }
}

@Composable
private fun PreviewLines(lines: List<String>) {
    if (lines.isEmpty()) return
    Section(stringResource(R.string.warn_first_lessons)) {
        lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium.merge(Figures)) }
    }
}

private const val PREVIEW_ROWS = 4
