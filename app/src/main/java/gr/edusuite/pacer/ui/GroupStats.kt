package gr.edusuite.pacer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Verdict
import gr.edusuite.pacer.engine.fixed2
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.includedPages
import gr.edusuite.pacer.engine.leadBook
import gr.edusuite.pacer.engine.monthShort
import gr.edusuite.pacer.engine.placedPages
import gr.edusuite.pacer.engine.plain
import gr.edusuite.pacer.engine.position
import gr.edusuite.pacer.engine.taughtPages
import gr.edusuite.pacer.engine.verdict
import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * The group's figures, away from the plan (design decision 11): the verdict as a heading with its fix, one figure
 * strip, a pace track per book, lessons by month as columns and a plain details list. Every lesson count comes from
 * the verdict through wholeLessons, so the heading reads as the group does on every other screen.
 */

/** "October 2026", as TalkBack reads a month's column. */
private val SPOKEN_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

@Composable
fun GroupStats(report: GroupReport, vm: PacerViewModel, padding: PaddingValues, seenTips: Set<String> = emptySet()) {
    val plan = report.plan
    var bookId by rememberSaveable(report.group.id) { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = Space.l, end = Space.l, top = Space.l, bottom = padding.calculateBottomPadding() + Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        VerdictHeading(report)
        FigureStrip(report)
        if (plan.books.isNotEmpty()) {
            PaceTrack(report) { bookId = it }
            if (plan.leadBook() != null) ShowOnceHint(Hints.PACE_TRACK, seenTips, stringResource(R.string.hint_pace_track), vm::markHintSeen)
        }
        if (report.lessonsByMonth.isNotEmpty()) MonthColumns(report)
        Details(report)
    }
    val book = plan.books.firstOrNull { it.id == bookId }
    if (book != null) {
        SheetFrame(stringResource(R.string.group_position, book.name), stringResource(R.string.group_done), true, {}, { bookId = null }) {
            Hint(stringResource(R.string.group_position_hint))
            IntField(position(report.group, book), { vm.setPosition(report.group.id, book.id, it) }, stringResource(R.string.group_resume), Modifier.fillMaxWidth(), min = book.first, max = MAX_PAGE + 1)
        }
    }
}

/** "Short by 2 lessons" in warn, or "On pace", with one sentence on what to do about it. */
@Composable
private fun VerdictHeading(report: GroupReport) {
    val c = Pacer.colors
    val verdict = remember(report) { report.verdict() }
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        when (verdict) {
            is Verdict.Short -> {
                val short = report.lessonsShort() ?: 0
                Text(pluralStringResource(R.plurals.status_short, short, short), style = MaterialTheme.typography.headlineSmall, color = c.warn, modifier = Modifier.semantics { heading() })
                val more = verdict.pagesMore()
                Text(
                    stringResource(R.string.stats_fix_short, pluralStringResource(R.plurals.stats_more_pages, more, more), pluralStringResource(R.plurals.stats_pages, verdict.cutPages, verdict.cutPages)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is Verdict.Fits -> {
                Text(stringResource(R.string.status_on_pace), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                // Spare lessons round down: only whole lessons are truly spare.
                val spare = (verdict.spare + 1e-9).toInt()
                Text(
                    if (spare > 0) pluralStringResource(R.plurals.stats_fix_fits, spare, spare, verdict.sustain.plain())
                    else stringResource(R.string.stats_fix_exact, verdict.sustain.plain()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            null -> Hint(if (report.teaching == 0) stringResource(R.string.group_no_measure) else stringResource(R.string.group_set_pace))
        }
    }
}

/** Lessons left, Pages left and Pace needed in one divided strip. Pace keeps two decimals, and only here. */
@Composable
private fun FigureStrip(report: GroupReport) {
    val figures = listOf(
        Triple(stringResource(R.string.group_lessons_left), "${report.lessons.size}", null),
        Triple(stringResource(R.string.group_pages_left), "${report.pages}", null),
        Triple(stringResource(R.string.group_pace_needed), if (report.teaching > 0) report.rate.fixed2() else "—", stringResource(R.string.stats_a_lesson)),
    )
    val fontScale = LocalDensity.current.fontScale
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = Pacer.colors.surface) {
        BoxWithConstraints {
            val stacked = fontScale >= 2f || maxWidth < 280.dp
            val cell = @Composable { f: Triple<String, String, String?>, m: Modifier ->
                Column(m.padding(Space.m).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(f.first, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted)
                    AnimatedFigure(f.second, style = MaterialTheme.typography.headlineSmall.merge(Figures))
                    f.third?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted) }
                }
            }
            if (stacked) Column {
                figures.forEachIndexed { i, f ->
                    if (i > 0) HorizontalDivider(color = Pacer.colors.line)
                    cell(f, Modifier.fillMaxWidth())
                }
            } else Row(Modifier.height(IntrinsicSize.Min)) {
                figures.forEachIndexed { i, f ->
                    if (i > 0) VerticalDivider(color = Pacer.colors.line)
                    cell(f, Modifier.weight(1f))
                }
            }
        }
    }
}

/** A titled part of Stats: a heading, then rows split by hairlines. */
@Composable
private fun StatsPart(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Space.s).semantics { heading() })
        HorizontalDivider(color = Pacer.colors.line)
        content()
    }
}

/**
 * The pace track: a bar per core book showing how far through it the group is, with a tick where it would be in step
 * with the lead book (the engine's followLead rule, [inStepPages]). Supplementary books show what is placed of their
 * pages. Tapping a core book opens its position.
 */
