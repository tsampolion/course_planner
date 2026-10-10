package com.coursebook.pacer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coursebook.pacer.engine.GroupReport
import com.coursebook.pacer.engine.Row as LessonRow
import com.coursebook.pacer.engine.Verdict
import com.coursebook.pacer.engine.WEEK
import com.coursebook.pacer.engine.fixed2
import com.coursebook.pacer.engine.fmt
import com.coursebook.pacer.engine.fmtShort
import com.coursebook.pacer.engine.monthShort
import com.coursebook.pacer.engine.plain
import com.coursebook.pacer.engine.position
import com.coursebook.pacer.engine.short
import com.coursebook.pacer.engine.verdict
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.ui.theme.Pacer
import java.time.LocalDate
import java.util.Locale

/** One schedule line: a month heading or a lesson. */
private sealed interface Line {
    data class Month(val first: LocalDate) : Line
    data class Lesson(val row: LessonRow) : Line
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupScreen(report: GroupReport, vm: PacerViewModel, padding: PaddingValues, onCopy: () -> Unit, onShare: () -> Unit) {
    val plan = report.plan
    val lines = remember(report) {
        val out = ArrayList<Line>(report.rows.size + 12)
        var month: LocalDate? = null
        for (r in report.rows) {
            val m = r.date.withDayOfMonth(1)
            if (m != month) { month = m; out += Line.Month(m) }
            out += Line.Lesson(r)
        }
        out
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 32.dp),
    ) {
        item(key = "summary") { SummaryCard(report, vm, Modifier.padding(horizontal = 16.dp)) }
        item(key = "ledgerhead") {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 22.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${report.group.name} · schedule", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f))
                TextButton(onClick = onCopy, enabled = report.rows.isNotEmpty()) { Text("Copy") }
                TextButton(onClick = onShare, enabled = report.rows.isNotEmpty()) { Text("Share") }
            }
        }
        stickyHeader(key = "cols") { HeaderRow(plan) }
        if (lines.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.padding(16.dp)) { Hint("No lessons left in this range.") }
            }
        }
        items(lines, key = { l -> if (l is Line.Lesson) "r${l.row.n}" else "m${(l as Line.Month).first}" }, contentType = { it::class }) { l ->
            when (l) {
                is Line.Month -> MonthRow(l.first)
                is Line.Lesson -> LessonLine(l.row)
            }
        }
    }
}

private val NumW = 34.dp
private val DateW = 92.dp

@Composable
private fun HeaderRow(plan: Plan) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            HeadCell("#", Modifier.width(NumW))
            HeadCell("Date", Modifier.width(DateW))
            plan.books.forEach { HeadCell(it.name, Modifier.weight(1f)) }
        }
        HorizontalDivider(color = Pacer.colors.line)
    }
}

@Composable
private fun HeadCell(text: String, modifier: Modifier) {
    Text(
        text.uppercase(), modifier = modifier, maxLines = 2, fontSize = 10.sp, letterSpacing = 0.6.sp,
        fontWeight = FontWeight.SemiBold, color = Pacer.colors.muted,
    )
}

