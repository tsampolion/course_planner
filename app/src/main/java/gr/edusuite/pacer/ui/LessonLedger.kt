package gr.edusuite.pacer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.closedDates
import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.notTaught
import gr.edusuite.pacer.engine.eventsOn
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.model.Brick
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.engine.assignmentsOn
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * The Plan tab's ledger (design decision 8): today's lesson in its box, then one line per lesson in week sections,
 * with an edge colour per kind (docs/PACER_DESIGN.md D6, D8). Each row is one TalkBack node carrying every action its
 * long-press menu offers, so nothing needs a long-press or a drag.
 */

/** One lesson as the schedule shows it: taught (from the group's done lessons) or still to come. */
internal data class Lesson(
    val n: Int,
    val date: LocalDate,
    val events: List<TestEvent>,
    /** A test takes the whole lesson. */
    val blocked: Boolean,
    val cells: List<String>,
    val done: Boolean,
    /** Recorded with pages other than the planned ones. */
    val edited: Boolean = false,
    /** The custom plan's note for the lesson. */
    val note: String = "",
    /** The custom plan's lesson on this day, its place in that plan, and whether the teacher changed it by hand. */
    val lessonId: String? = null,
    val index: Int = -1,
    val touched: Boolean = false,
    /** A teaching day the custom plan has no lesson for. */
    val free: Boolean = false,
    /** What the proposed plan teaches that day, when the custom plan shown teaches something else. */
    val proposed: String? = null,
    /** The books (by place) whose pages differ from the proposed plan's; see [differingBooks]. */
    val differs: Set<Int> = emptySet(),
    /** Its place among the teaching days still to come (the custom plan's lesson there, or the one a save would make). */
    val slot: Int = -1,
    /** The supplementary books that count in this planned lesson. */
    val inLesson: Set<String> = emptySet(),
    val extraId: String? = null,
    val assignments: List<Assignment> = emptyList(),
    val unitIndex: Int? = null,
    val unitLesson: Int = 0,
    val unitLessons: Int = 0,
    /** Done lessons only: pages taught from books the group no longer uses. They are never part of [cells]. */
    val history: List<HistoricBook> = emptyList(),
    /** Done with no pages taught: the lesson didn't happen, and its pages went back to the lessons left. */
    val notTaught: Boolean = false,
)

/** A book the group dropped (or never selected) that still has pages taught in a done lesson. */
internal data class HistoricBook(val id: String, val name: String, val pages: String)

/**
 * Done lessons and the lessons still to come, in date order and numbered together. With [compare], each lesson of
 * a custom plan that teaches something other than [compare] says what that plan teaches.
 */
internal fun lessonsOf(report: GroupReport, compare: GroupReport? = null): List<Lesson> {
    val plan = report.plan
    val g = report.group
    val dropped = report.classBooks.filter { !g.usesBook(it) }
    val done = g.done.map { d ->
        val events = plan.eventsOn(g, d.date)
        val revision = g.unitRevisions.any { revision ->
            g.assignments.any { it.id == revision.assignmentId && it.due == d.date && it.takesLesson }
        }
        val blocked = revision || (d.pages.isEmpty() && events.any { it.consumes })
        Lesson(0, d.date, events, blocked, plan.books.map { b -> if (blocked && d.pages.isEmpty()) "" else collapse(d.pages[b.id].orEmpty()) }, done = true, edited = d.edited, notTaught = d.notTaught, history = dropped.mapNotNull { b -> d.pages[b.id]?.takeIf { it.isNotEmpty() }?.let { HistoricBook(b.id, b.name, collapse(it)) } }, extraId = g.extras.firstOrNull { it.date == d.date }?.id, assignments = assignmentsOn(g, d.date))
    }
    val custom = report.custom
    val places = custom?.lessons?.withIndex()?.associate { (i, l) -> l.id to (i to l.touched) }.orEmpty()
    val counting = custom?.lessons?.associate { it.id to it.inLesson }.orEmpty()
    val other = compare?.rows?.associateBy { it.date }
    var slot = 0
    val upcoming = report.rows.map { r ->
        val place = r.lessonId?.let(places::get)
        val proposed = other?.get(r.date)?.takeIf { custom != null && !r.blocked && it.cells != r.cells }
            ?.let { o -> plan.books.zip(o.cells).joinToString(" · ") { (b, cell) -> "${b.name} $cell" } }
        val lesson = Lesson(
            0, r.date, r.events, r.blocked, r.cells, done = false, note = r.note, lessonId = r.lessonId,
            index = place?.first ?: -1, touched = place?.second ?: false,
            free = custom != null && r.lessonId == null && r.extraId == null && !r.blocked, proposed = proposed,
            slot = if (r.blocked || r.extraId != null) -1 else slot++, inLesson = r.lessonId?.let(counting::get).orEmpty(), extraId = r.extraId, assignments = r.assignments,
        )
        val proposedLesson = if (custom == null) null else other?.get(r.date)?.let { o -> Lesson(0, o.date, o.events, o.blocked, o.cells, done = false) }
        lesson.copy(differs = differingBooks(lesson, proposedLesson, plan.books.map { it.supplementary }))
    }
    return (done + upcoming).sortedBy { it.date }.mapIndexed { i, l -> l.copy(n = i + 1) }
}

