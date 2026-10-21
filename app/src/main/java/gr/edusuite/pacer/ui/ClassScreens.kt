package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import gr.edusuite.pacer.R
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.WEEK
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.lessonsFor
import gr.edusuite.pacer.engine.position
import gr.edusuite.pacer.engine.parseRanges
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.engine.coreBooks
import gr.edusuite.pacer.engine.includedPages
import gr.edusuite.pacer.engine.parseInclude
import gr.edusuite.pacer.model.MAX_LAG_LESSONS
import gr.edusuite.pacer.engine.unitStarts
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate
import java.time.DayOfWeek

/**
 * The scrolling body every class-editing screen shares, with the problems this screen fixes on top.
 * The class screen also lists term problems, linked to School year settings. [hide] is a problem
 * a "Next step" card already explains.
 */
@Composable
private fun EditList(plan: Plan, screen: Screen, vm: PacerViewModel, padding: PaddingValues, hide: String? = null, content: LazyListScope.() -> Unit) {
    val problems = remember(plan, hide) {
        Nav.problems(plan).filter { (it.fix == screen || (screen == Screen.Class && it.fix == Screen.YearSettings)) && it.text != hide }
    }
    ScreenList(padding) {
        items(problems) { p -> ErrorBox(p.text, onFix = p.fix?.takeIf { it != screen }?.let { f -> { vm.open(f) } }) }
        content()
    }
}

/** The open class: its name, lesson length and pace, books and groups, with tests and homework a tap away. */
@Composable
fun ClassScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues, undoToast: (String) -> Unit) {
    val context = LocalContext.current
    var addingBook by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf<String?>(null) }
    // A new class has a group but no books; the card replaces the red "Add at least one book." problem.
    val hide = when {
        plan.books.isEmpty() -> NO_BOOKS
        plan.groups.isEmpty() -> NO_GROUPS
        else -> null
    }
    EditList(plan, Screen.Class, vm, padding, hide) {
        when (hide) {
            NO_BOOKS -> item(key = "guide") {
                GuideCard(
                    stringResource(R.string.class_add_the_books_you_teach_from),
                    stringResource(R.string.class_book_guide),
                    action = stringResource(R.string.class_add_a_book) to { addingBook = true },
                )
            }
            NO_GROUPS -> item(key = "guide") {
                GuideCard(
                    stringResource(R.string.class_add_a_group),
                    stringResource(R.string.class_a_group_is_a_set_of_students_taught_from_these_books_with_it),
                    action = stringResource(R.string.class_add_group) to vm::addGroup,
                )
            }
        }
        item(key = "class") {
            Section(stringResource(R.string.class_class)) {
                LinkRow(stringResource(R.string.class_name), plan.name) { settings = "name" }
                LinkRow(stringResource(R.string.class_lessons), stringResource(R.string.class_lesson_settings, plan.mins, plan.pace)) { settings = "lessons" }
            }
        }

        item(key = "books") {
            Section(stringResource(R.string.class_books), trailing = { TertiaryButton(stringResource(R.string.class_add), { addingBook = true }) }) {
                Hint(stringResource(R.string.class_each_book_is_paced_on_its_own_skipped_pages_apply_to_every_g))
                if (plan.books.isEmpty()) Hint(stringResource(R.string.class_no_books_yet))
                if (plan.coreBooks.size > 1) {
                    Picker(
                        stringResource(R.string.class_lead_book),
                        listOf("" to stringResource(R.string.class_lead_none)) + plan.coreBooks.map { it.id to it.name.ifBlank { stringResource(R.string.class_book) } },
                        plan.lead.orEmpty(), { vm.setLead(it.ifEmpty { null }) }, Modifier.fillMaxWidth(),
                    )
                    if (plan.lead != null) Hint(stringResource(R.string.class_lead_hint))
                }
                TileGrid(plan.books) { book, modifier -> BookEditor(book, book.id == plan.lead, vm, undoToast, modifier) }
            }
        }
        item(key = "groups") {
            Section(stringResource(R.string.class_groups), trailing = { TertiaryButton(stringResource(R.string.class_add), vm::addGroup) }) {
                if (plan.groups.isEmpty()) Hint(stringResource(R.string.class_add_a_group_for_each_set_of_students_taught_from_these_books))
                TileGrid(plan.groups) { group, modifier -> GroupEditor(plan, group, vm, undoToast, modifier) }
            }
        }

        item(key = "more") {
            Section(stringResource(R.string.class_tests_and_homework)) {
                Column {
                    LinkRow(stringResource(R.string.class_tests_worksheets), pluralStringResource(R.plurals.class_entries, plan.events.size, plan.events.size)) { vm.open(Screen.Tests) }
                    LinkRow(stringResource(R.string.class_homework_already_assigned), pluralStringResource(R.plurals.class_assignments, plan.homework.size, plan.homework.size)) { vm.open(Screen.Homework) }
                }
            }
        }
        item(key = "delete") {
            MoreOptions {
                TertiaryButton(stringResource(R.string.class_reset_all_positions), {
                    vm.resetPositions(); undoToast(context.getString(R.string.class_positions_reset))
                }, enabled = plan.groups.isNotEmpty(), color = Pacer.colors.warn)
                TertiaryButton(stringResource(R.string.class_delete_this_class), {
                    val name = plan.name.ifBlank { context.getString(R.string.class_class) }
                    vm.deleteClass(); undoToast(context.getString(R.string.class_deleted, name))
                }, color = Pacer.colors.warn)
            }
        }
    }
    if (settings != null) SheetFrame(stringResource(R.string.class_class_settings), stringResource(R.string.class_done), true, {}, { settings = null }) {
        when (settings) {
            "name" -> CommitField(plan.name, vm::renameClass, stringResource(R.string.class_name), Modifier.fillMaxWidth())
            "lessons" -> {
                IntField(plan.mins, vm::setMins, stringResource(R.string.class_lesson_length_min), Modifier.fillMaxWidth(), max = Settings.MAX_MINS)
                PaceField(plan.pace, vm::setPace, Modifier.fillMaxWidth())
                Hint(stringResource(R.string.class_pages_per_lesson_is_the_benchmark_the_verdict_measures_again))
                if (plan.books.size > 1) {
                    IntField(plan.lagLessons, vm::setLagLessons, stringResource(R.string.class_lag_lessons), Modifier.fillMaxWidth(), min = 1, max = MAX_LAG_LESSONS)
                    Hint(stringResource(R.string.class_lag_hint))
                }
            }
        }
    }

    if (addingBook) AddBookSheet(vm) { addingBook = false }
}

