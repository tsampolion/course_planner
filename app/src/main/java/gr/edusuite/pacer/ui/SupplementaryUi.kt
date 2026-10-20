package gr.edusuite.pacer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.LessonSave
import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.coreBooks
import gr.edusuite.pacer.engine.lessonPages
import gr.edusuite.pacer.engine.supplementaryBooks
import gr.edusuite.pacer.engine.unplacedPages
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.ui.theme.Mono
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.Space

/*
 * Planning a lesson in a class with supplementary books (docs/SUPPLEMENTARY_BOOKS_BRIEF.md). Core books as in
 * "Plan this lesson", then a Supplementary section: each book's pages and whether they come on top of the lesson or
 * count in it. Counting one in the lesson opens "Pages to teach this lesson" with every book, where the teacher says
 * what each core book will teach; nothing is taken away automatically. Save writes it all as one step (saveLesson).
 */

/**
 * The editor for [lesson] (null for a teaching day the plan has no lesson for yet). [proposed] is what the proposed
 * plan teaches that day, shown under each core field. [onSave] gets the whole lesson; the caller reports a refusal.
 * Save waits until the core pages are confirmed whenever a book counts in the lesson.
 */
@Composable
fun LessonPlanSheet(
    title: String,
    report: GroupReport,
    key: String,
    lesson: PlannedLesson?,
    proposed: Map<String, List<Int>>?,
    onSave: (LessonSave) -> Unit,
    onDismiss: () -> Unit,
) {
    val plan = report.plan
    val core = plan.coreBooks
    val supp = plan.supplementaryBooks
    val pages = lesson?.pages.orEmpty()
    val saver = listSaver<MutableList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() })
    val coreTexts = rememberSaveable(report.group.id, key, saver = saver) {
        mutableStateListOf(*core.map { b -> pages[b.id]?.let(::collapse).orEmpty() }.toTypedArray())
    }
    val suppTexts = rememberSaveable(report.group.id, key, saver = saver) {
        mutableStateListOf(*supp.map { b -> pages[b.id]?.let(::collapse).orEmpty() }.toTypedArray())
    }
    // Saved as text so rotation keeps it: "1" counts in the lesson, "0" comes on top.
    val modes = rememberSaveable(report.group.id, key, saver = saver) {
        mutableStateListOf(*supp.map { b -> if (lesson != null && b.id in lesson.inLesson || (pages[b.id] == null && b.countInLesson)) "1" else "0" }.toTypedArray())
    }
    var note by rememberSaveable(report.group.id, key) { mutableStateOf(lesson?.note.orEmpty()) }
    // The teacher has said what the core books teach since a book started counting in the lesson.
    var confirmed by rememberSaveable(report.group.id, key) { mutableStateOf(lesson?.inLesson?.isNotEmpty() == true) }
    var replacing by rememberSaveable(report.group.id, key) { mutableStateOf(false) }
    var restoredHint by rememberSaveable(report.group.id, key) { mutableStateOf(false) }

    val unplaced = remember(report) { supp.associate { b -> b.id to report.unplacedPages(b) } }
    val corePages = core.mapIndexed { i, b -> lessonPages(coreTexts[i], b) }
    val suppPages = supp.mapIndexed { i, b ->
        lessonPages(suppTexts[i], b)?.takeIf { ps -> ps.all { it in unplaced.getValue(b.id) || it in pages[b.id].orEmpty() } }
    }
    val inLesson = supp.indices.filter { modes[it] == "1" && suppPages[it]?.isNotEmpty() == true }.mapTo(HashSet()) { supp[it].id }
    val motion = animationsEnabled()

    /** Book [gone] stopped counting in the lesson: with none left, the core pages go back to what they were before. */
    fun released(gone: String) {
        if ((inLesson - gone).isNotEmpty()) { confirmed = false; replacing = true; return }
        val base = lesson?.replacementBase ?: return
        core.forEachIndexed { i, b -> coreTexts[i] = base[b.id]?.let(::collapse).orEmpty() }
        restoredHint = true
    }

    SheetFrame(
        title = title,
        primary = stringResource(R.string.group_save),
        enabled = corePages.all { it != null } && suppPages.all { it != null } && (inLesson.isEmpty() || confirmed),
        onPrimary = {
            onSave(LessonSave(
                core = core.zip(corePages).associate { (b, p) -> b.id to p.orEmpty() },
                supplementary = supp.zip(suppPages).associate { (b, p) -> b.id to p.orEmpty() },
                inLesson = inLesson, note = note,
            ))
        },
        onDismiss = onDismiss,
    ) {
        Hint(stringResource(R.string.custom_sheet_hint))
        core.forEachIndexed { i, b ->
            PageField(b, coreTexts[i], { coreTexts[i] = it }, corePages[i] == null, proposed?.let { stringResource(R.string.custom_proposed_line, collapse(it[b.id].orEmpty())) })
        }
        AnimatedVisibility(restoredHint, enter = if (motion) fadeIn(Motion.content()) + expandVertically(Motion.settle()) else fadeIn(snap()), exit = if (motion) fadeOut(Motion.quick()) + shrinkVertically(Motion.settle()) else fadeOut(snap())) {
            Hint(stringResource(R.string.supp_restored))
        }
        Eyebrow(stringResource(R.string.supp_section))
        supp.forEachIndexed { i, b ->
            val left = unplaced.getValue(b.id)
            PageField(
                b, suppTexts[i],
                { text -> val wasIn = b.id in inLesson; suppTexts[i] = text; if (wasIn && text.isBlank()) released(b.id) },
                suppPages[i] == null,
                if (left.isEmpty()) stringResource(R.string.supp_all_placed) else stringResource(R.string.supp_unplaced, collapse(left)),
                errorText = stringResource(R.string.supp_page_error),
            )
            SegmentedToggle(
                listOf(stringResource(R.string.supp_on_top), stringResource(R.string.supp_in_lesson)),
                if (modes[i] == "1") 1 else 0,
                { choice ->
                    val wasIn = b.id in inLesson
                    modes[i] = if (choice == 1) "1" else "0"
                    if (choice == 1 && suppPages[i]?.isNotEmpty() == true) { confirmed = false; restoredHint = false; replacing = true }
                    if (choice == 0 && wasIn) released(b.id)
                },
            )
        }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.custom_note)) },
            placeholder = { Text(stringResource(R.string.custom_note_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (inLesson.isNotEmpty()) TertiaryButton(stringResource(R.string.supp_choose_pages), { replacing = true })
    }

    if (replacing) {
        val bookName = supp.filter { it.id in inLesson }.joinToString(", ") { it.name }
        ReplacementDialog(
            key = key, report = report, bookName = bookName,
            entry = core.map { b -> corePages[core.indexOf(b)].orEmpty() },
            typedSupp = supp.mapIndexed { i, b -> b to suppPages[i].orEmpty() }.filter { it.second.isNotEmpty() },
            onApply = { settled -> settled.forEachIndexed { i, ps -> coreTexts[i] = collapse(ps).takeIf { ps.isNotEmpty() }.orEmpty() }; confirmed = true; replacing = false },
            onDismiss = { replacing = false },
        )
    }
}

/** A pages field for one book, strict like "Edit pages taught": [error] marks it, [support] explains. */
@Composable
private fun PageField(b: Book, text: String, onChange: (String) -> Unit, error: Boolean, support: String?, errorText: String = stringResource(R.string.group_page_error, b.first, b.last)) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        label = { Text(b.name) },
        placeholder = { Text(stringResource(R.string.group_none)) },
        singleLine = true,
        isError = error,
        supportingText = { (if (error) errorText else support)?.let { Text(it) } },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = Mono),
        modifier = Modifier.fillMaxWidth().fieldFeedback(error),
    )
}