/** A line of the ledger: a lesson, or a day off on one of the group's lesson days. */
internal sealed interface Entry {
    val date: LocalDate
    data class Item(val lesson: Lesson) : Entry { override val date: LocalDate get() = lesson.date }
    data class DayOff(override val date: LocalDate, val label: String) : Entry
}

/** The term's days off that fall on [g]'s lesson days and hold no lesson, in date order. */
internal fun Plan.daysOff(g: Group, lessonDates: Set<LocalDate>): List<Entry.DayOff> {
    val start = start ?: return emptyList()
    val end = end ?: return emptyList()
    return closedDates(g.id).filter { (d, _) -> d.dayOfWeek in g.days && d in start..end && d !in lessonDates }
        .map { (d, label) -> Entry.DayOff(d, label) }.sortedBy { it.date }
}

/** What a long-press on a lesson offers, and what tapping its tick does. */
internal class LessonActions(
    val bookNames: List<String>,
    /** The names a row shows: initials where they don't clash ([shortBookNames]). */
    val shortNames: List<String> = bookNames,
    val bookIds: List<String> = emptyList(),
    /** Per book, in the plan's order: a supplementary book, shown only where it has pages. */
    val supplementary: List<Boolean> = emptyList(),
    /** What "Add <book> pages…" opens; null when there are no supplementary books or it can't be planned here. */
    val addSupplementary: ((Lesson) -> Unit)? = null,
    val supplementaryBooks: List<Pair<String, String>> = emptyList(),
    val toggle: (LocalDate) -> Unit,
    /** Opens "Pages taught" for the lesson. */
    val edit: (Lesson) -> Unit,
    val upTo: (LocalDate) -> Unit,
    val notFrom: (LocalDate) -> Unit,
    /** Some lesson on or before this date is not done yet. */
    val canUpTo: (LocalDate) -> Boolean,
    /** Some lesson on or after this date is done. */
    val canNotFrom: (LocalDate) -> Boolean,
    /** The lessons shown are the ones being taught, so they can be ticked. A plan shown for comparison can't. */
    val canTick: Boolean = true,
    /** Set while a custom plan is shown: what its lessons' menus offer. */
    val custom: CustomActions? = null,
    val addExtra: (LocalDate) -> Unit = {},
    val editExtra: (Lesson) -> Unit = {},
    val deleteExtra: (Lesson) -> Unit = {},
    val addHomework: (Lesson) -> Unit = {},
    val editHomework: (Assignment) -> Unit = {},
    val toggleHomework: (Assignment) -> Unit = {},
    val deleteHomework: (Assignment) -> Unit = {},
    /** Asks whether the lesson didn't happen, and records it as not taught. */
    val notTaught: (Lesson) -> Unit = {},
)

/** What a lesson of a custom plan offers. [last] is the plan's last place. */
internal class CustomActions(
    val last: Int,
    val plan: (Lesson) -> Unit,
    val addBefore: (Lesson) -> Unit,
    val move: (Lesson, Int) -> Unit,
    val repace: (Lesson) -> Unit,
    val remove: (Lesson) -> Unit,
)

/** One entry of a lesson's menu, which TalkBack offers too while it is [enabled]. [divider] draws a line above it. */
internal class MenuItem(val label: String, val enabled: Boolean = true, val divider: Boolean = false, val section: MenuSection = MenuSection.LESSON, val run: () -> Unit)

/** Where an item sits in the long-press menu: about the lesson itself, or editing the custom plan (behind one item). */
internal enum class MenuSection { LESSON, MY_PLAN }

/**
 * Everything a lesson offers, shared by the long-press menu and TalkBack. Future lessons offer planned pages;
 * today and past lessons offer recording what was taught.
 */
