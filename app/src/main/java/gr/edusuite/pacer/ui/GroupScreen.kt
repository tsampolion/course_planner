package gr.edusuite.pacer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Source
import gr.edusuite.pacer.engine.bookLags
import gr.edusuite.pacer.engine.closedDates
import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.coverage
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.includedPages
import gr.edusuite.pacer.engine.lessonPages
import gr.edusuite.pacer.engine.moveLesson
import gr.edusuite.pacer.engine.plannedPages
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.engine.supplementaryBooks
import gr.edusuite.pacer.engine.unplacedPages
import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate
import kotlinx.coroutines.delay

private const val TAB_PLAN = 0
private const val TAB_STATS = 1
/** How long a new custom plan's lessons take to lift in, one after another. */
private const val REVEAL_MS = 1000L

/** The sheets the group's ⋮ menu opens on the screen itself. */
enum class GroupSheet { TERM_WEEKS, TIPS }

/**
 * A group (design decision 8). Plan opens on today's lesson, with the done lessons above it and the lessons to come
 * below, in week sections. Stats holds the figures. The ⋮ menu in the top bar opens [sheet].
 */
@Composable
fun GroupScreen(
    report: GroupReport,
    vm: PacerViewModel,
    padding: PaddingValues,
    undoToast: (String) -> Unit = {},
    /** The Warnings sheet, opened from the top bar's warning button. */
    warningsOpen: Boolean = false,
    onWarningsClose: () -> Unit = {},
    sheet: GroupSheet? = null,
    onSheetClose: () -> Unit = {},
    /** The show-once hints already seen (Settings.seenTips). */
    seenTips: Set<String> = emptySet(),
) {
    val context = LocalContext.current
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
    val today = remember { LocalDate.now() }
    var selectedDay by rememberSaveable(g.id) { mutableStateOf((report.lessons.firstOrNull() ?: today).toString()) }
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
            shortNames = shortBookNames(plan.books.map { it.name }),
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
    // Today's box holds the next lesson. Done lessons, and days off before it, sit above it; the rest follow it.
    val next = lessons.firstOrNull { !it.done }
    val ledger = remember(lessons, plan, g) {
        val entries = (lessons.filter { it.date != next?.date }.map { Entry.Item(it) } + plan.daysOff(g, lessons.mapTo(HashSet()) { it.date }))
            .sortedBy { it.date }
        val (above, below) = entries.partition { e ->
            when (e) {
                is Entry.Item -> e.lesson.done
                is Entry.DayOff -> next == null || e.date.isBefore(next.date)
            }
        }
        weekSections(above, today) { it.date } to weekSections(below, today) { it.date }
    }
    // The list opens on today's box, with what was taught a scroll up.
    val todayIndex = ledger.first.sumOf { it.items.size + 1 }

    // Plan is the main view; the figures wait one tap away on Stats. The plan keeps its scroll position across switches.
    var tab by rememberSaveable(g.id) { mutableIntStateOf(TAB_PLAN) }
    val planList = rememberLazyListState(initialFirstVisibleItemIndex = todayIndex)
    val motion = animationsEnabled()
    Column(Modifier.fillMaxSize().imePadding()) {
        SegmentedToggle(
            listOf(stringResource(R.string.group_tab_plan), stringResource(R.string.group_tab_stats)), tab, { tab = it },
            Modifier.padding(start = Space.l, end = Space.l, top = padding.calculateTopPadding() + Space.s),
        )
        AnimatedContent(tab, Modifier.weight(1f), transitionSpec = { sideways(targetState > initialState, motion) }) { view ->
            if (view == TAB_STATS) GroupStats(report, vm, padding, seenTips) else LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = planList,
                contentPadding = PaddingValues(top = Space.m, bottom = padding.calculateBottomPadding() + Space.xxl),
            ) {
                ledgerWeeks("a", ledger.first, actions)
                item(key = "today") {
                    TodayBox(
                        next, today, actions,
                        onDone = { toggleHaptic(true); vm.markNextLessonDone(g.id); undoToast(context.getString(R.string.group_lesson_done)) },
                        onPagesTaught = { editingDate = next?.date?.toString() },
                        modifier = Modifier.padding(horizontal = Space.l),
                    )
                }
                item(key = "custom") {
                    Column(Modifier.padding(start = Space.l, end = Space.l, top = Space.m), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                        if (custom == null) {
                            if (report.rows.isNotEmpty()) CustomPlanLine({
                                toggleHaptic(true)
                                vm.createCustomPlan(g.id)
                                showMine = true
                                reveal++
                                undoToast(context.getString(R.string.custom_created))
                            })
                        } else {
                            PlanSourceBar(custom, showMine, { showMine = it }, { vm.setCustomActive(g.id, showMine) })
                            if (showMine) PlanOptions(
                                plan.groups.filter { it.id != g.id },
                                onRepace = { vm.repaceFrom(g.id, 0); undoToast(context.getString(R.string.custom_repaced)) },
                                onCopy = { o -> vm.copyCustomPlan(g.id, o.id); undoToast(context.getString(R.string.custom_copied, o.name)) },
                                onDelete = { vm.deleteCustomPlan(g.id); undoToast(context.getString(R.string.custom_deleted)) },
                            )
                        }
                        // Quiet, not a warning: what each supplementary book still has to place.
                        plan.supplementaryBooks.forEach { b ->
                            val left = remember(report, b) { report.unplacedPages(b) }
                            if (left.isNotEmpty()) Hint(stringResource(R.string.supp_left, b.name, collapse(left)))
                        }
                        if (lessons.isNotEmpty()) ShowOnceHint(Hints.LESSON_MENU, seenTips, stringResource(R.string.hint_lesson_menu), vm::markHintSeen)
                        if (plan.books.isEmpty()) {
                            GuideCard(stringResource(R.string.group_add_books), stringResource(R.string.group_add_books_hint))
                        }
                    }
                }
                if (lessons.isEmpty()) {
                    item(key = "empty") { Box(Modifier.padding(Space.l)) { Hint(stringResource(R.string.group_no_range)) } }
                } else ledger.second.forEach { section ->
                    item(key = "wb${section.start}", contentType = "week") { WeekHeader(section, Modifier.animateItem()) }
                    items(section.items, key = { e -> e.key() }, contentType = { e -> if (e is Entry.Item && !e.lesson.blocked) "lesson" else "slim" }) { e ->
                        val ls = (e as? Entry.Item)?.lesson
                        val id = ls?.lessonId
                        val ca = actions.custom
                        if (ls != null && !ls.blocked && ca != null && id != null) {
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
                            ) { grip -> LessonRow(ls, actions, grip) }
                        } else {
                            Box(Modifier.animateItem()) { EntryRow(e, actions) }
                        }
                    }
                }
            }
        }
    }

    when (sheet) {
        GroupSheet.TERM_WEEKS -> {
            val start = plan.start
            val end = plan.end
            HomeSheet(stringResource(R.string.group_term_weeks), onSheetClose) {
                if (start == null || end == null) {
                    Hint(stringResource(R.string.group_no_range))
                    return@HomeSheet
                }
                DayStrip(start, end, LocalDate.parse(selectedDay), lessons.map { it.date }.toSet(), { selectedDay = it.toString() })
                val date = LocalDate.parse(selectedDay)
                val lesson = lessons.firstOrNull { it.date == date }
                if (lesson != null) {
                    EntryRow(Entry.Item(lesson), actions)
                } else {
                    val closure = plan.closedDates(g.id)[date]
                    Hint(if (closure != null) stringResource(R.string.group_day_off, closure) else stringResource(R.string.group_no_on_day, date.fmt()))
                }
            }
        }
        GroupSheet.TIPS -> HomeSheet(stringResource(R.string.group_tips), onSheetClose) {
            Note(stringResource(R.string.guide_schedule_note))
            Hint(stringResource(R.string.group_tick_hint))
            if (custom != null) Hint(stringResource(R.string.custom_hint))
        }
        null -> Unit
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

/** A stable key for a ledger line: the custom plan's lesson, else its date. */
private fun Entry.key(): String = when (this) {
    is Entry.Item -> lesson.lessonId?.let { "p$it" } ?: "r${lesson.date}"
    is Entry.DayOff -> "d$date"
}

/** A ledger line: a lesson on one line, or a slim row for a day off or a test that takes the whole lesson. */
@Composable
private fun EntryRow(e: Entry, actions: LessonActions) {
    if (e is Entry.Item && !e.lesson.blocked) LessonRow(e.lesson, actions) else SlimRow(e, actions)
}

/** Week sections of lines that don't move: a header, then a row per line. [tag] keeps keys apart above and below today. */
private fun LazyListScope.ledgerWeeks(tag: String, weeks: List<WeekSection<Entry>>, actions: LessonActions) {
    weeks.forEach { section ->
        item(key = "w$tag${section.start}", contentType = "week") { WeekHeader(section) }
        items(section.items, key = { e -> e.key() }) { e -> EntryRow(e, actions) }
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

