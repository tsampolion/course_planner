package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.UntickedGroup
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.recordNotTaught
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate
import java.util.Locale

/**
 * The first-launch question (`docs/AUTO_TICK_BRIEF.md`): lessons now tick themselves when their day has passed, so what
 * about the past ones nobody ticked? It has no dismiss; either answer settles it for good.
 */
@Composable
internal fun PastLessonsDialog(groups: List<UntickedGroup>, onTick: () -> Unit, onLeave: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.past_lessons_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Text(stringResource(R.string.past_lessons_body), style = MaterialTheme.typography.bodyMedium)
                groups.forEach { g ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.past_lessons_row, g.className, g.groupName), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(pluralStringResource(R.plurals.past_lessons_count, g.count, g.count), style = MaterialTheme.typography.bodyMedium.merge(Figures))
                    }
                }
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = { PrimaryButton(stringResource(R.string.past_lessons_tick), onTick) },
        dismissButton = { TertiaryButton(stringResource(R.string.past_lessons_leave), onLeave) },
    )
}

/** Asks before a lesson is recorded as not taught, and says what it does to the pace the rest of the term needs. */
@Composable
internal fun NotTaughtDialog(report: GroupReport, date: LocalDate, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val now = remember(report) { report.rate }
    val after = remember(report, date) { report.recordNotTaught(date)?.let { GroupReport(report.plan, it, report.source).rate } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.not_taught_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Text(stringResource(R.string.not_taught_body, date.fmtShort()), style = MaterialTheme.typography.bodyMedium)
                if (after != null) Text(
                    stringResource(R.string.not_taught_pace, perLesson(now), perLesson(after)),
                    style = MaterialTheme.typography.bodyMedium.merge(Figures),
                )
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = { PrimaryButton(stringResource(R.string.not_taught_confirm), { onDismiss(); onConfirm() }, enabled = after != null) },
        dismissButton = { TertiaryButton(stringResource(R.string.component_cancel), onDismiss) },
    )
}

private fun perLesson(rate: Double): String = String.format(Locale.US, "%.1f", rate)

/**
 * Group home's "Yesterday" line: how the lesson of the day before was counted, and a way to change it while it is fresh.
 * Change offers the pages taught and "Lesson didn't happen".
 */
@Composable
internal fun YesterdayLine(lesson: Lesson, onEdit: () -> Unit, onNotTaught: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val text = stringResource(when {
        lesson.notTaught -> R.string.yesterday_not_taught
        lesson.edited -> R.string.yesterday_edited
        else -> R.string.yesterday_taught
    })
    RowGroup {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = Space.l, end = Space.s), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Box {
                TertiaryButton(stringResource(R.string.yesterday_change), { open = true })
                PacerDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.group_edit_pages_menu)) }, onClick = { open = false; onEdit() })
                    if (!lesson.notTaught) DropdownMenuItem(text = { Text(stringResource(R.string.group_not_taught_menu)) }, onClick = { open = false; onNotTaught() })
                }
            }
        }
    }
}
