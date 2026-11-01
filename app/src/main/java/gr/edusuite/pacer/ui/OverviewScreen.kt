package gr.edusuite.pacer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * each class and one status mark per row. With [selected] (a group open beside it on a tablet) that row is highlighted. The year's problems are in the sheet behind the top bar's warning button.
 */
@Composable
fun OverviewScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues, selected: String? = null) {
    var order by rememberSaveable { mutableStateOf(GroupOrder.MOST_BEHIND) }
    val large = rememberLargeText()
    // Large text: the classes whose on-pace groups are shown, and whether the classes all on pace are.
    var opened by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var allOpen by rememberSaveable { mutableStateOf(false) }
    val today = rememberToday()
    val overview = remember(year, today) { pacingOverview(year, today) }
    val classes = remember(overview, order) { sortGroups(overview, order) }
    val groups = overview.sumOf { it.groups.size }
    val behind = overview.sumOf { it.behind }

    ScreenList(padding) {
        if (classes.isEmpty()) item(key = "empty") {
            EmptyState(AppIcons.Add, stringResource(R.string.overview_no_classes), stringResource(R.string.overview_empty_hint), stringResource(R.string.overview_open_classes), { vm.open(Screen.Classes) })
        } else item(key = "head") {
            val counts = @Composable { m: Modifier ->
                Row(m) {
                    Text(pluralStringResource(R.plurals.year_groups, groups, groups), style = MaterialTheme.typography.bodySmall.merge(Figures), color = Pacer.colors.muted)
                    if (behind > 0) Text(
                        pluralStringResource(R.plurals.overview_behind_suffix, behind, behind),
                        style = MaterialTheme.typography.bodySmall.merge(Figures), color = Pacer.colors.warn,
                    )
                }
            }
            // Large text: the sort pill goes under the counts.
            if (large) Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                counts(Modifier)
                SortButton(order) { order = it }
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                counts(Modifier.weight(1f))
                SortButton(order) { order = it }
            }
        }
        if (large) {
            // Large text: each class shows only its groups behind, the rest behind "N on pace ›"; classes all on pace wait in one link.
            val l = largeOverview(classes)
            l.classes.forEach { lc ->
                item(key = "class:${lc.shown.classId}") {
                    val open = lc.shown.classId in opened
                    SectionColumn(Modifier.animateItem()) {
                        ClassHead(year, lc.shown, if (lc.onPace.isEmpty()) null else {
                            { TertiaryButton(if (open) stringResource(R.string.overview_hide_on_pace) else when {
                                lc.setUp == 0 -> pluralStringResource(R.plurals.overview_on_pace_link, lc.onPace.size, lc.onPace.size)
                                lc.setUp == lc.onPace.size -> stringResource(R.string.home_set_up_needed, lc.setUp)
                                else -> stringResource(R.string.home_on_pace_set_up, lc.onPace.size - lc.setUp, lc.setUp)
                            }, { opened = if (open) opened - lc.shown.classId else opened + lc.shown.classId }, color = Pacer.colors.accentText) }
                        })
                        GroupRows(lc.shown.copy(groups = if (open) (lc.shown.groups + lc.onPace) else lc.shown.groups), vm, selected)
                    }
                }
            }
            if (l.allOnPace.isNotEmpty()) item(key = "all-on-pace") {
                SectionColumn {
                    TertiaryButton(
                        if (allOpen) stringResource(R.string.overview_hide_on_pace) else pluralStringResource(R.plurals.overview_classes_on_pace, l.allOnPace.size, l.allOnPace.size),
                        { allOpen = !allOpen }, Modifier.fillMaxWidth(), color = Pacer.colors.accentText,
                    )
                    if (allOpen) l.allOnPace.forEach { c ->
                        ClassHead(year, c, null)
                        GroupRows(c, vm, selected)
                    }
                }
            }
        } else classes.forEach { c ->
            item(key = "class:${c.classId}") {
                SectionColumn(Modifier.animateItem()) {
                    ClassHead(year, c, null)
                    GroupRows(c, vm, selected)
                }
            }
        }
    }
}

/** A class's heading after its colour's swatch (as on the Home board), with how many of its groups are behind (or "All on pace") and an optional [link] at the end. */
@Composable
private fun ClassHead(year: SchoolYear, c: OverviewClass, link: (@Composable () -> Unit)?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = Space.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        ClassSwatch(Pacer.colors.classTile(year, c.classId))
        Text(
            c.name.ifBlank { stringResource(R.string.overview_class) }, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (link != null) link() else when {
            c.behind > 0 -> Text(pluralStringResource(R.plurals.overview_class_behind, c.behind, c.behind), style = MaterialTheme.typography.labelMedium.merge(Figures), color = Pacer.colors.warn)
            c.groups.any { it.short != null } -> Text(stringResource(R.string.overview_all_on_pace), style = MaterialTheme.typography.labelMedium, color = Pacer.colors.muted)
        }
    }
}

/** A class's groups, one line each with the next lesson, lessons left and one status mark. [selected] is highlighted. */
@Composable
private fun GroupRows(c: OverviewClass, vm: PacerViewModel, selected: String?) {
    if (c.groups.isEmpty()) {
        Hint(stringResource(R.string.overview_no_groups))
        return
    }
    RowGroup {
        c.groups.forEachIndexed { i, g ->
            if (i > 0) HorizontalDivider(Modifier.padding(start = Space.l), color = Pacer.colors.line)
            val next = g.next?.let { stringResource(R.string.overview_next_on, it.dayOfWeek.short(), it.fmtShort()) }
            val left = pluralStringResource(R.plurals.class_lessons_left, g.lessonsLeft, g.lessonsLeft)
            LedgerRow(
                g.name.ifBlank { stringResource(R.string.overview_group) },
                if (g.groupId == selected) Modifier.background(Pacer.colors.accentSoft) else Modifier,
                detail = if (next == null) stringResource(R.string.overview_no_lessons) else stringResource(R.string.home_pair, next, left),
                onClick = { vm.openGroup(c.classId, g.groupId) },
            ) { StatusLabel(g.short, Modifier.padding(end = Space.s)) }
        }
    }
}

/** "Groups: most behind first ▾": a pill that opens the three orders. */
@Composable
private fun SortButton(order: GroupOrder, onOrder: (GroupOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        PaletteTextButton(
            onClick = { open = true }, modifier = Modifier.heightIn(min = 48.dp), shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(containerColor = Pacer.colors.surfaceContainer, contentColor = Pacer.colors.ink),
        ) {
            Text(stringResource(R.string.overview_sort, order.label()), style = MaterialTheme.typography.labelLarge)
            AppIcon(AppIcons.ArrowDropDown)
        }
        PacerDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
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
    val today = rememberToday()
    val problems = remember(year, today) { Nav.problems(year, today) }
    PacerSheet(stringResource(R.string.overview_problems), onDismiss) { close ->
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
