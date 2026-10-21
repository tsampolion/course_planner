package gr.edusuite.pacer.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.snap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.ui.theme.Motion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.launch

/** A responsive stat block; labels and units stay plain while figures use Figures. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = Pacer.colors.tileSky, unit: String? = null) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = color, contentColor = Pacer.colors.onTile) {
        Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
            AnimatedFigure(value)
            unit?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun MoreOptions(title: String = stringResource(R.string.component_more_options), modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val motion = animationsEnabled()
    val haptic = rememberSegmentHaptic()
    val description = stringResource(if (expanded) R.string.component_expanded else R.string.component_collapsed)
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, if (motion) Motion.content() else snap(), label = "expand")
    Column(modifier.fillMaxWidth().animateContentSize(if (motion) Motion.settle() else snap()), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        TextButton(onClick = { haptic(); expanded = !expanded }, modifier = Modifier.heightIn(min = 48.dp).semantics { stateDescription = description }) {
            Text(title)
            AppIcon(AppIcons.ExpandMore, Modifier.rotate(rotation))
        }
        if (expanded) Column(verticalArrangement = Arrangement.spacedBy(Space.m), content = content)
    }
}

/**
 * A row of mutually exclusive choices (docs/PACER_DESIGN.md D10): a filled 48dp switch whose ink selected side slides
 * from the old choice to the new one and never jumps. Each choice is a full-height 48dp tab for accessibility, and
 * switching ticks like a segment.
 */
@Composable
fun SegmentedToggle(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val motion = animationsEnabled()
    val haptic = rememberSegmentHaptic()
    var height by remember { mutableIntStateOf(0) }
    BoxWithConstraints(modifier.fillMaxWidth().background(Pacer.colors.surfaceContainer, CircleShape)) {
        val width = maxWidth / options.size.coerceAtLeast(1)
        val offset by animateDpAsState(width * selected.coerceIn(0, options.lastIndex.coerceAtLeast(0)), if (motion) Motion.settle() else snap(), label = "segment")
        val measured = with(LocalDensity.current) { height.toDp() }
        // The ink side sits inset in the track; the tabs under it fill the whole 48dp height.
        Box(Modifier.offset { IntOffset(offset.roundToPx(), 0) }.width(width).height(measured).padding(Space.xs).background(Pacer.colors.ink, CircleShape))
        Row(Modifier.fillMaxWidth().onSizeChanged { height = it.height }.selectableGroup()) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                val ink by animateColorAsState(if (on) Pacer.colors.paper else Pacer.colors.ink, if (motion) Motion.quick() else snap(), label = "segment ink")
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(CircleShape)
                        .selectable(selected = on, role = Role.Tab) { if (!on) { haptic(); onSelect(i) } },
                    contentAlignment = Alignment.Center,
                ) { Text(label, style = MaterialTheme.typography.labelLarge, color = ink, modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s)) }
            }
        }
    }
}

/** Grids become a single column below 360dp or at 200% font scale. */
@Composable
fun <T> TileGrid(items: List<T>, modifier: Modifier = Modifier, content: @Composable (T, Modifier) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 360.dp - Space.l * 2 || fontScale >= 2f) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            items.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    row.forEach { item -> Box(Modifier.weight(1f)) { content(item, Modifier.fillMaxWidth()) } }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DateBadge(date: LocalDate, modifier: Modifier = Modifier) {
    Surface(modifier, color = Pacer.colors.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(Space.m), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.dayOfWeek.short(), style = MaterialTheme.typography.labelSmall)
            Text(date.fmtShort(), style = MaterialTheme.typography.bodyMedium.merge(Figures))
        }
    }
}

