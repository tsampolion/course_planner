package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.*
import gr.edusuite.pacer.model.*
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate

/** Save first; skipping the optional pairing always keeps the teacher's original work. */
@Composable
internal fun AssignmentEditor(report: GroupReport, vm: PacerViewModel, initial: Assignment?,
    defaultType: AssignmentType = AssignmentType.HOMEWORK, defaultSetOn: LocalDate? = null,
    undoToast: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var savedId by rememberSaveable(report.group.id) { mutableStateOf<String?>(null) }
    var editorClosed by rememberSaveable(report.group.id) { mutableStateOf(false) }
    if (!editorClosed) {
        AssignmentSheet(report, initial, defaultType, defaultSetOn, onSave = { a ->
            if (vm.saveAssignment(report.group.id, a)) undoToast(context.getString(R.string.work_saved))
            savedId = a.id
        }, onDismiss = { editorClosed = true; if (savedId == null) onDismiss() })
    } else {
        val saved = report.group.assignments.firstOrNull { it.id == savedId }
        val schedule = saved?.let { suggestSchedule(report, it) }
        val homework = saved?.let { suggestHomework(report, it) }
        if (saved == null || (schedule == null && homework == null)) {
            LaunchedEffect(Unit) { onDismiss() }
        } else key(saved.id) {
            AssignmentLinkDialog(report, saved, schedule, homework, onConfirm = { date ->
                val counterpart = if (schedule != null) Assignment(newId(), homeworkContent(report, saved)!!,
                    saved.title, due = date, pages = saved.pages, note = saved.note, takesLesson = true)
                else Assignment(newId(), AssignmentType.HOMEWORK, saved.title, setOn = date, due = saved.due,
                    pages = saved.pages, note = saved.note, contentType = saved.type)
                if (vm.saveAssignmentPair(report.group.id, saved.id, counterpart)) undoToast(context.getString(R.string.homework_linked))
                onDismiss()
            }, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun AssignmentLinkDialog(report: GroupReport, a: Assignment, schedule: LocalDate?,
    homework: Pair<LocalDate, LocalDate>?, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val scheduling = schedule != null
    val choices = if (scheduling) scheduleChoices(report, a) else homeworkChoices(report, a)
    var selected by rememberSaveable(a.id) { mutableStateOf((schedule ?: homework!!.first).toString()) }
    val date = LocalDate.parse(selected)
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(if (scheduling) R.string.homework_schedule_title else R.string.homework_also_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Text(stringResource(if (scheduling) R.string.homework_schedule_hint else R.string.homework_also_hint, assignmentTitle(a)))
            Picker(stringResource(R.string.homework_lesson), choices.map { it.toString() to it.fmt() }, selected, { selected = it })
            if (scheduling) Hint(stringResource(R.string.homework_reserves_lesson))
            else Text(stringResource(R.string.work_due_date, homework!!.second.fmt()))
        } },
        confirmButton = { TertiaryButton(stringResource(if (scheduling) R.string.homework_schedule else R.string.homework_set),
            { onConfirm(date) }, enabled = date in choices) },
        dismissButton = { TertiaryButton(stringResource(R.string.homework_skip), onDismiss) },
    )
}
