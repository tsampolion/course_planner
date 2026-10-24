package gr.edusuite.pacer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import kotlinx.coroutines.launch

/**
 * A modal assignment sheet with explicit entrance/scrim timings. Material 3 1.3.2 does not expose its sheet motion;
 * Dialog supplies modal focus and system Back, while the existing Material shape, fields and controls remain native.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun WorkSheetFrame(title: String, primary: String, enabled: Boolean, onPrimary: () -> Unit,
    onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val motion = animationsEnabled()
    val sheet = remember { Animatable(if (motion) 0f else 1f) }
    val scrim = remember { Animatable(if (motion) 0f else 1f) }
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    var closing by remember { mutableStateOf(false) }
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
    LaunchedEffect(motion) {
        if (motion && !closing) coroutineScope {
            launch { scrim.animateTo(1f, Motion.work(MotionSpec.Step.SCRIM)) }
            sheet.animateTo(1f, Motion.work(MotionSpec.Step.SHEET))
        } else if (!motion) {
            if (closing) dismiss() else { sheet.snapTo(1f); scrim.snapTo(1f) }
        }
    }
    val closeLabel = stringResource(R.string.recovery_close)
    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val scrimColor = BottomSheetDefaults.ScrimColor
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(scrimColor.copy(alpha = scrimColor.alpha * if (motion) scrim.value else 1f))
                .clickable(role = Role.Button, onClickLabel = closeLabel, onClick = ::close).semantics { contentDescription = closeLabel })
            val shape = MaterialTheme.shapes.extraLarge.let {
                if (it is RoundedCornerShape) it.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)) else it
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = BottomSheetDefaults.SheetMaxWidth).fillMaxWidth()
                    .statusBarsPadding().navigationBarsPadding().imePadding()
                    .heightIn(max = maxHeight).graphicsLayer { translationY = size.height * (1f - if (motion) sheet.value else 1f) }
                    .pointerInput(Unit) { detectTapGestures(onTap = {}) }.semantics { paneTitle = title },
                shape = shape, color = Pacer.colors.surface, contentColor = Pacer.colors.ink,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(start = Space.l, end = Space.l, bottom = Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    Row(Modifier.fillMaxWidth().padding(top = Space.s), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                        PillIconButton(closeLabel, ::close) { AppIcon(AppIcons.Close) }
                    }
                    content()
                    PrimaryButton(primary, {
                        if (!closing) { onPrimary(); close() }
                    }, Modifier.fillMaxWidth(), enabled = enabled && !closing)
                }
            }
        }
    }
}
