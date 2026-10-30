package gr.edusuite.pacer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.*
import gr.edusuite.pacer.model.*
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Rhythm
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate

private fun AssignmentType.brick(): Brick = when (this) {
    AssignmentType.HOMEWORK -> Brick.HOMEWORK
    AssignmentType.TEST, AssignmentType.WORKSHEET -> Brick.TESTS
}

/** Keep the editor's order and any saved type, even when it is unavailable for new work. */
fun workAssignmentTypes(features: Features, existing: AssignmentType? = null): List<AssignmentType> =
    listOf(AssignmentType.HOMEWORK, AssignmentType.WORKSHEET, AssignmentType.TEST)
        .filter { features.has(it.brick()) || it == existing }

/** All stays first; saved work always has a filter, independent of creation availability. */
fun workFilterTypes(features: Features, work: List<Assignment>): List<AssignmentType?> =
    listOf(null, AssignmentType.HOMEWORK, AssignmentType.TEST, AssignmentType.WORKSHEET)
        .filter { type -> type == null || features.has(type.brick()) || work.any { it.type == type } }

fun workDefaultType(types: List<AssignmentType>, preferred: AssignmentType): AssignmentType? =
    preferred.takeIf { it in types } ?: types.firstOrNull()

/** A removed chip must not change the selected type to its new neighbor. */
fun workSelectedFilter(types: List<AssignmentType?>, selected: AssignmentType?): AssignmentType? =
    selected.takeIf { it in types }

@Composable
internal fun assignmentTypeName(type: AssignmentType): String = stringResource(when (type) {
    AssignmentType.HOMEWORK -> R.string.work_homework
    AssignmentType.TEST -> R.string.work_test
    AssignmentType.WORKSHEET -> R.string.work_worksheet
})

@Composable
internal fun assignmentTitle(a: Assignment): String = a.title.ifBlank { assignmentTypeName(a.type) }

@Composable
internal fun assignmentEdge(type: AssignmentType) = when (type) {
    AssignmentType.HOMEWORK -> Pacer.colors.edgeHand
    AssignmentType.TEST -> Pacer.colors.edgeTest
    AssignmentType.WORKSHEET -> Pacer.colors.edgeWorksheet
}

@Composable
internal fun AssignmentRow(a: Assignment, report: GroupReport, onOpen: () -> Unit, trailing: (@Composable RowScope.() -> Unit)? = null) {
    val pages = report.plan.books.mapNotNull { b -> a.pages[b.id]?.takeIf { it.isNotEmpty() }?.let { "${b.name} ${collapse(it)}" } }
    val details = listOf(assignmentTypeName(a.type)) + (if (a.completed) listOf(stringResource(R.string.pr6_done)) else emptyList()) + pages + listOfNotNull(
        a.setOn?.let { stringResource(R.string.work_set_date, it.fmtShort()) },
        a.due?.let { stringResource(R.string.work_due_date, it.fmtShort()) },
        if (a.takesLesson && a.type != AssignmentType.HOMEWORK) stringResource(R.string.class_takes_lesson) else null,
        if (a.type == AssignmentType.HOMEWORK && a.pages.isNotEmpty()) {
            (a.due ?: a.setOn)?.let { stringResource(R.string.pr6_homework_pacing, it.fmtShort()) }
                ?: stringResource(R.string.pr6_homework_immediate)
        } else null,
        a.note.takeIf { it.isNotBlank() },
    )
    LedgerRow(assignmentTitle(a), Modifier.background(Pacer.colors.surface), detail = details.joinToString(" · "),
        date = a.workDate()?.fmtShort(), edge = assignmentEdge(a.type), onClick = onOpen, trailing = trailing)
}

