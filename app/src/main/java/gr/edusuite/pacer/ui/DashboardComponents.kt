package gr.edusuite.pacer.ui

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
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.ui.theme.Mono
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.launch

/** A responsive stat block; labels and units remain sans while figures use mono. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = Pacer.colors.tileSky, unit: String? = null) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = color, contentColor = Pacer.colors.onTile) {
        Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
            Text(value, style = MaterialTheme.typography.titleLarge, fontFamily = Mono)
            unit?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun MoreOptions(title: String = "More options", modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }) {
            Text(title)
            Text(if (expanded) " ▴" else " ▾")
        }
        if (expanded) Column(verticalArrangement = Arrangement.spacedBy(Space.m), content = content)
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
            Text(date.fmtShort(), style = MaterialTheme.typography.bodyMedium, fontFamily = Mono)
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
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, enabled = pager.currentPage > 0,
            modifier = Modifier.size(Space.xl * 2).semantics { contentDescription = "Previous week" }) { Text("‹") }
        val weekStart = first.plusWeeks(pager.currentPage.toLong())
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Eyebrow("${weekStart.fmtShort()} – ${weekStart.plusDays(6).fmtShort()}") }
        TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, enabled = pager.currentPage < weeks - 1,
            modifier = Modifier.size(Space.xl * 2).semantics { contentDescription = "Next week" }) { Text("›") }
      }
      BoxWithConstraints(Modifier.fillMaxWidth()) {
      val dayWidth = ((maxWidth - Space.xs * 6) / 7).coerceAtLeast(if (fontScale >= 2f) Space.xxl * 2 else Space.xl * 2)
      HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), key = { first.plusWeeks(it.toLong()).toString() }) { week ->
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            (0L..6L).forEach { offset ->
                val date = first.plusWeeks(week.toLong()).plusDays(offset)
                val isSelected = date == selected
                val valid = !date.isBefore(start) && !date.isAfter(end)
                val ink = if (isSelected) Pacer.colors.paper else Pacer.colors.ink
                Surface(
                    modifier = Modifier.width(dayWidth).heightIn(min = Space.xxl * 2).semantics(mergeDescendants = true) {
                        this.selected = isSelected
                        contentDescription = date.toString() + (if (date == today) ", Today" else "") + (if (date in lessonDates) ", Lesson" else "")
                    }.clickable(enabled = valid, role = Role.Tab) { onSelect(date) },
                    shape = CircleShape, color = if (isSelected) Pacer.colors.ink else Pacer.colors.surfaceContainer,
                    contentColor = ink,
                ) {
                    Column(Modifier.padding(vertical = Space.s), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(Space.xs).background(if (date == today) ink else Color.Transparent, CircleShape))
                        Text(date.dayOfWeek.short(), style = MaterialTheme.typography.labelSmall)
                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall, fontFamily = Mono)
                        Box(Modifier.size(Space.xs).background(if (date in lessonDates) ink else Color.Transparent, CircleShape))
                    }
                }
            }
        }
      }
      }
    }
}
