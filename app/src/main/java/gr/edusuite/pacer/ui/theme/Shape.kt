package gr.edusuite.pacer.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * EduSuite corners (§4): pills for chips and buttons (`extraSmall`), 12dp text fields, 16dp list groups and panels,
 * 24dp tiles, 28dp dialogs and sheets. The logo tile's corners are a quarter of its size (`Logo`).
 */
internal val PacerShapes = Shapes(
    extraSmall = RoundedCornerShape(50),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Fixed corners keep the first/last menu rows inside the popup at every height. */
internal val PacerMenuShape = PacerShapes.small

/** The 4dp spacing grid (§4). */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Shared motion specs (§5); work uses explicit timings with a finished reduced-motion state. */
object Motion {
    private val WorkEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    fun <T> work(step: MotionSpec.Step, enabled: Boolean = true) =
        tween<T>(MotionSpec.duration(step, if (enabled) 1f else 0f), easing = WorkEase)

    fun <T> bouncy() = spring<T>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
    fun <T> settle() = spring<T>(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
    fun <T> quick() = tween<T>(150, easing = FastOutSlowInEasing)
    fun <T> content() = tween<T>(250, easing = FastOutSlowInEasing)
    fun <T> screen() = tween<T>(300, easing = FastOutSlowInEasing)
}
