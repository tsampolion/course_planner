package gr.edusuite.pacer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import gr.edusuite.pacer.R
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A sheet's filled action (Save, Add, Apply): it runs [onClick] and closes the sheet, and stays off until [enabled]. */
class SheetAction(val label: String, val enabled: Boolean, val onClick: () -> Unit)

/** Whether any of [values] differs from what they were when the sheet opened (survives rotation). */
@Composable
fun rememberDraftChanged(vararg values: Any?): Boolean {
    val now = draftSignature(values.toList())
    val opened = rememberSaveable { now }
    return now != opened
}

/**
 * The one sheet frame (§6 Bottom sheet; streamlining S4). A title, ✕ to close, the content, and a filled [action] only
 * when there is something to save. Pickers and notices leave [action] off and call the content's `close`. Back, a scrim
 * tap, a swipe down and ✕ all close it, and when [dirty] each asks Keep editing / Discard first. It is a Dialog drawn
 * as a sheet so its motion uses [MotionSpec] timings and snaps when animations are off.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PacerSheet(
    title: String,
    onDismiss: () -> Unit,
    action: SheetAction? = null,
    dirty: Boolean = false,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val motion = animationsEnabled()
    val sheet = remember { Animatable(if (motion) 0f else 1f) }
    val scrim = remember { Animatable(if (motion) 0f else 1f) }
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    val isDirty by rememberUpdatedState(dirty)
    var closing by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var asking by rememberSaveable { mutableStateOf(false) }
    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            if (motion) coroutineScope {
                launch { scrim.animateTo(0f, Motion.work(MotionSpec.Step.SCRIM)) }
                sheet.animateTo(0f, Motion.work(MotionSpec.Step.SHEET))
            }
            dismiss()
        }
    }
    fun requestClose(via: SheetClose) {
        if (closing) return
        when (sheetCloseDecision(isDirty, via)) {
            SheetCloseDecision.CLOSE -> close()
            SheetCloseDecision.ASK -> {
                asking = true
                scope.launch { if (motion) drag.animateTo(0f, Motion.settle()) else drag.snapTo(0f) }
            }
        }
    }
    LaunchedEffect(motion) {
        if (motion && !closing) coroutineScope {
            launch { scrim.animateTo(1f, Motion.work(MotionSpec.Step.SCRIM)) }
            sheet.animateTo(1f, Motion.work(MotionSpec.Step.SHEET))
        } else if (!motion) {
            if (closing) dismiss() else { sheet.snapTo(1f); scrim.snapTo(1f) }
        }
    }
    val closeLabel = stringResource(R.string.recovery_close)
    val swipeClose = with(LocalDensity.current) { 96.dp.toPx() }
    val dragState = rememberDraggableState { delta -> scope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) } }
    Dialog(onDismissRequest = { requestClose(SheetClose.BACK) }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val scrimColor = BottomSheetDefaults.ScrimColor
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(scrimColor.copy(alpha = scrimColor.alpha * if (motion) scrim.value else 1f))
                .clickable(role = Role.Button, onClickLabel = closeLabel, onClick = { requestClose(SheetClose.SCRIM) })
                .semantics { contentDescription = closeLabel })
            val shape = MaterialTheme.shapes.extraLarge.let {
                if (it is RoundedCornerShape) it.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)) else it
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = BottomSheetDefaults.SheetMaxWidth).fillMaxWidth()
                    .statusBarsPadding().navigationBarsPadding().imePadding()
                    .heightIn(max = maxHeight)
                    .graphicsLayer { translationY = size.height * (1f - if (motion) sheet.value else 1f) + drag.value }
                    .pointerInput(Unit) { detectTapGestures(onTap = {}) }.semantics { paneTitle = title },
                shape = shape, color = Pacer.colors.surface, contentColor = Pacer.colors.ink,
            ) {
                Column(Modifier.padding(start = Space.l, end = Space.l, bottom = Space.xl)) {
                    // The handle and title row are what a swipe down drags; the fields below keep their own scrolling.
                    Column(Modifier.draggable(dragState, Orientation.Vertical, onDragStopped = { velocity ->
                        if (drag.value > swipeClose || velocity > 1500f) requestClose(SheetClose.SWIPE)
                        else if (motion) drag.animateTo(0f, Motion.settle()) else drag.snapTo(0f)
                    })) {
                        BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
                            PillIconButton(closeLabel, { requestClose(SheetClose.CLOSE_BUTTON) }) { AppIcon(AppIcons.Close) }
                        }
                    }
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(top = Space.m),
                        verticalArrangement = Arrangement.spacedBy(if (action == null) Space.s else Space.m)) {
                        content { requestClose(SheetClose.ACTION) }
                        if (action != null) PrimaryButton(action.label, {
                            if (!closing && !completed) {
                                action.onClick()
                                if (motion) {
                                    completed = true
                                    scope.launch { delay(600); close() }
                                } else close()
                            }
                        }, Modifier.fillMaxWidth(), enabled = action.enabled && !closing && !completed, completed = completed)
                    }
                }
            }
        }
        if (asking) AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(stringResource(R.string.sheet_discard_title)) },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = Pacer.colors.surface,
            tonalElevation = 0.dp,
            confirmButton = { PaletteTextButton(onClick = { asking = false }) { Text(stringResource(R.string.sheet_keep_editing)) } },
            dismissButton = { PaletteTextButton(onClick = { asking = false; close() }) { Text(stringResource(R.string.sheet_discard)) } },
        )
    }
}
