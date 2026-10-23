package gr.edusuite.pacer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.Coverage
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.coreBooks
import gr.edusuite.pacer.engine.lessonPages
import gr.edusuite.pacer.engine.remainingPages
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import kotlinx.coroutines.delay

/*
 * The custom plan on the group screen (docs/CUSTOM_PLANS_BRIEF.md): the line that creates one, the switch between it
 * and the proposed plan with what the plan offers as a whole, the lesson editor and the drag handle. The rules live in
 * engine/Custom.kt; this file only lays them out.
 */

/**
 * The custom-plan line under today's box (decision 8): two sentences and Create, with no card. Once a plan exists the
 * My plan | Proposed switch takes its place.
 */
@Composable
fun CustomPlanLine(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.custom_line_title), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.custom_line_body), style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted)
        }
        SecondaryButton(stringResource(R.string.custom_line_create), onCreate)
    }
}

/**
 * My plan | Proposed. Showing the plan that isn't in use says so, with a button to use it; nothing is lost either way.
 */
@Composable
fun PlanSourceBar(custom: CustomPlan, showingMine: Boolean, onShow: (Boolean) -> Unit, onUse: () -> Unit, modifier: Modifier = Modifier) {
    val motion = animationsEnabled()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        SegmentedToggle(
            listOf(
                if (custom.active) stringResource(R.string.plan_in_use, stringResource(R.string.custom_mine)) else stringResource(R.string.custom_mine),
                if (!custom.active) stringResource(R.string.plan_in_use, stringResource(R.string.custom_proposed)) else stringResource(R.string.custom_proposed),
            ),
            if (showingMine) 0 else 1, { onShow(it == 0) },
        )
        val preview = showingMine != custom.active
        AnimatedVisibility(preview, enter = if (motion) fadeIn(Motion.content()) + expandVertically(Motion.settle()) else fadeIn(snap()), exit = if (motion) fadeOut(Motion.quick()) + shrinkVertically(Motion.settle()) else fadeOut(snap())) {
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Pacer.colors.surfaceContainer)
                    .padding(start = Space.m, top = Space.xs, bottom = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(if (showingMine) R.string.custom_preview_mine else R.string.custom_preview_proposed),
                    style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted, modifier = Modifier.weight(1f),
                )
                TertiaryButton(stringResource(R.string.custom_use_this), onUse)
            }
        }
    }
}

/**
 * Plans a lesson of the custom plan: the pages for each book (strict, as when recording a lesson) and a note.
 * [pages] and [note] are what it has now; [proposed] what the proposed plan gives that day, shown under each field.
 */
