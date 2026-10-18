package gr.edusuite.pacer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Verdict
import gr.edusuite.pacer.engine.WEEK
import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.eventsOn
import gr.edusuite.pacer.engine.fixed2
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.lessonPages
import gr.edusuite.pacer.engine.monthShort
import gr.edusuite.pacer.engine.plain
import gr.edusuite.pacer.engine.plannedPages
import gr.edusuite.pacer.engine.position
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.engine.verdict
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.ui.theme.Mono
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.launch

/** One lesson as the schedule shows it: taught (from the group's done lessons) or still to come. */
private data class Lesson(
    val n: Int,
    val date: LocalDate,
    val events: List<TestEvent>,
    /** A test takes the whole lesson. */
    val blocked: Boolean,
    val cells: List<String>,
    val done: Boolean,
    /** Recorded with pages other than the planned ones. */
    val edited: Boolean = false,
)

/** One line of the all-months list: a month heading or a lesson. */
private sealed interface Line {
    data class Month(val first: LocalDate) : Line
    data class Item(val lesson: Lesson) : Line
}

/** Done lessons and the lessons still to come, in date order and numbered together. */
private fun lessonsOf(report: GroupReport): List<Lesson> {
    val plan = report.plan
    val g = report.group
    val done = g.done.map { d ->
        val events = plan.eventsOn(g, d.date)
        val blocked = d.pages.isEmpty() && events.any { it.consumes }
        Lesson(0, d.date, events, blocked, plan.books.map { b -> if (blocked) "" else collapse(d.pages[b.id].orEmpty()) }, done = true, edited = d.edited)
    }
    val upcoming = report.rows.map { r -> Lesson(0, r.date, r.events, r.blocked, r.cells, done = false) }
    return (done + upcoming).sortedBy { it.date }.mapIndexed { i, l -> l.copy(n = i + 1) }
}

