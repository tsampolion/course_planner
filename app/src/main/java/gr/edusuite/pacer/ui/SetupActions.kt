package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.DayOfWeek

/** What a set-up confirmation asks: an optional [title], the [body], and the confirm button's [confirm] label, in the warning colour when [destructive]. */
data class SetupConfirm(val title: String?, val body: String, val confirm: String, val destructive: Boolean)

/** One explicit confirmation before a destructive or Undo-clearing set-up action, in the house dialog style. */
@Composable
internal fun rememberSetupConfirmation(): (SetupConfirm, () -> Unit) -> Unit {
    var pending by remember { mutableStateOf<Pair<SetupConfirm, () -> Unit>?>(null) }
    pending?.let { (ask, action) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = ask.title?.let { { Text(it) } },
            text = { Text(ask.body) },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = Pacer.colors.surface,
            tonalElevation = 0.dp,
            confirmButton = {
                PaletteTextButton(onClick = { pending = null; action() }) {
                    Text(ask.confirm, color = if (ask.destructive) Pacer.colors.warn else androidx.compose.ui.graphics.Color.Unspecified)
                }
            },
            dismissButton = { PaletteTextButton(onClick = { pending = null }) { Text(stringResource(R.string.component_cancel)) } },
        )
    }
    return { ask, action -> pending = ask to action }
}

/** The confirmation for deleting [name]: its title names it and [body] says what goes with it. */
@Composable
internal fun deleteConfirm(name: String, body: Int): SetupConfirm =
    SetupConfirm(stringResource(R.string.setup_delete_title, name), stringResource(body), stringResource(R.string.setup_delete), destructive = true)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddGroupSheet(vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    PacerSheet(stringResource(R.string.class_add_group), onDismiss,
        SheetAction(stringResource(R.string.class_add), name.isNotBlank() && selected.isNotEmpty()) {
            vm.addGroup(name.trim(), selected.map(DayOfWeek::valueOf).toSet())
        }, dirty = rememberDraftChanged(name, selected)) {
        CommitField(name, { name = it }, stringResource(R.string.class_name), Modifier.fillMaxWidth())
        Text(stringResource(R.string.group_lesson_days), style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            DayOfWeek.entries.forEach { day ->
                DayChip(day, day.name in selected) { selected = if (day.name in selected) selected - day.name else selected + day.name }
            }
        }
    }
}