/** The problems ([Plan.problems]) that the class screen's "Next step" card covers. */
private const val NO_BOOKS = "Add at least one book."
private const val NO_GROUPS = "Add at least one group."

@Composable
private fun GroupEditor(plan: Plan, g: Group, vm: PacerViewModel, undoToast: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val fallbackName = stringResource(R.string.class_group)
    val name = g.name.ifBlank { fallbackName }
    // Its ticked lessons, homework, and the days off and tests aimed only at it go too; Undo brings them back.
    var editing by rememberSaveable(g.id) { mutableStateOf(false) }
    val report = remember(plan, g) { GroupReport(plan, g) }
    Tile(Pacer.colors.tileLilac, modifier, onClick = { vm.open(Screen.Group(g.id)) }) {
        AppIcon(AppIcons.Groups)
        Text(name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        AnimatedFigure(report.rows.size.toString())
        Text(pluralStringResource(R.plurals.class_lessons_unit, report.rows.size) + (report.rows.firstOrNull()?.date?.let { stringResource(R.string.class_next_suffix, stringResource(classDayResource(it.dayOfWeek)), it.fmtShort()) } ?: ""), style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.onTileMuted)
        val separator = stringResource(R.string.class_day_separator)
        Text(g.days.sortedBy { it.value }.joinToString(separator) { day -> context.getString(classDayResource(day)) }, color = Pacer.colors.onTileMuted)
        SecondaryButton(stringResource(R.string.class_edit), { editing = true })
    }
    if (editing) SheetFrame(name, stringResource(R.string.class_done), true, {}, { editing = false }) {
        GroupFields(plan, g, vm) { editing = false; vm.removeGroup(g.id); undoToast(context.getString(R.string.class_removed, name)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.GroupFields(plan: Plan, g: Group, vm: PacerViewModel, onRemove: () -> Unit) {
    val segmentHaptic = rememberSegmentHaptic()
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommitField(g.name, { vm.renameGroup(g.id, it) }, stringResource(R.string.class_name), Modifier.weight(1f))

    }
    // Seven chips need about 330dp; the row wraps its last chips on very narrow phones.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        WEEK.forEach { day ->
            FilterChip(
                selected = day in g.days,
                onClick = { segmentHaptic(); vm.toggleDay(g.id, day) },
                label = { Text(stringResource(classDayResource(day))) },
                shape = MaterialTheme.shapes.extraSmall,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
    val lessons = remember(plan, g) { plan.lessonsFor(g) }
    // The first lesson still to teach; once lessons are ticked, the stored date is the day after the last one taught.
    DateField(lessons.firstOrNull() ?: g.from ?: plan.start, { vm.setNextLesson(g.id, it) }, stringResource(R.string.class_next_lesson), Modifier.fillMaxWidth())
    MoreOptions { TertiaryButton(stringResource(R.string.class_remove_group), onRemove, color = Pacer.colors.warn) }
    val count = lessons.size
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        AnimatedFigure(pluralStringResource(R.plurals.class_lessons_left, count, count), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { vm.open(Screen.Group(g.id)) }, modifier = Modifier.heightIn(min = 48.dp)) {
            AppIcon(AppIcons.ChevronRight)
            Text(stringResource(R.string.class_open_schedule))
        }
    }
}

@Composable
fun TestsScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues, undoToast: (String) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val lessonDays = remember(plan) { plan.groups.associate { g -> g.id to (plan.lessonsFor(g) + g.done.map { it.date }).toHashSet() } }
    EditList(plan, Screen.Tests, vm, padding) {
        if (plan.events.isEmpty()) item(key = "empty") {
            EmptyState(AppIcons.Tests, stringResource(R.string.class_no_tests_yet), stringResource(R.string.class_plan_a_test_or_worksheet_for_your_next_lesson), stringResource(R.string.class_add_a_test)) { adding = true }
        }
        item(key = "upcoming") {
            Section(stringResource(R.string.class_upcoming), modifier = Modifier.animateItem()) {
                val upcoming = plan.events.filter { it.date >= today }.sortedBy { it.date }
                if (upcoming.isEmpty() && plan.events.isNotEmpty()) Hint(stringResource(R.string.class_no_upcoming_tests))
                upcoming.forEach { TestRow(plan, it, lessonDays, vm, undoToast) }
            }
        }
        val past = plan.events.filter { it.date < today }.sortedByDescending { it.date }
        if (past.isNotEmpty()) item(key = "past") {
            MoreOptions(pluralStringResource(R.plurals.class_past_tests, past.size, past.size), modifier = Modifier.animateItem()) { past.forEach { TestRow(plan, it, lessonDays, vm, undoToast) } }
        }
        item(key = "add") { PrimaryButton(stringResource(R.string.class_add_a_test), { adding = true }) }
    }
    if (adding) AddEventDialog(plan, vm) { adding = false }
}

@Composable
private fun TestRow(plan: Plan, event: TestEvent, lessonDays: Map<String, Set<LocalDate>>, vm: PacerViewModel, undoToast: (String) -> Unit) {
    val context = LocalContext.current
    val fallbackName = stringResource(R.string.class_test)
    val title = event.name.ifBlank { fallbackName }
    val group = if (event.who == ALL_GROUPS) stringResource(R.string.class_all_groups) else plan.groups.firstOrNull { it.id == event.who }?.name ?: stringResource(R.string.class_group)
    val hit = plan.groups.any { (event.who == ALL_GROUPS || event.who == it.id) && event.date in lessonDays[it.id].orEmpty() }
    val remove = { vm.removeEvent(event.id); undoToast(context.getString(R.string.class_removed, title)) }
    SwipeRow(title, remove) {
        ListGroup {
            Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                DateBadge(event.date)
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.class_test_detail, stringResource(classDayResource(event.date.dayOfWeek)), event.date.fmtShort(), if (event.consumes) stringResource(R.string.class_takes_lesson) else stringResource(R.string.class_alongside_lesson)), style = MaterialTheme.typography.bodyMedium)
                Tag(group)
                if (!hit) Tag(stringResource(R.string.class_not_a_lesson_day), warn = true)
                TertiaryButton(stringResource(R.string.class_remove_test), remove)
            }
        }
    }
}