/** What a long-press on a lesson offers, and what tapping its tick does. */
private class LessonActions(
    val toggle: (LocalDate) -> Unit,
    /** Opens "Pages taught" for the lesson. */
    val edit: (Lesson) -> Unit,
    val upTo: (LocalDate) -> Unit,
    val notFrom: (LocalDate) -> Unit,
    /** Some lesson on or before this date is not done yet. */
    val canUpTo: (LocalDate) -> Boolean,
    /** Some lesson on or after this date is done. */
    val canNotFrom: (LocalDate) -> Boolean,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupScreen(
    report: GroupReport,
    byMonth: Boolean,
    vm: PacerViewModel,
    padding: PaddingValues,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    val plan = report.plan
    val g = report.group
    val lessons = remember(report) { lessonsOf(report) }
    val months = remember(lessons) { lessons.groupBy { it.date.withDayOfMonth(1) }.toList() }
    val lines = remember(months) {
        months.flatMap { (m, ls) -> listOf<Line>(Line.Month(m)) + ls.map { Line.Item(it) } }
    }
    var editing by remember(g.id) { mutableStateOf<Lesson?>(null) }
    val actions = remember(lessons, g.id) {
        LessonActions(
            toggle = { vm.toggleLesson(g.id, it) },
            edit = { editing = it },
            upTo = { vm.markDoneUpTo(g.id, it) },
            notFrom = { vm.unmarkDoneFrom(g.id, it) },
            canUpTo = { d -> lessons.any { !it.done && !it.date.isAfter(d) } },
            canNotFrom = { d -> lessons.any { it.done && !it.date.isBefore(d) } },
        )
    }
    // By month opens on this month, or on the first month with a lesson still to come.
    val startPage = remember(months) {
        val now = LocalDate.now().withDayOfMonth(1)
        months.indexOfFirst { it.first == now }.takeIf { it >= 0 }
            ?: months.indexOfFirst { (_, ls) -> ls.any { !it.done } }.takeIf { it >= 0 }
            ?: 0
    }
    val pager = rememberPagerState(initialPage = startPage) { months.size }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(top = padding.calculateTopPadding() + Space.m, bottom = padding.calculateBottomPadding() + Space.xxl),
    ) {
        item(key = "summary") { SummaryCard(report, vm, Modifier.padding(horizontal = Space.l)) }
        item(key = "ledgerhead") {
            Row(
                Modifier.fillMaxWidth().padding(start = Space.l, end = Space.s, top = Space.xl, bottom = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${g.name} · schedule", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onCopy, enabled = report.rows.isNotEmpty()) { Text("Copy") }
                TextButton(onClick = onShare, enabled = report.rows.isNotEmpty()) { Text("Share") }
            }
        }
        item(key = "mode") {
            Row(Modifier.padding(horizontal = Space.l), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                FilterChip(selected = !byMonth, onClick = { vm.setScheduleByMonth(false) }, label = { Text("All months") })
                FilterChip(selected = byMonth, onClick = { vm.setScheduleByMonth(true) }, label = { Text("By month") })
            }
        }
        item(key = "tickhint") {
            Column(Modifier.padding(start = Space.l, end = Space.l, top = Space.s, bottom = Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Note(SCHEDULE_NOTE)
                Hint(
                    "Tick a lesson once it's taught. Long-press a lesson to change the pages it covered, " +
                        "to mark everything up to it, or to undo from it.",
                )
                if (plan.books.isEmpty()) {
                    GuideCard("Add this class's books", "Each lesson shows its pages once the class has books. Add them on the class screen.")
                }
            }
        }
        if (lessons.isEmpty()) {
            item(key = "empty") { Box(Modifier.padding(Space.l)) { Hint("No lessons left in this range.") } }
        } else if (byMonth) {
            stickyHeader(key = "tabs") {
                Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                    ScrollableTabRow(
                        selectedTabIndex = pager.currentPage.coerceIn(0, months.lastIndex),
                        edgePadding = Space.m,
                        containerColor = MaterialTheme.colorScheme.background,
                    ) {
                        months.forEachIndexed { i, (m, _) ->
                            Tab(
                                selected = pager.currentPage == i,
                                onClick = { scope.launch { pager.animateScrollToPage(i) } },
                                text = { Text("${m.monthShort()} ${m.year % 100}") },
                            )
                        }
                    }
                    HeaderRow(plan)
                }
            }
            item(key = "pager") {
                // A month holds a few dozen lessons at most, so each page is a plain column.
                HorizontalPager(state = pager, verticalAlignment = Alignment.Top, key = { months[it].first.toString() }) { page ->
                    Column(Modifier.fillMaxWidth()) {
                        months[page].second.forEach { LessonLine(it, actions) }
                    }
                }
            }
        } else {
            stickyHeader(key = "cols") { HeaderRow(plan) }
            items(lines, key = { l -> if (l is Line.Item) "r${l.lesson.date}" else "m${(l as Line.Month).first}" }, contentType = { it::class }) { l ->
                when (l) {
                    is Line.Month -> MonthRow(l.first)
                    is Line.Item -> LessonLine(l.lesson, actions)
                }
            }
        }
    }

    editing?.let { l -> PagesTaughtDialog(report, l, { vm.recordLesson(g.id, l.date, it) }, { editing = null }) }
}

private val TickW = 40.dp
private val NumW = 30.dp
private val DateW = 92.dp

@Composable
private fun HeaderRow(plan: Plan) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(start = Space.xs, end = Space.m, top = Space.s, bottom = Space.s)) {
            HeadCell("✓", Modifier.width(TickW), center = true)
            HeadCell("#", Modifier.width(NumW))
            HeadCell("Date", Modifier.width(DateW))
            plan.books.forEach { HeadCell(it.name, Modifier.weight(1f)) }
        }
        HorizontalDivider(color = Pacer.colors.line)
    }
}

@Composable
private fun HeadCell(text: String, modifier: Modifier, center: Boolean = false) {
    Text(
        text, modifier = modifier, maxLines = 2, style = MaterialTheme.typography.labelSmall,
        color = Pacer.colors.muted, textAlign = if (center) TextAlign.Center else TextAlign.Start,
    )
}

