package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.model.ClassCollection
import gr.edusuite.pacer.model.ClassOrder
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

@Composable
internal fun ClassOrderPicker(order: ClassOrder, onSelect: (ClassOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val choices = listOf(
        ClassOrder.YOUR_ORDER to stringResource(R.string.collections_your_order),
        ClassOrder.NAME to stringResource(R.string.collections_name_order),
        ClassOrder.MOST_BEHIND to stringResource(R.string.collections_behind_order),
    )
    Box {
        PaletteTextButton(
            onClick = { open = true }, modifier = Modifier.heightIn(min = 48.dp), shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(containerColor = Pacer.colors.surfaceContainer, contentColor = Pacer.colors.ink),
        ) {
            Text(stringResource(R.string.collections_sort, choices.first { it.first == order }.second), style = MaterialTheme.typography.labelLarge)
            AppIcon(AppIcons.ArrowDropDown)
        }
        PacerDropdownMenu(open, { open = false }) {
            choices.forEach { (value, label) ->
                DropdownMenuItem(text = { Text(label) }, trailingIcon = if (value == order) ({ AppIcon(AppIcons.Check) }) else null,
                    onClick = { open = false; onSelect(value) })
            }
        }
    }
}

@Composable
internal fun CollectionSectionHead(collection: ClassCollection, onEdit: () -> Unit, onDelete: () -> Unit) {
    var open by remember(collection.id) { mutableStateOf(false) }
    SectionHead(collection.name, trailing = {
        Box {
            PillIconButton(stringResource(R.string.collections_edit), { open = true }) { AppIcon(AppIcons.MoreVert) }
            PacerDropdownMenu(open, { open = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.collections_edit)) }, onClick = { open = false; onEdit() })
                DropdownMenuItem(text = { Text(stringResource(R.string.collections_delete), color = Pacer.colors.warn) }, onClick = { open = false; onDelete() })
            }
        }
    })
}

/** Editing only changes membership and title: removing a collection leaves every class in the year. */
@Composable
internal fun CollectionSheet(
    year: SchoolYear,
    collection: ClassCollection?,
    vm: PacerViewModel,
    undoToast: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by rememberSaveable(collection?.id) { mutableStateOf(collection?.name.orEmpty()) }
    var selected by rememberSaveable(collection?.id) { mutableStateOf(collection?.classIds?.toList().orEmpty()) }
    SheetFrame(
        stringResource(if (collection == null) R.string.collections_new else R.string.collections_edit),
        stringResource(R.string.home_save), name.isNotBlank(),
        {
            val changed = if (collection == null) vm.addCollection(name.trim(), selected.toSet())
                else vm.updateCollection(collection.id, name.trim(), selected.toSet())
            if (changed) undoToast(context.getString(R.string.collections_saved))
        }, onDismiss,
    ) {
        CommitField(name, { name = it }, stringResource(R.string.year_name), Modifier.fillMaxWidth())
        Hint(stringResource(R.string.collections_move_hint))
        year.classes.forEach { c ->
            val checked = c.id in selected
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(checked, role = Role.Checkbox) { selected = if (it) selected + c.id else selected - c.id }
                    .padding(vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked, onCheckedChange = null)
                Spacer(Modifier.width(Space.s))
                Text(c.name.ifBlank { stringResource(R.string.year_class) }, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (collection != null) {
            PaletteTextButton(onClick = {
                if (vm.removeCollection(collection.id)) undoToast(context.getString(R.string.collections_deleted, collection.name))
                onDismiss()
            }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.collections_delete)) }
            Hint(stringResource(R.string.collections_keep_classes))
        }
    }
}
