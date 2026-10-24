package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.coreBooks
import gr.edusuite.pacer.engine.validUnitStarts
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

/** Three steps stay editable after setup; links are always optional. */
@Composable
fun UnitsSettingsScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val lead = plan.coreBooks.firstOrNull { it.id == plan.lead }
    val leadStarts = lead?.validUnitStarts()
    val linked = linkedUnitBooks(plan)
    ScreenList(padding) {
        item { Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            SwitchRow(stringResource(R.string.units_enable), plan.unitsEnabled, vm::setUnitsEnabled)
            Hint(stringResource(R.string.units_rule))
        } }
        item { Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            SectionHead(stringResource(R.string.units_step_lead))
            if (plan.coreBooks.isEmpty()) {
                Hint(stringResource(R.string.units_no_books))
                TertiaryButton(stringResource(R.string.class_add_a_book), { vm.goTo(Screen.Class) })
            } else Picker(stringResource(R.string.class_lead_book), plan.coreBooks.map { it.id to it.name }, plan.lead,
                { vm.setLead(it) }, Modifier.fillMaxWidth())
        } }
        item { Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            SectionHead(stringResource(R.string.units_step_starts))
            Hint(stringResource(R.string.units_starts_hint))
            RowGroup { plan.coreBooks.forEachIndexed { i, book ->
                if (i > 0) RowDivider()
                val starts = book.validUnitStarts()
                LedgerRow(book.name, detail = if (starts == null) stringResource(R.string.units_needs_starts)
                    else stringResource(R.string.units_count, starts.size), onClick = { editing = book.id }) {
                    TertiaryButton(stringResource(if (starts == null) R.string.units_add else R.string.class_edit), { editing = book.id })
                }
            } }
        } }
        item { Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            SectionHead(stringResource(R.string.units_step_links))
            Hint(stringResource(R.string.units_links_hint))
            if (lead == null || leadStarts == null) Hint(stringResource(R.string.units_choose_lead_first), color = Pacer.colors.warn)
            else plan.coreBooks.filter { it.id != lead.id }.forEach { book ->
                val starts = book.validUnitStarts()
                val compatible = starts?.size == leadStarts.size
                val selected = book.id in linked
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    SwitchRow(book.name, selected) { checked ->
                        if (!checked || compatible) {
                            val next = if (checked) linked + lead.id + book.id else linked - book.id
                            vm.setBookLinks(if (next.size > 1) listOf(next) else emptyList())
                        } else editing = book.id
                    }
                    Hint(stringResource(if (compatible) R.string.units_matches else R.string.units_mismatch, starts?.size ?: 0),
                        color = if (compatible) Pacer.colors.muted else Pacer.colors.warn)
                    if (!compatible) TertiaryButton(stringResource(R.string.units_add), { editing = book.id })
                }
            }
            TertiaryButton(stringResource(R.string.units_dont_link), { vm.setBookLinks(emptyList()) })
            Hint(stringResource(R.string.units_links_later))
        } }
    }
    plan.coreBooks.firstOrNull { it.id == editing }?.let { book ->
        UnitStartsSheet(book, { vm.saveUnitStarts(book.id, it) }) { editing = null }
    }
}

@Composable
private fun UnitStartsSheet(book: Book, onSave: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val old = book.units.takeIf { it.isNotBlank() }?.split(',', ';')?.map { it.trim() }.orEmpty()
    var rows by rememberSaveable(book.id) { mutableStateOf(old.takeIf { it.isNotEmpty() } ?: listOf(book.first.toString())) }
    val starts = validateUnitStarts(book.first, book.last, rows)
    WorkSheetFrame(book.name, stringResource(R.string.component_save), starts != null, { onSave(rows); onDismiss() }, onDismiss) {
        val maximum = (book.last - book.first + 1).coerceAtLeast(1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            PillIconButton(stringResource(R.string.units_fewer), { rows = rows.dropLast(1) }, enabled = rows.size > 1) {
                Text(stringResource(R.string.units_minus))
            }
            IntField(rows.size, { count -> rows = List(count) { rows.getOrNull(it).orEmpty() } },
                stringResource(R.string.units_number), Modifier.weight(1f), max = maximum)
            PillIconButton(stringResource(R.string.units_more), { rows = rows + "" }, enabled = rows.size < maximum) { AppIcon(AppIcons.Add) }
        }
        TertiaryButton(stringResource(R.string.units_space_evenly), {
            evenlySpacedUnits(book.first, book.last, rows.size)?.let { rows = it.map(Int::toString) }
        })
        rows.forEachIndexed { i, value ->
            CommitField(value, { text -> rows = rows.toMutableList().also { it[i] = text } },
                stringResource(R.string.units_start_page, i + 1), Modifier.fillMaxWidth(), numeric = true)
            if (starts != null) Hint(stringResource(R.string.units_end_page, i + 1, starts.getOrNull(i + 1)?.minus(1) ?: book.last))
        }
        if (starts == null) Hint(stringResource(R.string.units_start_error, book.first, book.last), color = Pacer.colors.warn)
    }
}
