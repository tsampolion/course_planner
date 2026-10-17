package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import gr.edusuite.pacer.ui.theme.Mono
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
            // A class's problem opens that class; the rest open the screen that fixes them.
            val fix: (() -> Unit)? = p.classId?.let { id -> { vm.openClass(id) } } ?: p.fix?.let { f -> { vm.open(f) } }
            ErrorBox(p.text, onFix = fix)
        }
        if (classes.isEmpty()) item(key = "empty") {
            EmptyState("+", "No classes yet", "Add a class to see how its groups are progressing.", "Open classes", { vm.open(Screen.Classes) })
        }
        classes.forEach { (c, reports) ->
            item(key = "class:${c.id}") {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Eyebrow(c.name.ifBlank { "Class" })
                    if (reports.isEmpty()) Hint("No groups in this class yet.")
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
        when (verdict) {
            is Verdict.Fits -> Tag("On track", onTile = true)
            is Verdict.Short -> Tag("Short by ${verdict.by.fixed2()} lessons", warn = true, onTile = true)
            null -> if (report.teaching > 0 && plan.pace <= 0) Text("No pace set", style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
        }
        Text(g.name.ifBlank { "Group" }, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(WEEK.filter { it in g.days }.joinToString(" · ") { it.short() }.ifEmpty { "No days set" }, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
        TileGrid(listOf("Lessons left" to "${report.lessons.size}", "Pages left" to "${report.pages}", "Pace needed" to if (report.teaching > 0) report.rate.fixed2() else "—")) { (label, value), modifier ->
            Column(modifier.padding(vertical = Space.s), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
                Text(value, style = MaterialTheme.typography.titleLarge, fontFamily = Mono)
            }
        }
        when (verdict) {
            is Verdict.Short -> Text("Short by ${verdict.by.fixed2()} lessons at ${verdict.sustain.plain()} pages each", style = MaterialTheme.typography.bodyMedium)
            is Verdict.Fits -> Text("On track · ${verdict.spare.fixed2()} lessons to spare", style = MaterialTheme.typography.bodyMedium)
            null -> if (report.teaching > 0 && plan.pace <= 0) Text("Set the pace you can sustain on the class screen to get a verdict.", style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
        }
        if (next != null) {
            val what = if (next.blocked) next.events.joinToString(" · ") { it.name }
            else plan.books.zip(next.cells).filter { it.second.isNotEmpty() }.joinToString(" · ") { (b, c) -> "${b.name} $c" }.ifEmpty { "No pages" }
            Text("Next: ${next.date.dayOfWeek.short()} ${next.date.fmtShort()} · $what", style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
        } else Text("No lessons left in this term.", style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
    }
}
