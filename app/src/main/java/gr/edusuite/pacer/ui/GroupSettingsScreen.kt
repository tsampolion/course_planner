package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.endFor
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.lessonsFor
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Closure
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
    var editingClosure by rememberSaveable(group.id) { mutableStateOf<String?>(null) }
    var optionsBook by rememberSaveable(group.id) { mutableStateOf<String?>(null) }
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
                        var menu by remember { mutableStateOf(false) }
                        Box {
                            LedgerRow(
                                title, date = dayOffDates(c.from, c.to), edge = Pacer.colors.edgeDayOff,
                                onLongClick = { menu = true }, longClickLabel = stringResource(R.string.component_options),
                            ) { RemoveMenu(title, remove) }
                            PacerDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.class_edit)) }, leadingIcon = { AppIcon(AppIcons.Edit) },
                                    onClick = { menu = false; editingClosure = c.id },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.rows_delete), color = Pacer.colors.warn) }, leadingIcon = { AppIcon(AppIcons.Delete) },
                                    onClick = { menu = false; remove() },
                                )
                            }
                        }
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
                plan.books.filter { !it.supplementary && group.usesBook(it) }.forEach { book ->
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
                Hint(stringResource(R.string.group_settings_books_long_press))
                RowGroup {
                    plan.books.forEachIndexed { i, book ->
                        if (i > 0) RowDivider()
                        val title = book.name.ifBlank { stringResource(R.string.class_book) }
                        val removed = book.id in group.removedBooks
                        val own = group.bookDue[book.id]
                        val detail = listOfNotNull(
                            stringResource(if (removed) R.string.group_settings_removed_book else if (book.groupOnly) R.string.group_settings_optional_book else R.string.group_settings_default_book),
                            own?.takeIf { !removed && !book.supplementary }?.let { stringResource(R.string.class_due_by, it.fmtShort()) },
                        ).joinToString(" · ")
                        val options = stringResource(R.string.component_options)
                        if (!book.groupOnly) LedgerRow(title, detail = detail, onLongClick = { optionsBook = book.id }, longClickLabel = options)
                        else {
                            val selected = book.id in group.extraBooks
                            LedgerRow(title, detail = detail, onClick = { vm.setGroupExtraBook(group.id, book.id, !selected) }, onLongClick = { optionsBook = book.id }, longClickLabel = options) {
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
    if (addingClosure) GroupClosureSheet(group.id, null, vm) { addingClosure = false }
    ownClosures.firstOrNull { it.id == editingClosure }?.let { c -> GroupClosureSheet(group.id, c, vm) { editingClosure = null } }
    plan.books.firstOrNull { it.id == optionsBook }?.let { book ->
        GroupBookOptionsSheet(plan, group, book, vm, toast) { optionsBook = null }
    }
    if (addingBook) GroupBookSheet(group.id, vm) { addingBook = false }
}

/** Scope is fixed to this group so the form cannot accidentally change the class calendar. With [editing] the sheet starts filled in and saves over that day off. */
@Composable
private fun GroupClosureSheet(groupId: String, editing: Closure?, vm: PacerViewModel, onDismiss: () -> Unit) {
    var label by rememberSaveable(editing?.id) { mutableStateOf(editing?.label.orEmpty()) }
    var fromText by rememberSaveable(editing?.id) { mutableStateOf(editing?.from?.toString()) }
    var toText by rememberSaveable(editing?.id) { mutableStateOf(editing?.takeIf { it.to != it.from }?.to?.toString()) }
    val from = fromText?.let(LocalDate::parse)
    val to = toText?.let(LocalDate::parse)
    SheetFrame(
        stringResource(if (editing == null) R.string.year_add_day_off else R.string.year_edit_day_off),
        stringResource(if (editing == null) R.string.year_add else R.string.component_save),
        from != null && (to == null || to >= from), {
            if (editing == null) vm.addClosure(label.trim(), from!!, to, groupId)
            else vm.editClosure(editing.id, groupId, label.trim(), from!!, to)
        }, onDismiss,
    ) {
        Hint(stringResource(R.string.group_settings_days_off_hint))
        CommitField(label, { label = it }, stringResource(R.string.year_reason), Modifier.fillMaxWidth(), placeholder = stringResource(R.string.year_reason_example))
        DateField(from, { fromText = it?.toString() }, stringResource(R.string.year_from), Modifier.fillMaxWidth())
        DateField(to, { toText = it?.toString() }, stringResource(R.string.year_to_optional), Modifier.fillMaxWidth(), clearable = true)
    }
}

/**
 * What the group can change about one book, opened by a long-press: leave the book out (a default book) or bring it
 * back, and a finish-by date for this group only, with the class's date shown and a way back to it.
 */
@Composable
private fun GroupBookOptionsSheet(plan: Plan, group: Group, book: Book, vm: PacerViewModel, toast: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val removed = book.id in group.removedBooks
    val own = group.bookDue[book.id]
    val end = plan.endFor(group)
    SheetFrame(book.name.ifBlank { stringResource(R.string.class_book) }, stringResource(R.string.class_done), true, {}, onDismiss) {
        if (!removed && !book.supplementary) {
            DateField(own, { vm.setGroupBookDue(group.id, book.id, it) }, stringResource(R.string.class_finish_by), Modifier.fillMaxWidth())
            Hint(stringResource(R.string.group_settings_class_date, book.due?.fmt() ?: stringResource(R.string.component_not_set)))
            if (own != null && end != null && own > end) Hint(stringResource(R.string.group_settings_book_due_clamped, end.fmt()), color = Pacer.colors.warn)
            if (own != null) TertiaryButton(stringResource(R.string.group_settings_use_class_date), { vm.setGroupBookDue(group.id, book.id, null) })
        }
        if (!book.groupOnly) {
            if (removed) TertiaryButton(stringResource(R.string.group_settings_add_back), { vm.setGroupBookRemoved(group.id, book.id, false) })
            else TertiaryButton(stringResource(R.string.group_settings_remove_book), {
                if (vm.setGroupBookRemoved(group.id, book.id, true)) onDismiss()
                else toast(context.getString(R.string.group_settings_keep_one_book))
            }, color = Pacer.colors.warn)
            if (!removed) Hint(stringResource(R.string.group_settings_remove_book_hint))
        }
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