/** Seven accessible day pills per term week, paged horizontally. Dots distinguish today and lessons. */
@Composable
fun DayStrip(start: LocalDate, end: LocalDate, selected: LocalDate, lessonDates: Set<LocalDate>, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier, today: LocalDate = LocalDate.now()) {
    val first = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weeks = (ChronoUnit.WEEKS.between(first, end.coerceAtLeast(start)) + 1).toInt().coerceAtLeast(1)
    val initial = if (today in start..end) ChronoUnit.WEEKS.between(first, today).toInt().coerceIn(0, weeks - 1) else 0
    val pager = rememberPagerState(initialPage = initial) { weeks }
    val scope = rememberCoroutineScope()
    val fontScale = LocalDensity.current.fontScale
    val motion = animationsEnabled()
    val haptic = rememberSegmentHaptic()
    val previousLabel = stringResource(R.string.component_previous_week)
    val nextLabel = stringResource(R.string.component_next_week)
    val todayLabel = stringResource(R.string.component_today)
    val lessonLabel = stringResource(R.string.component_lesson)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { haptic(); scope.launch { if (motion) pager.animateScrollToPage(pager.currentPage - 1) else pager.scrollToPage(pager.currentPage - 1) } }, enabled = pager.currentPage > 0,
            modifier = Modifier.size(Space.xl * 2).semantics { contentDescription = previousLabel }) { AppIcon(AppIcons.Back) }
        val weekStart = first.plusWeeks(pager.currentPage.toLong())
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Eyebrow(stringResource(R.string.component_week_range, weekStart.fmtShort(), weekStart.plusDays(6).fmtShort())) }
        TextButton(onClick = { haptic(); scope.launch { if (motion) pager.animateScrollToPage(pager.currentPage + 1) else pager.scrollToPage(pager.currentPage + 1) } }, enabled = pager.currentPage < weeks - 1,
            modifier = Modifier.size(Space.xl * 2).semantics { contentDescription = nextLabel }) { AppIcon(AppIcons.Forward) }
      }
      BoxWithConstraints(Modifier.fillMaxWidth()) {
      val dayWidth = ((maxWidth - Space.xs * 6) / 7).coerceAtLeast(if (fontScale >= 2f) Space.xxl * 2 else Space.xl * 2)
      HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), key = { first.plusWeeks(it.toLong()).toString() }) { week ->
        val weekStart = first.plusWeeks(week.toLong())
        val index = ChronoUnit.DAYS.between(weekStart, selected).toInt()
        val indicator by animateDpAsState((dayWidth + Space.xs) * index.coerceIn(0, 6), if (motion) Motion.settle() else snap(), label = "selected day")
        var stripHeight by remember { mutableStateOf(0) }
        val measuredHeight = with(LocalDensity.current) { stripHeight.toDp() }.coerceAtLeast(Space.xxl * 2)
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                repeat(7) { Box(Modifier.width(dayWidth).height(measuredHeight).background(Pacer.colors.surfaceContainer, CircleShape)) }
            }
            if (index in 0..6) Box(Modifier.offset(x = indicator).width(dayWidth).height(measuredHeight).background(Pacer.colors.ink, CircleShape))
            Row(Modifier.onSizeChanged { stripHeight = it.height }, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                (0L..6L).forEach { offset ->
                    val date = weekStart.plusDays(offset)
                    val isSelected = date == selected
                    val valid = !date.isBefore(start) && !date.isAfter(end)
                    val ink = if (isSelected) Pacer.colors.paper else Pacer.colors.ink
                    val description = stringResource(R.string.component_day_description, date.toString(), if (date == today) todayLabel else "", if (date in lessonDates) lessonLabel else "")
                    Box(
                        modifier = Modifier.width(dayWidth).heightIn(min = Space.xxl * 2).semantics(mergeDescendants = true) {
                            this.selected = isSelected
                            contentDescription = description
                        }.clickable(enabled = valid, role = Role.Tab) { haptic(); onSelect(date) },
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides ink) {
                            Column(Modifier.padding(vertical = Space.s), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(Space.xs).background(if (date == today) ink else Color.Transparent, CircleShape))
                                Text(date.dayOfWeek.short(), style = MaterialTheme.typography.labelSmall)
                                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall.merge(Figures))
                                Box(Modifier.size(Space.xs).background(if (date in lessonDates) ink else Color.Transparent, CircleShape))
                            }
                        }
                    }
                }
            }
        }
      }
      }
    }
}
