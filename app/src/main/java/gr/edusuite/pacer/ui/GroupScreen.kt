package gr.edusuite.pacer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.closedDates
import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.eventsOn
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.lessonPages
import gr.edusuite.pacer.engine.monthShort
import gr.edusuite.pacer.engine.plain
import gr.edusuite.pacer.engine.plannedPages
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.runtime.mutableIntStateOf
import gr.edusuite.pacer.engine.Source
import gr.edusuite.pacer.engine.bookLags
import gr.edusuite.pacer.engine.coverage
import gr.edusuite.pacer.engine.moveLesson
import gr.edusuite.pacer.engine.supplementaryBooks
import gr.edusuite.pacer.engine.includedPages
import gr.edusuite.pacer.engine.unplacedPages
import kotlinx.coroutines.delay

private const val TAB_PLAN = 0
private const val TAB_STATS = 1
/** How long a new custom plan's lessons take to lift in, one after another. */
private const val REVEAL_MS = 1000L

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
    /** The custom plan's note for the lesson. */
    val note: String = "",
    /** The custom plan's lesson on this day, its place in that plan, and whether the teacher changed it by hand. */
    val lessonId: String? = null,
    val index: Int = -1,
    val touched: Boolean = false,
    /** A teaching day the custom plan has no lesson for. */
    val free: Boolean = false,
    /** What the proposed plan teaches that day, when the custom plan shown teaches something else. */
    val proposed: String? = null,
    /** Its place among the teaching days still to come (the custom plan's lesson there, or the one a save would make). */
    val slot: Int = -1,
    /** The supplementary books that count in this planned lesson. */
    val inLesson: Set<String> = emptySet(),
)

/** One line of the all-months list: a month heading or a lesson. */
private sealed interface Line {
    data class Month(val first: LocalDate) : Line
    data class Item(val lesson: Lesson) : Line
}

/**
 * Done lessons and the lessons still to come, in date order and numbered together. With [compare], each lesson of
 * a custom plan that teaches something other than [compare] says what that plan teaches.
 */
private fun lessonsOf(report: GroupReport, compare: GroupReport? = null): List<Lesson> {
    val plan = report.plan
    val g = report.group
    val done = g.done.map { d ->
        val events = plan.eventsOn(g, d.date)
        val blocked = d.pages.isEmpty() && events.any { it.consumes }
        Lesson(0, d.date, events, blocked, plan.books.map { b -> if (blocked) "" else collapse(d.pages[b.id].orEmpty()) }, done = true, edited = d.edited)
    }
    val custom = report.custom
    val places = custom?.lessons?.withIndex()?.associate { (i, l) -> l.id to (i to l.touched) }.orEmpty()
    val counting = custom?.lessons?.associate { it.id to it.inLesson }.orEmpty()
    val other = compare?.rows?.associateBy { it.date }
    var slot = 0
    val upcoming = report.rows.map { r ->
        val place = r.lessonId?.let(places::get)
        val proposed = other?.get(r.date)?.takeIf { custom != null && !r.blocked && it.cells != r.cells }
            ?.let { o -> plan.books.zip(o.cells).joinToString(" · ") { (b, cell) -> "${b.name} $cell" } }
        Lesson(
            0, r.date, r.events, r.blocked, r.cells, done = false, note = r.note, lessonId = r.lessonId,
            index = place?.first ?: -1, touched = place?.second ?: false,
            free = custom != null && r.lessonId == null && !r.blocked, proposed = proposed,
            slot = if (r.blocked) -1 else slot++, inLesson = r.lessonId?.let(counting::get).orEmpty(),
        )
    }
    return (done + upcoming).sortedBy { it.date }.mapIndexed { i, l -> l.copy(n = i + 1) }
}

