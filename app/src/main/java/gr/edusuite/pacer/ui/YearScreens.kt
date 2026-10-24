package gr.edusuite.pacer.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import gr.edusuite.pacer.R
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.model.ClassOrder
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate

/**
 * Who a day off can apply to: everyone, a whole class, or one group. With a single class
 * the class level is left out, since it would mean the same as everyone.
 */
private fun whoOptions(context: Context, year: SchoolYear): List<Pair<String, String>> {
    val many = year.classes.size > 1
    val out = mutableListOf(ALL_GROUPS to if (many) context.getString(R.string.year_all_classes) else context.getString(R.string.year_all_groups))
    for (c in year.classes) {
        if (many) out += c.id to context.getString(R.string.year_class_all, c.name)
        c.groups.forEach { g -> out += g.id to if (many) "${c.name} · ${g.name}" else context.getString(R.string.year_group_only, g.name) }
    }
    return out
}

/** The tag a day off shows: null for everyone, else the class or group it is aimed at. */
private fun whoTag(context: Context, year: SchoolYear, who: String): String? {
    if (who == ALL_GROUPS) return null
    val many = year.classes.size > 1
    for (c in year.classes) {
        if (c.id == who) return c.name
        val g = c.groups.firstOrNull { it.id == who } ?: continue
        return if (many) "${c.name} · ${g.name}" else g.name
    }
    return context.getString(R.string.year_removed_group)
}

/** "Wed 28 Oct" for one day, "23 Dec – 6 Jan" for a run of days. */
internal fun dayOffDates(from: LocalDate, to: LocalDate): String =
    if (to == from) "${from.dayOfWeek.short()} ${from.fmtShort()}" else "${from.fmtShort()} – ${to.fmtShort()}"

/**
 * School year (3.5.0): the years as chips at the top with + New year (decision 6), the term on one line, the days off
 * as dated rows with the day-off edge (swipe or long-press to remove), Add Greek public holidays, then Rename and Delete this school year. Printing is
 * in a group's ⋮ menu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YearSettingsScreen(years: List<SchoolYear>, year: SchoolYear, vm: PacerViewModel, padding: PaddingValues, toast: (String) -> Unit, undoToast: (String) -> Unit, onDeleted: (String) -> Unit) {
    val context = LocalContext.current
    val tick = rememberSegmentHaptic()
    var adding by rememberSaveable { mutableStateOf(false) }
    var newYear by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var editingTerm by rememberSaveable(year.id) { mutableStateOf(false) }
    val problems = remember(year) { Nav.problems(year).filter { it.fix == Screen.YearSettings } }
    val sorted = remember(years) { years.sortedByDescending { it.start } }
    val term = remember(year) { termSummary(year) }

    ScreenList(padding) {
        item(key = "years") {
            FlowRow(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                verticalArrangement = Arrangement.Center,
            ) {
                sorted.forEach { y ->
                    YearChip(y.name.ifBlank { stringResource(R.string.home_year) }, y.id == year.id) { tick(); vm.switchYear(y.id) }
                }
                PaletteTextButton(onClick = { newYear = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    AppIcon(AppIcons.Add, Modifier.size(18.dp).padding(end = Space.xs))
                    Text(stringResource(R.string.home_new_year), style = MaterialTheme.typography.labelLarge, color = Pacer.colors.accentText)
                }
            }
        }
        items(problems) { Box(Modifier.animateItem()) { ErrorBox(it.text) } }
        item(key = "term") {
            Column {
                SectionHead(stringResource(R.string.year_term), action = stringResource(R.string.year_edit), onAction = { editingTerm = true }, plus = false)
                val datesSet = year.start != null && year.end != null
                val validDates = datesSet && !year.end!!.isBefore(year.start)
                RowGroup {
                    LedgerRow(
                        if (datesSet) "${year.start!!.dayOfWeek.short()} ${year.start!!.fmt()} – ${year.end!!.dayOfWeek.short()} ${year.end!!.fmt()}" else stringResource(R.string.year_dates_not_set),
                        detail = term?.let { stringResource(R.string.home_pair, pluralStringResource(R.plurals.year_weeks, it.weeks, it.weeks), pluralStringResource(R.plurals.year_days_off_count, it.daysOff, it.daysOff)) },
                        onClick = { editingTerm = true },
                    ) { Chevron() }
                }
                if (!validDates) Hint(stringResource(R.string.year_date_warning), Modifier.padding(top = Space.s), color = Pacer.colors.warn)
            }
        }
        item(key = "offhead") { SectionHead(stringResource(R.string.year_days_off), action = stringResource(R.string.year_add), onAction = { adding = true }) }
        if (year.closures.isEmpty()) item(key = "empty") { Hint(stringResource(R.string.year_no_days_off)) }
        else item(key = "days-off") {
            RowGroup(Modifier.animateItem()) {
                year.closures.sortedBy { it.from }.forEachIndexed { i, c ->
                    if (i > 0) RowDivider()
                    val title = c.label.ifBlank { stringResource(R.string.year_day_off) }
                    val remove = { vm.removeClosure(c.id); undoToast(context.getString(R.string.year_removed, title)) }
                    val who = whoTag(context, year, c.who)?.let { stringResource(R.string.year_group_only, it) } ?: stringResource(R.string.year_all_groups)
                    SwipeRow(title, remove) {
                        LedgerRow(title, detail = who, date = dayOffDates(c.from, c.to), edge = Pacer.colors.edgeDayOff, modifier = Modifier.background(Pacer.colors.surface))
                    }
                }
            }
        }
        item(key = "greek") {
            Column {
                PaletteTextButton(onClick = {
                    val n = vm.addGreekHolidays()
                    toast(if (n > 0) context.resources.getQuantityString(R.plurals.year_holidays_added, n, n) else context.getString(R.string.year_already_there))
                }, enabled = year.start != null && year.end != null, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.year_add_greek)) }
                Hint(stringResource(R.string.year_holidays_hint))
            }
        }
        item(key = "delete") {
            FootActions(
                start = stringResource(R.string.home_rename) to { renaming = true },
                end = stringResource(R.string.year_delete) to {
                    val name = year.name.ifBlank { context.getString(R.string.year_school_year) }
                    vm.deleteYear(year.id)
                    onDeleted(name)
                },
            )
        }
    }

    if (adding) AddClosureSheet(year, vm) { adding = false }
    if (newYear) NewYearDialog(years, vm) { newYear = false }
    if (renaming) RenameYearSheet(year, vm) { renaming = false }
    if (editingTerm) SheetFrame(stringResource(R.string.year_term), stringResource(R.string.year_done), true, {}, { editingTerm = false }) {
        DateField(year.start, vm::setStart, stringResource(R.string.year_starts), Modifier.fillMaxWidth(), clearable = true)
        DateField(year.end, vm::setEnd, stringResource(R.string.year_ends), Modifier.fillMaxWidth(), clearable = true)
        Hint(stringResource(R.string.year_term_hint))
        if (year.start != null && year.end != null && year.end!!.isBefore(year.start)) ErrorBox(stringResource(R.string.year_date_warning))
    }
}

/** A year in the switcher: a 48dp pill, ink when it is the open year (D10), read as a radio button. */
@Composable
private fun YearChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val c = Pacer.colors
    Box(
        Modifier.heightIn(min = 48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name, style = MaterialTheme.typography.labelLarge.merge(Figures), color = if (selected) c.paper else c.ink,
            modifier = Modifier.clip(CircleShape).background(if (selected) c.ink else c.surfaceContainer).padding(horizontal = Space.l, vertical = Space.s),
        )
    }
}

