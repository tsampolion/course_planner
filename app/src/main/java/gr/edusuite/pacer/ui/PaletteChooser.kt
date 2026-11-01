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
    // Three cards don't fit side by side at phone width with their descriptions, so they stack at every size.
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Text(stringResource(R.string.settings_palette), style = MaterialTheme.typography.titleSmall)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            PaletteChoice.entries.forEach { PaletteCard(it, it == selected, { onSelect(it) }, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun PaletteCard(choice: PaletteChoice, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val (nameRes, bodyRes) = when (choice) {
        PaletteChoice.BONE -> R.string.palette_bone to R.string.palette_bone_body
        PaletteChoice.SAFFRON -> R.string.palette_saffron to R.string.palette_saffron_body
        PaletteChoice.OXFORD -> R.string.palette_oxford to R.string.palette_oxford_body
    }
    val name = stringResource(nameRes)
    val body = stringResource(bodyRes)
    val description = stringResource(R.string.palette_description, name, body)
    val state = stringResource(if (selected) R.string.palette_selected else R.string.palette_unselected)
    val t = Tokens.of(choice, Pacer.dark)
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier.clip(shape).border(if (selected) 2.dp else 1.dp, if (selected) Pacer.colors.accentText else Pacer.colors.outline, shape)
            .heightIn(min = 48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .clearAndSetSemantics {
                contentDescription = description; stateDescription = state; this.selected = selected; role = Role.RadioButton
                onClick { onSelect(); true }
            },
        color = if (selected) Pacer.colors.accentSoft else Pacer.colors.surface, contentColor = Pacer.colors.ink, shape = shape,
    ) {
        Row(Modifier.padding(Space.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, color = Pacer.colors.ink)
                    if (selected) AppIcon(AppIcons.Check, Modifier.size(Space.l))
                }
                Text(body, style = MaterialTheme.typography.bodySmall, color = if (selected) Pacer.colors.onAccentSoft else Pacer.colors.muted)
            }
            // Paper, the two primaries and ink: the palette at a glance.
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                listOf(t.paper, t.accent, t.second, t.ink).forEach { swatch ->
                    Box(Modifier.size(Space.l).clip(CircleShape).background(Color(swatch)).border(1.dp, Pacer.colors.outline, CircleShape))
                }
            }
        }
    }
}