@Composable
private fun MonthRow(first: LocalDate) {
    Text(
        "${first.monthShort()} ${first.year}",
        style = MaterialTheme.typography.labelMedium, color = Pacer.colors.muted,
        modifier = Modifier.fillMaxWidth().background(Pacer.colors.surfaceContainer).padding(horizontal = Space.m, vertical = Space.xs),
    )
}

/** A lesson: tick it on its own, or long-press for "Done up to here" and "Not done from here". */
@Composable
private fun LessonLine(l: Lesson, actions: LessonActions) {
    val names = l.events.joinToString(" · ") { it.name }
    var menu by remember { mutableStateOf(false) }
    val edit = "Edit pages taught…"
    val upTo = "Done up to here"
    val notFrom = "Not done from here"
    Box {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (l.blocked) Pacer.colors.accentSoft else Pacer.colors.surface)
                    .pointerInput(l.date) { detectTapGestures(onLongPress = { menu = true }) }
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(edit) { actions.edit(l); true },
                            CustomAccessibilityAction(upTo) { actions.upTo(l.date); true },
                            CustomAccessibilityAction(notFrom) { actions.notFrom(l.date); true },
                        )
                    }
                    .padding(start = Space.xs, end = Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(TickW), contentAlignment = Alignment.Center) {
                    Checkbox(
                        checked = l.done,
                        onCheckedChange = { actions.toggle(l.date) },
                        modifier = Modifier.semantics { contentDescription = "Lesson ${l.n} done" },
                    )
                }
                // Done lessons stay in the list, dimmed, with the pages they covered.
                Row(Modifier.weight(1f).alpha(if (l.done) 0.5f else 1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("${l.n}", style = MaterialTheme.typography.bodyMedium, fontFamily = Mono, color = Pacer.colors.muted, modifier = Modifier.width(NumW))
                    Column(Modifier.width(DateW)) {
                        Text("${l.date.dayOfWeek.short()} ${l.date.fmtShort()}", style = MaterialTheme.typography.bodyMedium)
                        if (!l.blocked && names.isNotEmpty()) Tag(names, Modifier.padding(top = 2.dp))
                        if (l.edited) Tag("edited", Modifier.padding(top = 2.dp))
                    }
                    if (l.blocked) {
                        Text(names, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    } else {
                        l.cells.forEach { c -> Text(c, style = MaterialTheme.typography.bodyMedium, fontFamily = Mono, modifier = Modifier.weight(1f)) }
                    }
                }
            }
            HorizontalDivider(color = Pacer.colors.line)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(edit) }, onClick = { menu = false; actions.edit(l) })
            DropdownMenuItem(text = { Text(upTo) }, enabled = actions.canUpTo(l.date), onClick = { menu = false; actions.upTo(l.date) })
            DropdownMenuItem(text = { Text(notFrom) }, enabled = actions.canNotFrom(l.date), onClick = { menu = false; actions.notFrom(l.date) })
        }
    }
}

/**
 * The pages a lesson covered, one field per book, prefilled with what it covered or is planned to.
 * Saving marks it done with those pages; the pages still to teach re-spread over the lessons left.
 */
