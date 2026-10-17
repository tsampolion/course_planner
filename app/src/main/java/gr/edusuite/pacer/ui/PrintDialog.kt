package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import gr.edusuite.pacer.R
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Space

/** Picks which classes to print, all by default, and the page margins. */
@Composable
fun PrintDialog(year: SchoolYear, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val toggle = rememberToggleHaptic()
    val segment = rememberSegmentHaptic()
    var chosen by rememberSaveable(year.id) { mutableStateOf(year.classes.map { it.id }) }
    var margin by rememberSaveable { mutableStateOf(PrintMargin.NORMAL) }
    AddDialogFrame(
        stringResource(R.string.print_title),
        canAdd = chosen.isNotEmpty(),
        onAdd = { PrintSheet.print(context, year.name, year.classes.filter { it.id in chosen }.map(year::planFor), margin) },
        onDismiss = onDismiss,
        confirm = stringResource(R.string.print_print),
    ) {
        Eyebrow(stringResource(R.string.print_classes))
        if (year.classes.isEmpty()) Hint(stringResource(R.string.print_none))
        year.classes.forEach { c ->
            val on = c.id in chosen
            Row(
                Modifier.fillMaxWidth().toggleable(value = on, role = Role.Checkbox) { value -> toggle(value); chosen = if (value) chosen + c.id else chosen - c.id },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = on, onCheckedChange = null)
                Text(c.name.ifBlank { context.getString(R.string.print_class) }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = Space.m, top = Space.s, bottom = Space.s))
            }
        }
        Eyebrow(stringResource(R.string.print_margins))
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            PrintMargin.entries.forEach { m ->
                FilterChip(modifier = Modifier.semantics { role = Role.RadioButton }, selected = margin == m, onClick = { segment(); margin = m }, label = { Text(m.localizedLabel()) }, shape = MaterialTheme.shapes.extraSmall)
            }
        }
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
