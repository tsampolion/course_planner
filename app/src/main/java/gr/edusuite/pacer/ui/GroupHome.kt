package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.ui.theme.Figures
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.unitProgress
import gr.edusuite.pacer.engine.coreBooks
import gr.edusuite.pacer.engine.position
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Rhythm
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate

@Composable
internal fun GroupHome(report: GroupReport, next: Lesson?, today: LocalDate, actions: LessonActions,
    padding: PaddingValues, onDone: () -> Unit, onPagesTaught: () -> Unit, onFix: () -> Unit, onOpen: (GroupDestination) -> Unit,
    yesterday: Lesson? = null, onYesterdayEdit: (Lesson) -> Unit = {}, onYesterdayNotTaught: (Lesson) -> Unit = {}) {
    val fixable = remember(report) { showsFix(progressLine(report).status, groupWarnings(report)) }
    LazyColumn(contentPadding = PaddingValues(start = Space.l, end = Space.l,
        top = padding.calculateTopPadding() + Space.m, bottom = padding.calculateBottomPadding() + Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Rhythm.section)) {
        if (yesterday != null) item { YesterdayLine(yesterday, { onYesterdayEdit(yesterday) }, { onYesterdayNotTaught(yesterday) }) }
        item {
            TodayBox(next, today, actions, onDone, onPagesTaught) {
                val due = report.group.assignments.filter { it.type == AssignmentType.HOMEWORK && it.due == today && next?.date != today }
                val set = report.group.assignments.filter { it.setOn == today && (it.type != AssignmentType.HOMEWORK || next?.date != today) }
                if (due.isNotEmpty()) Text(stringResource(R.string.work_due_today, due.map { assignmentTitle(it) }.joinToString()), style = MaterialTheme.typography.bodySmall)
                if (set.isNotEmpty()) Text(stringResource(R.string.work_set_today, set.map { assignmentTitle(it) }.joinToString()), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            RowGroup {
                Column(Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    ProgressStrip(report, onFix.takeIf { fixable }) { onOpen(GroupDestination.STATS) }
                    if (unitProgress(report.plan, report.group).isNotEmpty()) UnitProgressStrip(report)
                }
            }
        }
        if (report.plan.books.isNotEmpty() && unitProgress(report.plan, report.group).isEmpty()) item {
            RowGroup { Column(Modifier.padding(horizontal = Space.l, vertical = Space.m)) { PaceTrack(report, compact = true) { onOpen(GroupDestination.STATS) } } }
        }
        item {
            RowGroup {
                Column {
                    SectionHead(stringResource(R.string.work_due_soon), Modifier.padding(horizontal = Space.l))
                    val due = dueSoon(report, today)
                    if (due.isEmpty()) Hint(stringResource(R.string.work_no_due), Modifier.padding(start = Space.l, end = Space.l, bottom = Space.m))
                    else due.forEachIndexed { i, a ->
                        if (i > 0) RowDivider()
                        AssignmentRow(a, report, { onOpen(GroupDestination.WORK) }) { Chevron() }
                    }
                }
            }
        }
        item { GroupHomePills(onOpen = onOpen) }
    }
}

/** One line of progress, "Lesson 14 of 40 · On pace", over a thin bar. Tapping it opens Stats for the details. */
@Composable
internal fun ProgressStrip(report: GroupReport, onFix: (() -> Unit)? = null, onOpen: () -> Unit) {
    val line = remember(report) { progressLine(report) }
    val c = Pacer.colors
    val fill = c.mark
    val motion = animationsEnabled()
    val fraction by animateFloatAsState(line.fraction, if (motion) Motion.work(MotionSpec.Step.PACE) else snap())
    val verdict = when (line.status) {
        ProgressStatus.ON_PACE -> stringResource(R.string.status_on_pace)
        ProgressStatus.SHORT -> pluralStringResource(R.plurals.status_short, line.shortBy, line.shortBy)
        ProgressStatus.UNMEASURED -> null
    }
    val text = when {
        line.total == 0 -> stringResource(R.string.group_no_term_lessons)
        verdict == null -> stringResource(R.string.progress_strip, line.lesson, line.total, "").trimEnd(' ', '·')
        else -> stringResource(R.string.progress_strip, line.lesson, line.total, verdict)
    }
    val heading = stringResource(R.string.work_progress)
    val open = stringResource(R.string.work_open_stats)
    val fixLabel = stringResource(R.string.progress_fix)
    val fixDescription = stringResource(R.string.progress_fix_description)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.weight(1f).heightIn(min = 48.dp).clickable(onClickLabel = open, role = Role.Button, onClick = onOpen)
                .semantics(mergeDescendants = true) { contentDescription = "$heading. $text" },
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text, style = MaterialTheme.typography.titleSmall.merge(Figures), color = if (line.status == ProgressStatus.SHORT) c.warn else c.ink, modifier = Modifier.weight(1f))
                if (onFix == null) Chevron()
            }
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(), color = fill, trackColor = c.progressTrack)
        }
        // D1: a short group's repair is one tap away; the sheet it opens offers only repairs that keep the teacher's lessons.
        if (onFix != null) Box(
            Modifier.size(48.dp).clickable(onClickLabel = fixDescription, role = Role.Button, onClick = onFix).semantics { contentDescription = fixDescription },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.accentSoft), contentAlignment = Alignment.Center) {
                Text(fixLabel, style = MaterialTheme.typography.labelLarge, color = c.onAccentSoft)
            }
        }
    }
}

/** The four page pills: Plan, Work, Stats, Settings, filled with the accent's soft colour and outlined so they read as buttons (X5, D14). 2×2 on phones, one row on tablets, one column at large text. */
@Composable
internal fun GroupHomePills(showPlan: Boolean = true, onOpen: (GroupDestination) -> Unit) {
    val pills = groupDestinations(LocalFeatures.current).filter { showPlan || it != GroupDestination.PLAN }.map { it to it.titleRes() }
    val layout = rememberHomePills()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        pills.chunked(layout.perLine()).forEach { line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                line.forEach { (page, title) -> HomePill(stringResource(title), { onOpen(page) }, Modifier.weight(1f)) }
                repeat(layout.perLine() - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HomePill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick, interactionSource = interaction, shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Pacer.colors.accentSoft, contentColor = Pacer.colors.onAccentSoft),
        border = BorderStroke(1.dp, Pacer.colors.outline),
        modifier = modifier.heightIn(min = 48.dp).pressFeedback(interaction),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
}

/** The tablet detail pane's links, below the Plan: Work, Stats and Settings. */
@Composable
internal fun GroupHomeLinks(onOpen: (GroupDestination) -> Unit) {
    RowGroup {
        groupDestinations(LocalFeatures.current).filter { it != GroupDestination.PLAN }.map { it to it.titleRes() }.forEachIndexed { i, (page, title) ->
            if (i > 0) RowDivider()
            LedgerRow(stringResource(title), onClick = { onOpen(page) }) { Chevron() }
        }
    }
}
