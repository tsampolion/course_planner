package com.coursebook.pacer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coursebook.pacer.engine.GroupReport
import com.coursebook.pacer.engine.Verdict
import com.coursebook.pacer.engine.WEEK
import com.coursebook.pacer.engine.fixed2
import com.coursebook.pacer.engine.fmtShort
import com.coursebook.pacer.engine.plain
import com.coursebook.pacer.engine.short
import com.coursebook.pacer.engine.verdict
import com.coursebook.pacer.model.SchoolYear
import com.coursebook.pacer.ui.theme.Pacer

/** How every group in the year stands, class by class, with the year's problems first. */
@Composable
fun OverviewScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues) {
    val problems = remember(year) { Nav.problems(year) }
    val classes = remember(year) {
        year.classes.map { c -> val plan = year.planFor(c); c to plan.groups.map { GroupReport(plan, it) } }
    }

    ScreenList(padding, spacing = 12.dp) {
        items(problems) { p ->
            // A class's problem opens that class; the rest open the screen that fixes them.
            val fix: (() -> Unit)? = p.classId?.let { id -> { vm.openClass(id) } } ?: p.fix?.let { f -> { vm.open(f) } }
            ErrorBox(p.text, onFix = fix)
        }
        classes.forEach { (c, reports) ->
            item(key = "head:${c.id}") { Heading(c.name.ifBlank { "Class" }) }
            if (reports.isEmpty()) item(key = "none:${c.id}") { Hint("No groups in this class yet.") }
            items(reports, key = { "${c.id}:${it.group.id}" }) { r -> GroupCard(r) { vm.openGroup(c.id, r.group.id) } }
        }
    }
}

@Composable
private fun GroupCard(report: GroupReport, onOpen: () -> Unit) {
    val plan = report.plan
    val g = report.group
    val verdict = remember(report) { report.verdict() }
    val short = verdict is Verdict.Short
    val next = report.rows.firstOrNull()

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, Pacer.colors.line),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(if (short) Pacer.colors.warn else MaterialTheme.colorScheme.primary))
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(g.name.ifBlank { "Group" }, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                        Hint(WEEK.filter { it in g.days }.joinToString(" · ") { it.short() }.ifEmpty { "No days set" })
                    }
                    Text("›", fontSize = 22.sp, color = Pacer.colors.muted)
                }
                Row {
                    Stat("Lessons left", "${report.lessons.size}", Modifier.weight(1f))
                    Stat("Pages left", "${report.pages}", Modifier.weight(1f))
                    Stat("Pace needed", if (report.teaching > 0) report.rate.fixed2() else "—", Modifier.weight(1f))
                }
                when (verdict) {
                    is Verdict.Short -> Text(
                        "Short by ${verdict.by.fixed2()} lessons at ${verdict.sustain.plain()} pages each",
                        style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.warn,
                    )
                    is Verdict.Fits -> Text(
                        "Fits, with ${verdict.spare.fixed2()} lessons to spare",
                        style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.ok,
                    )
                    null -> if (report.teaching > 0 && plan.pace <= 0) Hint("Set the pace you can sustain on the class screen to get a verdict.")
                }
                if (next != null) {
                    val what = if (next.blocked) {
                        next.events.joinToString(" · ") { it.name }
                    } else {
                        plan.books.zip(next.cells).filter { it.second.isNotEmpty() }
                            .joinToString(" · ") { (b, c) -> "${b.name} $c" }
                            .ifEmpty { "No pages" }
                    }
                    Text(
                        "Next: ${next.date.dayOfWeek.short()} ${next.date.fmtShort()} · $what",
                        style = MaterialTheme.typography.bodySmall, fontFamily = Mono,
                    )
                } else {
                    Hint("No lessons left in this term.")
                }
            }
        }
    }
}
