package com.coursebook.pacer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.ui.theme.Pacer

/** How each group stands, with the plan's problems first. */
@Composable
fun SchedulesScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues) {
    val problems = remember(plan) { Nav.problems(plan) }
    val reports = remember(plan) { plan.groups.map { GroupReport(plan, it) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(problems) { p -> ErrorBox(p.text, onFix = if (p.fix == Screen.Home) null else ({ vm.open(p.fix) })) }
        if (reports.isEmpty()) {
            item(key = "noGroups") {
                Section("Get started") {
                    Hint("Add a group for each class you teach from this plan. Its schedule appears here once the term dates and books are in.")
                    OutlinedButton(onClick = { vm.addGroup(); vm.open(Screen.Groups) }) { Text("Add a group") }
                }
            }
        }
        items(reports, key = { it.group.id }) { r -> GroupCard(r) { vm.open(Screen.Group(r.group.id)) } }
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
                    null -> if (report.teaching > 0 && plan.pace <= 0) Hint("Set the pace you can sustain under Term to get a verdict.")
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
