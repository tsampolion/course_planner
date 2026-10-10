package com.coursebook.pacer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coursebook.pacer.engine.fmt
import com.coursebook.pacer.model.AppData
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.ui.theme.Pacer

/** Every plan: tap one to make it the open plan. Rename and delete act on the open plan. */
@Composable
fun PlansScreen(data: AppData, vm: PacerViewModel, padding: PaddingValues) {
    val active = data.plan
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "hint") { Hint("Each plan has its own term, groups and books. Tap a plan to open it.") }
        items(data.plans, key = { it.id }) { p -> PlanRow(p, p.id == active.id) { vm.selectPlan(p.id) } }
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(onClick = vm::newPlan) { Text("New plan") }
                OutlinedButton(onClick = { renaming = true }) { Text("Rename") }
                OutlinedButton(onClick = { confirmDelete = true }, enabled = data.plans.size > 1) { Text("Delete") }
            }
        }
        item(key = "foot") { Hint("Rename and Delete apply to the open plan, “${active.name}”. New plans start from the sample, with the lesson length and pace from Settings.") }
    }

    if (renaming) {
        var name by rememberSaveable { mutableStateOf(active.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename plan") },
            text = { CommitField(name, { name = it }, "Plan name") },
            confirmButton = { TextButton(onClick = { vm.renamePlan(name.trim().ifEmpty { active.name }); renaming = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${active.name}”?") },
            text = { Text("Undo brings it back until you close the app.") },
            confirmButton = { TextButton(onClick = { vm.deletePlan(); confirmDelete = false }) { Text("Delete", color = Pacer.colors.warn) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PlanRow(p: Plan, open: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(if (open) 2.dp else 1.dp, if (open) MaterialTheme.colorScheme.primary else Pacer.colors.line),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                val s = p.start
                val e = p.end
                val dates = if (s != null && e != null) "${s.fmt()} – ${e.fmt()}" else "Dates not set"
                val groups = "${p.groups.size} ${if (p.groups.size == 1) "group" else "groups"}"
                Hint("$dates · $groups")
            }
            if (open) Tag("Open", Modifier.padding(start = 8.dp))
        }
    }
}
