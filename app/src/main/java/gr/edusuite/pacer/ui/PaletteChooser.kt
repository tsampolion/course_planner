package gr.edusuite.pacer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import gr.edusuite.pacer.R
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.model.PaletteChoice
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import gr.edusuite.pacer.ui.theme.Tokens

@Composable
fun PaletteChooser(selected: PaletteChoice, onSelect: (PaletteChoice) -> Unit) {
    val large = rememberLargeText()
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Text(stringResource(R.string.settings_palette), style = MaterialTheme.typography.titleSmall)
        if (large) Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            PaletteChoice.entries.forEach { PaletteCard(it, it == selected, { onSelect(it) }, Modifier.fillMaxWidth()) }
        } else Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            PaletteChoice.entries.forEach { PaletteCard(it, it == selected, { onSelect(it) }, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun PaletteCard(choice: PaletteChoice, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val name = stringResource(if (choice == PaletteChoice.BONE) R.string.palette_bone else R.string.palette_saffron)
    val body = stringResource(if (choice == PaletteChoice.BONE) R.string.palette_bone_body else R.string.palette_saffron_body)
    val description = stringResource(R.string.palette_description, name, body)
    val state = stringResource(if (selected) R.string.palette_selected else R.string.palette_unselected)
    val t = Tokens.of(choice, Pacer.dark)
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier.clip(shape).border(if (selected) 2.dp else 1.dp, if (selected) Pacer.colors.ink else Pacer.colors.outline, shape)
            .heightIn(min = 48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .clearAndSetSemantics {
                contentDescription = description; stateDescription = state; this.selected = selected; role = Role.RadioButton
                onClick { onSelect(); true }
            },
        color = Pacer.colors.surface, shape = shape,
    ) {
        Column(Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                if (selected) AppIcon(AppIcons.Check, Modifier.size(Space.l))
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                listOf(t.paper,t.accent,t.ink).forEach { swatch ->
                    Box(Modifier.size(Space.l).clip(CircleShape).background(Color(swatch)).border(1.dp, Pacer.colors.outline, CircleShape))
                }
            }
        }
    }
}