@Composable
internal fun lessonMenu(l: Lesson, actions: LessonActions): List<MenuItem> {
    val ca = actions.custom?.takeIf { !l.done && !l.blocked && (l.lessonId != null || l.free) }
    val addSupp = actions.addSupplementary?.takeIf { !l.done && !l.blocked && l.slot >= 0 }
    val suppLabels = actions.supplementaryBooks.map { (_, name) -> stringResource(R.string.supp_add_pages, name) }
    val planIt = stringResource(if (l.lessonId != null) R.string.custom_plan_lesson else R.string.custom_add_here)
    val addBefore = stringResource(R.string.custom_add_before)
    val earlier = stringResource(R.string.custom_move_earlier)
    val later = stringResource(R.string.custom_move_later)
    val repace = stringResource(R.string.custom_repace_here)
    val remove = stringResource(R.string.custom_remove)
    val editExtra = stringResource(R.string.extra_edit)
    val deleteExtra = stringResource(R.string.extra_delete)
    val edit = stringResource(R.string.group_edit_pages_menu)
    val upTo = stringResource(R.string.group_done_up_to)
    val notFrom = stringResource(R.string.group_not_done_from)
    val notTaught = stringResource(R.string.group_not_taught_menu)
    val addHomework = stringResource(R.string.homework_add)
    val work = homeworkForLesson(l)
    val workNames = homeworkActionNames(work, actions.bookIds, actions.shortNames).mapIndexed { i, text ->
        text.ifBlank { "${i + 1}. ${assignmentTitle(work[i])}" }
    }
    val toggleHomework = workNames.mapIndexed { i, name -> stringResource(if (work[i].completed) R.string.pr6_mark_not_done_named else R.string.pr6_mark_done_named, name) }
    val editHomework = workNames.map { stringResource(R.string.homework_edit_named, it) }
    val deleteHomework = workNames.map { stringResource(R.string.homework_delete_named, it) }
    val kinds = lessonMenuItems(l, isPast = !l.date.isAfter(rememberToday()), hasCustomPlan = ca != null, canTick = actions.canTick, ticksItself = l.date.isBefore(rememberToday()), features = LocalFeatures.current)
    return buildList {
        kinds.forEach { kind ->
            when (kind) {
                MenuItemKind.ADD_HOMEWORK -> add(MenuItem(addHomework) { actions.addHomework(l) })
                MenuItemKind.TOGGLE_HOMEWORK -> work.forEachIndexed { i, a -> add(MenuItem(toggleHomework[i]) { actions.toggleHomework(a) }) }
                MenuItemKind.EDIT_HOMEWORK -> work.forEachIndexed { i, a -> add(MenuItem(editHomework[i]) { actions.editHomework(a) }) }
                MenuItemKind.DELETE_HOMEWORK -> work.forEachIndexed { i, a -> add(MenuItem(deleteHomework[i]) { actions.deleteHomework(a) }) }
                MenuItemKind.EDIT_EXTRA -> add(MenuItem(editExtra) { actions.editExtra(l) })
                MenuItemKind.DELETE_EXTRA -> add(MenuItem(deleteExtra) { actions.deleteExtra(l) })
                MenuItemKind.ADD_SUPPLEMENTARY -> if (addSupp != null) suppLabels.forEach { label -> add(MenuItem(label) { addSupp(l) }) }
                MenuItemKind.PLAN_LESSON -> if (ca != null) add(MenuItem(planIt, section = kind.section()) { ca.plan(l) })
                MenuItemKind.ADD_BEFORE -> if (ca != null) add(MenuItem(addBefore, section = kind.section()) { ca.addBefore(l) })
                MenuItemKind.MOVE_EARLIER -> if (ca != null) add(MenuItem(earlier, enabled = l.index > 0, section = kind.section()) { ca.move(l, l.index - 1) })
                MenuItemKind.MOVE_LATER -> if (ca != null) add(MenuItem(later, enabled = l.index < ca.last, section = kind.section()) { ca.move(l, l.index + 1) })
                MenuItemKind.REPACE -> if (ca != null) add(MenuItem(repace, section = kind.section()) { ca.repace(l) })
                MenuItemKind.REMOVE -> if (ca != null) add(MenuItem(remove, section = kind.section()) { ca.remove(l) })
                MenuItemKind.RECORD_TAUGHT -> add(MenuItem(edit, divider = isNotEmpty()) { actions.edit(l) })
                MenuItemKind.NOT_TAUGHT -> add(MenuItem(notTaught) { actions.notTaught(l) })
                MenuItemKind.DONE_UP_TO -> add(MenuItem(upTo, enabled = actions.canUpTo(l.date)) { actions.upTo(l.date) })
                MenuItemKind.NOT_DONE_FROM -> add(MenuItem(notFrom, enabled = actions.canNotFrom(l.date)) { actions.notFrom(l.date) })
            }
        }
    }
}