@Composable
private fun MonthRow(first: LocalDate) {
    Text(
        "${first.monthShort()} ${first.year}".uppercase(),
        fontSize = 11.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, color = Pacer.colors.muted,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
private fun LessonLine(r: LessonRow) {
    val names = r.events.joinToString(" · ") { it.name }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (r.blocked) Pacer.colors.accentSoft else MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${r.n}", fontFamily = Mono, fontSize = 13.sp, color = Pacer.colors.muted, modifier = Modifier.width(NumW))
            Column(Modifier.width(DateW)) {
                Text(
                    "${r.date.dayOfWeek.short()} ${r.date.fmtShort()}",
                    fontSize = 13.sp,
                )
                if (!r.blocked && names.isNotEmpty()) Tag(names, Modifier.padding(top = 2.dp))
            }
            if (r.blocked) {
                Text(names, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            } else {
                r.cells.forEach { c ->
                    Text(c, fontFamily = Mono, fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }
        }
        HorizontalDivider(color = Pacer.colors.line)
    }
}

@Composable
private fun SummaryCard(report: GroupReport, vm: PacerViewModel, modifier: Modifier) {
    val plan = report.plan
    val g = report.group
    val verdict = remember(report) { report.verdict() }
    val short = verdict is Verdict.Short

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, Pacer.colors.line),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(if (short) Pacer.colors.warn else MaterialTheme.colorScheme.primary))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(g.name, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                    val days = WEEK.filter { it in g.days }.joinToString(" · ") { it.short() }.ifEmpty { "no days set" }
                    val from = if (report.lessons.isNotEmpty()) " · from " + (g.from ?: plan.start)?.fmt() else ""
                    val hours = (report.lessons.size * plan.mins / 60.0)
                    Hint("$days$from · ${String.format(Locale.US, "%.1f", hours)} contact hours")
                }

                Row {
                    Stat("Lessons left", "${report.lessons.size}", Modifier.weight(1f))
                    Stat("Pages left", "${report.pages}", Modifier.weight(1f))
                }
                Row {
                    Stat("Required pace", if (report.teaching > 0) report.rate.fixed2() else "—", Modifier.weight(1f))
                    Stat("Heaviest lesson", "${report.peak}", Modifier.weight(1f))
                }

                if (report.blocked > 0) {
                    Hint(
                        "${report.blocked} ${if (report.blocked == 1) "lesson goes" else "lessons go"} to tests or worksheets, " +
                            "so the pages spread over ${report.teaching}.",
                    )
                }

                when (verdict) {
                    is Verdict.Short -> VerdictBox(
                        "Short by ${verdict.by.fixed2()} lessons at ${verdict.sustain.plain()} pages each. Finishing needs " +
                            "${verdict.needRate.fixed2()} per lesson, or ${verdict.cutPages} fewer pages.",
                        bad = true,
                    )
                    is Verdict.Fits -> VerdictBox("Fits, with ${verdict.spare.fixed2()} lessons to spare at ${verdict.sustain.plain()} pages each.", bad = false)
                    null -> Unit
                }

                if (report.banked > 0) Hint("${report.banked} pages banked as homework, already removed from the sequence.")

                if (plan.books.isNotEmpty()) Positions(report, vm)

                MonthBars(report)
            }
        }
    }
}

@Composable
private fun Stat(k: String, v: String, modifier: Modifier) {
    Column(modifier) {
        Text(k.uppercase(), fontSize = 10.sp, letterSpacing = 0.7.sp, color = Pacer.colors.muted)
        Text(v, fontFamily = Mono, fontSize = 19.sp)
    }
}

@Composable
private fun VerdictBox(text: String, bad: Boolean) {
    val c = Pacer.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(if (bad) c.warnSoft else c.okSoft, RoundedCornerShape(3.dp))) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (bad) c.warn else c.ok))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
    }
}

@Composable
private fun Positions(report: GroupReport, vm: PacerViewModel) {
    val plan = report.plan
    val g = report.group
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider(color = Pacer.colors.line)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BOOK", fontSize = 10.sp, color = Pacer.colors.muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("RESUME", fontSize = 10.sp, color = Pacer.colors.muted, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.width(92.dp))
            Text("LEFT", fontSize = 10.sp, color = Pacer.colors.muted, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
            Text("PACE", fontSize = 10.sp, color = Pacer.colors.muted, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.width(50.dp))
        }
        plan.books.forEachIndexed { i, b ->
            val st = report.bookStats[i]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(b.name, style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.muted)
                    b.due?.let { due -> Tag(if (st.crammed) "due passed" else "by ${due.fmtShort()}", warn = st.crammed) }
                }
                IntField(position(g, b), { vm.setPosition(g.id, b.id, it) }, "p.", Modifier.width(92.dp))
                Text("${st.pages}", fontFamily = Mono, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
                Text(
                    if (st.lessons > 0) st.rate.fixed2() else "—",
                    fontFamily = Mono, textAlign = TextAlign.End, modifier = Modifier.width(50.dp),
                    color = if (b.due != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        OutlinedButton(onClick = { vm.markNextLessonDone(g.id) }, enabled = report.lessons.isNotEmpty()) {
            Text("Mark next lesson done")
        }
    }
}

@Composable
private fun MonthBars(report: GroupReport) {
    val months = report.lessonsByMonth
    if (months.isEmpty()) return
    val max = months.maxOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        months.forEach { (m, n) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${m.monthShort()} ${m.year % 100}", fontFamily = Mono, fontSize = 11.sp, color = Pacer.colors.muted, modifier = Modifier.width(54.dp))
                Box(Modifier.weight(1f).height(9.dp).background(Pacer.colors.accentSoft)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(n.toFloat() / max).background(MaterialTheme.colorScheme.primary))
                }
                Spacer(Modifier.width(7.dp))
                Text("$n", fontFamily = Mono, fontSize = 11.sp, color = Pacer.colors.muted, textAlign = TextAlign.End, modifier = Modifier.width(22.dp))
            }
        }
    }
}