@Composable
private fun BookEditor(b: Book, leads: Boolean, vm: PacerViewModel, undoToast: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val fallbackName = stringResource(R.string.class_book)
    val name = b.name.ifBlank { fallbackName }
    // Its positions, homework and the pages ticked lessons recorded in it go too; Undo brings them back.
    var editing by rememberSaveable(b.id) { mutableStateOf(false) }
    val skipped = remember(b) { parseRanges(b.skip).count { it in b.first..b.last } }
    Tile(Pacer.colors.tileSky, modifier, onClick = { editing = true }) {
        AppIcon(AppIcons.Book)
        Text(name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        if (b.supplementary) {
            val n = remember(b.include) { b.includedPages().size }
            Text(b.include, style = MaterialTheme.typography.titleMedium.merge(Figures))
            Text(pluralStringResource(R.plurals.supp_pages, n, n), color = Pacer.colors.onTileMuted)
        } else {
        Text(stringResource(R.string.class_page_range, b.first, b.last), style = MaterialTheme.typography.titleMedium)
        Text(pluralStringResource(R.plurals.class_teachable, maxOf(0, b.last - b.first + 1 - skipped), maxOf(0, b.last - b.first + 1 - skipped)), color = Pacer.colors.onTileMuted)
        }
        if (b.skip.isNotBlank() && !b.supplementary) Tag(pluralStringResource(R.plurals.class_skipped, skipped, skipped), onTile = true)
        b.due?.let { Tag(stringResource(R.string.class_due_by, it.fmtShort()), onTile = true) }
        if (leads) Tag(stringResource(R.string.class_leads), onTile = true)
        if (b.units.isNotBlank()) Tag(pluralStringResource(R.plurals.class_units, b.unitStarts().size, b.unitStarts().size), onTile = true)
        if (b.supplementary) Tag(stringResource(R.string.supp_tag), onTile = true)
    }
    if (editing) SheetFrame(name, stringResource(R.string.class_done), true, {}, { editing = false }) {
        BookFields(b, vm) { editing = false; vm.removeBook(b.id); undoToast(context.getString(R.string.class_removed, name)) }
    }
}

@Composable
private fun ColumnScope.BookFields(b: Book, vm: PacerViewModel, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommitField(b.name, { vm.renameBook(b.id, it) }, stringResource(R.string.class_title), Modifier.weight(1f))

    }
    if (b.supplementary) {
        // Placed by hand, so none of the pacing settings apply.
        IncludeField(b.include, { vm.setInclude(b.id, it) })
        SwitchRow(stringResource(R.string.supp_count_default), b.countInLesson) { vm.setCountInLesson(b.id, it) }
        Hint(stringResource(R.string.supp_count_default_hint))
        TertiaryButton(stringResource(R.string.class_remove_book), onRemove, color = Pacer.colors.warn)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
        IntField(b.first, { vm.setFirstPage(b.id, it) }, stringResource(R.string.class_first_page), Modifier.fillMaxWidth(), max = MAX_PAGE)
        IntField(b.last, { vm.setLastPage(b.id, it) }, stringResource(R.string.class_last_page), Modifier.fillMaxWidth(), max = MAX_PAGE)
    }
    MoreOptions {
        CommitField(b.skip, { vm.setSkip(b.id, it) }, stringResource(R.string.class_skip_pages), Modifier.fillMaxWidth(), placeholder = stringResource(R.string.class_8_24_27))
        DateField(b.due, { vm.setDue(b.id, it) }, stringResource(R.string.class_finish_by), Modifier.fillMaxWidth(), clearable = true)
        if (b.due != null) Hint(stringResource(R.string.class_only_this_book_re_paces_the_others_keep_their_own_rate))
        CommitField(b.units, { vm.setUnits(b.id, it) }, stringResource(R.string.class_units), Modifier.fillMaxWidth(), placeholder = stringResource(R.string.class_units_placeholder))
        Hint(stringResource(R.string.class_units_hint))
        TertiaryButton(stringResource(R.string.class_remove_book), onRemove, color = Pacer.colors.warn)
    }
    val skipN = remember(b.skip, b.first, b.last) { parseRanges(b.skip).count { it in b.first..b.last } }
    Text(
        stringResource(R.string.class_book_summary, stringResource(R.string.class_page_range, b.first, b.last), pluralStringResource(R.plurals.class_teachable, maxOf(0, b.last - b.first + 1 - skipN), maxOf(0, b.last - b.first + 1 - skipN))),
        style = MaterialTheme.typography.bodySmall.merge(Figures), color = Pacer.colors.muted,
    )
}