/**
 * The menu a long-press opens: the same items TalkBack offers as custom actions. With a custom plan it has two parts,
 * "This lesson" and "My plan", and the plan's actions sit behind one "Edit my plan" item.
 */
@Composable
internal fun LessonMenu(expanded: Boolean, items: List<MenuItem>, onDismiss: () -> Unit) {
    val lesson = items.filter { it.section == MenuSection.LESSON }
    val plan = items.filter { it.section == MenuSection.MY_PLAN }
    var editingPlan by remember(expanded) { mutableStateOf(false) }
    val row = @Composable { item: MenuItem -> DropdownMenuItem(text = { Text(item.label) }, enabled = item.enabled, onClick = { onDismiss(); item.run() }) }
    PacerDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (plan.isEmpty()) {
            items.forEach { item ->
                if (item.divider) HorizontalDivider(color = Pacer.colors.line)
                row(item)
            }
        } else {
            MenuHeading(stringResource(R.string.menu_this_lesson))
            lesson.forEach { row(it) }
            HorizontalDivider(color = Pacer.colors.line)
            MenuHeading(stringResource(R.string.menu_my_plan))
            if (editingPlan) plan.forEach { row(it) }
            else DropdownMenuItem(text = { Text(stringResource(R.string.menu_edit_my_plan)) }, trailingIcon = { Chevron() }, onClick = { editingPlan = true })
        }
    }
}

@Composable
private fun MenuHeading(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Pacer.colors.muted, modifier = Modifier.padding(horizontal = Space.l, vertical = Space.xs))
}

/**
 * Makes a lesson one TalkBack node: it reads [description], is a checkbox when it can be ticked ([onToggle]), and
 * offers every enabled [items] entry as a custom action. A long-press opens [onLongPress], the same menu.
 */
@Composable
internal fun Modifier.lessonNode(description: String, done: Boolean, onToggle: (() -> Unit)?, items: List<MenuItem>, onLongPress: () -> Unit): Modifier {
    val toggleLabel = stringResource(if (done) R.string.ledger_mark_not_done else R.string.group_mark_done)
    val optionsLabel = stringResource(R.string.component_options)
    val hasMenu = items.isNotEmpty()
    // A reused row gets another lesson's menu, so the gesture always calls the latest one.
    val longPress by rememberUpdatedState(onLongPress)
    val motion = animationsEnabled()
    val ring = remember { Animatable(0f) }
    var point by remember { mutableStateOf(Offset.Zero) }
    val ringColor = Pacer.colors.accentText
    return this
        .pointerInput(hasMenu, motion) {
            detectTapGestures(onLongPress = { if (hasMenu) longPress() }, onPress = { position ->
                point = position
                coroutineScope {
                    val drawing = launch {
                        if (hasMenu && motion) { ring.snapTo(0f); ring.animateTo(1f, Motion.work(MotionSpec.Step.LONG_PRESS)) }
                    }
                    tryAwaitRelease()
                    drawing.cancel()
                    ring.snapTo(0f)
                }
            })
        }
        .drawWithContent {
            drawContent()
            if (motion && ring.value > 0f) {
                val radius = Space.l.toPx() * ring.value
                drawArc(ringColor, -90f, 360f * ring.value, false,
                    topLeft = point - Offset(radius, radius), size = Size(radius * 2, radius * 2), style = Stroke(2.dp.toPx()))
            }
        }
        .clearAndSetSemantics {
            contentDescription = description
            if (onToggle != null) {
                role = Role.Checkbox
                toggleableState = ToggleableState(done)
                onClick(toggleLabel) { onToggle(); true }
            }
            if (hasMenu) onLongClick(optionsLabel) { onLongPress(); true }
            customActions = items.filter { it.enabled }.map { item -> CustomAccessibilityAction(item.label) { item.run(); true } }
        }
}

/** "Friday 23 October", as TalkBack reads a lesson's date. */
private val SPOKEN_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

/** The kinds a row's edge tells apart (D8). One edge per row; the row's words still say the kind. */
internal enum class LessonKind { PLAIN, TEST, WORKSHEET, DAY_OFF, EDITED, HAND, FREE, SUPP }

