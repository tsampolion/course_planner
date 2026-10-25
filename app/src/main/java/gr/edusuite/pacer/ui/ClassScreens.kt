package gr.edusuite.pacer.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.WEEK
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.plain
import gr.edusuite.pacer.engine.lessonsFor
import gr.edusuite.pacer.engine.position
import gr.edusuite.pacer.engine.parseRanges
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.BookType
import gr.edusuite.pacer.engine.forGroup
import gr.edusuite.pacer.engine.coreBooks
import gr.edusuite.pacer.engine.includedPages
import gr.edusuite.pacer.engine.parseInclude
import gr.edusuite.pacer.model.MAX_LAG_LESSONS
import gr.edusuite.pacer.engine.unitStarts
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate
import java.time.DayOfWeek

/**
 * The problems [screen] fixes. The class screen also lists term problems, linked to School year settings, and leaves
 * out the one a "Next step" card already explains ([guideProblem]).
 */
internal fun screenProblems(plan: Plan, screen: Screen): List<Problem> {
    val hide = if (screen == Screen.Class) guideProblem(plan) else null
    return Nav.problems(plan).filter { (it.fix == screen || (screen == Screen.Class && it.fix == Screen.YearSettings)) && it.text != hide }
}

/** The problem the class screen's "Next step" card explains instead: no books, else no groups. */
private fun guideProblem(plan: Plan): String? = when {
    plan.books.isEmpty() -> NO_BOOKS
    plan.groups.isEmpty() -> NO_GROUPS
    else -> null
}

/**
 * The scrolling body every class-editing screen shares, with the problems this screen fixes on top. The class screen
 * has none on the page: they are behind its top bar's warning button ([ClassProblemsSheet], D7).
 */
@Composable
private fun EditList(plan: Plan, screen: Screen, vm: PacerViewModel, padding: PaddingValues, content: LazyListScope.() -> Unit) {
    val problems = remember(plan, screen) { if (screen == Screen.Class) emptyList() else screenProblems(plan, screen) }
    ScreenList(padding) {
        items(problems) { p -> ErrorBox(p.text, onFix = p.fix?.takeIf { it != screen }?.let { f -> { vm.open(f) } }) }
        content()
    }
}

/** The class's problems, behind the warning button in its top bar, each with Fix where another screen fixes it. */
@Composable
fun ClassProblemsSheet(plan: Plan, vm: PacerViewModel, onDismiss: () -> Unit) {
    val problems = remember(plan) { screenProblems(plan, Screen.Class) }
    HomeSheet(stringResource(R.string.class_problems_title), onDismiss) { close ->
        if (problems.isEmpty()) Hint(stringResource(R.string.warnings_none))
        problems.forEach { p -> ErrorBox(p.text, onFix = p.fix?.takeIf { it != Screen.Class }?.let { f -> { close(); vm.open(f) } }) }
    }
}

/**
 * The open class (3.5.0): its lesson length and pace on one line with Edit, then its groups with their status, its
 * books (the lead book and supplementary books marked), Tests & worksheets and Homework, and Reset all positions and
 * Delete this class at the end.
 */
