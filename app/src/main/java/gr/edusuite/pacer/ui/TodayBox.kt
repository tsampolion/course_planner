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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
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
    val addHomework = stringResource(R.string.homework_add)
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
                    PagesLine(next, actions, onEdit, compact = false)
                    Column(Modifier.clearAndSetSemantics {}) {
                        next.unitIndex?.let { Text(stringResource(R.string.units_lesson_position, it + 1, next.unitLesson, next.unitLessons), style = MaterialTheme.typography.bodySmall) }
                        HomeworkLines(next, actions, c.onAccent)
                    }
                }
            }
            // Done, with Homework under it in the same shape. Large text: full width under the lesson, at 48dp.
            val buttons = @Composable { m: Modifier, height: Dp ->
                Column(m, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    val doneModifier = Modifier.fillMaxWidth().then(if (actions.canTick) Modifier else Modifier.alpha(0f).clearAndSetSemantics {})
                    HeroButton(stringResource(R.string.today_done), actions.canTick && !completing, complete, doneModifier, height) { DrawingCheck(tickPulse, c.ink, Modifier.size(18.dp)) }
                    HeroButton(stringResource(R.string.today_homework), true, { actions.addHomework(next) }, Modifier.fillMaxWidth().semantics { contentDescription = addHomework }, height) { AppIcon(AppIcons.Homework, Modifier.size(18.dp)) }
                }
            }
            if (large) Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                info(Modifier.fillMaxWidth())
                details()
                buttons(Modifier.fillMaxWidth(), 48.dp)
                footer?.invoke()
            } else Column(Modifier.padding(horizontal = Space.l, vertical = Space.s)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Column(Modifier.lessonNode("$label. $description", next.done, null, items) { menu = true }) {
                            Text(if (next.extraId != null) "$label · ${stringResource(R.string.extra_label)}" else label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${next.date.dayOfWeek.short()} ${next.date.fmtShort()}", style = MaterialTheme.typography.titleMedium.merge(Figures), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        // Two lines at most, and only as wide as the column beside the buttons, so the pages never run under them.
                        PagesLine(next, actions, onEdit, compact = true)
                        Column(Modifier.clearAndSetSemantics {}) {
                            val detail = (next.events.map { it.name } + listOf(next.note).filter { it.isNotBlank() }).joinToString(" · ")
                            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            HomeworkLines(next, actions, c.onAccent)
                        }
                    }
                    buttons(Modifier.width(IntrinsicSize.Max), 40.dp)
                }
                footer?.invoke()
            }
        }
        LessonMenu(menu, items) { menu = false }
    }
}

/**
 * Today's pages one step below the date (14sp). Beside the buttons they wrap to two lines; at large text they take the full width, with
 * supplementary books on their own lines. An edit icon shows when tapping records what was taught; TalkBack reads the full
 * book names and gets a named "Pages taught" action.
 */
@Composable
private fun PagesLine(next: Lesson, actions: LessonActions, onEdit: (() -> Unit)?, compact: Boolean) {
    val shown = !next.blocked || next.cells.any { it.isNotBlank() && it != collapse(emptyList()) }
    val core = if (shown) corePages(next, actions.shortNames, actions.supplementary) else ""
    val supp = if (shown) suppPages(next, actions).map { it.second } else emptyList()
    if (core.isBlank() && supp.isEmpty()) return
    val spoken = ((if (shown) listOf(corePages(next, actions.bookNames, actions.supplementary)) else emptyList()) + supp).filter { it.isNotBlank() }.joinToString(". ")
    val editLabel = stringResource(R.string.today_pages_taught)
    val tap = if (onEdit != null) Modifier.heightIn(min = 48.dp).clickable(onClickLabel = editLabel, role = Role.Button, onClick = onEdit) else Modifier
    Row(
        Modifier.fillMaxWidth().then(tap).clearAndSetSemantics {
            contentDescription = spoken
            if (onEdit != null) { role = Role.Button; onClick(editLabel) { onEdit(); true } }
        },
        horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically,
    ) {
        val style = MaterialTheme.typography.bodyMedium.merge(Figures)
        if (compact) Text((listOf(core) + supp).filter { it.isNotBlank() }.joinToString(" · "), style = style, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        else Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            if (core.isNotBlank()) Text(core, style = style)
            supp.forEach { Text(stringResource(R.string.ledger_supp, it), style = MaterialTheme.typography.bodySmall.merge(Figures)) }
        }
        if (onEdit != null) AppIcon(AppIcons.Edit, Modifier.size(18.dp).alpha(.8f))
    }
}

/** A button of the lesson box: a surface pill with ink content, drawn at [height] (the box does not grow for it). */
@Composable
private fun HeroButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier, height: Dp, icon: @Composable () -> Unit) {
    val c = Pacer.colors
    Row(
        modifier.height(height).alpha(if (enabled) 1f else .5f).clip(CircleShape).background(c.surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = Space.m),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(Space.xs))
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.ink, maxLines = 1)
    }
}
