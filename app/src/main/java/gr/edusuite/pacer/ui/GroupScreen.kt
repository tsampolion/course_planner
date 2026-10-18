package gr.edusuite.pacer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import gr.edusuite.pacer.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Verdict
import gr.edusuite.pacer.engine.WEEK
import gr.edusuite.pacer.engine.closedDates
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
    val bookNames: List<String>,
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
    undoToast: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val segmentHaptic = rememberSegmentHaptic()
    val plan = report.plan
    val g = report.group
    val lessons = remember(report) { lessonsOf(report) }
    val months = remember(lessons) { lessons.groupBy { it.date.withDayOfMonth(1) }.toList() }
    val lines = remember(months) {
        months.flatMap { (m, ls) -> listOf<Line>(Line.Month(m)) + ls.map { Line.Item(it) } }
    }
    var selectedDay by rememberSaveable(g.id) { mutableStateOf((report.lessons.firstOrNull() ?: LocalDate.now()).toString()) }
    var selectedMonth by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    var editingDate by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    val editing = lessons.firstOrNull { it.date.toString() == editingDate }
    val actions = remember(lessons, g.id, plan.books) {
        LessonActions(
            bookNames = plan.books.map { it.name },
            toggle = { vm.toggleLesson(g.id, it); undoToast(context.getString(R.string.group_updated)) },
            edit = { editingDate = it.date.toString() },
            upTo = { vm.markDoneUpTo(g.id, it); undoToast(context.getString(R.string.group_marked_done)) },
            notFrom = { vm.unmarkDoneFrom(g.id, it); undoToast(context.getString(R.string.group_marked_not_done)) },
            canUpTo = { d -> lessons.any { !it.done && !it.date.isAfter(d) } },
            canNotFrom = { d -> lessons.any { it.done && !it.date.isBefore(d) } },
        )
    }
    // By month opens on this month, or on the first month with a lesson still to come.
    val startPage = remember(months) {
        val now = selectedMonth?.let(LocalDate::parse) ?: LocalDate.now().withDayOfMonth(1)
        months.indexOfFirst { it.first == now }.takeIf { it >= 0 }
            ?: months.indexOfFirst { (_, ls) -> ls.any { !it.done } }.takeIf { it >= 0 }
            ?: 0
    }
    val pager = rememberPagerState(initialPage = startPage) { months.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage, months) {
        months.getOrNull(pager.currentPage)?.let { selectedMonth = it.first.toString() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(top = padding.calculateTopPadding() + Space.m, bottom = padding.calculateBottomPadding() + Space.xxl),
    ) {
        item(key = "summary") {
            SummaryCard(report, vm, Modifier.padding(horizontal = Space.l), undoToast) { next -> editingDate = next.date.toString() }
        }
        item(key = "daystrip") {
            val start = plan.start
            val end = plan.end
            if (start != null && end != null) {
                Column(Modifier.padding(horizontal = Space.l, vertical = Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Eyebrow(stringResource(R.string.group_term_weeks))
                    DayStrip(start, end, LocalDate.parse(selectedDay), lessons.map { it.date }.toSet(), { selectedDay = it.toString() })
                    val lesson = lessons.firstOrNull { it.date.toString() == selectedDay }
                    if (lesson != null) {
                        LessonLine(lesson, actions)
                    } else {
                        val date = LocalDate.parse(selectedDay)
                        val closure = plan.closedDates(g.id)[date]
                        Hint(if (closure != null) stringResource(R.string.group_day_off, closure) else stringResource(R.string.group_no_on_day, date.fmt()))
                    }
                }
            }
        }
        item(key = "ledgerhead") {
            Row(
                Modifier.fillMaxWidth().padding(start = Space.l, end = Space.s, top = Space.xl, bottom = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.group_schedule_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TertiaryButton(stringResource(R.string.group_copy), onCopy, enabled = report.rows.isNotEmpty())
                TertiaryButton(stringResource(R.string.group_share), onShare, enabled = report.rows.isNotEmpty())
            }
        }
        item(key = "mode") {
            Row(Modifier.padding(horizontal = Space.l), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                FilterChip(selected = !byMonth, onClick = { segmentHaptic(); vm.setScheduleByMonth(false) }, label = { Text(stringResource(R.string.group_all_months)) }, shape = CircleShape)
                FilterChip(selected = byMonth, onClick = { segmentHaptic(); vm.setScheduleByMonth(true) }, label = { Text(stringResource(R.string.group_by_month)) }, shape = CircleShape)
            }
        }
        item(key = "tickhint") {
            Column(Modifier.padding(start = Space.l, end = Space.l, top = Space.s, bottom = Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Note(stringResource(R.string.guide_schedule_note))
                Hint(
                    stringResource(R.string.group_tick_hint),
                )
                if (plan.books.isEmpty()) {
                    GuideCard(stringResource(R.string.group_add_books), stringResource(R.string.group_add_books_hint))
                }
            }
        }
        if (lessons.isEmpty()) {
            item(key = "empty") { Box(Modifier.padding(Space.l)) { Hint(stringResource(R.string.group_no_range)) } }
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
                                onClick = { segmentHaptic(); selectedMonth = m.toString(); scope.launch { pager.animateScrollToPage(i) } },
                                text = { Text("${m.monthShort()} ${m.year % 100}") },
                            )
                        }
                    }

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

            items(lines, key = { l -> if (l is Line.Item) "r${l.lesson.date}" else "m${(l as Line.Month).first}" }, contentType = { it::class }) { l ->
                when (l) {
                    is Line.Month -> MonthRow(l.first)
                    is Line.Item -> Box(Modifier.animateItem()) { LessonLine(l.lesson, actions) }
                }
            }
        }
    }

    editing?.let { l -> PagesTaughtDialog(report, l, { vm.recordLesson(g.id, l.date, it); undoToast(context.getString(R.string.group_saved)) }, { editingDate = null }) }
}