/** Reason and From up front; the end date and who it applies to sit behind More options (§14 Progressive disclosure). */
@Composable
private fun AddClosureSheet(year: SchoolYear, vm: PacerViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val segmentHaptic = rememberSegmentHaptic()
    var label by rememberSaveable { mutableStateOf("") }
    var fromText by rememberSaveable { mutableStateOf<String?>(null) }
    var toText by rememberSaveable { mutableStateOf<String?>(null) }
    val from = fromText?.let(LocalDate::parse)
    val to = toText?.let(LocalDate::parse)
    var who by rememberSaveable { mutableStateOf(ALL_GROUPS) }
    SheetFrame(stringResource(R.string.year_add_day_off), stringResource(R.string.year_add), from != null, { vm.addClosure(label.trim(), from!!, to, who) }, onDismiss) {
        CommitField(label, { label = it }, stringResource(R.string.year_reason), Modifier.fillMaxWidth(), placeholder = stringResource(R.string.year_reason_example))
        DateField(from, { fromText = it?.toString() }, stringResource(R.string.year_from), Modifier.fillMaxWidth())
        MoreOptions {
            DateField(to, { toText = it?.toString() }, stringResource(R.string.year_to_optional), Modifier.fillMaxWidth(), clearable = true)
            Picker(stringResource(R.string.year_applies_to), whoOptions(context, year), who, { segmentHaptic(); who = it }, Modifier.fillMaxWidth())
        }
    }
}

/**
 * The year's classes (3.5.0): one row per class with its groups and books, and one status mark (how many groups are
 * behind, its problems, or a muted "On pace"). Tap one to open it; New class floats at the bottom end.
 */
