package gr.edusuite.pacer.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.SchoolYear
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

/**
 * The year switcher at the top (decision 6: every school year, with + New year, Rename and Delete), then the open
 * year's name, term dates and days off, and printing.
 */
@Composable
fun YearSettingsScreen(years: List<SchoolYear>, year: SchoolYear, vm: PacerViewModel, padding: PaddingValues, toast: (String) -> Unit, undoToast: (String) -> Unit, onDeleted: (String) -> Unit) {
    val context = LocalContext.current
    var adding by rememberSaveable { mutableStateOf(false) }
    var newYear by rememberSaveable { mutableStateOf(false) }
    var printing by rememberSaveable { mutableStateOf(false) }
    // The year being renamed, by id.
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable(year.id) { mutableStateOf<String?>(null) }
    val problems = remember(year) { Nav.problems(year).filter { it.fix == Screen.YearSettings } }
    val sorted = remember(years) { years.sortedByDescending { it.start } }
    val deleteYear = { y: SchoolYear ->
        val name = y.name.ifBlank { context.getString(R.string.year_school_year) }
        vm.deleteYear(y.id)
        onDeleted(name)
    }

    ScreenList(padding) {
        item(key = "years") {
            Section(stringResource(R.string.year_switch)) {
                Column(Modifier.selectableGroup()) {
                    sorted.forEach { y ->
                        YearRow(
                            y, selected = y.id == year.id,
                            onOpen = { vm.switchYear(y.id) },
                            onRename = { renaming = y.id },
                            onDelete = { deleteYear(y) },
                        )
                    }
                }
                AddButton(stringResource(R.string.home_new_year)) { newYear = true }
            }
        }
        items(problems) { Box(Modifier.animateItem()) { ErrorBox(it.text) } }
        item(key = "year") {
            Section(stringResource(R.string.year_school_year)) {
                LinkRow(stringResource(R.string.year_name), year.name.ifBlank { stringResource(R.string.year_school_year) }) { editing = "name" }
                val datesSet = year.start != null && year.end != null
                val validDates = datesSet && !year.end!!.isBefore(year.start)
                LinkRow(stringResource(R.string.year_term), if (datesSet) "${year.start!!.fmt()} – ${year.end!!.fmt()}" else stringResource(R.string.year_dates_not_set)) { editing = "term" }
                if (!validDates) Hint(stringResource(R.string.year_date_warning), color = Pacer.colors.warn)
            }
        }
        item(key = "offhead") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Heading(stringResource(R.string.year_days_off), Modifier.weight(1f))
                TertiaryButton(stringResource(R.string.year_add), { adding = true })
            }
        }
        if (year.closures.isEmpty()) item(key = "empty") { Hint(stringResource(R.string.year_no_days_off)) }
        items(year.closures.sortedBy { it.from }, key = { it.id }) { c ->
            val detail = if (c.to == c.from) "${c.from.fmt()} · ${c.from.dayOfWeek.short()}" else "${c.from.fmt()} – ${c.to.fmt()}"
            val title = c.label.ifBlank { stringResource(R.string.year_day_off) }
            val remove = { vm.removeClosure(c.id); undoToast(context.getString(R.string.year_removed, title)) }
            Box(Modifier.animateItem()) { SwipeRow(title, remove) { ListItemRow(title, detail, whoTag(context, year, c.who), onRemove = remove) } }
        }

        item(key = "greek") {
            Column {
                TextButton(onClick = {
                    val n = vm.addGreekHolidays()
                    toast(if (n > 0) context.resources.getQuantityString(R.plurals.year_holidays_added, n, n) else context.getString(R.string.year_already_there))
                }, enabled = year.start != null && year.end != null, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.year_add_holidays)) }
                Hint(
                    stringResource(R.string.year_holidays_hint),
                )
            }
        }
        item(key = "print") {
            // Until the group's ⋮ menu offers Print (3.4.0), the year's print lives here.
            Section(stringResource(R.string.home_print)) {
                LinkRow(stringResource(R.string.home_print), stringResource(R.string.home_print_hint)) { printing = true }
            }
        }
        item(key = "delete") {
            MoreOptions {
                TertiaryButton(stringResource(R.string.year_delete), { deleteYear(year) }, color = Pacer.colors.warn)
            }
        }
    }

    if (adding) AddClosureSheet(year, vm) { adding = false }
    if (newYear) NewYearDialog(years, vm) { newYear = false }
    if (printing) PrintDialog(year) { printing = false }
    years.firstOrNull { it.id == renaming }?.let { y -> RenameYearSheet(y, vm) { renaming = null } }
    editing?.let { field ->
        SheetFrame(if (field == "name") stringResource(R.string.year_school_year_name) else stringResource(R.string.year_term), stringResource(R.string.year_done), true, {}, { editing = null }) {
            if (field == "name") CommitField(year.name, { vm.renameYear(year.id, it) }, stringResource(R.string.year_name), Modifier.fillMaxWidth())
            else {
                DateField(year.start, vm::setStart, stringResource(R.string.year_starts), Modifier.fillMaxWidth(), clearable = true)
                DateField(year.end, vm::setEnd, stringResource(R.string.year_ends), Modifier.fillMaxWidth(), clearable = true)
                Hint(stringResource(R.string.year_term_hint))
                if (year.start != null && year.end != null && year.end!!.isBefore(year.start)) ErrorBox(stringResource(R.string.year_date_warning))
            }
        }
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

/** The year's classes. Tap one to open it. */
@Composable
fun ClassesScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val problems = remember(year) { Nav.problems(year).groupingBy { it.classId }.eachCount() }

    ScreenList(padding) {
        item(key = "hint") { Hint(stringResource(R.string.year_classes_hint)) }
        if (year.classes.isEmpty()) {
            item(key = "empty") {
                EmptyState(AppIcons.Add, stringResource(R.string.year_first_class), stringResource(R.string.year_first_class_hint), stringResource(R.string.year_new_class), { adding = true })
            }
        }
        item(key = "classes") {
            TileGrid(year.classes) { c, tileModifier ->
                val n = problems[c.id] ?: 0
                val lessons = remember(year, c) { val plan = year.planFor(c); c.groups.sumOf { GroupReport(plan, it).lessons.size } }
                Tile(Pacer.colors.tileSky, modifier = tileModifier.semantics(mergeDescendants = true) {}, onClick = { vm.openClass(c.id) }) {
                    AppIcon(AppIcons.School)
                    if (n > 0) Tag(pluralStringResource(R.plurals.year_problems, n, n), warn = true, onTile = true)
                    Text(c.name.ifBlank { stringResource(R.string.year_class) }, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(c.groups.joinToString(", ") { it.name }.ifEmpty { stringResource(R.string.year_no_groups) } + " · " + pluralStringResource(R.plurals.year_books, c.books.size, c.books.size), style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.onTileMuted)
                    AnimatedFigure(lessons.toString(), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.overview_lessons_left), style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
                }
            }
        }
        item(key = "add") { PrimaryButton(stringResource(R.string.year_new_class), { adding = true }, Modifier.fillMaxWidth()) }
        item(key = "foot") { Hint(stringResource(R.string.year_defaults_hint)) }
    }

    if (adding) {
        var name by rememberSaveable { mutableStateOf("") }
        SheetFrame(stringResource(R.string.year_new_class), stringResource(R.string.year_add), true, { vm.newClass(name.trim()) }, { adding = false }) {
            CommitField(name, { name = it }, stringResource(R.string.year_name), Modifier.fillMaxWidth(), placeholder = stringResource(R.string.year_class_example))
        }
    }
}

/** A year in the picker: a radio, its name over its dates and class count, and a ⋮ menu. */
@Composable
internal fun YearRow(y: SchoolYear, selected: Boolean, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val tick = rememberSegmentHaptic()
    val name = y.name.ifBlank { context.getString(R.string.home_year) }
    val options = stringResource(R.string.home_options, name)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { tick(); onOpen() }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = Space.m))
        Column(Modifier.weight(1f).padding(vertical = Space.s)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.home_pair, y.dates(context), pluralStringResource(R.plurals.home_classes_count, y.classes.size, y.classes.size)),
                style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted,
            )
        }
        Box {
            TextButton(
                onClick = { menu = true },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp).semantics { contentDescription = options },
            ) { AppIcon(AppIcons.MoreVert) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.home_rename)) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text(stringResource(R.string.home_delete), color = Pacer.colors.warn) }, onClick = { menu = false; onDelete() })
            }
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