@Composable
fun ClassScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues, undoToast: (String) -> Unit) {
    val context = LocalContext.current
    var addingBook by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    // A new class has a group but no books; the card replaces the red "Add at least one book." problem.
    val hide = guideProblem(plan)
    EditList(plan, Screen.Class, vm, padding) {
        item(key = "class") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.class_summary, plan.mins, plan.pace.plain()),
                    style = MaterialTheme.typography.bodyMedium.merge(Figures), color = Pacer.colors.muted, modifier = Modifier.weight(1f),
                )
                TertiaryButton(stringResource(R.string.class_edit), { settings = true })
            }
        }
        item(key = "units") {
            LedgerRow(stringResource(R.string.units_title), detail = stringResource(if (plan.unitsEnabled) R.string.units_on else R.string.units_off), onClick = { vm.open(Screen.Units) }) { Chevron() }
        }
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
                    action = stringResource(R.string.class_add_group) to { vm.addGroup(); Unit },
                )
            }
        }
        item(key = "groups") {
            SectionColumn {
                SectionHead(stringResource(R.string.class_groups), action = stringResource(R.string.class_add), onAction = vm::addGroup)
                if (plan.groups.isEmpty()) Hint(stringResource(R.string.class_add_a_group_for_each_set_of_students_taught_from_these_books))
                else RowGroup {
                    plan.groups.forEachIndexed { i, g ->
                        if (i > 0) RowDivider()
                        ClassGroupRow(plan, g, vm)
                    }
                }
            }
        }
        item(key = "books") {
            SectionColumn {
                SectionHead(stringResource(R.string.class_books), action = stringResource(R.string.class_add), onAction = { addingBook = true })
                if (plan.books.isEmpty()) Hint(stringResource(R.string.class_no_books_yet))
                else RowGroup {
                    plan.books.forEachIndexed { i, b ->
                        if (i > 0) RowDivider()
                        BookRow(b, b.id == plan.lead, vm, undoToast)
                    }
                }
                if (plan.coreBooks.size > 1) {
                    Picker(
                        stringResource(R.string.class_lead_book),
                        listOf("" to stringResource(R.string.class_lead_none)) + plan.coreBooks.map { it.id to it.name.ifBlank { stringResource(R.string.class_book) } },
                        plan.lead.orEmpty(), { vm.setLead(it.ifEmpty { null }) }, Modifier.fillMaxWidth(),
                    )
                    Hint(stringResource(if (plan.lead != null) R.string.class_lead_hint else R.string.class_each_book_is_paced_on_its_own_skipped_pages_apply_to_every_g))
                }
            }
        }
        item(key = "more") {
            SectionColumn {
                SectionHead(stringResource(R.string.work_title))
                Hint(stringResource(R.string.work_moved))
                RowGroup {
                    plan.groups.forEachIndexed { i, group ->
                        if (i > 0) RowDivider()
                        LedgerRow(group.name, icon = AppIcons.Homework, onClick = { vm.goTo(Screen.Group(group.id, GroupPage.WORK)) }) { Chevron() }
                    }
                }
            }
        }
        item(key = "delete") {
            FootActions(
                start = stringResource(R.string.class_reset_all_positions) to { vm.resetPositions(); undoToast(context.getString(R.string.class_positions_reset)) },
                startEnabled = plan.groups.isNotEmpty(),
                end = stringResource(R.string.class_delete_this_class) to {
                    val name = plan.name.ifBlank { context.getString(R.string.class_class) }
                    vm.deleteClass(); undoToast(context.getString(R.string.class_deleted, name))
                },
            )
        }
    }
    if (settings) SheetFrame(stringResource(R.string.class_class_settings), stringResource(R.string.class_done), true, {}, { settings = false }) {
        CommitField(plan.name, vm::renameClass, stringResource(R.string.class_name), Modifier.fillMaxWidth())
        IntField(plan.mins, vm::setMins, stringResource(R.string.class_lesson_length_min), Modifier.fillMaxWidth(), max = Settings.MAX_MINS)
        PaceField(plan.pace, vm::setPace, Modifier.fillMaxWidth())
        SwitchRow(stringResource(R.string.units_enable), plan.unitsEnabled, vm::setUnitsEnabled)
        TertiaryButton(stringResource(R.string.units_title), { settings = false; vm.open(Screen.Units) })
        Hint(stringResource(R.string.class_pages_per_lesson_is_the_benchmark_the_verdict_measures_again))
        if (plan.books.size > 1) {
            IntField(plan.lagLessons, vm::setLagLessons, stringResource(R.string.class_lag_lessons), Modifier.fillMaxWidth(), min = 1, max = MAX_LAG_LESSONS)
            Hint(stringResource(R.string.class_lag_hint))
        }
    }

    if (addingBook) AddBookSheet(vm) { addingBook = false }
}

/** "Fri 23 Oct". */
@Composable
private fun shortDate(d: LocalDate): String = "${stringResource(classDayResource(d.dayOfWeek))} ${d.fmtShort()}"

/** The problems ([Plan.problems]) that the class screen's "Next step" card covers. */
private const val NO_BOOKS = "Add at least one book."
private const val NO_GROUPS = "Add at least one group."