/** Every assignment belongs to this group; edits and deletes each use one undo step. */
@Composable
fun WorkScreen(report: GroupReport, vm: PacerViewModel, padding: PaddingValues, undoToast: (String) -> Unit) {
    val context = LocalContext.current
    var filter by rememberSaveable(report.group.id) { mutableIntStateOf(0) }
    var adding by rememberSaveable(report.group.id) { mutableStateOf(false) }
    var editing by rememberSaveable(report.group.id) { mutableStateOf<String?>(null) }
    val features = LocalFeatures.current
    val types = workFilterTypes(features, report.group.assignments)
    val selectedType = workSelectedFilter(types, AssignmentType.entries.getOrNull(filter - 1))
    val canAdd = workAssignmentTypes(features).isNotEmpty()
    val labels = types.map { stringResource(when (it) {
        null -> R.string.work_all
        AssignmentType.HOMEWORK -> R.string.work_homework
        AssignmentType.TEST -> R.string.work_tests
        AssignmentType.WORKSHEET -> R.string.work_worksheets
    }) }
    LaunchedEffect(types) { if (selectedType == null) filter = 0 }
    val today = rememberToday()
    val motion = animationsEnabled()
    val revealed = remember(report.group.id) { mutableSetOf<String>() }
    val sections = workSections(report.group.assignments, today, selectedType)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding() + Space.m,
            bottom = padding.calculateBottomPadding() + Space.xxl * 3)) {
            item {
                LazyRow(Modifier.padding(bottom = Rhythm.inSection), contentPadding = PaddingValues(horizontal = Space.l), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    items(labels.indices.toList()) { index -> FilterChip(selected = selectedType == types[index],
                        onClick = { filter = types[index]?.ordinal?.plus(1) ?: 0 }, label = { Text(labels[index]) }) }
                }
            }
            if (sections.isEmpty()) item {
                Column(Modifier.padding(Space.l)) {
                    EmptyState(AppIcons.Homework, stringResource(R.string.work_empty), stringResource(R.string.work_empty_hint),
                        if (canAdd) stringResource(R.string.work_add) else null) { adding = true }
                }
            }
            sections.forEachIndexed { sectionIndex, section ->
                item { SectionHead(stringResource(when (section.period) {
                    WorkPeriod.THIS_WEEK -> R.string.work_this_week
                    WorkPeriod.LATER -> R.string.work_later
                    WorkPeriod.UNDATED -> R.string.work_undated
                    WorkPeriod.EARLIER -> R.string.work_earlier
                }), Modifier.padding(horizontal = Space.l).let { if (sectionIndex > 0) it.padding(top = Rhythm.section) else it }) }
                itemsIndexed(section.assignments, key = { _, a -> a.id }) { index, a ->
                    var menu by remember { mutableStateOf(false) }
                    val title = assignmentTitle(a)
                    val remove = { if (vm.removeAssignment(report.group.id, a.id)) undoToast(context.getString(R.string.work_deleted)) }
                    Box(Modifier.padding(horizontal = Space.l).workReveal(a.id, index, revealed).animateItem(
                        fadeInSpec = if (motion) Motion.work(MotionSpec.Step.ROW) else null,
                        placementSpec = if (motion) Motion.work(MotionSpec.Step.ROW) else null,
                        fadeOutSpec = if (motion) Motion.work(MotionSpec.Step.MENU) else null)) {
                        SwipeRow(title, remove) {
                            AssignmentRow(a, report, { editing = a.id }) {
                                Box {
                                    PillIconButton(stringResource(R.string.rows_options, title), { menu = true }) { AppIcon(AppIcons.MoreVert) }
                                    PacerDropdownMenu(menu, { menu = false }) {
                                        if (a.type == AssignmentType.HOMEWORK) DropdownMenuItem(
                                            text = { Text(stringResource(if (a.completed) R.string.pr6_mark_not_done else R.string.pr6_mark_done)) },
                                            onClick = { menu = false; vm.saveAssignment(report.group.id, a.copy(completed = !a.completed)) },
                                        )
                                        DropdownMenuItem(text = { Text(stringResource(R.string.work_edit)) }, onClick = { menu = false; editing = a.id })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.component_remove), color = Pacer.colors.warn) }, onClick = { menu = false; remove() })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (sections.isNotEmpty() && canAdd) ExtendedFloatingActionButton(
            onClick = { adding = true }, icon = { AppIcon(AppIcons.Add) }, text = { Text(stringResource(R.string.work_add)) },
            containerColor = Pacer.colors.accent, contentColor = Pacer.colors.onAccent,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = Space.l, bottom = padding.calculateBottomPadding() + Space.l),
        )
    }
    val assignment = report.group.assignments.firstOrNull { it.id == editing }
    if (adding || assignment != null) key(editing) {
        AssignmentEditor(report, vm, assignment, defaultType = selectedType ?: AssignmentType.HOMEWORK,
            undoToast = undoToast, onDismiss = { adding = false; editing = null })
    }
}

@Composable
fun AssignmentSheet(report: GroupReport, initial: Assignment?, defaultType: AssignmentType = AssignmentType.HOMEWORK,
    defaultSetOn: LocalDate? = null, onSave: (Assignment) -> Unit, onDismiss: () -> Unit) {
    val typeOptions = workAssignmentTypes(LocalFeatures.current, initial?.type)
    val startingType = workDefaultType(typeOptions, initial?.type ?: defaultType) ?: return
    val typeLabels = typeOptions.map { assignmentTypeName(it) }
    val defaultTitle = assignmentTypeName(startingType)
    val id = rememberSaveable { initial?.id ?: newId() }
    var type by rememberSaveable { mutableStateOf(startingType) }
    var title by rememberSaveable { mutableStateOf(initial?.title?.takeIf { it.isNotBlank() } ?: defaultTitle) }
    var setText by rememberSaveable { mutableStateOf((initial?.setOn ?: if (initial == null) defaultSetOn else null)?.toString()) }
    var dueText by rememberSaveable { mutableStateOf(initial?.due?.toString()) }
    var note by rememberSaveable { mutableStateOf(initial?.note.orEmpty()) }
    var takesLesson by rememberSaveable { mutableStateOf(initial?.takesLesson ?: false) }
    var contentType by rememberSaveable { mutableStateOf(initial?.contentType) }
    val books = report.plan.books
    val texts = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf(*books.map { b -> collapse(initial?.pages?.get(b.id).orEmpty()).takeUnless { initial?.pages?.get(b.id).isNullOrEmpty() }.orEmpty() }.toTypedArray())
    }
    val pages = books.mapIndexed { i, b -> assignmentPages(texts[i], b) }
    val setOn = setText?.let(LocalDate::parse)
    val due = dueText?.let(LocalDate::parse)
    val datesValid = due == null || setOn == null || due >= setOn
    val datedLesson = type == AssignmentType.HOMEWORK || !takesLesson || due != null
    val valid = title.isNotBlank() && assignmentDatesValid(Assignment(id, type, title, setOn, due, takesLesson = takesLesson)) && pages.all { it != null }
    PacerSheet(stringResource(if (initial == null) R.string.work_add else R.string.work_edit), onDismiss, SheetAction(stringResource(R.string.component_save), valid) {
        onSave(Assignment(id, type, title.trim(), setOn, due,
            assignmentEditedPages(initial?.pages.orEmpty(), books.mapIndexed { i, b -> b.id to pages[i].orEmpty() }.toMap()),
            note.trim(), type != AssignmentType.HOMEWORK && takesLesson,
            contentType.takeIf { type == AssignmentType.HOMEWORK }, initial?.linkedId, initial?.completed ?: false))
    }, dirty = rememberDraftChanged(type, title, setText, dueText, note, takesLesson, contentType, texts.toList())) {
        if (typeOptions.size > 1) SegmentedToggle(typeLabels, typeOptions.indexOf(type), { selected ->
            if (title == typeLabels[typeOptions.indexOf(type)]) title = typeLabels[selected]
            type = typeOptions[selected]
        })
        if (type == AssignmentType.HOMEWORK) Picker(stringResource(R.string.homework_content), listOf(
            "" to stringResource(R.string.homework_pages_notes),
            AssignmentType.WORKSHEET.name to assignmentTypeName(AssignmentType.WORKSHEET),
            AssignmentType.TEST.name to assignmentTypeName(AssignmentType.TEST)), contentType?.name.orEmpty(),
            { value -> contentType = AssignmentType.entries.firstOrNull { it.name == value } })
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.class_title)) },
            colors = paletteFieldColors(), singleLine = true, modifier = Modifier.fillMaxWidth())
        DateField(setOn, { setText = it?.toString() }, stringResource(R.string.work_set_on), Modifier.fillMaxWidth(), clearable = true)
        DateField(due, { dueText = it?.toString() }, stringResource(R.string.work_due), Modifier.fillMaxWidth(), clearable = true)
        if (!datesValid) Hint(stringResource(R.string.work_date_error))
        if (!datedLesson) Hint(stringResource(R.string.work_needs_date))
        val lessonDates = report.lessons.toSet() + report.group.done.map { it.date }
        if ((setOn != null && setOn !in lessonDates) || (due != null && due !in lessonDates)) Hint(stringResource(R.string.work_not_lesson))
        books.forEachIndexed { i, b ->
            val bad = pages[i] == null
            OutlinedTextField(value = texts[i], onValueChange = { texts[i] = it }, label = { Text(b.name) }, singleLine = true,
                colors = paletteFieldColors(), isError = bad, textStyle = MaterialTheme.typography.bodyLarge.merge(Figures),
                supportingText = if (bad) ({ Text(stringResource(if (b.supplementary) R.string.supp_page_error else R.string.group_page_error, b.first, b.last)) }) else null,
                modifier = Modifier.fillMaxWidth().fieldFeedback(bad))
        }
        if (initial?.pages?.keys.orEmpty().any { bookId -> books.none { it.id == bookId } }) {
            Hint(stringResource(R.string.work_unselected_books))
        }
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.custom_note)) },
            colors = paletteFieldColors(), minLines = 2, modifier = Modifier.fillMaxWidth())
        if (type != AssignmentType.HOMEWORK) SwitchRow(stringResource(R.string.class_takes_lesson), takesLesson) { takesLesson = it }
    }
}
