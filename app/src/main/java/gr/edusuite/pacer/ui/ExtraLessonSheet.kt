package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.*
import gr.edusuite.pacer.model.newId
import gr.edusuite.pacer.model.ExtraLesson
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import java.time.LocalDate

/** A pin shared by both plans. Only the draft report changes while the teacher types. */
@Composable
fun ExtraLessonSheet(
    report: GroupReport,
    initialDate: LocalDate,
    chooseDate: Boolean,
    onSave: (LocalDate, String?, Map<String, List<Int>>) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var dateText by rememberSaveable(report.group.id, initialDate.toString()) { mutableStateOf(initialDate.toString()) }
    val date = LocalDate.parse(dateText)
    val old = report.group.extras.firstOrNull { it.date == date }
    val books = report.plan.coreBooks
    val proposed = report.proposedExtraPages(date)
    val texts = rememberSaveable(report.group.id, dateText, saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf(*books.map { b -> (old?.pages ?: proposed)[b.id]?.let(::collapse).orEmpty() }.toTypedArray())
    }
    val parsed = books.mapIndexed { i, b -> lessonPages(texts.getOrElse(i) { "" }, b) }
    val pages = books.zip(parsed).associate { (b, ps) -> b.id to ps.orEmpty() }
    val first = report.group.from ?: report.plan.start
    val last = report.plan.end
    val inTerm = first != null && last != null && date in first..last
    val done = report.group.done.any { it.date == date }
    val blocked = report.plan.eventsOn(report.group, date).any { it.consumes }
    val valid = inTerm && !done && !blocked && parsed.all { it != null }
    val draftId = rememberSaveable(report.group.id) { newId() }
    val draft = report.plan.withExtra(report.group, ExtraLesson(old?.id ?: draftId, date, pages))
    val after = GroupReport(report.plan, draft)
    SheetFrame(
        title = stringResource(if (old == null) R.string.extra_add else R.string.extra_edit),
        primary = stringResource(R.string.group_save), enabled = valid,
        onPrimary = { onSave(date, old?.id, pages) }, onDismiss = onDismiss,
    ) {
        if (chooseDate && old == null) DateField(date, { if (it != null) dateText = it.toString() }, stringResource(R.string.class_date), Modifier.fillMaxWidth())
        else {
            Hint(date.fmt())
            // When a date chosen from the menu already has a pin, allow picking another date too.
            if (chooseDate) DateField(date, { if (it != null) dateText = it.toString() }, stringResource(R.string.class_date), Modifier.fillMaxWidth())
        }
        Hint(stringResource(R.string.extra_hint))
        when {
            done -> Hint(stringResource(R.string.extra_done_hint))
            !inTerm -> Hint(stringResource(R.string.extra_outside))
            blocked -> Hint(stringResource(R.string.extra_blocked))
        }
        books.forEachIndexed { i, b ->
            val bad = parsed[i] == null
            OutlinedTextField(
                value = texts[i], onValueChange = { texts[i] = it }, label = { Text(b.name) }, singleLine = true,
                placeholder = { Text(stringResource(R.string.group_none)) }, isError = bad,
                supportingText = {
                    if (bad) Text(stringResource(R.string.group_page_error, b.first, b.last))
                    else Text(stringResource(R.string.custom_proposed_line, collapse(proposed[b.id].orEmpty())))
                },
                textStyle = MaterialTheme.typography.bodyLarge.merge(Figures), modifier = Modifier.fillMaxWidth().fieldFeedback(bad),
            )
        }
        if (valid) {
            val short = after.lessonsShort()
            Text(if (short == null) stringResource(R.string.extra_after_unmeasured)
                else if (short == 0) stringResource(R.string.extra_after_fits)
                else pluralStringResource(R.plurals.extra_after_short, short, short), style = MaterialTheme.typography.bodyMedium)
        }
        if (old != null) TertiaryButton(stringResource(R.string.extra_delete), { onDelete(old.id); onDismiss() }, color = Pacer.colors.warn)
    }
}
