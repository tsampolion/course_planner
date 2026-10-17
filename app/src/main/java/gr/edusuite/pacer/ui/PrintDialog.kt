package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Space

/** Picks which classes to print, all by default, and the page margins. */
@Composable
fun PrintDialog(year: SchoolYear, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val chosen = remember(year.id) { mutableStateListOf(*year.classes.map { it.id }.toTypedArray()) }
    var margin by rememberSaveable { mutableStateOf(PrintMargin.NORMAL) }
    AddDialogFrame(
        "Print or save PDF",
        canAdd = chosen.isNotEmpty(),
        onAdd = { PrintSheet.print(context, year.name, year.classes.filter { it.id in chosen }.map(year::planFor), margin) },
        onDismiss = onDismiss,
        confirm = "Print",
    ) {
        Eyebrow("Classes")
        if (year.classes.isEmpty()) Hint("No classes yet.")
        year.classes.forEach { c ->
            val on = c.id in chosen
            Row(
                Modifier.fillMaxWidth().toggleable(value = on, role = Role.Checkbox) { if (on) chosen.remove(c.id) else chosen.add(c.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = on, onCheckedChange = null)
                Text(c.name.ifBlank { "Class" }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = Space.m, top = Space.s, bottom = Space.s))
            }
        }
        Eyebrow("Margins")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            PrintMargin.entries.forEach { m ->
                FilterChip(selected = margin == m, onClick = { margin = m }, label = { Text(m.label) }, shape = MaterialTheme.shapes.extraSmall)
            }
        }
        Hint("${margin.label}: ${margin.mm} mm on every side. Each group starts on a new page; the system print dialog then offers paper size, orientation and Save as PDF.")
    }
}
