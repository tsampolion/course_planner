package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.lessonsFor
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate

/** Overrides are edited against the class catalogue; defaults remain visible beside them. */
@Composable
fun GroupSettingsScreen(
    year: SchoolYear,
    plan: Plan,
    group: Group,
    vm: PacerViewModel,
    padding: PaddingValues,
    undoToast: (String) -> Unit = {},
    toast: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var addingClosure by rememberSaveable(group.id) { mutableStateOf(false) }
    var addingBook by rememberSaveable(group.id) { mutableStateOf(false) }
    val classId = year.classes.firstOrNull { c -> c.groups.any { it.id == group.id } }?.id
    val inherited = year.closures.filter { it.who == ALL_GROUPS || it.who == classId }
    val ownClosures = year.closures.filter { it.who == group.id }.sortedBy { it.from }
    val nextLesson = remember(plan, group) { plan.lessonsFor(group).firstOrNull() ?: group.from ?: plan.start }
    ScreenList(padding) {
        item(key = 1) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                SectionHead(stringResource(R.string.group_settings_dates))
                DateField(nextLesson, { vm.setNextLesson(group.id, it) }, stringResource(R.string.class_next_lesson), Modifier.fillMaxWidth(), clearable = true)
                Hint(stringResource(R.string.group_settings_default_start, plan.start?.fmt() ?: stringResource(R.string.component_not_set)))
                DateField(group.end, { date -> vm.setGroupEnd(group.id, date) }, stringResource(R.string.group_settings_end), Modifier.fillMaxWidth(), clearable = true)
                Hint(stringResource(R.string.group_settings_default_end, plan.end?.fmt() ?: stringResource(R.string.component_not_set)))
                if (group.end != null && plan.end != null && group.end > plan.end) Hint(stringResource(R.string.group_settings_end_clamped), color = Pacer.colors.warn)
            }
        }
        item(key = 2) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                SectionHead(stringResource(R.string.group_settings_days_off), action = stringResource(R.string.year_add), onAction = { addingClosure = true })
                Hint(stringResource(R.string.group_settings_days_off_hint))
                if (inherited.isNotEmpty()) {
                    Text(stringResource(R.string.group_settings_inherited_days_off), style = MaterialTheme.typography.labelLarge)
                    RowGroup {
                        inherited.sortedBy { it.from }.forEachIndexed { i, c ->
                            if (i > 0) RowDivider()
                            LedgerRow(c.label.ifBlank { stringResource(R.string.year_day_off) }, date = dayOffDates(c.from, c.to), edge = Pacer.colors.edgeDayOff)
                        }
                    }
                }
                if (ownClosures.isEmpty()) Hint(stringResource(R.string.group_settings_no_days_off))
                else RowGroup {
                    ownClosures.forEachIndexed { i, c ->
                        if (i > 0) RowDivider()
                        val title = c.label.ifBlank { stringResource(R.string.year_day_off) }
                        val remove = { vm.removeClosure(c.id); undoToast(context.getString(R.string.year_removed, title)) }
                        LedgerRow(title, date = dayOffDates(c.from, c.to), edge = Pacer.colors.edgeDayOff) { RemoveMenu(title, remove) }
                    }
                }
                PaletteTextButton(onClick = {
                    val count = vm.addGreekHolidays(group.id)
                    if (count > 0) undoToast(context.resources.getQuantityString(R.plurals.year_holidays_added, count, count))
                    else toast(context.getString(R.string.year_already_there))
                }, enabled = year.start != null && year.end != null, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.year_add_greek))
                }
            }
        }
        item(key = 3) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                SectionHead(stringResource(R.string.group_settings_skipped_pages))
                Hint(stringResource(R.string.group_settings_skip_hint))
                plan.books.filter { !it.supplementary && (!it.groupOnly || it.id in group.extraBooks) }.forEach { book ->
                    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        CommitField(group.skip[book.id].orEmpty(), { vm.setGroupSkip(group.id, book.id, it) }, book.name.ifBlank { stringResource(R.string.class_book) }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.group_settings_skip_example))
                        Hint(stringResource(R.string.group_settings_class_skip, book.skip.ifBlank { stringResource(R.string.group_settings_none) }))
                    }
                }
            }
        }
        item(key = 4) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                SectionHead(stringResource(R.string.group_settings_books), action = stringResource(R.string.class_add_a_book), onAction = { addingBook = true })
                Hint(stringResource(R.string.group_settings_books_hint))
                RowGroup {
                    plan.books.forEachIndexed { i, book ->
                        if (i > 0) RowDivider()
                        val title = book.name.ifBlank { stringResource(R.string.class_book) }
                        if (!book.groupOnly) LedgerRow(title, detail = stringResource(R.string.group_settings_default_book))
                        else {
                            val selected = book.id in group.extraBooks
                            LedgerRow(title, detail = stringResource(R.string.group_settings_optional_book), onClick = { vm.setGroupExtraBook(group.id, book.id, !selected) }) {
                                Checkbox(checked = selected, onCheckedChange = null)
                            }
                        }
                    }
                }
            }
        }
        item(key = "units") {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                SectionHead(stringResource(R.string.units_title))
                Picker(stringResource(R.string.units_group_setting), listOf(
                    "inherit" to stringResource(R.string.units_follow_class),
                    "on" to stringResource(R.string.units_on), "off" to stringResource(R.string.units_off)),
                    group.unitsOverride?.let { if (it) "on" else "off" } ?: "inherit",
                    { vm.setGroupUnitsOverride(group.id, when (it) { "on" -> true; "off" -> false; else -> null }) }, Modifier.fillMaxWidth())
                if (!plan.unitsEnabled) Hint(stringResource(R.string.units_class_off))
                else if (group.unitsOverride != null && group.unitsOverride != plan.unitsEnabled)
                    Hint(stringResource(R.string.units_override_warning), color = Pacer.colors.warn)
                TertiaryButton(stringResource(R.string.units_title), { vm.open(Screen.Units) })
            }
        }
        item(key = 5) {
            FootActions(null, stringResource(R.string.group_settings_delete) to {
                val name = group.name.ifBlank { context.getString(R.string.class_group) }
                vm.removeGroup(group.id)
                undoToast(context.getString(R.string.class_removed, name))
            })
        }
    }
    if (addingClosure) GroupClosureSheet(group.id, vm) { addingClosure = false }
    if (addingBook) GroupBookSheet(group.id, vm) { addingBook = false }
}

