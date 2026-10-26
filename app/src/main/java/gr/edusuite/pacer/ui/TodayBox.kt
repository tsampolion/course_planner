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
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.closedDates
import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.eventsOn
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.short
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

/**
 * Today's lesson, first on the Plan tab (decision 8): its date and pages, with Done; tapping the pages records what was taught. A long-press
 * opens the lesson's menu, and TalkBack reads it with the books' full names. [next] null means none is left.
 */
@Composable
internal fun TodayBox(next: Lesson?, today: LocalDate, actions: LessonActions, onDone: () -> Unit, onPagesTaught: () -> Unit, modifier: Modifier = Modifier, footer: @Composable (() -> Unit)? = null) {
    val c = Pacer.colors
    val motion = animationsEnabled()
    var tickPulse by remember { mutableIntStateOf(0) }
    var completing by remember { mutableStateOf(false) }
    LaunchedEffect(tickPulse, motion) {
        if (tickPulse > 0 && motion) kotlinx.coroutines.delay(MotionSpec.duration(MotionSpec.Step.TICK, 1f).toLong())
        completing = false
    }
    val complete = { if (!completing) { completing = true; tickPulse++; onDone() } }
    if (next == null) {
        Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = c.surfaceContainer, contentColor = c.ink) {
            Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Text(stringResource(R.string.group_no_term_lessons), style = MaterialTheme.typography.titleMedium)
                footer?.invoke()
            }
        }
        return
    }
    var menu by rememberSaveable(next.date.toString()) { mutableStateOf(false) }
    val items = lessonMenu(next, actions)
    val description = spokenLesson(next, actions)
    val label = stringResource(if (next.date == today) R.string.today_label else R.string.today_next_label, next.n)
    val large = rememberLargeText()
    // Tapping the pages records what was taught and ticks the lesson (the lesson menu keeps the same action for TalkBack).
    // The lesson box is a fill, so the accent is a background here, never text.
    val fill = c.accent
    val hero = heroActions(next, actions.canTick)
    val onEdit: (() -> Unit)? = if (hero.canEditPages && !completing) onPagesTaught else null
    Box(modifier) {
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = fill, contentColor = c.onAccent) {
            val info = @Composable { m: Modifier ->
                Column(
                    m.lessonNode("$label. $description", next.done, null, items) { menu = true },
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    if (next.extraId != null) Text(stringResource(R.string.extra_label), style = MaterialTheme.typography.labelMedium)
                    Text("${next.date.dayOfWeek.short()} ${next.date.fmtShort()}", style = MaterialTheme.typography.headlineSmall.merge(Figures))
                    if (next.events.isNotEmpty()) Text(next.events.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodyMedium)
                    if (next.note.isNotEmpty()) Text(next.note, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                }
            }
            // The pages and the homework line sit outside the lesson node, which clears its children's semantics.
            val details = @Composable {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    PagesHeadline(next, actions, onEdit)
                    Column(Modifier.clearAndSetSemantics {}) {
                        next.unitIndex?.let { Text(stringResource(R.string.units_lesson_position, it + 1, next.unitLesson, next.unitLessons), style = MaterialTheme.typography.bodySmall) }
                        HomeworkLines(next, actions, c.onAccent)
                    }
                    HomeworkAction(next, actions)
                }
            }
            // Large text: the buttons go full width under the lesson instead of beside it.
            val buttons = @Composable { m: Modifier, end: Alignment.Horizontal ->
                Column(m, horizontalAlignment = end, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Button(
                        onClick = complete,
                        enabled = actions.canTick && !completing,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = c.surface, contentColor = c.ink),
                        modifier = Modifier.heightIn(min = 48.dp).then(if (large) Modifier.fillMaxWidth() else Modifier),
                    ) {
                        DrawingCheck(tickPulse, c.ink, Modifier.size(18.dp))
                        Spacer(Modifier.width(Space.xs))
                        Text(stringResource(R.string.today_done), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (large) Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                info(Modifier.fillMaxWidth())
                details()
                if (actions.canTick) buttons(Modifier.fillMaxWidth(), Alignment.CenterHorizontally)
                else Box(Modifier.fillMaxWidth().alpha(0f).clearAndSetSemantics {}) { buttons(Modifier.fillMaxWidth(), Alignment.CenterHorizontally) }
                footer?.invoke()
            } else Column(Modifier.padding(horizontal = Space.l, vertical = Space.s)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).lessonNode("$label. $description", next.done, null, items) { menu = true }) {
                        Text(if (next.extraId != null) "$label · ${stringResource(R.string.extra_label)}" else label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${next.date.dayOfWeek.short()} ${next.date.fmtShort()}", style = MaterialTheme.typography.titleMedium.merge(Figures), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(enabled = actions.canTick && !completing, onClick = complete, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = c.surface, contentColor = c.ink), modifier = Modifier.heightIn(min = 48.dp).then(if (actions.canTick) Modifier else Modifier.alpha(0f).clearAndSetSemantics {})) {
                        DrawingCheck(tickPulse, c.ink, Modifier.size(18.dp))
                        Spacer(Modifier.width(Space.xs))
                        Text(stringResource(R.string.today_done), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    PagesHeadline(next, actions, onEdit)
                    Column(Modifier.clearAndSetSemantics {}) {
                        val detail = (next.events.map { it.name } + listOf(next.note).filter { it.isNotBlank() }).joinToString(" · ")
                        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
                        HomeworkLines(next, actions, c.onAccent)
                    }
                    HomeworkAction(next, actions)
                }
                footer?.invoke()
            }
        }
        LessonMenu(menu, items) { menu = false }
    }
}