@Composable
fun HomeworkScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues, undoToast: (String) -> Unit) {
    val context = LocalContext.current
    var adding by rememberSaveable { mutableStateOf(false) }
    EditList(plan, Screen.Homework, vm, padding) {
        item(key = "hint") {
            Hint(stringResource(R.string.class_pages_set_for_homework_leave_the_classroom_sequence_the_rest))
        }
        if (plan.homework.isEmpty()) item(key = "empty") {
            EmptyState(AppIcons.Homework, stringResource(R.string.class_no_homework_assigned), stringResource(R.string.class_assign_pages_to_ease_the_classroom_pace),
                if (plan.groups.isNotEmpty() && plan.coreBooks.isNotEmpty()) stringResource(R.string.class_add_homework) else null,
                if (plan.groups.isNotEmpty() && plan.coreBooks.isNotEmpty()) ({ adding = true }) else null)
        }
        plan.homework.groupBy { it.groupId }.forEach { (groupId, assignments) ->
            item(key = "group:$groupId") {
                Section(plan.groups.firstOrNull { it.id == groupId }?.name ?: stringResource(R.string.class_group), modifier = Modifier.animateItem()) {
                    assignments.sortedBy { it.setOn ?: LocalDate.MIN }.forEach { h ->
                        val book = plan.books.firstOrNull { it.id == h.bookId }?.name ?: stringResource(R.string.class_book)
                        val n = h.to - h.from + 1
                        val title = stringResource(R.string.class_homework_pages, book, h.from, h.to)
                        val detail = pluralStringResource(R.plurals.class_pages, n, n) + (h.setOn?.let { stringResource(R.string.class_set_suffix, it.fmtShort()) } ?: "")
                        val remove = { vm.removeHomework(h.id); undoToast(context.getString(R.string.class_removed, title)) }
                        SwipeRow(title, remove) { ListItemRow(title, detail, onRemove = remove) }
                    }
                }
            }
        }
        item(key = "add") {
            if (plan.groups.isEmpty() || plan.books.isEmpty()) Hint(stringResource(R.string.class_add_a_group_and_a_book_first))
            else PrimaryButton(stringResource(R.string.class_add_homework), { adding = true })
        }
    }
    if (adding) AddHomeworkDialog(plan, vm) { adding = false }
}