/** The one kind a lesson's edge shows: a test first, then a free lesson, pages taught as edited, by hand, supplementary. */
internal fun Lesson.kind(supplementary: List<Boolean>): LessonKind = when {
    events.isNotEmpty() && assignments.any { it.type == AssignmentType.WORKSHEET && it.due == date } &&
        assignments.none { it.type == AssignmentType.TEST && it.due == date } -> LessonKind.WORKSHEET
    events.isNotEmpty() -> LessonKind.TEST
    free -> LessonKind.FREE
    edited -> LessonKind.EDITED
    touched && !done -> LessonKind.HAND
    supplementary.indices.any { supplementary[it] && cells.getOrNull(it).let { c -> c != null && c != collapse(emptyList()) } } -> LessonKind.SUPP
    else -> LessonKind.PLAIN
}

@Composable
private fun LessonKind.edge(): Color = when (this) {
    LessonKind.PLAIN -> Color.Transparent
    LessonKind.TEST -> Pacer.colors.edgeTest
    LessonKind.WORKSHEET -> Pacer.colors.edgeWorksheet
    LessonKind.DAY_OFF -> Pacer.colors.edgeDayOff
    LessonKind.EDITED -> Pacer.colors.edgeEdited
    LessonKind.HAND -> Pacer.colors.edgeHand
    LessonKind.FREE -> Pacer.colors.edgeFree
    LessonKind.SUPP -> Pacer.colors.edgeSupp
}

/**
 * A lesson row's ground (D14): the selected row is the accent's soft colour, today's row the second primary's, and a row
 * of any other kind a tint of its kind's colour; a plain lesson stays on surface.
 */
@Composable
private fun rowGround(kind: LessonKind?, date: LocalDate, selected: Boolean): Color {
    val c = Pacer.colors
    return when {
        selected -> c.accentSoft
        date == rememberToday() -> c.secondSoft
        kind == null || kind == LessonKind.PLAIN -> c.surface
        else -> c.kindTint(kind.edge())
    }
}

private val EdgeW = 4.dp
/** One width for the date gutter of every lesson row, day-off rows and tests included. */
private val GutterW = 56.dp
private val TickW = 48.dp

/** A book's pages as a row shows them, "SB 38–40 · WB 20–21", with supplementary books left for the line under it. */
internal fun corePages(l: Lesson, names: List<String>, supplementary: List<Boolean>): String =
    corePageParts(l, names, supplementary).joinToString(" · ") { it.second }

/** Each core book's "SB 38–40" with the book's place, so a row can colour the books that differ from the proposed plan. */
internal fun corePageParts(l: Lesson, names: List<String>, supplementary: List<Boolean>): List<Pair<Int, String>> =
    l.cells.withIndex().filter { (i, cell) -> !supplementary.getOrElse(i) { false } && (!l.blocked || (cell.isNotBlank() && cell != collapse(emptyList()))) }
        .map { (i, cell) -> i to names.getOrElse(i) { (i + 1).toString() } + " " + cell }

/** The supplementary pages a lesson places, by full name: "Reader 5–8". */
internal fun suppPages(l: Lesson, actions: LessonActions): List<Pair<Int, String>> =
    l.cells.withIndex().filter { (i, cell) -> actions.supplementary.getOrElse(i) { false } && cell.isNotBlank() && cell != collapse(emptyList()) }
        .map { (i, cell) -> i to actions.bookNames.getOrElse(i) { "" } + " " + cell }

@Composable
private fun homeworkSummaries(l: Lesson, actions: LessonActions): List<String> = homeworkForLesson(l).map { a ->
    val content = homeworkTextParts(a, actions.bookIds, actions.shortNames) +
        (if (a.completed) listOf(stringResource(R.string.pr6_done)) else emptyList()) +
        listOfNotNull(a.contentType?.let { assignmentTypeName(it) }) + listOfNotNull(
            a.due?.let { stringResource(R.string.homework_due_short, "${it.dayOfWeek.short()} ${it.fmtShort()}") })
    stringResource(R.string.homework_row, content.joinToString(" · ").ifBlank { a.note.ifBlank { assignmentTypeName(a.type) } })
}

@Composable
internal fun HomeworkLines(l: Lesson, actions: LessonActions, color: Color = Pacer.colors.muted) {
    homeworkSummaries(l, actions).forEach { Text(it, style = MaterialTheme.typography.bodySmall.merge(Figures), color = color) }
}

