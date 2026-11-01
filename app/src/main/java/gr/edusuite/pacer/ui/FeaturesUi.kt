package gr.edusuite.pacer.ui

import androidx.compose.animation.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.model.Features
import gr.edusuite.pacer.model.Module
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

fun Module.titleRes(): Int = when (this) {
    Module.WORK -> R.string.module_work_title
    Module.BOOK_TOOLS -> R.string.module_book_tools_title
    Module.PLANNING -> R.string.module_planning_title
    Module.OUTPUT -> R.string.module_output_title
}

fun Module.detailRes(): Int = when (this) {
    Module.WORK -> R.string.module_work_detail
    Module.BOOK_TOOLS -> R.string.module_book_tools_detail
    Module.PLANNING -> R.string.module_planning_detail
    Module.OUTPUT -> R.string.module_output_detail
}

fun Module.addsRes(): Int = when (this) {
    Module.WORK -> R.string.module_work_adds
    Module.BOOK_TOOLS -> R.string.module_book_tools_adds
    Module.PLANNING -> R.string.module_planning_adds
    Module.OUTPUT -> R.string.module_output_adds
}

/**
 * Settings › Features: each tool that is off says what it adds and has a Turn on button; a tool that is on reads On.
 * There is no way to turn one off. Turning one on confirms with a tick, a wash that fades and a polite announcement.
 */
@Composable
fun FeaturesSheet(features: Features, onTurnOn: (Module) -> Unit, onDismiss: () -> Unit) {
    val confirm = rememberConfirmHaptic()
    var justOn by rememberSaveable { mutableStateOf<Module?>(null) }
    PacerSheet(stringResource(R.string.features_title), onDismiss) {
        Text(stringResource(R.string.features_note), style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.muted)
        moduleRows(features).forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(color = Pacer.colors.line)
            ModuleRowItem(row, washed = justOn == row.module) { onTurnOn(row.module); confirm(); justOn = row.module }
        }
        justOn?.let { m ->
            Text(
                stringResource(R.string.features_now_on, stringResource(m.titleRes())),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** One tool: its name and what it is for, then On, or what it adds and a 48dp Turn on. The new On row washes in the second primary. */
@Composable
private fun ModuleRowItem(row: ModuleRow, washed: Boolean, onTurnOn: () -> Unit) {
    val c = Pacer.colors
    val motion = animationsEnabled()
    val wash = remember { Animatable(if (washed) c.secondSoft else Color.Transparent) }
    LaunchedEffect(washed) {
        if (washed) {
            wash.snapTo(c.secondSoft)
            wash.animateTo(Color.Transparent, Motion.work(MotionSpec.Step.REVISION, motion))
        }
    }
    val title = stringResource(row.module.titleRes())
    Column(Modifier.fillMaxWidth().background(wash.value).padding(vertical = Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(row.module.detailRes()), style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
            if (row.on) {
                val on = stringResource(R.string.features_on)
                Row(
                    Modifier.heightIn(min = 32.dp).background(c.accentSoft, MaterialTheme.shapes.extraSmall).padding(horizontal = Space.m).semantics(mergeDescendants = true) { stateDescription = on },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    AppIcon(AppIcons.Check, Modifier.size(16.dp))
                    Text(on, style = MaterialTheme.typography.labelLarge, color = c.onAccentSoft)
                }
            } else {
                val label = stringResource(R.string.features_turn_on_named, title)
                PrimaryButton(stringResource(R.string.features_turn_on), onTurnOn, Modifier.semantics { contentDescription = label })
            }
        }
        if (!row.on) Text(stringResource(row.module.addsRes()), style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

/** Welcome's "Choose tools…": the four tools as a checklist. Nothing is checked at first, which is Just pacing. */
@Composable
fun ChooseToolsSheet(initial: Set<Module>, onUse: (Set<Module>) -> Unit, onDismiss: () -> Unit) {
    var picked by rememberSaveable { mutableStateOf(initial.map { it.name }.toSet()) }
    PacerSheet(
        stringResource(R.string.preset_choose_title), onDismiss,
        action = SheetAction(stringResource(R.string.preset_choose_use), true) { onUse(picked.mapNotNull(Module::parse).toSet()) },
    ) {
        Module.entries.forEach { m ->
            val checked = m.name in picked
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .clickable(role = Role.Checkbox) { picked = if (checked) picked - m.name else picked + m.name }
                    .padding(vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                Checkbox(checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Pacer.colors.accent, checkmarkColor = Pacer.colors.onAccent))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(m.titleRes()), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(m.detailRes()), style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.muted)
                }
            }
        }
    }
}

/** The moment-of-need offer, in place: what happened, then two named buttons. */
@Composable
fun ModuleHintCard(hint: ModuleHint, aheadPages: Int, onTurnOn: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pacer.colors
    val (title, turnOn) = when (hint) {
        ModuleHint.WORK -> R.string.hint_work_title to R.string.hint_turn_on_work
        ModuleHint.PLANNING -> R.string.hint_planning_title to R.string.hint_turn_on_planning
    }
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = c.secondSoft, contentColor = c.onSecondSoft) {
        Column(Modifier.padding(horizontal = Space.l, vertical = Space.m)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Text(
                if (hint == ModuleHint.WORK) pluralStringResource(R.plurals.hint_work_body, aheadPages, aheadPages) else stringResource(R.string.hint_planning_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                TertiaryButton(stringResource(turnOn), onTurnOn, color = c.onSecondSoft)
                TertiaryButton(stringResource(R.string.hint_not_now), onNotNow, color = c.onSecondSoft)
            }
        }
    }
}

/** What a hint's tool did once on, shown where the hint was, and announced. */
@Composable
fun ModuleHintDone(hint: ModuleHint, modifier: Modifier = Modifier) {
    val c = Pacer.colors
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = c.secondSoft, contentColor = c.onSecondSoft) {
        Text(
            stringResource(if (hint == ModuleHint.WORK) R.string.hint_done_work else R.string.hint_done_planning),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Space.l, vertical = Space.m).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