@Composable
private fun PagesTaughtDialog(report: GroupReport, l: Lesson, onSave: (Map<String, List<Int>>) -> Unit, onDismiss: () -> Unit) {
    val books = report.plan.books
    val planned = remember(report, l.date) { report.plannedPages(l.date).orEmpty() }
    val texts = remember(report.group.id, l.date) {
        val now = report.group.done.firstOrNull { it.date == l.date }?.pages ?: planned
        mutableStateListOf(*books.map { b -> now[b.id]?.let(::collapse).orEmpty() }.toTypedArray())
    }
    val parsed = books.mapIndexed { i, b -> lessonPages(texts[i], b) }
    AddDialogFrame(
        title = "Lesson ${l.n} · ${l.date.dayOfWeek.short()} ${l.date.fmtShort()}",
        canAdd = parsed.all { it != null },
        onAdd = { onSave(books.zip(parsed).associate { (b, p) -> b.id to p.orEmpty() }) },
        onDismiss = onDismiss,
        confirm = "Save",
    ) {
        Hint(
            "Pages taught from each book, like 5-8, 10. Leave a book empty if you taught none. " +
                "Pages left out go back into the schedule, and extra pages come off later lessons." +
                if (l.done) "" else " Saving marks the lesson done.",
        )
        books.forEachIndexed { i, b ->
            val bad = parsed[i] == null
            OutlinedTextField(
                value = texts[i],
                onValueChange = { texts[i] = it },
                label = { Text(b.name) },
                placeholder = { Text("none") },
                singleLine = true,
                isError = bad,
                supportingText = {
                    Text(if (bad) "Use pages ${b.first}–${b.last}, like 5-8, 10" else "Planned: ${collapse(planned[b.id].orEmpty())}")
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = Mono),
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
        shape = MaterialTheme.shapes.medium,
        color = Pacer.colors.surface,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(if (short) Pacer.colors.warn else MaterialTheme.colorScheme.primary))
            Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                Column {
                    Text(g.name, style = MaterialTheme.typography.titleLarge)
                    val days = WEEK.filter { it in g.days }.joinToString(" · ") { it.short() }.ifEmpty { "no days set" }
                    // Lessons marked done drop out, so the first one left is the next to teach.
                    val from = report.lessons.firstOrNull()?.let { " · next " + it.fmt() } ?: ""
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
fun Stat(k: String, v: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(k, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted)
        // A stat tile's figure (§6): mono in titleLarge, which is already SemiBold.
        Text(v, style = MaterialTheme.typography.titleLarge, fontFamily = Mono)
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
    val plan = report.plan
    val g = report.group
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        HorizontalDivider(color = Pacer.colors.line)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Book", style = MaterialTheme.typography.labelSmall, color = Pacer.colors.muted, modifier = Modifier.weight(1f))
            Text("Resume", style = MaterialTheme.typography.labelSmall, color = Pacer.colors.muted, textAlign = TextAlign.End, modifier = Modifier.width(92.dp))
            Text("Left", style = MaterialTheme.typography.labelSmall, color = Pacer.colors.muted, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
            Text("Pace", style = MaterialTheme.typography.labelSmall, color = Pacer.colors.muted, textAlign = TextAlign.End, modifier = Modifier.width(50.dp))
        }
        plan.books.forEachIndexed { i, b ->
            val st = report.bookStats[i]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(b.name, style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.muted)
                    b.due?.let { due -> Tag(if (st.crammed) "due passed" else "by ${due.fmtShort()}", warn = st.crammed) }
                }
                IntField(position(g, b), { vm.setPosition(g.id, b.id, it) }, "p.", Modifier.width(92.dp), min = b.first, max = MAX_PAGE + 1)
                Text("${st.pages}", fontFamily = Mono, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
                Text(
                    if (st.lessons > 0) st.rate.fixed2() else "—",
                    fontFamily = Mono, textAlign = TextAlign.End, modifier = Modifier.width(50.dp),
                    color = if (b.due != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { vm.markNextLessonDone(g.id) }, enabled = report.lessons.isNotEmpty()) {
                Text("Mark next lesson done")
            }
            if (g.done.isNotEmpty()) {
                Hint("${g.done.size} ${if (g.done.size == 1) "lesson" else "lessons"} done", Modifier.padding(start = Space.m))
            }
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
                Box(Modifier.weight(1f).height(9.dp).clip(MaterialTheme.shapes.extraSmall).background(Pacer.colors.accentSoft)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(n.toFloat() / max).background(MaterialTheme.colorScheme.primary))
                }
                Spacer(Modifier.width(Space.s))
                Text("$n", style = MaterialTheme.typography.labelSmall, fontFamily = Mono, color = Pacer.colors.muted, textAlign = TextAlign.End, modifier = Modifier.width(22.dp))
            }
        }
    }
}
