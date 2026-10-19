package gr.edusuite.pacer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.Space

/**
 * Home's tile grid (§6 "Tile grid"): [columns] equal columns [Space.s] apart, each tile dropped into the shortest column
 * ([masonryPlacement]). Every tile stays composed, unlike a lazy grid, so the whole of Home scrolls as one page.
 */
@Composable
fun HomeGrid(columns: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = Space.s.roundToPx()
        val width = constraints.maxWidth
        val columnWidth = ((width - gap * (columns - 1)) / columns).coerceAtLeast(0)
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(columnWidth)) }
        val spots = masonryPlacement(placeables.map { it.height }, columns, gap)
        val height = spots.zip(placeables).maxOfOrNull { (spot, p) -> spot.second + p.height } ?: 0
        layout(width, height) {
            placeables.zip(spots).forEach { (p, spot) -> p.place(spot.first * (columnWidth + gap), spot.second) }
        }
    }
}

/**
 * Glides a tile to its new place when the grid reflows (§5: `Motion.settle`), for example when a rotation changes the
 * column count. The first placement doesn't move, and with animations off the tile jumps straight there.
 */
fun Modifier.animatePlacement(): Modifier = composed {
    val enabled by rememberUpdatedState(animationsEnabled())
    // Where the grid puts the tile, and where it is drawn on its way there.
    var target by remember { mutableStateOf<IntOffset?>(null) }
    var shown by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    LaunchedEffect(target) {
        val to = target ?: return@LaunchedEffect
        val current = shown
        when {
            current == null -> shown = Animatable(to, IntOffset.VectorConverter)
            enabled -> current.animateTo(to, Motion.settle())
            else -> current.snapTo(to)
        }
    }
    onPlaced { target = it.positionInParent().round() }
        .layout { measurable, constraints ->
            val p = measurable.measure(constraints)
            layout(p.width, p.height) {
                val to = target
                val from = shown?.value
                p.place(if (to == null || from == null) IntOffset.Zero else from - to)
            }
        }
}
