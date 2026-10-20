package gr.edusuite.pacer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Verdict
import gr.edusuite.pacer.engine.fixed2
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.monthShort
import gr.edusuite.pacer.engine.plain
import gr.edusuite.pacer.engine.position
import gr.edusuite.pacer.engine.verdict
import gr.edusuite.pacer.engine.includedPages
import gr.edusuite.pacer.engine.placedPages
import gr.edusuite.pacer.engine.taughtPages
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.ui.theme.Mono
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate
import java.util.Locale

/** The group's figures, away from the plan: tiles, the verdict, details, lessons by month and the books' positions. */
@Composable
fun GroupStats(report: GroupReport, vm: PacerViewModel, padding: PaddingValues) {
    val plan = report.plan
    val verdict = remember(report) { report.verdict() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = Space.l, end = Space.l, top = Space.m, bottom = padding.calculateBottomPadding() + Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        val fontScale = LocalDensity.current.fontScale
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val figures = listOf(stringResource(R.string.group_lessons_left) to "${report.lessons.size}", stringResource(R.string.group_pages_left) to "${report.pages}", stringResource(R.string.group_pace_needed) to if (report.teaching > 0) report.rate.fixed2() else "—")
            val colors = listOf(Pacer.colors.tileSky, Pacer.colors.tileLilac, Pacer.colors.tileMint)
            if (fontScale >= 2f || maxWidth < 360.dp - Space.l * 2) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    figures.forEachIndexed { i, stat -> StatTile(stat.first, stat.second, Modifier.fillMaxWidth(), color = colors[i], unit = if (i == 2) stringResource(R.string.group_pace_unit) else null) }
                }
            } else Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                figures.forEachIndexed { i, stat -> StatTile(stat.first, stat.second, Modifier.weight(1f), color = colors[i], unit = if (i == 2) stringResource(R.string.group_pace_unit) else null) }
            }
        }
        when (verdict) {
            is Verdict.Short -> VerdictBox(stringResource(R.string.group_verdict_short, verdict.by.fixed2(), verdict.sustain.plain(), verdict.needRate.fixed2(), verdict.cutPages), true)
            is Verdict.Fits -> VerdictBox(stringResource(R.string.group_verdict_fits, verdict.spare.fixed2(), verdict.sustain.plain()), false)
            null -> Hint(if (report.teaching == 0) stringResource(R.string.group_no_measure) else stringResource(R.string.group_set_pace))
        }
        Section(stringResource(R.string.group_details_title)) {
            Stat(stringResource(R.string.group_peak), "${report.peak}")
            Stat(stringResource(R.string.group_hours), String.format(Locale.US, "%.1f", report.lessons.size * plan.mins / 60.0))
            Stat(stringResource(R.string.group_banked), "${report.banked}")
            Stat(stringResource(R.string.group_blocked_lessons), "${report.blocked}")
            if (report.blocked > 0) Hint(pluralStringResource(R.plurals.group_blocked, report.blocked, report.blocked, pluralStringResource(R.plurals.group_teaching_count, report.teaching, report.teaching)))
        }
        if (report.lessonsByMonth.isNotEmpty()) Section(stringResource(R.string.group_by_month_title)) { MonthBars(report) }
        if (plan.books.isNotEmpty()) Positions(report, vm)
    }
}

/**
 * The verdict in one line on the plan: "On track", or how short the group is, in warn colours.
 * Tapping it opens the stats. Nothing shows when there is nothing to measure.
 */
@Composable
fun StatusChip(report: GroupReport, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val verdict = remember(report) { report.verdict() } ?: return
    val bad = verdict is Verdict.Short
    val c = Pacer.colors
    val text = when (verdict) {
        is Verdict.Short -> stringResource(R.string.group_status_short, verdict.by.fixed2())
        is Verdict.Fits -> stringResource(R.string.group_on_track)
    }
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.heightIn(min = 48.dp).pressFeedback(interaction),
        shape = CircleShape,
        color = if (bad) c.warnSoft else c.okSoft,
        contentColor = if (bad) c.onWarnSoft else c.onOkSoft,
    ) {
        Row(
            Modifier.clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClickLabel = stringResource(R.string.group_open_stats), onClick = onOpen)
                .padding(start = Space.m, end = Space.s, top = Space.s, bottom = Space.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Box(Modifier.width(Space.s).height(Space.s).background(if (bad) c.warn else c.ok, CircleShape))
            Text(text, style = MaterialTheme.typography.labelLarge)
            AppIcon(AppIcons.Forward)
        }
    }
}

