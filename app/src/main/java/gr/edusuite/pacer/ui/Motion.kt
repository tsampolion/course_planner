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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import gr.edusuite.pacer.ui.theme.Mono
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
    }, label = "figure") { Text(it, style = style, fontFamily = Mono, color = color) }
}

/** The same interaction source drives ripple and the 0.96 press response. */
fun Modifier.pressFeedback(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val enabled = animationsEnabled()
    val scale by animateFloatAsState(if (pressed && enabled) .96f else 1f, if (enabled) Motion.bouncy() else snap(), label = "press")
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

@Composable
fun rememberSegmentHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.SegmentTick) } }
}