// ---- add dialogs and sheets ---------------------------------------------------------

@Composable
private fun whoOptions(plan: Plan, allFirst: Boolean): List<Pair<String, String>> {
    val groups = plan.groups.map { it.id to it.name }
    val all = ALL_GROUPS to stringResource(R.string.class_all_groups)
    return if (allFirst) listOf(all) + groups else groups + all
}

@Composable
private fun AddEventDialog(plan: Plan, vm: PacerViewModel, onDismiss: () -> Unit) {
    val toggleHaptic = rememberToggleHaptic()
    var name by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(plan.groups.firstOrNull()?.let { GroupReport(plan, it).rows.firstOrNull()?.date }?.toString()) }
    val date = dateText?.let(LocalDate::parse)
    var who by rememberSaveable { mutableStateOf(plan.groups.firstOrNull()?.id ?: ALL_GROUPS) }
    var consumes by rememberSaveable { mutableStateOf(true) }
    SheetFrame(stringResource(R.string.class_add_a_test_or_worksheet), stringResource(R.string.class_add), date != null, { vm.addEvent(name.trim(), date!!, who, consumes) }, onDismiss) {
        CommitField(name, { name = it }, stringResource(R.string.class_name), placeholder = stringResource(R.string.class_unit_3_test))
        Picker(stringResource(R.string.class_group), whoOptions(plan, allFirst = false), who, { selected ->
            who = selected
            dateText = plan.groups.filter { selected == ALL_GROUPS || it.id == selected }
                .mapNotNull { GroupReport(plan, it).rows.firstOrNull()?.date }.minOrNull()?.toString()
        })
        DateField(date, { dateText = it?.toString() }, stringResource(R.string.class_date))
        MoreOptions {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                Text(stringResource(R.string.class_takes_the_whole_lesson), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = consumes, onCheckedChange = { toggleHaptic(it); consumes = it },
                    thumbContent = { AppIcon(if (consumes) AppIcons.Check else AppIcons.Close, Modifier.size(Space.l)) })
            }
        }
    }
}

/** A supplementary book's pages, strict: an unreadable or empty list shows an error and isn't saved. */
@Composable
private fun IncludeField(value: String, onCommit: (String) -> Unit) {
    CommitField(
        value, onCommit, stringResource(R.string.supp_include), Modifier.fillMaxWidth(),
        placeholder = stringResource(R.string.supp_include_placeholder),
        isValid = { !parseInclude(it).isNullOrEmpty() }, errorMessage = stringResource(R.string.supp_include_error),
    )
}

/** A labelled switch with the shared toggle haptic and the ✓/✕ thumb (§5). */
@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val toggleHaptic = rememberToggleHaptic()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = { toggleHaptic(it); onChange(it) },
            thumbContent = { AppIcon(if (checked) AppIcons.Check else AppIcons.Close, Modifier.size(Space.l)) })
    }
}