/** Scope is fixed to this group so the form cannot accidentally change the class calendar. */
@Composable
private fun GroupClosureSheet(groupId: String, vm: PacerViewModel, onDismiss: () -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var fromText by rememberSaveable { mutableStateOf<String?>(null) }
    var toText by rememberSaveable { mutableStateOf<String?>(null) }
    val from = fromText?.let(LocalDate::parse)
    val to = toText?.let(LocalDate::parse)
    SheetFrame(stringResource(R.string.year_add_day_off), stringResource(R.string.year_add), from != null && (to == null || to >= from), {
        vm.addClosure(label.trim(), from!!, to, groupId)
    }, onDismiss) {
        Hint(stringResource(R.string.group_settings_days_off_hint))
        CommitField(label, { label = it }, stringResource(R.string.year_reason), Modifier.fillMaxWidth(), placeholder = stringResource(R.string.year_reason_example))
        DateField(from, { fromText = it?.toString() }, stringResource(R.string.year_from), Modifier.fillMaxWidth())
        DateField(to, { toText = it?.toString() }, stringResource(R.string.year_to_optional), Modifier.fillMaxWidth(), clearable = true)
    }
}

/** New optional definitions enter the class catalogue and are selected only for this group. */
@Composable
private fun GroupBookSheet(groupId: String, vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var first by rememberSaveable { mutableStateOf("1") }
    var last by rememberSaveable { mutableStateOf("") }
    val firstN = first.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE }
    val lastN = last.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE }
    val valid = name.isNotBlank() && firstN != null && lastN != null && lastN >= firstN
    SheetFrame(stringResource(R.string.class_add_a_book), stringResource(R.string.class_add), valid, {
        vm.addGroupBook(groupId, name.trim(), firstN!!, lastN!!)
    }, onDismiss) {
        Hint(stringResource(R.string.group_settings_new_book_hint))
        CommitField(name, { name = it }, stringResource(R.string.class_title), Modifier.fillMaxWidth())
        CommitField(first, { first = it }, stringResource(R.string.class_first_page), Modifier.fillMaxWidth(), numeric = true,
            error = first.isNotBlank() && firstN == null, errorMessage = stringResource(R.string.component_integer_range, 1, MAX_PAGE))
        CommitField(last, { last = it }, stringResource(R.string.class_last_page), Modifier.fillMaxWidth(), numeric = true,
            error = last.isNotBlank() && (lastN == null || (firstN != null && lastN < firstN)), errorMessage = stringResource(R.string.group_settings_page_range))
    }
}
