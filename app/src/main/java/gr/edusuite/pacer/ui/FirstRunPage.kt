package gr.edusuite.pacer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import gr.edusuite.pacer.R
import gr.edusuite.pacer.model.Module
import gr.edusuite.pacer.model.Preset
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

/**
 * Welcome on a fresh install (`docs/FEATURE_MODULES_BRIEF.md`, Presets): three presets and Choose tools…, then the
 * first school year. Just pacing is selected. Either button keeps the selection; Not now still keeps it, so the app
 * never starts with everything on by accident.
 */
@Composable
fun FirstRunPage(onStart: (Set<Module>) -> Unit, onNotNow: (Set<Module>) -> Unit) {
    val c = Pacer.colors
    var picked by rememberSaveable { mutableStateOf(Preset.JUST_PACING.modules.map { it.name }.toSet()) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val selection = picked.mapNotNull(Module::parse).toSet()
    val preset = Preset.of(selection)
    Dialog(onDismissRequest = { onNotNow(selection) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = c.paper, contentColor = c.ink) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).systemBarsPadding().padding(horizontal = Space.xl, vertical = Space.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    Logo(48.dp)
                    Text(stringResource(R.string.welcome_presets_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = Space.s).semantics { heading() })
                    Text(stringResource(R.string.welcome_presets_sub), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    val label = stringResource(R.string.welcome_presets_label)
                    Column(Modifier.selectableGroup().semantics { contentDescription = label }, verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        PresetCard(R.string.preset_just, R.string.preset_just_gets, preset == Preset.JUST_PACING) { picked = Preset.JUST_PACING.modules.map { it.name }.toSet() }
                        PresetCard(R.string.preset_work, R.string.preset_work_gets, preset == Preset.PACING_AND_WORK) { picked = Preset.PACING_AND_WORK.modules.map { it.name }.toSet() }
                        PresetCard(R.string.preset_everything, R.string.preset_everything_gets, preset == Preset.EVERYTHING) { picked = Preset.EVERYTHING.modules.map { it.name }.toSet() }
                        ChooseCard(selected = preset == null, selection = selection) { choosing = true }
                    }
                    PrimaryButton(stringResource(R.string.guide_create_year), { onStart(selection) }, Modifier.fillMaxWidth().padding(top = Space.m))
                    TertiaryButton(stringResource(R.string.guide_not_now), { onNotNow(selection) }, Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
    if (choosing) ChooseToolsSheet(selection, { picked = it.map { m -> m.name }.toSet(); choosing = false }, { choosing = false })
}

@Composable
private fun PresetCard(title: Int, gets: Int, selected: Boolean, onSelect: () -> Unit) {
    val c = Pacer.colors
    val interaction = remember { MutableInteractionSource() }
    Surface(
        Modifier.fillMaxWidth().pressFeedback(interaction).selectable(selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onSelect),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) c.accentSoft else c.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.line),
    ) {
        Row(Modifier.padding(Space.l).heightIn(min = 72.dp), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Tick(selected)
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(gets), style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
        }
    }
}

/** The fourth choice, a peer of the presets: opens the checklist, and reads which tools it holds once chosen. */
@Composable
private fun ChooseCard(selected: Boolean, selection: Set<Module>, onOpen: () -> Unit) {
    val c = Pacer.colors
    val interaction = remember { MutableInteractionSource() }
    val separator = stringResource(R.string.settings_name_separator)
    val detail = if (selected) selection.map { stringResource(it.titleRes()) }.joinToString(separator) else stringResource(R.string.preset_choose_detail)
    Surface(
        Modifier.fillMaxWidth().pressFeedback(interaction).selectable(selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onOpen),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) c.accentSoft else c.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.line),
    ) {
        Row(Modifier.padding(Space.l).heightIn(min = 72.dp), horizontalArrangement = Arrangement.spacedBy(Space.m), verticalAlignment = Alignment.CenterVertically) {
            Tick(selected)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(stringResource(R.string.preset_choose), style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
            AppIcon(AppIcons.ChevronRight)
        }
    }
}

/** The radio mark: a ring that fills with a check drawn in (fade and a small scale, 150–300ms, snapping with animations off). */
@Composable
private fun Tick(selected: Boolean) {
    val c = Pacer.colors
    val motion = animationsEnabled()
    val t by animateFloatAsState(if (selected) 1f else 0f, Motion.work(MotionSpec.Step.PRESS, motion))
    Box(Modifier.size(24.dp).border(2.dp, if (selected) c.accent else c.outline, CircleShape).background(c.accent.copy(alpha = t), CircleShape), contentAlignment = Alignment.Center) {
        AppIcon(AppIcons.Check, Modifier.size(16.dp).alpha(t).scale(0.6f + 0.4f * t))
    }
}