@Composable
fun ClassesScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues, order: ClassOrder = ClassOrder.YOUR_ORDER, undoToast: (String) -> Unit = {}) {
    val context = LocalContext.current
    var newCollection by rememberSaveable { mutableStateOf(false) }
    var editingCollection by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val problems = remember(year) { Nav.problems(year).groupingBy { it.classId }.eachCount() }
    val rows = remember(year) { classSummaries(year) }
    val groups = rows.sumOf { it.groupNames.size }
    val sections = remember(rows, year.collections, order) { classSections(rows, year.collections, order) }

    Box(Modifier.fillMaxSize()) {
    ScreenList(padding) {
        if (year.classes.isEmpty() && year.collections.isEmpty()) {
            item(key = "hint") { Hint(stringResource(R.string.year_classes_hint)) }
            item(key = "empty") {
                EmptyState(AppIcons.Add, stringResource(R.string.year_first_class), stringResource(R.string.year_first_class_hint), stringResource(R.string.year_new_class), { adding = true })
            }
        } else {
            item(key = "head") {
                Text(
                    stringResource(R.string.home_pair, pluralStringResource(R.plurals.home_classes_count, rows.size, rows.size), pluralStringResource(R.plurals.year_groups, groups, groups)),
                    style = MaterialTheme.typography.bodySmall.merge(Figures), color = Pacer.colors.muted,
                )
            }
            item(key = "classes") {
                Column {
                    ClassOrderPicker(order, vm::setClassOrder)
                }
            }
            sections.forEach { section ->
                item(key = classSectionKey(section.collection?.id)) {
                    Column {
                        if (section.collection != null) CollectionSectionHead(section.collection,
                            onEdit = { editingCollection = section.collection.id },
                            onDelete = { if (vm.removeCollection(section.collection.id)) undoToast(context.getString(R.string.collections_deleted, section.collection.name)) },
                        )
                        else if (year.collections.isNotEmpty()) SectionHead(stringResource(R.string.collections_uncollected))
                        if (section.classes.isEmpty()) Hint(stringResource(R.string.collections_empty))
                        else RowGroup {
                            section.classes.forEachIndexed { i, r ->
                                if (i > 0) RowDivider()
                                val days = r.groupNames.map { it.ifBlank { stringResource(R.string.app_group) } }.joinToString(" · ").ifEmpty { stringResource(R.string.year_no_groups) }
                                LedgerRow(
                                    r.name.ifBlank { stringResource(R.string.year_class) },
                                    detail = stringResource(R.string.home_pair, days, pluralStringResource(R.plurals.year_books, r.books, r.books)),
                                    onClick = { vm.openClass(r.classId) },
                                ) {
                                    val n = problems[r.classId] ?: 0
                                    when {
                                        r.behind > 0 -> Tag(pluralStringResource(R.plurals.classes_behind, r.behind, r.behind), warn = true)
                                        n > 0 -> Tag(pluralStringResource(R.plurals.year_problems, n, n), warn = true)
                                        r.measured -> StatusLabel(0)
                                    }
                                    Chevron()
                                }
                            }
                        }
                    }
                }
            }
            item(key = "foot") {
                Column {
                    TertiaryButton(stringResource(R.string.collections_new), { newCollection = true })
                    Hint(stringResource(R.string.year_defaults_hint))
                }
            }
            // Room under the last row for the New class button.
            item(key = "space") { Spacer(Modifier.height(72.dp)) }
        }
    }
        if (year.classes.isNotEmpty() || year.collections.isNotEmpty()) ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { AppIcon(AppIcons.Add) },
            text = { Text(stringResource(R.string.year_new_class)) },
            containerColor = Pacer.colors.accent, contentColor = Pacer.colors.onAccent,
            // Flat at rest (decision 1).
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = Space.l, bottom = padding.calculateBottomPadding() + Space.l),
        )
    }

    if (newCollection) CollectionSheet(year, null, vm, undoToast) { newCollection = false }
    editingCollection?.let { id ->
        year.collections.firstOrNull { it.id == id }?.let { collection ->
            CollectionSheet(year, collection, vm, undoToast) { editingCollection = null }
        }
    }

    if (adding) {
        var name by rememberSaveable { mutableStateOf("") }
        var unitsEnabled by rememberSaveable { mutableStateOf(false) }
        SheetFrame(stringResource(R.string.year_new_class), stringResource(R.string.year_add), true, { vm.newClass(name.trim(), unitsEnabled) }, { adding = false }) {
            CommitField(name, { name = it }, stringResource(R.string.year_name), Modifier.fillMaxWidth(), placeholder = stringResource(R.string.year_class_example))
            SwitchRow(stringResource(R.string.units_enable), unitsEnabled) { unitsEnabled = it }
            Hint(stringResource(R.string.units_creation_hint))
        }
    }
}

/** Renames a year in a sheet; the name is saved once, on Save. */
@Composable
internal fun RenameYearSheet(y: SchoolYear, vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(y.name) }
    SheetFrame(stringResource(R.string.home_rename_year), stringResource(R.string.home_save), name.isNotBlank(), { vm.renameYear(y.id, name.trim()) }, onDismiss) {
        CommitField(name, { name = it }, stringResource(R.string.home_name), Modifier.fillMaxWidth())
    }
}
