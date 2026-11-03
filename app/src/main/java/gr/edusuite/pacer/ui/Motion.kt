package gr.edusuite.pacer.ui

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.MotionSpec
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.Space

private val LocalMotionEnabled = compositionLocalOf { true }

/** One platform observer serves every control in the themed app. */
@Composable
fun PacerMotion(content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var enabled by remember(resolver) { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { enabled = read() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    CompositionLocalProvider(LocalMotionEnabled provides enabled, content = content)
}

/** Reads the live app setting; previews outside the provider retain motion. */
@Composable
fun animationsEnabled(): Boolean = LocalMotionEnabled.current

/** Values roll upward as they increase and downward as they decrease. */
@Composable
fun AnimatedFigure(value: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.titleLarge, color: Color = LocalContentColor.current) {
    val enabled = animationsEnabled()
    AnimatedContent(value, modifier, transitionSpec = {
        val increasing = (targetState.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0) >=
            (initialState.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0)
        if (enabled) (slideInVertically(Motion.content()) { if (increasing) it else -it } + fadeIn(Motion.quick())) togetherWith
            (slideOutVertically(Motion.content()) { if (increasing) -it else it } + fadeOut(Motion.quick()))
        else EnterTransition.None togetherWith ExitTransition.None
    }, label = "figure") { Text(it, style = style.merge(Figures), color = color) }
}

/** Switching between sibling views: a shared-axis slide plus fade toward the side chosen (§5). */
fun sideways(forward: Boolean, enabled: Boolean): ContentTransform {
    if (!enabled) return EnterTransition.None togetherWith ExitTransition.None
    val direction = if (forward) 1 else -1
    return (slideInHorizontally(Motion.content()) { it / 12 * direction } + fadeIn(Motion.content())) togetherWith
        (slideOutHorizontally(Motion.content()) { -it / 12 * direction } + fadeOut(Motion.quick()))
}

/** The same interaction source drives ripple and the 0.96 press response. */
fun Modifier.pressFeedback(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val enabled = animationsEnabled()
    val scale by animateFloatAsState(if (pressed && enabled) .96f else 1f, if (enabled) Motion.work(MotionSpec.Step.PRESS) else snap(), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** One three-cycle, 4dp shake for each transition into an invalid state. */
fun Modifier.fieldFeedback(isError: Boolean): Modifier = composed {
    val enabled = animationsEnabled()
    val haptic = LocalHapticFeedback.current
    val shift = remember { Animatable(0f) }
    val distance = with(LocalDensity.current) { Space.xs.toPx() }
    var previousError by remember { mutableStateOf(isError) }
    LaunchedEffect(enabled) { if (!enabled) shift.snapTo(0f) }
    LaunchedEffect(isError) {
        shift.snapTo(0f)
        val newlyInvalid = isError && !previousError
        previousError = isError
        if (newlyInvalid) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            if (enabled) shift.animateTo(0f, keyframes {
                durationMillis = 300
                distance at 25; -distance at 75; distance at 125; -distance at 175; distance at 225; -distance at 275; 0f at 300
            })
        }
    }
    graphicsLayer { translationX = shift.value }
}

@Composable
fun rememberToggleHaptic(): (Boolean) -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { on -> haptic.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff) } }
}

/** The confirm tick when a tool turns on. */
@Composable
fun rememberConfirmHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.Confirm) } }
}

@Composable
fun rememberSegmentHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.SegmentTick) } }
}

/** A visible row rises once per Work opening; revisiting a composed row does not replay it. */
fun Modifier.workReveal(id: String, index: Int, seen: MutableSet<String>): Modifier = composed {
    val enabled = animationsEnabled()
    val progress = remember(id) { Animatable(if (!enabled || id in seen) 1f else 0f) }
    LaunchedEffect(id, enabled) {
        if (!enabled || id in seen) progress.snapTo(1f) else {
            kotlinx.coroutines.delay(MotionSpec.entranceDelay(index, false, 1f))
            progress.animateTo(1f, Motion.work(MotionSpec.Step.ROW))
        }
        seen.add(id)
    }
    graphicsLayer { alpha = if (enabled) progress.value else 1f; translationY = if (enabled) MotionSpec.ROW_RISE_PX * (1f - progress.value) else 0f }
}

/** A lazy list item that slides to its new place, or snaps there when the system's animations are off. */
@Composable
fun androidx.compose.foundation.lazy.LazyItemScope.itemMotion(): Modifier =
    if (animationsEnabled()) Modifier.animateItem() else Modifier

/** The hint whose Turn on was just tapped on this screen ([arrives]); the controls it brings read it to arrive with a wash. */
val LocalArrivedHint = androidx.compose.runtime.compositionLocalOf<String?> { null }

/**
 * A control a tool just brought arrives with a wash of the second primary around it that fades over [MotionSpec.Step.REVISION]
 * (800ms, `FEATURE_MODULES_BRIEF.md` Motion); nothing when the system's animations are off. It draws outside the control, so
 * nothing moves.
 */
fun Modifier.arrivalWash(active: Boolean): Modifier = composed {
    val enabled = animationsEnabled()
    val c = gr.edusuite.pacer.ui.theme.Pacer.colors
    val wash = remember { Animatable(0f) }
    LaunchedEffect(active, enabled) {
        if (active && enabled) { wash.snapTo(1f); wash.animateTo(0f, Motion.work(MotionSpec.Step.REVISION)) } else wash.snapTo(0f)
    }
    val spread = with(LocalDensity.current) { Space.xs.toPx() }
    drawBehind {
        if (wash.value > 0f) drawRoundRect(
            c.secondSoft.copy(alpha = wash.value),
            topLeft = androidx.compose.ui.geometry.Offset(-spread, -spread),
            size = androidx.compose.ui.geometry.Size(size.width + 2 * spread, size.height + 2 * spread),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2 + spread),
        )
    }
}