@Composable
fun PlannedLessonSheet(
    title: String,
    report: GroupReport,
    key: String,
    pages: Map<String, List<Int>>,
    note: String,
    proposed: Map<String, List<Int>>?,
    onSave: (Map<String, List<Int>>, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val books = report.plan.books
    val texts = rememberSaveable(report.group.id, key, saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf(*books.map { b -> pages[b.id]?.let(::collapse).orEmpty() }.toTypedArray())
    }
    var noteText by rememberSaveable(report.group.id, key) { mutableStateOf(note) }
    val parsed = books.mapIndexed { i, b -> lessonPages(texts.getOrElse(i) { "" }, b) }
    SheetFrame(
        title = title,
        primary = stringResource(R.string.group_save),
        enabled = parsed.all { it != null },
        onPrimary = { onSave(books.zip(parsed).associate { (b, p) -> b.id to p.orEmpty() }, noteText) },
        onDismiss = onDismiss,
    ) {
        Hint(stringResource(R.string.custom_sheet_hint))
        books.forEachIndexed { i, b ->
            val bad = parsed[i] == null
            OutlinedTextField(
            colors = paletteFieldColors(),
                value = texts[i],
                onValueChange = { texts[i] = it },
                label = { Text(b.name) },
                placeholder = { Text(stringResource(R.string.group_none)) },
                singleLine = true,
                isError = bad,
                supportingText = {
                    when {
                        bad -> Text(stringResource(R.string.group_page_error, b.first, b.last))
                        proposed != null -> Text(stringResource(R.string.custom_proposed_line, collapse(proposed[b.id].orEmpty())))
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge.merge(Figures),
                modifier = Modifier.fillMaxWidth().fieldFeedback(bad),
            )
        }
        OutlinedTextField(
            colors = paletteFieldColors(),
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text(stringResource(R.string.custom_note)) },
            placeholder = { Text(stringResource(R.string.custom_note_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A lesson of the custom plan that can be dragged by its handle. While dragging, the lesson follows the finger, lifts
 * with a shadow, and [onTarget] hears the place it would land (one haptic tick per place); the screen lays the other
 * lessons out around it, so they glide out of the way. Letting go calls [onDrop], which moves it in one undo step.
 * [index] is the lesson's place in the plan; [last] the last place.
 */
@Composable
fun LazyItemScope.DraggableLesson(
    index: Int,
    last: Int,
    dragging: Boolean,
    reveal: Int,
    onStart: () -> Unit,
    onTarget: (Int) -> Unit,
    onDrop: (Boolean) -> Unit,
    content: @Composable (grip: @Composable (label: String) -> Unit) -> Unit,
) {
    val haptic = rememberSegmentHaptic()
    val motion = animationsEnabled()
    var dy by remember { mutableFloatStateOf(0f) }
    var startY by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(1) }
    var origin by remember { mutableIntStateOf(index) }
    var target by remember { mutableIntStateOf(index) }
    val now by rememberUpdatedState(index)
    val lastNow by rememberUpdatedState(last)
    val start by rememberUpdatedState(onStart)
    val move by rememberUpdatedState(onTarget)
    val drop by rememberUpdatedState(onDrop)
    val appear = remember { Animatable(1f) }
    LaunchedEffect(reveal) {
        // A plan just made: its lessons lift in one after another.
        if (reveal > 0 && motion && index < REVEAL_ROWS) {
            appear.snapTo(0f)
            delay(index * REVEAL_STEP_MS)
            appear.animateTo(1f, Motion.content())
        }
    }
    Box(
        Modifier
            .then(if (dragging) Modifier else Modifier.animateItem())
            .zIndex(if (dragging) 1f else 0f)
            .onGloballyPositioned { y = it.positionInRoot().y; height = it.size.height.coerceAtLeast(1) }
            .graphicsLayer {
                alpha = appear.value
                translationY = if (dragging) dy - (y - startY) else (1f - appear.value) * Space.xl.toPx()
                if (dragging) { shadowElevation = 8.dp.toPx(); scaleX = 1.02f; scaleY = 1.02f }
            },
    ) {
        val handle = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { _: Offset -> dy = 0f; startY = y; origin = now; target = now; haptic(); start() },
                onDrag = { change, amount ->
                    change.consume()
                    dy += amount.y
                    val t = (origin + Math.round(dy / height)).coerceIn(0, lastNow)
                    if (t != target) { target = t; haptic(); move(t) }
                },
                onDragEnd = { dy = 0f; drop(true) },
                onDragCancel = { dy = 0f; drop(false) },
            )
        }
        content { label -> DragGrip(label, handle) }
    }
}

/** Six dots, the handle a lesson is dragged by. */
@Composable
fun DragGrip(label: String, modifier: Modifier = Modifier) {
    val ink = Pacer.colors.muted
    Box(modifier.size(Space.xxl + Space.l).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(Space.l)) {
            val r = size.minDimension / 10
            for (col in 0..1) for (row in 0..2) {
                drawCircle(ink, r, Offset(size.width * (0.3f + col * 0.4f), size.height * (0.2f + row * 0.3f)))
            }
        }
    }
}

private const val REVEAL_ROWS = 12
private const val REVEAL_STEP_MS = 30L