/** What a long-press on a lesson offers, and what tapping its tick does. */
private class LessonActions(
    val bookNames: List<String>,
    val bookIds: List<String> = emptyList(),
    /** Per book, in the plan's order: a supplementary book, shown only where it has pages. */
    val supplementary: List<Boolean> = emptyList(),
    /** The supplementary books' ids and names, and what "Add <book> pages…" opens; null when there are none or it can't be planned here. */
    val addSupplementary: ((Lesson) -> Unit)? = null,
    val supplementaryBooks: List<Pair<String, String>> = emptyList(),
    val toggle: (LocalDate) -> Unit,
    /** Opens "Pages taught" for the lesson. */
    val edit: (Lesson) -> Unit,
    val upTo: (LocalDate) -> Unit,
    val notFrom: (LocalDate) -> Unit,
    /** Some lesson on or before this date is not done yet. */
    val canUpTo: (LocalDate) -> Boolean,
    /** Some lesson on or after this date is done. */
    val canNotFrom: (LocalDate) -> Boolean,
    /** The lessons shown are the ones being taught, so they can be ticked. A plan shown for comparison can't. */
    val canTick: Boolean = true,
    /** Set while a custom plan is shown: what its lessons' menus offer. */
    val custom: CustomActions? = null,
)

/** What a lesson of a custom plan offers. [last] is the plan's last place. */
private class CustomActions(
    val last: Int,
    val plan: (Lesson) -> Unit,
    val addBefore: (Lesson) -> Unit,
    val move: (Lesson, Int) -> Unit,
    val repace: (Lesson) -> Unit,
    val remove: (Lesson) -> Unit,
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
    /** The Warnings sheet, opened from the top bar's warning button. */
    warningsOpen: Boolean = false,
    onWarningsClose: () -> Unit = {},
) {
    val context = LocalContext.current
    val segmentHaptic = rememberSegmentHaptic()
    val toggleHaptic = rememberToggleHaptic()
    val plan = report.plan
    val g = report.group
    val custom = g.custom
    // With a custom plan the schedule shows either plan; ticking works on the one in use.
    var showMine by rememberSaveable(g.id) { mutableStateOf(true) }
    val shown = remember(report, showMine) { if (custom == null) report else GroupReport(plan, g, if (showMine) Source.CUSTOM else Source.PROPOSED) }
    val compare = remember(report, showMine) { if (custom != null && showMine) GroupReport(plan, g, Source.PROPOSED) else null }
    // While a lesson is dragged, the others are laid out as if it had landed where it would land now.
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragTo by remember { mutableIntStateOf(0) }
    val viewed = remember(shown, dragId, dragTo) {
        val id = dragId
        val c = shown.custom
        if (id == null || c == null) shown else GroupReport(plan, g.copy(custom = c.moveLesson(id, dragTo)), Source.CUSTOM)
    }
    var reveal by remember { mutableIntStateOf(0) }
    LaunchedEffect(reveal) { if (reveal > 0) { delay(REVEAL_MS); reveal = 0 } }
    val canTick = custom == null || showMine == custom.active
    var planning by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    var catchingUp by rememberSaveable(g.id) { mutableStateOf(false) }
    var folding by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    // An active custom plan that fits every page into the term already answers the warnings.
    val handled = remember(report) { report.custom != null && report.coverage()?.ok == true }
    val lags = remember(report, handled) { if (handled) emptyList() else report.bookLags(plan.lead, plan.lagLessons.toDouble()) }
    val lessons = remember(viewed, compare) { lessonsOf(viewed, compare) }
    val months = remember(lessons) { lessons.groupBy { it.date.withDayOfMonth(1) }.toList() }
    val lines = remember(months) {
        months.flatMap { (m, ls) -> listOf<Line>(Line.Month(m)) + ls.map { Line.Item(it) } }
    }
    var selectedDay by rememberSaveable(g.id) { mutableStateOf((report.lessons.firstOrNull() ?: LocalDate.now()).toString()) }
    var selectedMonth by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    var editingDate by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    val editing = lessons.firstOrNull { it.date.toString() == editingDate }
    val customActions = remember(shown, g.id) {
        val c = shown.custom ?: return@remember null
        CustomActions(
            last = c.lessons.lastIndex,
            plan = { l -> planning = if (l.lessonId != null) "L${l.lessonId}" else "N${c.lessons.size}" },
            addBefore = { l -> planning = "N${l.index}" },
            move = { l, to -> vm.movePlannedLesson(g.id, l.lessonId ?: return@CustomActions, to); undoToast(context.getString(R.string.custom_moved)) },
            repace = { l -> vm.repaceFrom(g.id, l.index); undoToast(context.getString(R.string.custom_repaced)) },
            remove = { l -> vm.removePlannedLesson(g.id, l.lessonId ?: return@CustomActions); undoToast(context.getString(R.string.custom_removed)) },
        )
    }
    // Supplementary pages are planned in the custom plan: from it, or from the proposed plan when there is none yet.
    val planSupplementary = plan.supplementaryBooks.isNotEmpty() && (customActions != null || (custom == null && canTick))
    val actions = remember(lessons, g.id, plan.books, canTick, customActions) {
        LessonActions(
            bookNames = plan.books.map { it.name },
            bookIds = plan.books.map { it.id },
            supplementary = plan.books.map { it.supplementary },
            addSupplementary = if (planSupplementary) { l -> planning = l.lessonId?.let { "L$it" } ?: "S${l.slot}" } else null,
            supplementaryBooks = plan.supplementaryBooks.map { it.id to it.name },
            toggle = { vm.toggleLesson(g.id, it); undoToast(context.getString(R.string.group_updated)) },
            edit = { editingDate = it.date.toString() },
            upTo = { vm.markDoneUpTo(g.id, it); undoToast(context.getString(R.string.group_marked_done)) },
            notFrom = { vm.unmarkDoneFrom(g.id, it); undoToast(context.getString(R.string.group_marked_not_done)) },
            canUpTo = { d -> lessons.any { !it.done && !it.date.isAfter(d) } },
            canNotFrom = { d -> lessons.any { it.done && !it.date.isBefore(d) } },
            canTick = canTick,
            custom = customActions,
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

    // Plan is the main view; the figures wait one tap away on Stats. The plan keeps its scroll position across switches.
    var tab by rememberSaveable(g.id) { mutableIntStateOf(TAB_PLAN) }
    val planList = rememberLazyListState()
    val motion = animationsEnabled()
    Column(Modifier.fillMaxSize().imePadding()) {
        SegmentedToggle(
            listOf(stringResource(R.string.group_tab_plan), stringResource(R.string.group_tab_stats)), tab, { tab = it },
            Modifier.padding(start = Space.l, end = Space.l, top = padding.calculateTopPadding() + Space.s),
        )
        AnimatedContent(tab, Modifier.weight(1f), transitionSpec = { sideways(targetState > initialState, motion) }) { view ->
            if (view == TAB_STATS) GroupStats(report, vm, padding) else LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = planList,
                contentPadding = PaddingValues(top = Space.m, bottom = padding.calculateBottomPadding() + Space.xxl),
            ) {
                item(key = "hero") {
                    NextLessonTile(report, vm, Modifier.padding(horizontal = Space.l), undoToast) { next -> editingDate = next.date.toString() }
                }
                item(key = "custom") {
                    Column(Modifier.padding(start = Space.l, end = Space.l, top = Space.m), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                        // Quiet, not a warning: what each supplementary book still has to place.
                        plan.supplementaryBooks.forEach { b ->
                            val left = remember(report, b) { report.unplacedPages(b) }
                            if (left.isNotEmpty()) Hint(stringResource(R.string.supp_left, b.name, collapse(left)))
                        }
                        if (custom == null) {
                            CreatePlanCard({
                                toggleHaptic(true)
                                vm.createCustomPlan(g.id)
                                showMine = true
                                reveal++
                                undoToast(context.getString(R.string.custom_created))
                            })
                        } else {
                            PlanSourceBar(custom, showMine, { showMine = it }, { vm.setCustomActive(g.id, showMine) })
                            val cov = remember(shown) { shown.coverage() }
                            if (showMine && cov != null) CoverageCard(
                                shown, cov, plan.groups.filter { it.id != g.id },
                                onRepace = { vm.repaceFrom(g.id, 0); undoToast(context.getString(R.string.custom_repaced)) },
                                onCopy = { o -> vm.copyCustomPlan(g.id, o.id); undoToast(context.getString(R.string.custom_copied, o.name)) },
                                onDelete = { vm.deleteCustomPlan(g.id); undoToast(context.getString(R.string.custom_deleted)) },
                            )
                        }
                    }
                }
                item(key = "daystrip") {
                    val start = plan.start
                    val end = plan.end
                    if (start != null && end != null) {
                        MoreOptions(stringResource(R.string.group_term_weeks), Modifier.padding(horizontal = Space.l, vertical = Space.xs)) {
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
                    Column(Modifier.padding(start = Space.l, end = Space.l, bottom = Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        MoreOptions(stringResource(R.string.group_tips)) {
                            Note(stringResource(R.string.guide_schedule_note))
                            Hint(stringResource(R.string.group_tick_hint))
                        }
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

                    items(lines, key = { l -> if (l is Line.Item) l.lesson.lessonId?.let { "p$it" } ?: "r${l.lesson.date}" else "m${(l as Line.Month).first}" }, contentType = { it::class }) { l ->
                        when (l) {
                            is Line.Month -> MonthRow(l.first)
                            is Line.Item -> {
                                val ls = l.lesson
                                val id = ls.lessonId
                                val ca = actions.custom
                                if (ca != null && id != null) {
                                    DraggableLesson(
                                        ls.index, ca.last, dragId == id, reveal,
                                        onStart = { dragId = id; dragTo = ls.index },
                                        onTarget = { dragTo = it },
                                        onDrop = { commit ->
                                            val to = dragTo
                                            dragId = null
                                            if (commit && custom?.lessons?.indexOfFirst { it.id == id } != to) {
                                                vm.movePlannedLesson(g.id, id, to)
                                                undoToast(context.getString(R.string.custom_moved))
                                            }
                                        },
                                    ) { grip -> LessonLine(ls, actions, grip) }
                                } else {
                                    Box(Modifier.animateItem()) { LessonLine(ls, actions) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    planning?.takeIf { plan.supplementaryBooks.isNotEmpty() && (it.startsWith("L") || it.startsWith("S")) }?.let { key ->
        // With supplementary books, planning a lesson goes through saveLesson: whole or nothing, one undo step.
        val stored = g.custom
        val slot = if (key.startsWith("L")) stored?.lessons?.indexOfFirst { it.id == key.drop(1) } ?: -1 else key.drop(1).toIntOrNull() ?: -1
        if (slot < 0) return@let
        val lesson = lessons.firstOrNull { !it.done && it.slot == slot }
        LessonPlanSheet(
            title = lesson?.let { stringResource(R.string.custom_sheet_title, it.n, it.date.dayOfWeek.short(), it.date.fmtShort()) } ?: stringResource(R.string.custom_sheet_new),
            report = shown,
            key = key,
            lesson = stored?.lessons?.getOrNull(slot),
            proposed = lesson?.let { (compare ?: report).plannedPages(it.date) },
            onSave = { save ->
                if (vm.saveLesson(g.id, slot, save)) { showMine = true; undoToast(context.getString(R.string.custom_saved)) }
                else undoToast(context.getString(R.string.supp_save_failed))
            },
            onDismiss = { planning = null },
        )
        return@let
    } ?: planning?.let { key ->
        val c = shown.custom ?: return@let
        val id = key.removePrefix("L").takeIf { key.startsWith("L") }
        val at = key.removePrefix("N").toIntOrNull() ?: c.lessons.size
        val planned = id?.let { i -> c.lessons.firstOrNull { it.id == i } }
        if (id != null && planned == null) return@let
        val lesson = id?.let { i -> lessons.firstOrNull { it.lessonId == i } }
        PlannedLessonSheet(
            title = lesson?.let { stringResource(R.string.custom_sheet_title, it.n, it.date.dayOfWeek.short(), it.date.fmtShort()) } ?: stringResource(R.string.custom_sheet_new),
            report = shown,
            key = key,
            pages = planned?.pages.orEmpty(),
            note = planned?.note.orEmpty(),
            proposed = lesson?.let { compare?.plannedPages(it.date) },
            onSave = { pages, note -> vm.savePlannedLesson(g.id, id, at, pages, note); undoToast(context.getString(R.string.custom_saved)) },
            onDismiss = { planning = null },
        )
    }

    if (warningsOpen) {
        val warnings = remember(report) { groupWarnings(report) }
        WarningsSheet(report, warnings, WarningFixes(
            catchUp = { catchingUp = true },
            fold = { folding = it.bookId },
            repace = { vm.repaceFrom(g.id, 0); undoToast(context.getString(R.string.custom_repaced)) },
            fit = { vm.fitCustomPlanToTerm(g.id); undoToast(context.getString(R.string.custom_fitted)) },
            tidy = { vm.tidyCustomPlan(g.id); undoToast(context.getString(R.string.custom_tidied)) },
        ), onWarningsClose)
    }
    if (catchingUp) CatchUpSheet(report, { boost ->
        vm.applyCatchUp(g.id, boost)
        showMine = true
        undoToast(context.getString(R.string.warn_caught_up))
    }, { catchingUp = false })
    folding?.let { id ->
        val lag = lags.firstOrNull { it.bookId == id }
        if (lag == null) folding = null else FoldSheet(report, lag, { over ->
            vm.foldBacklog(g.id, id, over)
            showMine = true
            undoToast(context.getString(R.string.warn_folded, plan.books.firstOrNull { it.id == id }?.name.orEmpty()))
        }, { folding = null })
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

/**
 * A lesson: tick it on its own, or long-press for "Edit pages taught…", "Done up to here" and "Not done from here".
 * A custom plan's lesson also offers planning its pages, adding a lesson before it, moving it, re-pacing from it and
 * removing it, and is dragged by [grip]. Lessons the teacher changed by hand carry an accent edge.
 */
@Composable
private fun LessonLine(l: Lesson, actions: LessonActions, grip: (@Composable (label: String) -> Unit)? = null) {
    val names = l.events.joinToString(" · ") { it.name }
    var menu by rememberSaveable(l.date.toString()) { mutableStateOf(false) }
    val toggleHaptic = rememberToggleHaptic()
    val doneDescription = stringResource(R.string.group_lesson_done_label, l.n)
    val edit = stringResource(R.string.group_edit_pages_menu)
    val upTo = stringResource(R.string.group_done_up_to)
    val notFrom = stringResource(R.string.group_not_done_from)
    val planIt = stringResource(if (l.lessonId != null) R.string.custom_plan_lesson else R.string.custom_add_here)
    val addBefore = stringResource(R.string.custom_add_before)
    val earlier = stringResource(R.string.custom_move_earlier)
    val later = stringResource(R.string.custom_move_later)
    val repace = stringResource(R.string.custom_repace_here)
    val remove = stringResource(R.string.custom_remove)
    val ca = actions.custom?.takeIf { !l.done && !l.blocked && (l.lessonId != null || l.free) }
    val placed = ca != null && l.lessonId != null
    val tick = actions.canTick
    val addSupp = actions.addSupplementary?.takeIf { !l.done && !l.blocked && l.slot >= 0 }
    val hasMenu = tick || ca != null || addSupp != null
    val suppLabels = actions.supplementaryBooks.map { (_, name) -> stringResource(R.string.supp_add_pages, name) }
    Box {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(if (l.blocked) Pacer.colors.accentSoft else Pacer.colors.surface)
                    .pointerInput(l.date, hasMenu) { detectTapGestures(onLongPress = { if (hasMenu) menu = true }) }
                    .semantics {
                        customActions = buildList {
                            if (addSupp != null) suppLabels.forEach { label -> add(CustomAccessibilityAction(label) { addSupp(l); true }) }
                            if (tick) {
                                add(CustomAccessibilityAction(edit) { actions.edit(l); true })
                                add(CustomAccessibilityAction(upTo) { if (actions.canUpTo(l.date)) { actions.upTo(l.date); true } else false })
                                add(CustomAccessibilityAction(notFrom) { if (actions.canNotFrom(l.date)) { actions.notFrom(l.date); true } else false })
                            }
                            if (ca != null) {
                                add(CustomAccessibilityAction(planIt) { ca.plan(l); true })
                                if (placed) {
                                    add(CustomAccessibilityAction(addBefore) { ca.addBefore(l); true })
                                    if (l.index > 0) add(CustomAccessibilityAction(earlier) { ca.move(l, l.index - 1); true })
                                    if (l.index < ca.last) add(CustomAccessibilityAction(later) { ca.move(l, l.index + 1); true })
                                    add(CustomAccessibilityAction(repace) { ca.repace(l); true })
                                    add(CustomAccessibilityAction(remove) { ca.remove(l); true })
                                }
                            }
                        }
                    }
                    .padding(end = if (grip != null) 0.dp else Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The accent edge marks a lesson planned by hand, which re-pacing leaves alone.
                Box(Modifier.width(3.dp).fillMaxHeight().background(if (l.touched && !l.done) Pacer.colors.accent else Color.Transparent))
                Spacer(Modifier.width(Space.xs - 3.dp))
                AppIcon(AppIcons.Calendar)
                Box(Modifier.width(TickW), contentAlignment = Alignment.Center) {
                    Checkbox(
                        checked = l.done,
                        onCheckedChange = { toggleHaptic(it); actions.toggle(l.date) },
                        enabled = tick,
                        modifier = Modifier.semantics { contentDescription = doneDescription },
                    )
                }
                // A done lesson reads in muted text at full contrast, not faded.
                CompositionLocalProvider(LocalContentColor provides if (l.done) Pacer.colors.muted else Pacer.colors.ink) {
                    Column(Modifier.weight(1f).padding(vertical = Space.m), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
                            Text("#${l.n}", style = MaterialTheme.typography.labelMedium.merge(Figures), color = Pacer.colors.muted)
                            Text("${l.date.dayOfWeek.short()} ${l.date.fmtShort()}", style = MaterialTheme.typography.titleMedium)
                        }
                        if (names.isNotEmpty()) Surface(shape = CircleShape, color = Pacer.colors.tileLilac, contentColor = Pacer.colors.onTile) {
                            Text(names, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = Space.s, vertical = Space.xs))
                        }
                        if (l.edited) Tag(stringResource(R.string.group_edited))
                        if (l.done) Tag(stringResource(R.string.group_done))
                        if (l.free) Tag(stringResource(R.string.custom_free))
                        if (l.note.isNotEmpty()) Text(l.note, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                        if (!l.blocked) l.cells.forEachIndexed { i, cell ->
                            val supp = actions.supplementary.getOrElse(i) { false }
                            // A supplementary book shows only where the lesson has its pages.
                            if (supp && cell == collapse(emptyList())) return@forEachIndexed
                            Text(stringResource(R.string.group_book_pages, actions.bookNames.getOrNull(i) ?: stringResource(R.string.group_book_number, i + 1), cell), style = MaterialTheme.typography.bodyMedium)
                            if (supp && !l.done) Tag(stringResource(if (actions.bookIds.getOrNull(i) in l.inLesson) R.string.supp_in_lesson else R.string.supp_on_top))
                        }
                        l.proposed?.let { Hint(stringResource(R.string.custom_proposed_line, it)) }
                    }
                }
                if (grip != null) grip(stringResource(R.string.custom_drag, l.n))
            }
            HorizontalDivider(color = Pacer.colors.line)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (addSupp != null) {
                actions.supplementaryBooks.forEach { (_, name) ->
                    DropdownMenuItem(text = { Text(stringResource(R.string.supp_add_pages, name)) }, onClick = { menu = false; addSupp(l) })
                }
                HorizontalDivider(color = Pacer.colors.line)
            }
            if (ca != null) {
                DropdownMenuItem(text = { Text(planIt) }, onClick = { menu = false; ca.plan(l) })
                if (placed) {
                    DropdownMenuItem(text = { Text(addBefore) }, onClick = { menu = false; ca.addBefore(l) })
                    DropdownMenuItem(text = { Text(earlier) }, enabled = l.index > 0, onClick = { menu = false; ca.move(l, l.index - 1) })
                    DropdownMenuItem(text = { Text(later) }, enabled = l.index < ca.last, onClick = { menu = false; ca.move(l, l.index + 1) })
                    DropdownMenuItem(text = { Text(repace) }, onClick = { menu = false; ca.repace(l) })
                    DropdownMenuItem(text = { Text(remove) }, onClick = { menu = false; ca.remove(l) })
                }
                if (tick) HorizontalDivider(color = Pacer.colors.line)
            }
            if (tick) {
                DropdownMenuItem(text = { Text(edit) }, onClick = { menu = false; actions.edit(l) })
                DropdownMenuItem(text = { Text(upTo) }, enabled = actions.canUpTo(l.date), onClick = { menu = false; actions.upTo(l.date) })
                DropdownMenuItem(text = { Text(notFrom) }, enabled = actions.canNotFrom(l.date), onClick = { menu = false; actions.notFrom(l.date) })
            }
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
    // A supplementary book takes only the pages it includes.
    val parsed = books.mapIndexed { i, b -> lessonPages(texts[i], b)?.takeIf { ps -> !b.supplementary || b.includedPages().containsAll(ps) } }
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
                    Text(if (bad && b.supplementary) stringResource(R.string.supp_page_error) else if (bad) stringResource(R.string.group_page_error, b.first, b.last) else stringResource(R.string.group_planned, collapse(planned[b.id].orEmpty())))
                },
                textStyle = MaterialTheme.typography.bodyLarge.merge(Figures),
                modifier = Modifier.fillMaxWidth().fieldFeedback(bad),
            )
        }
    }
}

/** The next lesson to teach, with Mark done and Edit pages, at the top of the plan. */
@Composable
private fun NextLessonTile(report: GroupReport, vm: PacerViewModel, modifier: Modifier, undoToast: (String) -> Unit, onEdit: (Lesson) -> Unit) {
    val plan = report.plan
    val g = report.group
    val context = LocalContext.current
    val next = remember(report) { lessonsOf(report).firstOrNull { !it.done } }
    if (next == null) {
        Tile(Pacer.colors.surfaceContainer, modifier, contentColor = Pacer.colors.ink) {
            Text(stringResource(R.string.group_no_term_lessons), style = MaterialTheme.typography.headlineSmall)
        }
    } else Tile(Pacer.colors.accent, modifier, contentColor = Pacer.colors.onAccent) {
        Text(stringResource(R.string.group_next_number, next.n), style = MaterialTheme.typography.labelLarge)
        Text("${next.date.dayOfWeek.short()} ${next.date.fmtShort()}", style = MaterialTheme.typography.headlineSmall.merge(Figures))
        if (next.events.isNotEmpty()) Text(next.events.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodyMedium)
        if (next.note.isNotEmpty()) Text(next.note, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
        if (!next.blocked) plan.books.forEachIndexed { i, book ->
            if (book.supplementary && next.cells.getOrNull(i) == collapse(emptyList())) return@forEachIndexed
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
}