private val TickW = 48.dp
@Composable
private fun MonthRow(first: LocalDate) {
    Text(
        "${first.monthShort()} ${first.year}",
        style = MaterialTheme.typography.labelMedium, color = Pacer.colors.muted,
        modifier = Modifier.fillMaxWidth().background(Pacer.colors.surfaceContainer).padding(horizontal = Space.m, vertical = Space.xs),
    )
}

/** A lesson: tick it on its own, or long-press for stringResource(R.string.group_done_up_to) and stringResource(R.string.group_not_done_from). */
@Composable
private fun LessonLine(l: Lesson, actions: LessonActions) {
    val names = l.events.joinToString(" · ") { it.name }
    var menu by rememberSaveable(l.date.toString()) { mutableStateOf(false) }
    val toggleHaptic = rememberToggleHaptic()
    val doneDescription = stringResource(R.string.group_lesson_done_label, l.n)
    val edit = stringResource(R.string.group_edit_pages_menu)
    val upTo = stringResource(R.string.group_done_up_to)
    val notFrom = stringResource(R.string.group_not_done_from)
    Box {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .alpha(if (l.done) 0.6f else 1f)
                    .background(if (l.blocked) Pacer.colors.accentSoft else Pacer.colors.surface)
                    .pointerInput(l.date) { detectTapGestures(onLongPress = { menu = true }) }
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(edit) { actions.edit(l); true },
                            CustomAccessibilityAction(upTo) { if (actions.canUpTo(l.date)) { actions.upTo(l.date); true } else false },
                            CustomAccessibilityAction(notFrom) { if (actions.canNotFrom(l.date)) { actions.notFrom(l.date); true } else false },
                        )
                    }
                    .padding(start = Space.xs, end = Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(AppIcons.Calendar)
                Box(Modifier.width(TickW), contentAlignment = Alignment.Center) {
                    Checkbox(
                        checked = l.done,
                        onCheckedChange = { toggleHaptic(it); actions.toggle(l.date) },
                        modifier = Modifier.semantics { contentDescription = doneDescription },
                    )
                }
                Column(Modifier.weight(1f).padding(vertical = Space.m), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${l.n}", style = MaterialTheme.typography.labelMedium, fontFamily = Mono, color = Pacer.colors.muted)
                        Text("${l.date.dayOfWeek.short()} ${l.date.fmtShort()}", style = MaterialTheme.typography.titleMedium)
                    }
                    if (names.isNotEmpty()) Surface(shape = CircleShape, color = Pacer.colors.tileLilac, contentColor = Pacer.colors.onTile) {
                        Text(names, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = Space.s, vertical = Space.xs))
                    }
                    if (l.edited) Tag(stringResource(R.string.group_edited))
                    if (l.done) Tag(stringResource(R.string.group_done))
                    if (!l.blocked) l.cells.forEachIndexed { i, cell ->
                        Text(stringResource(R.string.group_book_pages, actions.bookNames.getOrNull(i) ?: stringResource(R.string.group_book_number, i + 1), cell), style = MaterialTheme.typography.bodyMedium)
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
    val texts = rememberSaveable(report.group.id, l.date.toString(), saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        val now = report.group.done.firstOrNull { it.date == l.date }?.pages ?: planned
        mutableStateListOf(*books.map { b -> now[b.id]?.let(::collapse).orEmpty() }.toTypedArray())
    }
    val parsed = books.mapIndexed { i, b -> lessonPages(texts[i], b) }
    SheetFrame(
        title = stringResource(R.string.group_pages_title, l.date.dayOfWeek.short(), l.date.fmtShort()),
        enabled = parsed.all { it != null },
        onPrimary = { onSave(books.zip(parsed).associate { (b, p) -> b.id to p.orEmpty() }) },
        onDismiss = onDismiss,
        primary = stringResource(R.string.group_save),
    ) {
        Hint(
            stringResource(R.string.group_pages_hint, if (l.done) "" else stringResource(R.string.group_saving_marks_done)),
        )
        books.forEachIndexed { i, b ->
            val bad = parsed[i] == null
            OutlinedTextField(
                value = texts[i],
                onValueChange = { texts[i] = it },
                label = { Text(b.name) },
                placeholder = { Text(stringResource(R.string.group_none)) },
                singleLine = true,
                isError = bad,
                supportingText = {
                    Text(if (bad) stringResource(R.string.group_page_error, b.first, b.last) else stringResource(R.string.group_planned, collapse(planned[b.id].orEmpty())))
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = Mono),
                modifier = Modifier.fillMaxWidth().fieldFeedback(bad),
            )
        }
    }
}