/** A week's heading: "This week" or "Next week" with its dates beside, or only the dates. */
@Composable
internal fun WeekHeader(section: WeekSection<*>, modifier: Modifier = Modifier) {
    val range = weekRange(section.start, section.end)
    Row(
        modifier.fillMaxWidth().padding(start = Space.l, end = Space.l, top = Space.l, bottom = Space.xs).semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.Bottom,
    ) {
        val title = when (section.label) {
            WeekLabel.THIS -> stringResource(R.string.ledger_this_week)
            WeekLabel.NEXT -> stringResource(R.string.ledger_next_week)
            WeekLabel.DATES -> range
        }
        Text(title, style = MaterialTheme.typography.titleSmall.merge(Figures), modifier = Modifier.weight(1f))
        if (section.label != WeekLabel.DATES) Text(range, style = MaterialTheme.typography.labelSmall.merge(Figures), color = Pacer.colors.muted)
    }
}

/** What TalkBack reads for a lesson: its number, its date in full, and its pages with the books' full names. */
@Composable
internal fun spokenLesson(l: Lesson, actions: LessonActions): String {
    val parts = buildList {
        l.unitIndex?.let { add(stringResource(R.string.units_lesson_position, it + 1, l.unitLesson, l.unitLessons)) }
        if (l.events.isNotEmpty()) add(l.events.joinToString(", ") { it.name })
        if (l.note.isNotEmpty()) add(l.note)
        l.cells.forEachIndexed { i, cell ->
            if (l.blocked && (cell.isBlank() || cell == collapse(emptyList()))) return@forEachIndexed
            val supp = actions.supplementary.getOrElse(i) { false }
            if (supp && cell == collapse(emptyList())) return@forEachIndexed
            add(stringResource(R.string.group_book_pages, actions.bookNames.getOrNull(i) ?: stringResource(R.string.group_book_number, i + 1), cell))
        }
        l.history.forEach { add(stringResource(R.string.ledger_retired_spoken, it.name, it.pages)) }
        if (l.extraId != null) add(stringResource(R.string.extra_label))
        if (l.free) add(stringResource(R.string.custom_free))
        if (l.touched && !l.done) add(stringResource(R.string.ledger_by_hand))
        if (l.notTaught) add(stringResource(R.string.group_not_taught)) else if (l.edited) add(stringResource(R.string.group_edited))
        addAll(homeworkSummaries(l, actions))
        if (l.differs.isNotEmpty()) add(stringResource(R.string.custom_differs))
        l.proposed?.let { add(stringResource(R.string.custom_proposed_line, it)) }
    }
    return stringResource(R.string.ledger_lesson_spoken, l.n, l.date.format(SPOKEN_DATE), parts.joinToString(". "))
}

/** Reveals the approved check glyph, once per completion pulse, with a finished reduced-motion state. */
@Composable
internal fun DrawingCheck(pulse: Int, tint: Color, modifier: Modifier) {
    val motion = animationsEnabled()
    val progress = remember { Animatable(1f) }
    LaunchedEffect(pulse, motion) {
        if (pulse > 0 && motion) { progress.snapTo(0f); progress.animateTo(1f, Motion.work(MotionSpec.Step.TICK)) }
        else progress.snapTo(1f)
    }
    Icon(AppIcons.Check, contentDescription = null, tint = tint, modifier = modifier.drawWithContent {
        clipRect(right = size.width * if (motion) progress.value else 1f) { this@drawWithContent.drawContent() }
    })
}

/** The day number in figures over the weekday, the register-style gutter of every lesson row (it replaces the date text). */
@Composable
private fun DateGutter(date: LocalDate, color: Color) {
    Column(Modifier.width(GutterW).padding(start = Space.s), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium.merge(Figures), color = color)
        Text(date.dayOfWeek.short().uppercase(), style = MaterialTheme.typography.labelSmall, color = Pacer.colors.muted)
    }
}

