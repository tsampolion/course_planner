package gr.edusuite.pacer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import gr.edusuite.pacer.ui.theme.PacerMenuShape

/** Native popup positioning retains its anchor origin; fixed corners avoid clipping edge rows. */
@Composable
internal fun PacerDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = animationsEnabled()
    val reveal by animateFloatAsState(if (expanded) 1f else 0f, if (motion) Motion.work(MotionSpec.Step.MENU) else snap())
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.graphicsLayer {
            scaleX = .8f + .2f * if (motion) reveal else 1f
            scaleY = scaleX
            alpha = if (motion) reveal else 1f
        },
        shape = PacerMenuShape,
        content = content,
    )
}
