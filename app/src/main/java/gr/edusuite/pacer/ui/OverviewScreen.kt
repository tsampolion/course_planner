package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

/**
 * Pacing overview (decision 7): every group of the year class by class, with a sort that reorders groups only inside
 * each class and one status mark per row. The year's problems are in the sheet behind the top bar's warning button.
 */
@Composable
fun OverviewScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues) {
    var order by rememberSaveable { mutableStateOf(GroupOrder.MOST_BEHIND) }
    val overview = remember(year) { pacingOverview(year) }
    val classes = remember(overview, order) { sortGroups(overview, order) }
    val groups = overview.sumOf { it.groups.size }
    val behind = overview.sumOf { it.behind }

    ScreenList(padding) {
        if (classes.isEmpty()) item(key = "empty") {
            EmptyState(AppIcons.Add, stringResource(R.string.overview_no_classes), stringResource(R.string.overview_empty_hint), stringResource(R.string.overview_open_classes), { vm.open(Screen.Classes) })
        } else item(key = "head") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) {
                    Text(pluralStringResource(R.plurals.year_groups, groups, groups), style = MaterialTheme.typography.bodySmall.merge(Figures), color = Pacer.colors.muted)
                    if (behind > 0) Text(
                        pluralStringResource(R.plurals.overview_behind_suffix, behind, behind),
                        style = MaterialTheme.typography.bodySmall.merge(Figures), color = Pacer.colors.warn,
                    )
                }
                SortButton(order) { order = it }
            }
        }
        classes.forEach { c ->
            item(key = "class:${c.classId}") {
                Column(Modifier.animateItem()) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = Space.s), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            c.name.ifBlank { stringResource(R.string.overview_class) }, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).semantics { heading() },
                        )
                        when {
                            c.behind > 0 -> Text(pluralStringResource(R.plurals.overview_class_behind, c.behind, c.behind), style = MaterialTheme.typography.labelMedium.merge(Figures), color = Pacer.colors.warn)
                            c.groups.any { it.short != null } -> Text(stringResource(R.string.overview_all_on_pace), style = MaterialTheme.typography.labelMedium, color = Pacer.colors.muted)
                        }
                    }
                    if (c.groups.isEmpty()) Hint(stringResource(R.string.overview_no_groups))
                    else RowGroup {
                        c.groups.forEachIndexed { i, g ->
                            if (i > 0) HorizontalDivider(Modifier.padding(start = Space.l), color = Pacer.colors.line)
                            val next = g.next?.let { stringResource(R.string.overview_next_on, it.dayOfWeek.short(), it.fmtShort()) }
                            val left = pluralStringResource(R.plurals.class_lessons_left, g.lessonsLeft, g.lessonsLeft)
                            LedgerRow(
                                g.name.ifBlank { stringResource(R.string.overview_group) },
                                detail = if (next == null) stringResource(R.string.overview_no_lessons) else stringResource(R.string.home_pair, next, left),
                                onClick = { vm.openGroup(c.classId, g.groupId) },
                            ) { StatusLabel(g.short, Modifier.padding(end = Space.s)) }
                        }
                    }
                }
            }
        }
    }
}

/** "Groups: most behind first ▾": a pill that opens the three orders. */
@Composable
private fun SortButton(order: GroupOrder, onOrder: (GroupOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { open = true }, modifier = Modifier.heightIn(min = 48.dp), shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(containerColor = Pacer.colors.surfaceContainer, contentColor = Pacer.colors.ink),
        ) {
            Text(stringResource(R.string.overview_sort, order.label()), style = MaterialTheme.typography.labelLarge)
            AppIcon(AppIcons.ArrowDropDown)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            GroupOrder.entries.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o.label()) },
                    trailingIcon = if (o == order) ({ AppIcon(AppIcons.Check) }) else null,
                    onClick = { open = false; onOrder(o) },
                )
            }
        }
    }
}

@Composable
private fun GroupOrder.label(): String = stringResource(when (this) {
    GroupOrder.MOST_BEHIND -> R.string.overview_sort_behind
    GroupOrder.NAME -> R.string.overview_sort_name
    GroupOrder.NEXT_LESSON -> R.string.overview_sort_next
})

/** The year's problems (decision 7), behind Pacing overview's warning button: each with Fix where a screen fixes it. */
@Composable
fun YearProblemsSheet(year: SchoolYear, vm: PacerViewModel, onDismiss: () -> Unit) {
    val problems = remember(year) { Nav.problems(year) }
    HomeSheet(stringResource(R.string.overview_problems), onDismiss) { close ->
        if (problems.isEmpty()) Hint(stringResource(R.string.warnings_none))
        problems.forEach { p ->
            // A class's problem opens that class, or its group for a custom plan's; the rest open the screen that fixes them.
            val fix: (() -> Unit)? = p.classId?.let { id ->
                { (p.fix as? Screen.Group)?.let { vm.openGroup(id, it.id) } ?: vm.openClass(id) }
            } ?: p.fix?.let { f -> { vm.open(f) } }
            ErrorBox(p.text, onFix = fix?.let { f -> { close(); f() } })
        }
    }
}