/** The round tick at a row's end: a 48dp target holding a 24dp ring, filled with a check once the lesson is done. */
@Composable
private fun RoundTick(done: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val c = Pacer.colors
    val haptic = rememberToggleHaptic()
    Box(
        Modifier.size(TickW).clickable(enabled = enabled) { haptic(!done); onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(24.dp)
                .background(if (done) c.accent else Color.Transparent, CircleShape)
                .border(2.dp, if (done) c.accent else if (enabled) c.outline else c.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (done) DrawingCheck(1, c.onAccent, Modifier.size(18.dp)) }
    }
}

/**
 * A lesson on one line: the date, the books' initials with pages, and the tick, with one edge for its kind. A line
 * under it names the kind (a test, by hand, a free lesson, supplementary pages). A long-press opens its menu, and the
 * whole row is one TalkBack node with those actions. A custom plan's lesson is dragged by [grip]. Done lessons read in
 * muted text at full contrast, never faded.
 */
@Composable
internal fun LessonRow(l: Lesson, actions: LessonActions, grip: (@Composable (label: String) -> Unit)? = null, selected: Boolean = false) {
    val c = Pacer.colors
    var menu by rememberSaveable(l.date.toString()) { mutableStateOf(false) }
    val items = lessonMenu(l, actions)
    val description = spokenLesson(l, actions)
    val ink = if (l.done) c.muted else c.ink
    val large = rememberLargeText()
    val subtitle = buildList {
        l.unitIndex?.let { add(stringResource(R.string.units_lesson_position, it + 1, l.unitLesson, l.unitLessons)) }
        if (l.events.isNotEmpty()) add(l.events.joinToString(" · ") { it.name })
        if (l.extraId != null) add(stringResource(R.string.extra_label))
        if (l.free) add(stringResource(R.string.custom_free))
        if (l.touched && !l.done) add(stringResource(R.string.ledger_by_hand))
        if (l.notTaught) add(stringResource(R.string.group_not_taught)) else if (l.edited) add(stringResource(R.string.group_edited))
        suppPages(l, actions).forEach { (i, text) ->
            val where = if (l.done) null else stringResource(if (actions.bookIds.getOrNull(i) in l.inLesson) R.string.supp_in_lesson else R.string.supp_on_top)
            add(listOfNotNull(stringResource(R.string.ledger_supp, text), where).joinToString(" · "))
        }
    }
    val pages = corePages(l, actions.shortNames, actions.supplementary)
    val warn = c.warn
    val pagesText = if (l.differs.isEmpty()) AnnotatedString(pages) else buildAnnotatedString {
        corePageParts(l, actions.shortNames, actions.supplementary).forEachIndexed { n, (i, text) ->
            if (n > 0) append(" · ")
            if (i in l.differs) withStyle(SpanStyle(color = warn)) { append(text) } else append(text)
        }
    }
    // A free lesson with only a note reads as its note.
    val core = if (l.note.isNotEmpty() && l.cells.withIndex().all { (i, cell) -> actions.supplementary.getOrElse(i) { false } || cell == collapse(emptyList()) }) AnnotatedString(l.note) else pagesText
    // Books the group dropped after teaching from them: greyed and labelled, after the books it still uses.
    val retired = l.history.map { stringResource(R.string.ledger_retired_pages, it.name, it.pages) }
    val main = if (retired.isEmpty()) core else buildAnnotatedString {
        append(core)
        retired.forEach { text ->
            if (length > 0) append(" · ")
            withStyle(SpanStyle(color = c.muted)) { append(text) }
        }
    }
    Box {
        Column {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(rowGround(l.kind(actions.supplementary), l.date, selected))
                    .lessonNode(description, l.done, if (actions.canTick) ({ actions.toggle(l.date) }) else null, items) { menu = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(EdgeW).fillMaxHeight().padding(vertical = Space.s).background(l.kind(actions.supplementary).edge()))
                val date = "${l.date.dayOfWeek.short()} ${l.date.fmtShort()}"
                val details = @Composable {
                    Text(main, style = MaterialTheme.typography.bodyLarge.merge(Figures), color = ink)
                    if (main.text != l.note && l.note.isNotEmpty()) Text(l.note, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = c.muted)
                    if (subtitle.isNotEmpty()) Text(subtitle.joinToString(" · "), style = MaterialTheme.typography.bodySmall.merge(Figures), color = c.muted)
                    HomeworkLines(l, actions)
                    l.proposed?.let { Text(stringResource(R.string.custom_proposed_line, it), style = MaterialTheme.typography.bodySmall.merge(Figures), color = c.muted) }
                }
                // Large text: the date sits over the pages, so neither is squeezed into a column.
                if (large) Column(Modifier.weight(1f).padding(start = Space.m, top = Space.m, bottom = Space.m), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(date, style = MaterialTheme.typography.titleSmall.merge(Figures), color = ink)
                    details()
                } else {
                    DateGutter(l.date, if (l.date == rememberToday() && !l.done) c.onSecondSoft else ink)
                    Column(Modifier.weight(1f).padding(vertical = Space.m), verticalArrangement = Arrangement.spacedBy(2.dp)) { details() }
                }
                if (grip != null) grip(stringResource(R.string.custom_drag, l.n))
                RoundTick(l.done, actions.canTick) { actions.toggle(l.date) }
            }
            HorizontalDivider(Modifier.padding(start = Space.l), color = c.line)
        }
        LessonMenu(menu, items) { menu = false }
    }
}

