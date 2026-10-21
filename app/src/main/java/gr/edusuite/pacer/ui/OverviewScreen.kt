package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import gr.edusuite.pacer.R
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Verdict
import gr.edusuite.pacer.engine.WEEK
import gr.edusuite.pacer.engine.fixed2
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.plain
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.engine.verdict
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

/** How every group in the year stands, class by class, with the year's problems first. */
@Composable
fun OverviewScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues) {
    val problems = remember(year) { Nav.problems(year) }
    val classes = remember(year) {
        year.classes.map { c -> val plan = year.planFor(c); c to plan.groups.map { GroupReport(plan, it) } }
    }

    ScreenList(padding) {
        items(problems) { p ->
            // A class's problem opens that class, or its group for a custom plan's; the rest open the screen that fixes them.
            val fix: (() -> Unit)? = p.classId?.let { id ->
                { (p.fix as? Screen.Group)?.let { vm.openGroup(id, it.id) } ?: vm.openClass(id) }
            } ?: p.fix?.let { f -> { vm.open(f) } }
            Box(Modifier.animateItem()) { ErrorBox(p.text, onFix = fix) }
        }
        if (classes.isEmpty()) item(key = "empty") {
            EmptyState(AppIcons.Add, stringResource(R.string.overview_no_classes), stringResource(R.string.overview_empty_hint), stringResource(R.string.overview_open_classes), { vm.open(Screen.Classes) })
        }
        classes.forEach { (c, reports) ->
            item(key = "class:${c.id}") {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Eyebrow(c.name.ifBlank { stringResource(R.string.overview_class) })
                    if (reports.isEmpty()) Hint(stringResource(R.string.overview_no_groups))
                    else TileGrid(reports) { r, tileModifier ->
                        GroupCard(r, tileModifier) { vm.openGroup(c.id, r.group.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(report: GroupReport, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val plan = report.plan
    val g = report.group
    val verdict = remember(report) { report.verdict() }
    val next = report.rows.firstOrNull()

    val color = when (verdict) {
        is Verdict.Short -> Pacer.colors.tileApricot
        is Verdict.Fits -> Pacer.colors.tileMint
        null -> Pacer.colors.surfaceContainer
    }
    Tile(color, modifier = modifier.semantics(mergeDescendants = true) {}, onClick = onOpen) {
        AppIcon(AppIcons.Groups)
        when (verdict) {
            is Verdict.Fits -> Tag(stringResource(R.string.overview_on_track), onTile = true)
            is Verdict.Short -> Tag(pluralStringResource(R.plurals.overview_short_chip, if (verdict.by == 1.0) 1 else 2, verdict.by.fixed2()), warn = true, onTile = true)
            null -> if (report.teaching > 0 && plan.pace <= 0) Text(stringResource(R.string.overview_no_pace), style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
        }
        Text(g.name.ifBlank { stringResource(R.string.overview_group) }, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(WEEK.filter { it in g.days }.joinToString(" · ") { it.short() }.ifEmpty { stringResource(R.string.overview_no_days) }, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
        TileGrid(listOf(stringResource(R.string.overview_lessons_left) to "${report.lessons.size}", stringResource(R.string.overview_pages_left) to "${report.pages}", stringResource(R.string.overview_pace_needed) to if (report.teaching > 0) report.rate.fixed2() else "—")) { (label, value), modifier ->
            Column(modifier.padding(vertical = Space.s), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
                AnimatedFigure(value, style = MaterialTheme.typography.titleLarge)
            }
        }
        when (verdict) {
            is Verdict.Short -> Text(pluralStringResource(R.plurals.overview_short, if (verdict.by == 1.0) 1 else 2, verdict.by.fixed2(), verdict.sustain.plain()), style = MaterialTheme.typography.bodyMedium)
            is Verdict.Fits -> Text(pluralStringResource(R.plurals.overview_fits, if (verdict.spare == 1.0) 1 else 2, verdict.spare.fixed2()), style = MaterialTheme.typography.bodyMedium)
            null -> if (report.teaching > 0 && plan.pace <= 0) Text(stringResource(R.string.overview_set_pace), style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
        }
        if (next != null) {
            val what = if (next.blocked) next.events.joinToString(" · ") { it.name }
            else plan.books.zip(next.cells).filter { it.second.isNotEmpty() }.joinToString(" · ") { (b, c) -> "${b.name} $c" }.ifEmpty { stringResource(R.string.overview_no_pages) }
            Text(stringResource(R.string.overview_next, next.date.dayOfWeek.short(), next.date.fmtShort(), what), style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
        } else Text(stringResource(R.string.overview_no_lessons), style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
    }
}