@Composable
private fun AddBookSheet(vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var first by rememberSaveable { mutableStateOf("1") }
    var last by rememberSaveable { mutableStateOf("") }
    var supplementary by rememberSaveable { mutableStateOf(false) }
    var include by rememberSaveable { mutableStateOf("") }
    val lastN = last.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE }
    val includeOk = !parseInclude(include).isNullOrEmpty()
    SheetFrame(stringResource(R.string.class_add_a_book), stringResource(R.string.class_add), if (supplementary) includeOk else lastN != null, {
        if (supplementary) vm.addSupplementaryBook(name.trim(), include)
        else vm.addBook(name.trim(), first.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE } ?: 1, lastN!!)
    }, onDismiss) {
        CommitField(name, { name = it }, stringResource(R.string.class_title), Modifier.fillMaxWidth(), placeholder = stringResource(R.string.class_companion))
        SwitchRow(stringResource(R.string.supp_switch), supplementary) { supplementary = it }
        if (supplementary) {
            Hint(stringResource(R.string.supp_switch_hint))
            CommitField(include, { include = it }, stringResource(R.string.supp_include), Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.supp_include_placeholder),
                error = include.isNotBlank() && !includeOk, errorMessage = stringResource(R.string.supp_include_error))
        } else Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            CommitField(first, { first = it }, stringResource(R.string.class_first_page), Modifier.fillMaxWidth(), numeric = true)
            CommitField(last, { last = it }, stringResource(R.string.class_last_page), Modifier.fillMaxWidth(), numeric = true,
                error = last.isNotBlank() && lastN == null, errorMessage = stringResource(R.string.component_integer_range, 1, MAX_PAGE))
        }
    }
}

@Composable
private fun AddHomeworkDialog(plan: Plan, vm: PacerViewModel, onDismiss: () -> Unit) {
    var group by rememberSaveable { mutableStateOf(plan.groups.firstOrNull()?.id) }
    var book by rememberSaveable { mutableStateOf(plan.coreBooks.firstOrNull()?.id) }
    fun currentPage(): String {
        val g = plan.groups.firstOrNull { it.id == group }
        val b = plan.books.firstOrNull { it.id == book }
        return if (g != null && b != null) position(g, b).toString() else ""
    }
    var from by rememberSaveable { mutableStateOf(currentPage()) }
    var to by rememberSaveable { mutableStateOf("") }
    var setOnText by rememberSaveable { mutableStateOf<String?>(LocalDate.now().toString()) }
    val setOn = setOnText?.let(LocalDate::parse)
    val fromN = from.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE }
    val toN = to.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE } ?: fromN
    SheetFrame(stringResource(R.string.class_add_homework), stringResource(R.string.class_add), fromN != null && group != null && book != null, {
        vm.addHomework(group!!, book!!, fromN!!, toN!!, setOn)
    }, onDismiss) {
        Picker(stringResource(R.string.class_group), plan.groups.map { it.id to it.name }, group, { group = it; from = currentPage(); to = "" })
        Picker(stringResource(R.string.class_book), plan.coreBooks.map { it.id to it.name }, book, { book = it; from = currentPage(); to = "" })
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            CommitField(from, { from = it }, stringResource(R.string.class_from_p), Modifier.fillMaxWidth(), numeric = true,
                error = from.isNotBlank() && fromN == null, errorMessage = stringResource(R.string.component_integer_range, 1, MAX_PAGE))
            CommitField(to, { to = it }, stringResource(R.string.class_to_p), Modifier.fillMaxWidth(), numeric = true)
        }
        MoreOptions { DateField(setOn, { setOnText = it?.toString() }, stringResource(R.string.class_set_on_optional), clearable = true) }
    }
}

/** Resource ids keep weekday labels localised without changing the stored weekday convention. */
private fun classDayResource(day: DayOfWeek): Int = when (day) {
    DayOfWeek.MONDAY -> R.string.class_monday
    DayOfWeek.TUESDAY -> R.string.class_tuesday
    DayOfWeek.WEDNESDAY -> R.string.class_wednesday
    DayOfWeek.THURSDAY -> R.string.class_thursday
    DayOfWeek.FRIDAY -> R.string.class_friday
    DayOfWeek.SATURDAY -> R.string.class_saturday
    DayOfWeek.SUNDAY -> R.string.class_sunday
}