@Composable
private fun PaceTrack(report: GroupReport, onOpen: (String) -> Unit) {
    val plan = report.plan
    val c = Pacer.colors
    val inStep = remember(report) { report.inStepPages() }
    // The books behind are the Warnings sheet's, so both say the same.
    val behind = remember(report) { groupWarnings(report).filterIsInstance<GroupWarning.BookBehind>().associate { it.lag.bookId to it.lessons } }
    val openLabel = stringResource(R.string.stats_set_position)
    StatsPart(stringResource(R.string.stats_pace_track)) {
        plan.books.forEachIndexed { i, b ->
            if (i > 0) HorizontalDivider(color = c.line)
            if (b.supplementary) {
                val total = remember(b.include) { b.includedPages().size }
                val placed = report.placedPages(b).size
                TrackRow(b.name, stringResource(R.string.stats_supplementary), stringResource(R.string.stats_placed, placed, total),
                    if (total > 0) placed.toFloat() / total else 0f, null, c.edgeSupp,
                    pluralStringResource(R.plurals.stats_taught, report.taughtPages(b).size, report.taughtPages(b).size), c.muted)
                return@forEachIndexed
            }
            val all = remember(b) { b.allPages() }
            val pos = position(report.group, b)
            val through = if (all.isEmpty()) 0f else all.count { it < pos }.toFloat() / all.size
            val target = inStep[b.id]
            val tick = target?.let { t -> if (all.isEmpty()) null else all.count { it < t }.toFloat() / all.size }
            val st = report.bookStats[i]
            val late = behind[b.id]
            val under = when {
                late != null && target != null -> pluralStringResource(R.plurals.stats_behind_lead, late, late, target)
                late != null -> pluralStringResource(R.plurals.stats_behind_others, late, late)
                else -> pluralStringResource(R.plurals.stats_left_rate, st.pages, st.pages, if (st.lessons > 0) st.rate.fixed2() else "—")
            }
            val due = b.due?.let { d ->
                if (st.crammed) { if (d.isBefore(report.group.from ?: plan.start ?: LocalDate.now())) stringResource(R.string.group_due_passed) else stringResource(R.string.group_no_before_due) }
                else stringResource(R.string.group_due, d.fmtShort())
            }
            TrackRow(
                b.name, if (b.id == plan.lead) stringResource(R.string.stats_lead_book) else null,
                stringResource(R.string.stats_page_of, pos.coerceAtMost(b.last), b.last), through, tick,
                if (late != null) c.warn else c.accent, listOfNotNull(under, due).joinToString(" · "), if (late != null || st.crammed) c.warn else c.muted,
                Modifier.clickable(onClickLabel = openLabel) { onOpen(b.id) },
            )
        }
    }
}

/** One book on the pace track: its name and tag, where it is, the bar with an optional tick, and a line under it. */
@Composable
private fun TrackRow(name: String, tag: String?, where: String, fraction: Float, tick: Float?, fill: Color, under: String, underColor: Color, modifier: Modifier = Modifier) {
    val c = Pacer.colors
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(vertical = Space.m), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            tag?.let { Text(stringResource(R.string.stats_tag, it), style = MaterialTheme.typography.labelSmall, color = c.muted) }
            Box(Modifier.weight(1f))
            Text(where, style = MaterialTheme.typography.labelMedium.merge(Figures), color = c.muted)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp).clearAndSetSemantics {}, contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(c.surfaceContainer)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(CircleShape).background(fill))
            }
            if (tick != null) Box(Modifier.offset(x = maxWidth * tick.coerceIn(0f, 1f) - 1.5.dp).width(3.dp).fillMaxHeight().background(c.ink, CircleShape))
        }
        Text(under, style = MaterialTheme.typography.bodySmall.merge(Figures), color = underColor)
    }
}

/** Lessons by month as columns, this month in the accent. */
@Composable
private fun MonthColumns(report: GroupReport) {
    val months = report.lessonsByMonth
    val max = months.maxOf { it.second }.coerceAtLeast(1)
    val now = remember { LocalDate.now().withDayOfMonth(1) }
    val c = Pacer.colors
    StatsPart(stringResource(R.string.group_by_month_title)) {
        Row(Modifier.fillMaxWidth().padding(top = Space.m), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            months.forEach { (m, n) ->
                val spoken = pluralStringResource(R.plurals.stats_month_spoken, n, m.format(SPOKEN_MONTH), n)
                Column(Modifier.weight(1f).clearAndSetSemantics { contentDescription = spoken }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    AnimatedFigure("$n", style = MaterialTheme.typography.labelSmall.merge(Figures), color = c.muted)
                    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(n.toFloat() / max).clip(MaterialTheme.shapes.extraSmall).background(if (m == now) c.accent else c.surfaceContainer))
                    }
                    Text(m.monthShort(), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

/** The plain details list: a label on the left, its figure on the right. */
@Composable
private fun Details(report: GroupReport) {
    val plan = report.plan
    StatsPart(stringResource(R.string.group_details_title)) {
        DetailRow(stringResource(R.string.group_peak), pluralStringResource(R.plurals.stats_pages, report.peak, report.peak))
        DetailRow(stringResource(R.string.group_hours), String.format(Locale.US, "%.1f", report.lessons.size * plan.mins / 60.0))
        DetailRow(stringResource(R.string.group_banked), pluralStringResource(R.plurals.stats_pages, report.banked, report.banked))
        DetailRow(stringResource(R.string.group_blocked_lessons), "${report.blocked}")
        if (report.blocked > 0) Hint(
            pluralStringResource(R.plurals.group_blocked, report.blocked, report.blocked, pluralStringResource(R.plurals.group_teaching_count, report.teaching, report.teaching)),
            Modifier.padding(top = Space.s),
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            AnimatedFigure(value, style = MaterialTheme.typography.bodyMedium.merge(Figures))
        }
        HorizontalDivider(color = Pacer.colors.line)
    }
}

