package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.runtime.getValue
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.*
import gr.edusuite.pacer.ui.theme.Space
import gr.edusuite.pacer.ui.theme.Pacer

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UnitProgressStrip(report: GroupReport) {
    val progress = unitProgress(report.plan, report.group)
    if (progress.isEmpty()) return
    val motion = animationsEnabled()
    val fillColor = Pacer.colors.accent
    val completed = progress.count { it.complete }
    var previousCompleted by remember(report.group.id) { mutableIntStateOf(completed) }
    val revisionPulse = remember(report.group.id) { Animatable(1f) }
    LaunchedEffect(completed, motion) {
        val newlyCompleted = completed > previousCompleted
        previousCompleted = completed
        revisionPulse.snapTo(1f)
        if (newlyCompleted && motion) revisionPulse.animateTo(1f, keyframes {
            val duration = MotionSpec.duration(MotionSpec.Step.REVISION, 1f)
            durationMillis = duration
            1f at 0; 1.12f at duration / 2; 1f at duration
        })
    }
    val current = progress.firstOrNull { !it.complete } ?: progress.last()
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.units_position, current.index + 1, current.total), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(if (completed > 0) R.string.units_revision_mark_done else R.string.units_revision_mark),
                Modifier.graphicsLayer { scaleX = if (motion) revisionPulse.value else 1f; scaleY = scaleX }.clearAndSetSemantics {})
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            progress.forEach { unit ->
                val fill by animateFloatAsState(unitCompletion(report.plan, unit), if (motion) Motion.work(MotionSpec.Step.PACE) else snap())
                LinearProgressIndicator(progress = { fill }, modifier = Modifier.weight(1f),
                    color = fillColor, trackColor = Pacer.colors.surfaceContainer)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            current.remaining.forEach { (id, remaining) ->
                report.plan.books.firstOrNull { it.id == id }?.let { book ->
                    Text(stringResource(R.string.units_remaining, book.name, remaining), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (current.remaining.values.any { it == 0 } && current.remaining.values.any { it > 0 }) {
            val names = report.plan.books.filter { current.remaining[it.id]?.let { n -> n > 0 } == true }.joinToString { it.name }
            Hint(stringResource(R.string.units_holds, names), color = Pacer.colors.warn)
        }
    }
}

@Composable
internal fun UnitRevisionRow(report: GroupReport, vm: PacerViewModel, index: Int) {
    val revision = report.group.unitRevisions.firstOrNull { it.unitIndex == index }
    LedgerRow(stringResource(R.string.units_revision_title, index + 1),
        detail = if (revision == null) stringResource(R.string.units_revision_add) else if (revision.kind == gr.edusuite.pacer.model.RevisionKind.NONE) stringResource(R.string.units_revision_none) else
            report.group.assignments.firstOrNull { it.id == revision.assignmentId }?.due?.toString() ?: stringResource(R.string.units_revision_unscheduled)) {
        TertiaryButton(stringResource(if (revision == null) R.string.units_revision_add else R.string.class_edit), { vm.openUnitRevision(report.group.id, index) })
    }
}
