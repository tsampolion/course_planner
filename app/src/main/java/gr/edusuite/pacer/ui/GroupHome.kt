package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.runtime.getValue
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
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate

@Composable
internal fun GroupHome(report: GroupReport, next: Lesson?, today: LocalDate, actions: LessonActions,
    padding: PaddingValues, onDone: () -> Unit, onPagesTaught: () -> Unit, onOpen: (GroupPage) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = Space.l, end = Space.l,
        top = padding.calculateTopPadding() + Space.m, bottom = padding.calculateBottomPadding() + Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.m)) {
        item {
            TodayBox(next, today, actions, onDone, onPagesTaught) {
                val due = report.group.assignments.filter { it.type == AssignmentType.HOMEWORK && it.due == today && next?.date != today }
                val set = report.group.assignments.filter { it.setOn == today && (it.type != AssignmentType.HOMEWORK || next?.date != today) }
                if (due.isNotEmpty()) Text(stringResource(R.string.work_due_today, due.map { assignmentTitle(it) }.joinToString()), style = MaterialTheme.typography.bodySmall)
                if (set.isNotEmpty()) Text(stringResource(R.string.work_set_today, set.map { assignmentTitle(it) }.joinToString()), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                SectionHead(stringResource(R.string.work_progress))
                VerdictHeading(report)
                AnimatedFigure(pluralStringResource(R.plurals.class_lessons_left, report.lessons.size, report.lessons.size), style = MaterialTheme.typography.bodyMedium)
                if (unitProgress(report.plan, report.group).isNotEmpty()) UnitProgressStrip(report)
                else PaceTrack(report, compact = true) { onOpen(GroupPage.STATS) }
            }
        }
        item {
            Column {
                SectionHead(stringResource(R.string.work_due_soon))
                val due = dueSoon(report, today)
                if (due.isEmpty()) Hint(stringResource(R.string.work_no_due))
                else RowGroup { due.forEachIndexed { i, a ->
                    if (i > 0) RowDivider()
                    AssignmentRow(a, report, { onOpen(GroupPage.WORK) }) { Chevron() }
                } }
            }
        }
        item { GroupHomeLinks(onOpen = onOpen) }
    }
}

@Composable
internal fun GroupHomeLinks(showPlan: Boolean = true, onOpen: (GroupPage) -> Unit) {
    RowGroup {
        listOf(GroupPage.PLAN to R.string.group_tab_plan, GroupPage.WORK to R.string.work_title, GroupPage.STATS to R.string.group_tab_stats).filter { showPlan || it.first != GroupPage.PLAN }.forEachIndexed { i, (page, title) ->
            if (i > 0) RowDivider()
            LedgerRow(stringResource(title), onClick = { onOpen(page) }) { Chevron() }
        }
    }
}