/**
 * A row for a day off or a whole-lesson test or worksheet: the gutter and what the day holds, with its kind's edge and no
 * lesson number. A test or worksheet is as tall and as large as any lesson, with its kind in bold, and takes the lesson's
 * tick and menu. A day off stays slim, has no tick, and does nothing when tapped: a long-press offers Add extra lesson.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SlimRow(entry: Entry, actions: LessonActions, selected: Boolean = false) {
    val c = Pacer.colors
    val lesson = (entry as? Entry.Item)?.lesson
    val large = rememberLargeText()
    var menu by rememberSaveable(entry.date.toString()) { mutableStateOf(false) }
    val addLabel = stringResource(R.string.extra_add)
    // A day off offers only Add extra lesson, which needs the Extra lessons brick.
    val canAddExtra = LocalFeatures.current.has(Brick.EXTRA_LESSONS)
    val items = if (lesson != null) lessonMenu(lesson, actions) else if (canAddExtra) listOf(MenuItem(addLabel) { actions.addExtra(entry.date) }) else emptyList()
    val text = when (entry) {
        is Entry.DayOff -> stringResource(R.string.ledger_day_off, entry.label)
        is Entry.Item -> if (entry.lesson.kind(actions.supplementary) == LessonKind.WORKSHEET)
            stringResource(R.string.work_worksheet_lesson, entry.lesson.events.joinToString(" · ") { it.name })
        else stringResource(R.string.ledger_test, entry.lesson.events.joinToString(" · ") { it.name })
    }
    val pageSummary = lesson?.let { l -> l.cells.withIndex()
        .filter { (_, cell) -> cell.isNotBlank() && cell != collapse(emptyList()) }
        .joinToString(" · ") { (i, cell) ->
            val name = actions.bookNames.getOrElse(i) { "" }
            "$name $cell"
        }
    }.orEmpty()
    val summary = listOf(text, pageSummary).filter { it.isNotBlank() }.joinToString(" · ")
    // The kind word of a test or worksheet ("Test", "Worksheet") is the part before the first " · ".
    val shown = if (lesson == null) AnnotatedString(summary) else buildAnnotatedString {
        val cut = summary.indexOf(" · ").takeIf { it > 0 } ?: 0
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(summary.substring(0, cut)) }
        append(summary.substring(cut))
    }
    val homework = lesson?.let { homeworkSummaries(it, actions) }.orEmpty()
    val spoken = (listOf("${entry.date.format(SPOKEN_DATE)}. $summary") + homework).joinToString(". ")
    val textStyle = if (lesson != null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall
    val textColor = if (lesson != null) (if (lesson.done) c.muted else c.ink) else c.muted
    val pad = if (lesson != null) Space.m else Space.s
    val longPress by rememberUpdatedState { menu = true }
    Box {
        Column {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(rowGround(lesson?.kind(actions.supplementary) ?: LessonKind.DAY_OFF, entry.date, selected)).heightIn(min = 48.dp).then(
                    if (lesson != null) Modifier.lessonNode(spoken, lesson.done, if (actions.canTick) ({ actions.toggle(lesson.date) }) else null, items) { menu = true }
                    else if (canAddExtra) Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { longPress() }) }
                        .clearAndSetSemantics {
                            contentDescription = spoken
                            onLongClick(addLabel) { menu = true; true }
                            customActions = listOf(CustomAccessibilityAction(addLabel) { actions.addExtra(entry.date); true })
                        }
                    else Modifier.clearAndSetSemantics { contentDescription = spoken },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(EdgeW).fillMaxHeight().padding(vertical = Space.xs).background(lesson?.kind(actions.supplementary)?.edge() ?: c.edgeDayOff))
                val date = "${entry.date.dayOfWeek.short()} ${entry.date.fmtShort()}"
                if (large) Column(Modifier.weight(1f).padding(start = Space.m, top = pad, bottom = pad)) {
                    Text(date, style = MaterialTheme.typography.bodySmall.merge(Figures), color = c.muted)
                    Text(shown, style = textStyle, color = textColor)
                    if (lesson != null) HomeworkLines(lesson, actions)
                } else {
                    DateGutter(entry.date, if (lesson != null) textColor else c.muted)
                    Column(Modifier.weight(1f).padding(vertical = pad)) {
                        Text(shown, style = textStyle, color = textColor)
                        if (lesson != null) HomeworkLines(lesson, actions)
                    }
                }
                if (lesson != null) RoundTick(lesson.done, actions.canTick) { actions.toggle(lesson.date) }
            }
            HorizontalDivider(Modifier.padding(start = Space.l), color = c.line)
        }
        LessonMenu(menu, items) { menu = false }
    }
}