@Composable
private fun SummaryCard(report: GroupReport, vm: PacerViewModel, modifier: Modifier, undoToast: (String) -> Unit, onEdit: (Lesson) -> Unit) {
    val plan = report.plan
    val g = report.group
    val verdict = remember(report) { report.verdict() }
    val context = LocalContext.current
    val next = lessonsOf(report).firstOrNull { !it.done }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        if (next == null) {
            Tile(Pacer.colors.surfaceContainer, contentColor = Pacer.colors.ink) {
                Text(stringResource(R.string.group_no_term_lessons), style = MaterialTheme.typography.headlineSmall)
            }
        } else Tile(Pacer.colors.accent, contentColor = Pacer.colors.onAccent) {
            Text(stringResource(R.string.group_next_number, next.n), style = MaterialTheme.typography.labelLarge)
            Text("${next.date.dayOfWeek.short()} ${next.date.fmtShort()}", style = MaterialTheme.typography.headlineSmall, fontFamily = Mono)
            if (next.events.isNotEmpty()) Text(next.events.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodyMedium)
            if (!next.blocked) plan.books.forEachIndexed { i, book ->
                Text(stringResource(R.string.group_book_pages, book.name, next.cells.getOrElse(i) { "—" }), style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = { vm.markNextLessonDone(g.id); undoToast(context.getString(R.string.group_lesson_done)) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Pacer.colors.surface, contentColor = Pacer.colors.ink),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.group_mark_done), style = MaterialTheme.typography.labelLarge) }
            TertiaryButton(stringResource(R.string.group_edit_pages), { onEdit(next) }, color = Pacer.colors.onAccent)
        }
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
            is Verdict.Fits -> Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Tag(stringResource(R.string.group_on_track))
                VerdictBox(stringResource(R.string.group_verdict_fits, verdict.spare.fixed2(), verdict.sustain.plain()), false)
            }
            null -> Hint(if (report.teaching == 0) stringResource(R.string.group_no_measure) else stringResource(R.string.group_set_pace))
        }
        MoreOptions(stringResource(R.string.group_details)) {
            Stat(stringResource(R.string.group_peak), "${report.peak}")
            Stat(stringResource(R.string.group_hours), String.format(Locale.US, "%.1f", report.lessons.size * plan.mins / 60.0))
            Stat(stringResource(R.string.group_banked), "${report.banked}")
            Stat(stringResource(R.string.group_blocked_lessons), "${report.blocked}")
            if (report.blocked > 0) Hint(pluralStringResource(R.plurals.group_blocked, report.blocked, report.blocked, pluralStringResource(R.plurals.group_teaching_count, report.teaching, report.teaching)))
            MonthBars(report)
        }
        if (plan.books.isNotEmpty()) Positions(report, vm)
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
            val st = report.bookStats[i]
            Row(Modifier.fillMaxWidth().clickable { bookId = b.id }.padding(vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIcons.Book)
                Spacer(Modifier.width(Space.s))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(b.name, style = MaterialTheme.typography.titleMedium)
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