/**
 * Today's pages as the hero's headline: one book per line in large tabular figures, wrapping instead of cutting off, with an edit
 * icon when tapping records what was taught. TalkBack reads the full book names and gets a named "Pages taught" action.
 */
@Composable
private fun PagesHeadline(next: Lesson, actions: LessonActions, onEdit: (() -> Unit)?) {
    val shown = !next.blocked || next.cells.any { it.isNotBlank() && it != collapse(emptyList()) }
    val core = if (shown) corePageParts(next, actions.shortNames, actions.supplementary).map { it.second } else emptyList()
    val supp = if (shown) suppPages(next, actions).map { it.second } else emptyList()
    if (core.isEmpty() && supp.isEmpty()) return
    val spoken = ((if (shown) listOf(corePages(next, actions.bookNames, actions.supplementary)) else emptyList()) + supp).filter { it.isNotBlank() }.joinToString(". ")
    val editLabel = stringResource(R.string.today_pages_taught)
    val style = (if (core.size >= 4) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall).merge(Figures)
    val tap = if (onEdit != null) Modifier.heightIn(min = 48.dp).clickable(onClickLabel = editLabel, role = Role.Button, onClick = onEdit) else Modifier
    Row(
        Modifier.fillMaxWidth().then(tap).clearAndSetSemantics {
            contentDescription = spoken
            if (onEdit != null) { role = Role.Button; onClick(editLabel) { onEdit(); true } }
        },
        horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            core.forEach { Text(it, style = style) }
            supp.forEach { Text(stringResource(R.string.ledger_supp, it), style = MaterialTheme.typography.bodySmall.merge(Figures)) }
        }
        if (onEdit != null) AppIcon(AppIcons.Edit, Modifier.size(20.dp).padding(top = Space.xs).alpha(.8f))
    }
}

/** D3: adding homework for this lesson without the long-press menu. */
@Composable
private fun HomeworkAction(next: Lesson, actions: LessonActions) {
    val c = Pacer.colors
    val label = stringResource(R.string.homework_add)
    OutlinedButton(
        onClick = { actions.addHomework(next) }, shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = c.onAccent),
        border = BorderStroke(1.dp, c.onAccent),
        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = label },
    ) {
        AppIcon(AppIcons.Homework, Modifier.size(18.dp))
        Spacer(Modifier.width(Space.xs))
        Text(stringResource(R.string.today_homework), style = MaterialTheme.typography.labelLarge)
    }
}