@Composable
fun Stat(k: String, v: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(k, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted)
        // A stat tile's figure (§6): mono in titleLarge, which is already SemiBold.
        AnimatedFigure(v, style = MaterialTheme.typography.titleLarge.copy(fontFamily = Mono))
    }
}

@Composable
private fun VerdictBox(text: String, bad: Boolean) {
    val c = Pacer.colors
    // The clip keeps the coloured bar inside the banner's corners.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(MaterialTheme.shapes.medium).background(if (bad) c.warnSoft else c.okSoft)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (bad) c.warn else c.ok))
        Text(
            text, style = MaterialTheme.typography.bodyMedium, color = if (bad) c.onWarnSoft else c.onOkSoft,
            modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s),
        )
    }
}

@Composable
private fun Positions(report: GroupReport, vm: PacerViewModel) {
    var bookId by rememberSaveable(report.group.id) { mutableStateOf<String?>(null) }
    val book = report.plan.books.firstOrNull { it.id == bookId }
    Section(stringResource(R.string.group_books)) {
        report.plan.books.forEachIndexed { i, b ->
            if (b.supplementary) {
                // Placed by hand: how much of it is placed or taught, without a pace or a position to edit.
                val total = remember(b.include) { b.includedPages().size }
                Row(Modifier.fillMaxWidth().padding(vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppIcons.Book)
                    Spacer(Modifier.width(Space.s))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Text(b.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.supp_stats, report.placedPages(b).size, total, report.taughtPages(b).size), style = MaterialTheme.typography.bodyMedium)
                        Tag(stringResource(R.string.supp_tag))
                    }
                }
                if (i < report.plan.books.lastIndex) HorizontalDivider(Modifier.padding(horizontal = Space.l), color = Pacer.colors.line)
                return@forEachIndexed
            }
            val st = report.bookStats[i]
            Row(Modifier.fillMaxWidth().clickable { bookId = b.id }.padding(vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIcons.Book)
                Spacer(Modifier.width(Space.s))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(b.name, style = MaterialTheme.typography.titleMedium)
                    if (b.id == report.plan.lead) Tag(stringResource(R.string.class_leads))
                    Text(pluralStringResource(R.plurals.group_book_summary, st.pages, position(report.group, b), st.pages, if (st.lessons > 0) st.rate.fixed2() else "—"), style = MaterialTheme.typography.bodyMedium)
                    b.due?.let { Tag(if (st.crammed) { if (it.isBefore(report.group.from ?: report.plan.start ?: LocalDate.now())) stringResource(R.string.group_due_passed) else stringResource(R.string.group_no_before_due) } else stringResource(R.string.group_due, it.fmtShort()), warn = st.crammed) }
                }
            }
            if (i < report.plan.books.lastIndex) HorizontalDivider(Modifier.padding(horizontal = Space.l), color = Pacer.colors.line)
        }
    }
    if (book != null) {
        SheetFrame(stringResource(R.string.group_position, book.name), stringResource(R.string.group_done), true, {}, { bookId = null }) {
            Hint(stringResource(R.string.group_position_hint))
            IntField(position(report.group, book), { vm.setPosition(report.group.id, book.id, it) }, stringResource(R.string.group_resume), Modifier.fillMaxWidth(), min = book.first, max = MAX_PAGE + 1)
        }
    }
}

@Composable
private fun MonthBars(report: GroupReport) {
    val months = report.lessonsByMonth
    if (months.isEmpty()) return
    val max = months.maxOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        months.forEach { (m, n) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${m.monthShort()} ${m.year % 100}", style = MaterialTheme.typography.labelSmall, fontFamily = Mono, color = Pacer.colors.muted, modifier = Modifier.width(54.dp))
                Box(Modifier.weight(1f).height(9.dp).clip(CircleShape).background(Pacer.colors.accentSoft)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(n.toFloat() / max).background(MaterialTheme.colorScheme.primary))
                }
                Spacer(Modifier.width(Space.s))
                AnimatedFigure(n.toString(), style = MaterialTheme.typography.labelSmall.copy(fontFamily = Mono), color = Pacer.colors.muted, modifier = Modifier.width(22.dp))
            }
        }
    }
}