/**
 * A group of the class: its name over its next lesson, its status, and Edit for its days and next lesson. Tapping the
 * row opens the group.
 */
@Composable
private fun ClassGroupRow(plan: Plan, g: Group, vm: PacerViewModel) {
    val fallbackName = stringResource(R.string.class_group)
    val name = g.name.ifBlank { fallbackName }
    // The short edit sheet links to the dedicated override and deletion screen.
    var editing by rememberSaveable(g.id) { mutableStateOf(false) }
    val report = remember(plan, g) { GroupReport(plan, g) }
    val short = remember(report) { report.lessonsShort() }
    val next = report.rows.firstOrNull()?.date
    val editLabel = stringResource(R.string.class_edit_group, name)
    LedgerRow(
        name,
        detail = next?.let { stringResource(R.string.class_next_on, shortDate(it)) } ?: stringResource(R.string.overview_no_lessons),
        onClick = { vm.open(Screen.Group(g.id)) },
    ) {
        StatusLabel(short)
        PaletteTextButton(
            onClick = { editing = true }, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(48.dp).semantics { contentDescription = editLabel },
        ) { AppIcon(AppIcons.Edit) }
    }
    if (editing) SheetFrame(name, stringResource(R.string.class_done), true, {}, { editing = false }) {
        GroupFields(plan, g, vm) { editing = false; vm.open(Screen.GroupSettings(g.id)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.GroupFields(plan: Plan, g: Group, vm: PacerViewModel, onSettings: () -> Unit) {
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
    TertiaryButton(stringResource(R.string.group_settings_title), onSettings)
    val count = lessons.size
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        AnimatedFigure(pluralStringResource(R.plurals.class_lessons_left, count, count), style = MaterialTheme.typography.bodyMedium)
        PaletteTextButton(onClick = { vm.open(Screen.Group(g.id)) }, modifier = Modifier.heightIn(min = 48.dp)) {
            AppIcon(AppIcons.ChevronRight)
            Text(stringResource(R.string.class_open_schedule))
        }
    }
}

/**
 * A book of the class: its name over its pages (and skipped pages), with Lead book or Supplementary marked. Tapping it
 * opens its settings.
 */
@Composable
private fun BookRow(b: Book, leads: Boolean, vm: PacerViewModel, undoToast: (String) -> Unit) {
    val context = LocalContext.current
    val fallbackName = stringResource(R.string.class_book)
    val name = b.name.ifBlank { fallbackName }
    // Its positions, homework and the pages ticked lessons recorded in it go too; Undo brings them back.
    var editing by rememberSaveable(b.id) { mutableStateOf(false) }
    val detail = if (b.supplementary) {
        val n = remember(b.include) { b.includedPages().size }
        pluralStringResource(R.plurals.class_pages_chosen, n, n)
    } else listOfNotNull(
        stringResource(R.string.class_page_range, b.first, b.last),
        b.skip.takeIf { it.isNotBlank() }?.let { stringResource(R.string.class_skips, it) },
        b.due?.let { stringResource(R.string.class_due_by, it.fmtShort()) },
        b.units.takeIf { it.isNotBlank() }?.let { pluralStringResource(R.plurals.class_units, b.unitStarts().size, b.unitStarts().size) },
    ).joinToString(" · ")
    LedgerRow(name, detail = detail, onClick = { editing = true }) {
        if (b.groupOnly) Tag(stringResource(R.string.group_settings_optional_book))
        if (leads) Tag(stringResource(R.string.class_lead_book))
        if (b.supplementary) Tag(stringResource(R.string.supp_tag))
        Chevron()
    }
    if (editing) SheetFrame(name, stringResource(R.string.class_done), true, {}, { editing = false }) {
        BookFields(b, vm) { editing = false; vm.removeBook(b.id); undoToast(context.getString(R.string.class_removed, name)) }
    }
}

@Composable
private fun ColumnScope.BookFields(b: Book, vm: PacerViewModel, onRemove: () -> Unit) {
    val ui by vm.state.collectAsState()
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommitField(b.name, { vm.renameBook(b.id, it) }, stringResource(R.string.class_title), Modifier.weight(1f))
    }
    BookTypePicker(b.type) { vm.setBookType(b.id, it) }
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
        if (ui.data.schoolClass?.unitsEnabled == true) {
            TertiaryButton(stringResource(R.string.units_title), { vm.open(Screen.Units) })
        } else {
            CommitField(b.units, { vm.setUnits(b.id, it) }, stringResource(R.string.class_units), Modifier.fillMaxWidth(), placeholder = stringResource(R.string.class_units_placeholder))
            Hint(stringResource(R.string.class_units_hint))
        }
        TertiaryButton(stringResource(R.string.class_remove_book), onRemove, color = Pacer.colors.warn)
    }
    val skipN = remember(b.skip, b.first, b.last) { parseRanges(b.skip).count { it in b.first..b.last } }
    Text(
        stringResource(R.string.class_book_summary, stringResource(R.string.class_page_range, b.first, b.last), pluralStringResource(R.plurals.class_teachable, maxOf(0, b.last - b.first + 1 - skipN), maxOf(0, b.last - b.first + 1 - skipN))),
        style = MaterialTheme.typography.bodySmall.merge(Figures), color = Pacer.colors.muted,
    )
}

/** Compatibility destination for old class-level routes, kept for one release. */
@Composable
fun MovedWorkScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues) {
    EditList(plan, Screen.Class, vm, padding) {
        item { Hint(stringResource(R.string.work_moved)) }
        plan.groups.forEach { g -> item(key = g.id) {
            LedgerRow(g.name, onClick = { vm.goTo(Screen.Group(g.id, GroupPage.WORK)) }) { Chevron() }
        } }
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

/** The book's type, which gives it its short name; Supplementary is the one setting that makes a book hand-placed. */
@Composable
private fun BookTypePicker(type: BookType, onSelect: (BookType) -> Unit) {
    Picker(
        stringResource(R.string.book_type), BookType.entries.map { it.name to stringResource(bookTypeName(it)) },
        type.name, { onSelect(BookType.valueOf(it)) }, Modifier.fillMaxWidth(),
    )
}

private fun bookTypeName(type: BookType): Int = when (type) {
    BookType.STUDENT -> R.string.book_type_student
    BookType.WORKBOOK -> R.string.book_type_workbook
    BookType.GRAMMAR -> R.string.book_type_grammar
    BookType.WRITING -> R.string.book_type_writing
    BookType.SKILLS -> R.string.book_type_skills
    BookType.READER -> R.string.book_type_reader
    BookType.SUPPLEMENTARY -> R.string.book_type_supplementary
    BookType.UNCLASSIFIED -> R.string.book_type_none
}

/** A labelled switch with the shared toggle haptic and the ✓/✕ thumb (§5). */
@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val toggleHaptic = rememberToggleHaptic()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Pacer.colors.accentText, checkedThumbColor = Pacer.colors.paper, checkedIconColor = Pacer.colors.accentText), checked = checked, onCheckedChange = { toggleHaptic(it); onChange(it) },
            thumbContent = { AppIcon(if (checked) AppIcons.Check else AppIcons.Close, Modifier.size(Space.l)) })
    }
}

@Composable
private fun AddBookSheet(vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var first by rememberSaveable { mutableStateOf("1") }
    var last by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(BookType.UNCLASSIFIED.name) }
    val type = BookType.valueOf(typeName)
    val supplementary = type == BookType.SUPPLEMENTARY
    var include by rememberSaveable { mutableStateOf("") }
    val lastN = last.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE }
    val includeOk = !parseInclude(include).isNullOrEmpty()
    SheetFrame(stringResource(R.string.class_add_a_book), stringResource(R.string.class_add), if (supplementary) includeOk else lastN != null, {
        if (supplementary) vm.addSupplementaryBook(name.trim(), include)
        else vm.addBook(name.trim(), first.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE } ?: 1, lastN!!, type)
    }, onDismiss) {
        CommitField(name, { name = it }, stringResource(R.string.class_title), Modifier.fillMaxWidth(), placeholder = stringResource(R.string.class_companion))
        BookTypePicker(type) { typeName = it.name }
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
