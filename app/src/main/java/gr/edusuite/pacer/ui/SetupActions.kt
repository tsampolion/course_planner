package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.ui.theme.Space
import java.time.DayOfWeek

/** One explicit confirmation before a destructive setup action. */
@Composable
internal fun rememberSetupConfirmation(message: Int = R.string.pr6_delete_confirmation): ((() -> Unit) -> Unit) {
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    if (pending != null) AlertDialog(
        onDismissRequest = { pending = null },
        text = { Text(stringResource(message)) },
        confirmButton = { PaletteTextButton(onClick = { val action = pending; pending = null; action?.invoke() }) { Text(stringResource(R.string.pr6_continue)) } },
        dismissButton = { PaletteTextButton(onClick = { pending = null }) { Text(stringResource(R.string.component_cancel)) } },
    )
    return { pending = it }
}

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
        Text(stringResource(R.string.pr6_lesson_days))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(selected = day.name in selected,
                    onClick = { selected = if (day.name in selected) selected - day.name else selected + day.name },
                    label = { Text(stringResource(classDayResource(day))) })
            }
        }
    }
}
