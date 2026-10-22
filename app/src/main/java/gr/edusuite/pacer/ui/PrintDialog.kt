package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.R
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Space

/** What Print covers (decision 24). */
enum class PrintScope { GROUP, CLASS, YEAR }

/**
 * Print (decision 24): radio buttons for this group, every group in its class or every group this year, then the
 * margins. Opened from a group's ⋮ with [only] (its class and group ids), it starts on that group.
 */
@Composable
fun PrintDialog(year: SchoolYear, only: Pair<String, String>? = null, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val segment = rememberSegmentHaptic()
    val onlyClass = only?.let { (c, _) -> year.classes.firstOrNull { it.id == c } }
    val onlyGroup = only?.let { (_, g) -> onlyClass?.groups?.firstOrNull { it.id == g } }
    var scope by rememberSaveable(only) { mutableStateOf(if (onlyGroup != null) PrintScope.GROUP else PrintScope.YEAR) }
    var margin by rememberSaveable { mutableStateOf(PrintMargin.NORMAL) }
    val className = onlyClass?.name?.ifBlank { null } ?: stringResource(R.string.print_class)
    val choices = buildList {
        if (onlyClass != null && onlyGroup != null) add(PrintScope.GROUP to stringResource(R.string.print_only_group, className, onlyGroup.name))
        if (onlyClass != null) add(PrintScope.CLASS to stringResource(R.string.print_whole_class, className))
        add(PrintScope.YEAR to stringResource(R.string.print_whole_year))
    }
    AddDialogFrame(
        stringResource(R.string.print_title),
        canAdd = scope != PrintScope.YEAR || year.classes.isNotEmpty(),
        onAdd = {
            val plans = when {
                scope == PrintScope.GROUP && onlyClass != null && onlyGroup != null -> listOf(year.planFor(onlyClass).copy(groups = listOf(onlyGroup)))
                scope == PrintScope.CLASS && onlyClass != null -> listOf(year.planFor(onlyClass))
                else -> year.classes.map(year::planFor)
            }
            PrintSheet.print(context, year.name, plans, margin)
        },
        onDismiss = onDismiss,
        confirm = stringResource(R.string.print_print),
    ) {
        Column(Modifier.selectableGroup()) {
            choices.forEach { (s, label) ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = scope == s, role = Role.RadioButton) { segment(); scope = s },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = scope == s, onClick = null, colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Pacer.colors.accentText))
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = Space.m))
                }
            }
        }
        if (scope == PrintScope.YEAR && year.classes.isEmpty()) Hint(stringResource(R.string.print_none))
        Eyebrow(stringResource(R.string.print_margins))
        SegmentedToggle(PrintMargin.entries.map { it.localizedLabel() }, PrintMargin.entries.indexOf(margin), { margin = PrintMargin.entries[it] })
        Hint(stringResource(R.string.print_margin_hint, margin.localizedLabel(), margin.mm))
    }
}

/** Labels are localized here; the HTML renderer keeps its own plain Kotlin output. */
@Composable
private fun PrintMargin.localizedLabel(): String = stringResource(when (this) {
    PrintMargin.NARROW -> R.string.print_narrow
    PrintMargin.NORMAL -> R.string.print_normal
    PrintMargin.WIDE -> R.string.print_wide
})
