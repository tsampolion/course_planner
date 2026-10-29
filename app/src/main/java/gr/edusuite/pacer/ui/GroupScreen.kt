package gr.edusuite.pacer.ui

import androidx.compose.animation.*
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.Alignment
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.unitPlan
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Source
import gr.edusuite.pacer.engine.bookLags
import gr.edusuite.pacer.engine.closedDates
import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.catchUp
import gr.edusuite.pacer.engine.shortfallCoverage
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
import gr.edusuite.pacer.model.PlanGrouping
import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Rhythm
import gr.edusuite.pacer.ui.theme.Space
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import kotlinx.coroutines.delay

/** How long a new custom plan's lessons take to lift in, one after another. */
private const val REVEAL_MS = 1000L

/** The sheets the group's ⋮ menu opens on the screen itself. */
enum class GroupSheet { TIPS, EXTRA, OWN_PACE, CATCH_UP }

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
    toast: (String) -> Unit = {},
    /** The Warnings sheet, opened from the top bar's warning button. */
    warningsOpen: Boolean = false,
    onWarningsClose: () -> Unit = {},
    sheet: GroupSheet? = null,
    onSheetClose: () -> Unit = {},
    /** The show-once hints already seen (Settings.seenTips). */
    seenTips: Set<String> = emptySet(),
    grouping: PlanGrouping = PlanGrouping.WEEKS,
    page: GroupPage = GroupPage.HOME,
) {
    val context = LocalContext.current
    val insets = groupInsets(padding)
    val bodyPadding = insets.body
    fun changed(didChange: Boolean, message: String) {
        if (didChange) undoToast(message) else toast(context.getString(R.string.toast_nothing_changed))
    }
    val toggleHaptic = rememberToggleHaptic()
    val plan = report.plan
    val g = report.group
    val custom = g.custom
    // With a custom plan the schedule shows either plan; ticking works on the one in use.
    var showMine by rememberSaveable(g.id) { mutableStateOf(true) }
    val shown = remember(report, showMine, page) { groupPageReport(report, page, showMine) }
    val compare = remember(report, showMine, page) { if (page == GroupPage.PLAN && custom != null && showMine) GroupReport(plan, g, Source.PROPOSED) else null }
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
    val canTick = groupPageCanTick(report, page, showMine)
    var planning by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    var catchingUp by rememberSaveable(g.id) { mutableStateOf(false) }
    // Group home's Fix opens the same Warnings sheet as the top bar's warning button.
    var fixOpen by rememberSaveable(g.id) { mutableStateOf(false) }
    var folding by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    // An active custom plan that fits every page into the term already answers the warnings.
    val handled = remember(report) { report.custom != null && report.coverage()?.ok == true && (report.shortfallCoverage()?.complete != false) }
    val lags = remember(report, handled) { if (handled) emptyList() else report.bookLags(plan.lead, plan.lagLessons.toDouble()) }
    val unitWindows = remember(shown) { unitPlan(shown.plan, shown.group)?.windows.orEmpty() }
    val lessons = remember(viewed, compare) {
        val positions = unitLessonPositions(viewed)
        lessonsOf(viewed, compare).map { lesson ->
            val position = positions[lesson.date]
            if (position == null) lesson else lesson.copy(unitIndex = position.unitIndex, unitLesson = position.lesson, unitLessons = position.total)
        }
    }
    val today = rememberToday()
    // The strip starts on the lesson after the one in the box, which the day strip would otherwise repeat.
    var selectedDay by rememberSaveable(g.id) { mutableStateOf((report.lessons.drop(1).firstOrNull() ?: report.lessons.firstOrNull() ?: today).toString()) }
    var extraDate by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    var editingDate by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    var homeworkDate by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    var homeworkId by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    var notTaughtDate by rememberSaveable(g.id) { mutableStateOf<String?>(null) }
    val editing = lessons.firstOrNull { it.date.toString() == editingDate }
    val customActions = remember(shown, g.id) {
        val c = shown.custom ?: return@remember null
        CustomActions(
            last = c.lessons.lastIndex,
            plan = { l -> planning = if (l.lessonId != null) "L${l.lessonId}" else "N${c.lessons.size}" },
            addBefore = { l -> planning = "N${l.index}" },
            move = { l, to -> vm.movePlannedLesson(g.id, l.lessonId ?: return@CustomActions, to); undoToast(context.getString(R.string.custom_moved)) },
            repace = { l -> changed(vm.repaceFrom(g.id, l.index), context.getString(R.string.custom_repaced)) },
            remove = { l -> vm.removePlannedLesson(g.id, l.lessonId ?: return@CustomActions); undoToast(context.getString(R.string.custom_removed)) },
        )
    }
    // Supplementary pages are planned in the custom plan: from it, or from the proposed plan when there is none yet.
    val planSupplementary = plan.supplementaryBooks.isNotEmpty() && (customActions != null || (custom == null && canTick))
    val actions = remember(lessons, g.id, plan.books, canTick, customActions) {
        LessonActions(
            bookNames = plan.books.map { it.name },
            shortNames = classBookLabels(report.classBooks, plan.books),
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
            addExtra = { extraDate = it.toString() },
            editExtra = { extraDate = it.date.toString() },
            deleteExtra = { l -> l.extraId?.let { changed(vm.removeExtra(g.id, it), context.getString(R.string.extra_deleted)) } },
            addHomework = { homeworkDate = it.date.toString(); homeworkId = null },
            editHomework = { homeworkId = it.id; homeworkDate = null },
            deleteHomework = { changed(vm.removeAssignment(g.id, it.id), context.getString(R.string.work_deleted)) },
            notTaught = { notTaughtDate = it.date.toString() },
        )
    }
    // Filter the calendar view before partitioning so headers and the initial list index agree.
    val next = lessons.firstOrNull { !it.done && !it.date.isBefore(today) }
    var monthText by rememberSaveable(g.id) { mutableStateOf(YearMonth.from(next?.date ?: today).toString()) }
    val selectedMonth = YearMonth.parse(monthText)
    var pickMonth by remember { mutableStateOf(false) }
    var earlierOpen by rememberSaveable(g.id) { mutableStateOf(false) }
    fun makeLedger(grouping: PlanGrouping, selectedMonth: YearMonth): Pair<List<Entry>, List<Entry>> {
        val entries = (lessons.filter { it.date != next?.date }.map { Entry.Item(it) } + plan.daysOff(g, lessons.mapTo(HashSet()) { it.date }))
            .sortedBy { it.date }
        val filtered = if (grouping == PlanGrouping.MONTHS) {
            val earlier = entries.filter { e -> when (e) {
                is Entry.Item -> e.lesson.done || e.date.isBefore(today)
                is Entry.DayOff -> e.date.isBefore(next?.date ?: today)
            } }
            val upcoming = entries.filterNot { it in earlier }
            (earlier + filterMonth(upcoming, selectedMonth) { it.date }).sortedBy { it.date }
        } else entries
        return filtered.partition { e ->
            when (e) {
                is Entry.Item -> e.lesson.done || e.date.isBefore(today)
                is Entry.DayOff -> e.date.isBefore(next?.date ?: today)
            }
        }
    }
    val tablet = rememberWindowLayout() != WindowLayout.COMPACT
    val motion = animationsEnabled()
    val largeText = rememberLargeText()
    Column(Modifier.fillMaxSize().imePadding().padding(top = insets.top)) {
        if (page == GroupPage.PLAN) {
            Column(Modifier.padding(horizontal = Space.l), verticalArrangement = Arrangement.spacedBy(Rhythm.inSection)) {
                SegmentedToggle(listOf(stringResource(R.string.plan_weeks), stringResource(R.string.plan_months)), grouping.ordinal, { vm.setPlanGrouping(PlanGrouping.entries[it]) })
            }
        }
        when (page) {
            GroupPage.HOME -> GroupHome(report, next, today, actions, bodyPadding,
                onDone = { next?.let { toggleHaptic(true); vm.toggleLesson(g.id, it.date); undoToast(context.getString(R.string.group_lesson_done)) } },
                onPagesTaught = { editingDate = next?.date?.toString() },
                onFix = { fixOpen = true },
                onOpen = { vm.goTo(it.screen(g.id)) },
                yesterday = lessons.firstOrNull { it.done && it.date == today.minusDays(1) },
                onYesterdayEdit = { editingDate = it.date.toString() },
                onYesterdayNotTaught = { notTaughtDate = it.date.toString() })
            GroupPage.WORK -> WorkScreen(report, vm, bodyPadding, undoToast)
            GroupPage.STATS -> GroupStats(report, vm, bodyPadding, seenTips, onOwnPaceApplied = { changed(it, context.getString(R.string.custom_created)) })
            GroupPage.PLAN -> AnimatedContent(targetState = grouping to selectedMonth,
                modifier = Modifier.weight(1f), transitionSpec = {
                    if (!motion) EnterTransition.None togetherWith ExitTransition.None else {
                        val direction = if (targetState.second >= initialState.second) 1 else -1
                        (slideInHorizontally(Motion.work(MotionSpec.Step.CALENDAR)) { MotionSpec.CALENDAR_SHIFT_PX * direction } + fadeIn(Motion.work(MotionSpec.Step.CALENDAR))) togetherWith
                            (slideOutHorizontally(Motion.work(MotionSpec.Step.CALENDAR)) { -MotionSpec.CALENDAR_SHIFT_PX * direction } + fadeOut(Motion.work(MotionSpec.Step.CALENDAR)))
                    }
                }) { (grouping, selectedMonth) ->
                val ledger = remember(lessons, plan, g, grouping, selectedMonth) { makeLedger(grouping, selectedMonth) }
                val earlierCount = earlierSummary(ledger.first.filterIsInstance<Entry.Item>().filter { !it.lesson.blocked }, today, { it.date }, { it.lesson.done })
                val earlierWeeks = weekSections(ledger.first, today) { it.date }
                val laterWeeks = if (grouping == PlanGrouping.WEEKS) weekSections(ledger.second, today) { it.date }
                else monthSections(ledger.second, today) { it.date }.map { WeekSection(it.month.atDay(1), it.month.atEndOfMonth(), WeekLabel.DATES, it.items) }
                val showHero = grouping == PlanGrouping.WEEKS || next == null || YearMonth.from(next.date) == selectedMonth
                val todayIndex = (if (earlierCount > 0) 1 else 0) + (if (tablet) 1 else 0)

                val planList = rememberLazyListState(initialFirstVisibleItemIndex = todayIndex)
                LaunchedEffect(g.id, grouping, selectedMonth) { planList.scrollToItem(todayIndex) }
                // Picking a day in the strip scrolls the list to its row (or the next one); the first pick is the opening state.
                val chosen = LocalDate.parse(selectedDay)
                val leading = (if (tablet) 1 else 0) + (if (earlierCount > 0) 1 else 0) + (if (earlierOpen) earlierWeeks.sumOf { 1 + it.items.size } else 0) + (if (showHero) 1 else 0) + 3
                var pickedBefore by remember { mutableStateOf(false) }
                LaunchedEffect(selectedDay) {
                    if (!pickedBefore) { pickedBefore = true; return@LaunchedEffect }
                    val target = if (next != null && !chosen.isAfter(next.date)) todayIndex else planRowIndex(leading, laterWeeks.map { s -> s.items.map { it.date } }, chosen)
                    if (target != null) { if (motion) planList.animateScrollToItem(target) else planList.scrollToItem(target) }
                }
                LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = planList,
                contentPadding = PaddingValues(top = Space.m, bottom = padding.calculateBottomPadding() + Space.xxl),
            ) {
                if (tablet) item {
                    GroupHomeLinks { vm.goTo(it.screen(g.id)) }
                }
                if (earlierCount > 0) item(key = "earlier") {
                    TertiaryButton(if (earlierOpen) stringResource(R.string.plan_hide_earlier) else pluralStringResource(R.plurals.plan_earlier_lessons, earlierCount, earlierCount), { earlierOpen = !earlierOpen }, Modifier.padding(horizontal = Space.l))
                }
                if (earlierOpen) ledgerWeeks("a", earlierWeeks, actions) { e ->
                    unitWindows.filter { it.end == e.date }.forEach { UnitRevisionRow(shown, vm, it.index) }
                }
                if (showHero) item(key = "today") {
                    TodayBox(
                        next, today, actions,
                        onDone = { next?.let { toggleHaptic(true); vm.toggleLesson(g.id, it.date); undoToast(context.getString(R.string.group_lesson_done)) } },
                        onPagesTaught = { editingDate = next?.date?.toString() },
                        modifier = Modifier.padding(horizontal = Space.l),
                    )
                    unitWindows.filter { it.end == next?.date }.forEach { UnitRevisionRow(shown, vm, it.index) }
                }
                item(key = "custom") {
                    Column(Modifier.padding(start = Space.l, end = Space.l, top = Rhythm.section), verticalArrangement = Arrangement.spacedBy(Rhythm.inSection)) {
                        if (custom == null) {
                            // Large text: the line lives in the ⋮ menu instead.
                            if (report.rows.isNotEmpty() && !largeText) CustomPlanLine({
                                toggleHaptic(true)
                                vm.createCustomPlan(g.id)
                                showMine = true
                                reveal++
                                undoToast(context.getString(R.string.custom_created))
                            })
                        } else {
                            when (planBarMode(custom, LocalFeatures.current)) {
                                PlanBarMode.CATCH_UP_ONLY -> FixPlanBar(custom, onBack = {
                                    if (vm.deleteCustomPlan(g.id)) undoToast(context.getString(R.string.custom_deleted))
                                })
                                else -> PlanSourceBar(custom, showMine, { showMine = it }, { vm.setCustomActive(g.id, showMine) })
                            }
                        }
                    }
                }
                item(key = "view") {
                    Column(Modifier.padding(horizontal = Space.l, vertical = Rhythm.inSection), verticalArrangement = Arrangement.spacedBy(Rhythm.inSection)) {
                        if (grouping == PlanGrouping.MONTHS) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PillIconButton(stringResource(R.string.plan_previous_month), { monthText = selectedMonth.minusMonths(1).toString() }) { AppIcon(AppIcons.ChevronRight, modifier = Modifier.rotate(180f)) }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                TertiaryButton(selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH)), { pickMonth = true })
                                PacerDropdownMenu(pickMonth, { pickMonth = false }) {
                                    val first = YearMonth.from(plan.start ?: today)
                                    val last = YearMonth.from(plan.end ?: today)
                                    var month = first
                                    while (month <= last) {
                                        val option = month
                                        DropdownMenuItem(text = { Text(option.format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH))) }, onClick = { monthText = option.toString(); pickMonth = false })
                                        month = month.plusMonths(1)
                                    }
                                }
                            }
                            PillIconButton(stringResource(R.string.plan_next_month), { monthText = selectedMonth.plusMonths(1).toString() }) { AppIcon(AppIcons.ChevronRight) }
                        } else {
                            val start = plan.start
                            val end = plan.end
                            if (start != null && end != null) {
                                DayStrip(start, end, chosen, lessons.map { it.date }.toSet(), { selectedDay = it.toString() }, onAdd = {
                                    if (lessons.firstOrNull { it.date == chosen }?.done == true) toast(context.getString(R.string.extra_taught_day)) else extraDate = selectedDay
                                })
                            }
                        }
                    }
                }
                item(key = "notes") {
                    Column(Modifier.padding(horizontal = Space.l), verticalArrangement = Arrangement.spacedBy(Rhythm.inSection)) {
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
                } else laterWeeks.forEach { section ->
                    item(key = "wb${section.start}", contentType = "week") {
                        if (grouping == PlanGrouping.WEEKS) WeekHeader(section, Modifier.animateItem())
                        else {
                            val month = YearMonth.from(section.start)
                            val name = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH))
                            Text(if (month == YearMonth.from(today)) stringResource(R.string.plan_this_month, name) else name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(Space.l))
                        }
                    }
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
                            ) { grip -> LessonRow(ls, actions, grip, selected = e.date == chosen) }
                        } else {
                            Box(Modifier.animateItem()) { EntryRow(e, actions, selected = e.date == chosen) }
                        }
                        unitWindows.filter { it.end == e.date }.forEach { UnitRevisionRow(shown, vm, it.index) }
                    }
                }
            }
            }
        }
    }

    when (sheet) {
        GroupSheet.TIPS -> PacerSheet(stringResource(R.string.group_tips), onSheetClose) {
            Note(stringResource(R.string.guide_schedule_note))
            Hint(stringResource(R.string.group_tick_hint))
            if (custom != null) Hint(stringResource(R.string.custom_hint))
        }
        GroupSheet.OWN_PACE -> OwnPaceSheet(report, { rate -> changed(vm.applyOwnPace(g.id, rate), context.getString(R.string.custom_created)) }, onSheetClose)
        GroupSheet.CATCH_UP -> CatchUpSheet(report, { boost -> changed(vm.applyCatchUp(g.id, boost), context.getString(R.string.warn_caught_up)) }, onSheetClose)
        GroupSheet.EXTRA, null -> Unit
    }

    if (homeworkDate != null || homeworkId != null) key(homeworkDate, homeworkId) {
        AssignmentEditor(report, vm, g.assignments.firstOrNull { it.id == homeworkId },
            defaultSetOn = homeworkDate?.let(LocalDate::parse), undoToast = undoToast,
            onDismiss = { homeworkDate = null; homeworkId = null })
    }

    if (sheet == GroupSheet.EXTRA || extraDate != null) {
        ExtraLessonSheet(report, extraDate?.let(LocalDate::parse) ?: (report.lessons.firstOrNull() ?: g.from ?: plan.start ?: today),
            chooseDate = sheet == GroupSheet.EXTRA,
            onSave = { date, id, pages -> changed(if (id == null) vm.addExtra(g.id, date, pages) else vm.saveExtra(g.id, id, pages), context.getString(R.string.extra_saved)) },
            onDelete = { changed(vm.removeExtra(g.id, it), context.getString(R.string.extra_deleted)) },
            onDismiss = { extraDate = null; onSheetClose() },
        )
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

    if (warningsOpen || fixOpen) {
        val warnings = remember(report) { groupWarnings(report) }
        WarningsSheet(report, warnings, WarningFixes(
            units = { issue ->
                if (issue.kind == gr.edusuite.pacer.engine.UnitIssueKind.REVISION_UNSCHEDULED && issue.unitIndex != null) vm.openUnitRevision(g.id, issue.unitIndex)
                else vm.open(Screen.Units)
            },
            catchUp = { catchingUp = true },
            fold = { folding = it.bookId },
            repace = { changed(vm.repaceFrom(g.id, 0), context.getString(R.string.custom_repaced)) },
            repaceToFit = { changed(vm.repaceCustomPlanToFit(g.id), context.getString(R.string.custom_repaced)) },
            fit = { changed(vm.fitCustomPlanToTerm(g.id), context.getString(R.string.custom_fitted)) },
            tidy = { changed(vm.tidyCustomPlan(g.id), context.getString(R.string.custom_tidied)) },
            extra = { extraDate = it.toString() },
            openLesson = { id -> showMine = true; planning = "L$id" },
        ), { fixOpen = false; onWarningsClose() })
    }
    if (catchingUp) CatchUpSheet(report, { boost ->
        val didChange = vm.applyCatchUp(g.id, boost)
        showMine = true
        changed(didChange, context.getString(R.string.warn_caught_up))
    }, { catchingUp = false })
    folding?.let { id ->
        val lag = lags.firstOrNull { it.bookId == id }
        if (lag == null) folding = null else FoldSheet(report, lag, { over ->
            vm.foldBacklog(g.id, id, over)
            showMine = true
            undoToast(context.getString(R.string.warn_folded, plan.books.firstOrNull { it.id == id }?.name.orEmpty()))
        }, { folding = null })
    }

    notTaughtDate?.let { ds ->
        val date = LocalDate.parse(ds)
        NotTaughtDialog(report, date, { vm.lessonNotTaught(g.id, date); undoToast(context.getString(R.string.group_not_taught_done)) }, { notTaughtDate = null })
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
private fun EntryRow(e: Entry, actions: LessonActions, selected: Boolean = false) {
    if (e is Entry.Item && !e.lesson.blocked) LessonRow(e.lesson, actions, selected = selected) else SlimRow(e, actions, selected)
}

/** Week sections of lines that don't move: a header, then a row per line. [tag] keeps keys apart above and below today. */
private fun LazyListScope.ledgerWeeks(tag: String, weeks: List<WeekSection<Entry>>, actions: LessonActions, after: @Composable (Entry) -> Unit = {}) {
    weeks.forEach { section ->
        item(key = "w$tag${section.start}", contentType = "week") { WeekHeader(section) }
        items(section.items, key = { e -> e.key() }) { e -> EntryRow(e, actions); after(e) }
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
    PacerSheet(
        title = stringResource(R.string.group_pages_title, l.date.dayOfWeek.short(), l.date.fmtShort()),
        onDismiss = onDismiss,
        action = SheetAction(stringResource(R.string.group_save), parsed.all { it != null }) {
            onSave(books.zip(parsed).associate { (b, p) -> b.id to p.orEmpty() })
        },
        dirty = rememberDraftChanged(texts.toList()),
    ) {
        Hint(
            stringResource(R.string.group_pages_hint, if (l.done) "" else stringResource(R.string.group_saving_marks_done)),
        )
        books.forEachIndexed { i, b ->
            val bad = parsed[i] == null
            OutlinedTextField(
            colors = paletteFieldColors(),
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


/**
 * The LazyColumn position of the row for [date], or of the first listed day after it: [leading] items come first, then
 * for each later section a header and its rows (their [sections] dates, in order). Null when no later day is listed.
 */
internal fun planRowIndex(leading: Int, sections: List<List<LocalDate>>, date: LocalDate): Int? {
    var index = leading
    for (days in sections) {
        index += 1
        val at = days.indexOfFirst { !it.isBefore(date) }
        if (at >= 0) return index + at
        index += days.size
    }
    return null
}
