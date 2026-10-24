package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.model.RevisionKind
import java.time.LocalDate

@Composable
internal fun UnitRevisionSheet(report: GroupReport, target: UnitRevisionTarget, vm: PacerViewModel, onChanged: (String) -> Unit = {}) {
    val old = report.group.unitRevisions.firstOrNull { it.unitIndex == target.unitIndex }
    val assignment = report.group.assignments.firstOrNull { it.id == old?.assignmentId }
    val dates = revisionDates(report, target.unitIndex, LocalDate.now())
    var kind by rememberSaveable(target) { mutableStateOf(old?.kind ?: RevisionKind.TEST) }
    var date by rememberSaveable(target) { mutableStateOf(assignment?.due?.takeIf { it in dates }?.toString() ?: dates.firstOrNull()?.toString()) }
    val books = report.plan.books.filter { it.supplementary }
    var book by rememberSaveable(target) { mutableStateOf(old?.pages?.keys?.firstOrNull() ?: books.firstOrNull()?.id) }
    var pages by rememberSaveable(target) { mutableStateOf(old?.pages?.values?.flatten()?.joinToString(", ").orEmpty()) }
    val chosen = books.firstOrNull { it.id == book }
    val parsed = chosen?.let { assignmentPages(pages, it) }
    val valid = kind != RevisionKind.PAGES || (chosen != null && !parsed.isNullOrEmpty())
    val title = stringResource(R.string.units_revision_title, target.unitIndex + 1)
    val savedMessage = stringResource(R.string.units_revision_saved)
    val removedMessage = stringResource(R.string.units_revision_removed)
    WorkSheetFrame(title, stringResource(R.string.component_save), valid, {
        if (vm.saveUnitRevision(target, kind, date?.let(LocalDate::parse),
            if (kind == RevisionKind.PAGES && chosen != null) mapOf(chosen.id to parsed.orEmpty()) else emptyMap(), title)) onChanged(savedMessage)
    }, { vm.dismissUnitRevision(target) }) {
        Picker(stringResource(R.string.units_revision_format), RevisionKind.entries.map { it.name to stringResource(when(it) {
            RevisionKind.TEST -> R.string.work_test
            RevisionKind.WORKSHEET -> R.string.work_worksheet
            RevisionKind.PAGES -> R.string.units_revision_pages
            RevisionKind.NONE -> R.string.units_revision_none
        }) }, kind.name, { kind = RevisionKind.valueOf(it) })
        if (kind != RevisionKind.NONE) {
            if (dates.isEmpty()) Hint(stringResource(R.string.units_revision_unscheduled))
            else Picker(stringResource(R.string.units_revision_lesson), dates.map { it.toString() to it.toString() }, date,
                { date = it })
        }
        if (kind == RevisionKind.PAGES) {
            if (books.isEmpty()) Hint(stringResource(R.string.units_revision_no_books))
            else {
                Picker(stringResource(R.string.units_revision_book), books.map { it.id to it.name }, book, { book = it })
                OutlinedTextField(pages, { pages = it }, label = { Text(stringResource(R.string.units_revision_pages)) },
                    isError = !valid, modifier = Modifier.fillMaxWidth(), colors = paletteFieldColors())
                if (!valid) Hint(stringResource(R.string.units_revision_pages_error))
            }
        }
        if (old != null) TertiaryButton(stringResource(R.string.units_revision_remove), { if (vm.removeUnitRevision(target)) onChanged(removedMessage) })
    }
}