/**
 * "Pages to teach this lesson": every book of the class, the core ones editable and prefilled with [entry], the
 * supplementary ones as typed. Apply asks Keep or Skip for each core book typed 0 or left as it was, in class order,
 * then hands the settled pages back; Cancel on a question returns to the window with the draft intact.
 */
@Composable
private fun ReplacementDialog(
    key: String,
    report: GroupReport,
    bookName: String,
    entry: List<List<Int>>,
    typedSupp: List<Pair<Book, List<Int>>>,
    onApply: (List<List<Int>>) -> Unit,
    onDismiss: () -> Unit,
) {
    val core = report.plan.coreBooks
    val saver = listSaver<MutableList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() })
    val texts = rememberSaveable(report.group.id, key, "replace", saver = saver) { mutableStateListOf(*entry.map { collapse(it).takeIf { _ -> it.isNotEmpty() }.orEmpty() }.toTypedArray()) }
    // One answer per core book: "" unanswered, "k" keep, "s" skip. Changing a field forgets its answer.
    val answers = rememberSaveable(report.group.id, key, "answers", saver = saver) { mutableStateListOf(*core.map { "" }.toTypedArray()) }
    var asking by rememberSaveable(report.group.id, key, "asking") { mutableIntStateOf(-1) }
    val parse = { i: Int -> { t: String -> lessonPages(t, core[i]) } }
    val valid = core.indices.all { i -> texts[i].trim() == LEAVE_OUT || lessonPages(texts[i], core[i]) != null }

    fun next() {
        val i = core.indices.firstOrNull { answers[it].isEmpty() && needsKeepChoice(entry[it], texts[it], parse(it)) }
        if (i != null) { asking = i; return }
        onApply(core.indices.map { i -> settledPages(entry[i], texts[i], answers[i].takeIf { it.isNotEmpty() }?.let { it == "k" }, parse(i)).orEmpty() })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.supp_replace_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Hint(stringResource(R.string.supp_replace_hint, bookName))
                core.forEachIndexed { i, b ->
                    val bad = texts[i].trim() != LEAVE_OUT && lessonPages(texts[i], b) == null
                    PageField(b, texts[i], { texts[i] = it; answers[i] = "" }, bad, null)
                }
                typedSupp.forEach { (b, ps) -> Text(stringResource(R.string.supp_line, b.name, collapse(ps)), style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = { next() }, enabled = valid) { Text(stringResource(R.string.supp_replace_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.supp_cancel)) } },
    )

    if (asking in core.indices) {
        val b = core[asking]
        AlertDialog(
            onDismissRequest = { asking = -1 },
            title = { Text(stringResource(R.string.supp_keep_title, b.name)) },
            text = { Text(stringResource(R.string.supp_keep_text, b.name, collapse(entry[asking]))) },
            confirmButton = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    TextButton(onClick = { answers[asking] = "k"; asking = -1; next() }) { Text(stringResource(R.string.supp_keep)) }
                    TextButton(onClick = { answers[asking] = "s"; asking = -1; next() }) { Text(stringResource(R.string.supp_skip)) }
                    TextButton(onClick = { asking = -1 }) { Text(stringResource(R.string.supp_cancel)) }
                }
            },
        )
    }
}
