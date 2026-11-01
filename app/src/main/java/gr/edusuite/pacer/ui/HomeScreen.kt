package gr.edusuite.pacer.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.suggestYear
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "9 Sep 2026 – 31 May 2027", or a prompt when either date is missing. */
fun SchoolYear.dates(context: Context): String {
    val s = start
    val e = end
    return if (s != null && e != null) context.getString(R.string.home_date_range, s.fmt(), e.fmt()) else context.getString(R.string.home_dates_missing)
}

/** Today on the Home header, such as "Wednesday 21 October". */
private val HEADER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())

/**
 * The start screen (docs/PACER_DESIGN.md D3, D4): "Hello, name" over today's date and the open year, the Pacing
 * overview board and the next tests and worksheets. With no years it offers to create the first one.
 * With [welcome], the welcome guide opens over it. [undoToast] shows a snackbar with Undo.
 */
@Composable
fun HomeScreen(
    data: AppData,
    name: String,
    vm: PacerViewModel,
    padding: PaddingValues,
    welcome: Boolean,
    undoToast: (String) -> Unit = {},
    onMenu: (() -> Unit)? = null,
    menuBadge: Boolean = false,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val today = rememberToday()
    // The VM opens the latest year when none is open; until it has, show that one.
    val fallback = remember(data.years) { defaultYearId(data.years)?.let { id -> data.years.first { it.id == id } } }
    val year = data.year ?: fallback
    val dir = LocalLayoutDirection.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = padding.calculateStartPadding(dir) + Space.l,
                end = padding.calculateEndPadding(dir) + Space.l,
                top = padding.calculateTopPadding() + Space.xl,
                bottom = padding.calculateBottomPadding() + Space.xxl,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A readable width on a wide screen.
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.l)) {
            Header(name, year, today, onMenu, menuBadge)
            if (year == null) {
                EmptyState(
                    AppIcons.Add,
                    stringResource(R.string.home_create_year),
                    stringResource(R.string.home_create_year_hint),
                    stringResource(R.string.home_new_year),
                ) { adding = true }
            } else if (year.classes.isEmpty()) {
                GuideCard(
                    stringResource(R.string.home_add_first_class),
                    stringResource(R.string.home_first_class_hint),
                    action = stringResource(R.string.home_add_class) to { vm.open(Screen.Classes) },
                )
            } else {
                PacingBoard(year, vm)
                UpcomingCard(year, vm)
            }
        }
    }

    if (welcome) WelcomePage(onStart = { adding = true }, onDismiss = vm::setWelcomeSeen)
    if (adding) NewYearDialog(data.years, vm) { adding = false }
}

/** D3: ☰ for the menu (which holds Settings), the logo, "Hello, name" and, under it, today's date and the open year, on the accent's soft field. */
@Composable
private fun Header(name: String, year: SchoolYear?, today: LocalDate, onMenu: (() -> Unit)?, menuBadge: Boolean) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = Pacer.colors.accentSoft, contentColor = Pacer.colors.ink) {
    Row(Modifier.padding(Space.l), verticalAlignment = Alignment.CenterVertically) {
        // On a tablet with the menu always open there is no ☰.
        if (onMenu != null) {
            MenuButton(onMenu, menuBadge)
            Spacer(Modifier.width(Space.s))
        }
        Logo(48.dp)
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(
                if (name.isBlank()) stringResource(R.string.home_hello) else stringResource(R.string.home_hello_name, name),
                style = MaterialTheme.typography.headlineSmall,
                color = Pacer.colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            val date = today.format(HEADER_DATE)
            Text(
                if (year == null) date else stringResource(R.string.home_pair, date, year.name.ifBlank { context.getString(R.string.home_year) }),
                style = MaterialTheme.typography.bodyMedium,
                color = Pacer.colors.onAccentSoft,
            )
        }
    }
    }
}

/** A card on Home: surface, the large corners, a title row with an optional link at its end. */
@Composable
private fun HomeCard(title: String, link: String?, onLink: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = Pacer.colors.surface, contentColor = Pacer.colors.ink) {
        Column(Modifier.padding(vertical = Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(Modifier.padding(start = Space.l, end = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                if (link != null) TertiaryButton(link, onLink, color = Pacer.colors.accentText)
            }
            content()
        }
    }
}

/**
 * The Pacing overview board (decisions 5, 12): one line per class, one cell per group. On-pace cells wear their class's
 * colour (D14, a swatch beside the class name says which) and behind cells are the warning colour with "−N" whole lessons. A cell opens its group, and TalkBack
 * reads it as one item ("FCE Wed–Fri, short by 2 lessons"). Under it, the legend and "See all ›" to Pacing overview.
 */
@Composable
private fun PacingBoard(year: SchoolYear, vm: PacerViewModel) {
    val context = LocalContext.current
    val today = rememberToday()
    val full = remember(year, today) { pacingBoard(year, today) }
    val summary = remember(full) { full.summary() }
    // Large text: only the groups behind, with the groups on pace one tap away.
    val large = rememberLargeText()
    val board = if (large) full.behindOnly().rows else full
    val onPace = if (large) full.behindOnly().onPace else 0
    val setUp = if (large) full.behindOnly().setUp else 0
    val perLine = if (large) 1 else 2
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = Pacer.colors.surface, contentColor = Pacer.colors.ink) {
        Column(Modifier.padding(start = Space.l, end = Space.l, top = Space.m, bottom = Space.xs), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_overview), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                Text(
                    stringResource(
                        R.string.home_pair,
                        pluralStringResource(R.plurals.home_groups_count, summary.groups, summary.groups),
                        pluralStringResource(R.plurals.home_behind_count, summary.behind, summary.behind),
                    ),
                    style = MaterialTheme.typography.labelSmall.merge(Figures),
                    color = Pacer.colors.muted,
                )
            }
            if (board.isEmpty()) Hint(stringResource(R.string.home_board_empty))
            board.forEach { row ->
                val className = row.className.ifBlank { context.getString(R.string.home_class) }
                val tile = Pacer.colors.classTile(year, row.classId)
                if (large) Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    ClassName(className, tile)
                    row.cells.forEach { cell -> BoardCellView(className, cell, tile, Modifier.fillMaxWidth()) { vm.openGroup(cell.classId, cell.groupId) } }
                } else Row(verticalAlignment = Alignment.Top) {
                    // Level with the first line of cells.
                    Box(Modifier.weight(1f).heightIn(min = 48.dp).padding(end = Space.s), contentAlignment = Alignment.CenterStart) {
                        ClassName(className, tile)
                    }
                    Column(Modifier.weight(if (perLine == 1) 1.5f else 2f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        row.cells.chunked(perLine).forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                                line.forEach { cell -> BoardCellView(className, cell, tile, Modifier.weight(1f)) { vm.openGroup(cell.classId, cell.groupId) } }
                                repeat(perLine - line.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
            if (large) TertiaryButton(
                when {
                    onPace > 0 && setUp > 0 -> stringResource(R.string.home_on_pace_set_up, onPace, setUp)
                    setUp > 0 -> stringResource(R.string.home_set_up_needed, setUp)
                    onPace > 0 -> pluralStringResource(R.plurals.home_groups_on_pace, onPace, onPace)
                    else -> stringResource(R.string.home_see_all)
                },
                { vm.goTo(Screen.Overview) }, Modifier.fillMaxWidth(), color = Pacer.colors.accentText,
            ) else Row(verticalAlignment = Alignment.CenterVertically) {
                Legend(Modifier.weight(1f))
                TertiaryButton(stringResource(R.string.home_see_all), { vm.goTo(Screen.Overview) }, color = Pacer.colors.accentText)
            }
        }
    }
}

/** A class's name on the board, after a swatch of its colour. */
@Composable
private fun ClassName(name: String, tile: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        ClassSwatch(tile)
        Text(name, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** One group's cell, in its class's colour: its name, and "−N" when it is behind. One touch target and one TalkBack item. */
@Composable
private fun BoardCellView(className: String, cell: BoardCell, tile: Color, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val c = Pacer.colors
    val group = cell.groupName.ifBlank { context.getString(R.string.home_group) }
    val short = cell.short ?: 0
    val status = when {
        cell.short == null -> stringResource(R.string.home_cell, className, group)
        short > 0 -> pluralStringResource(R.plurals.home_cell_short, short, className, group, short)
        else -> stringResource(R.string.home_cell_on_pace, className, group)
    }
    val spoken = if (cell.today) stringResource(R.string.home_cell_spoken_today, status) else status
    val open = stringResource(R.string.home_open_group)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (cell.behind) c.warn else tile,
        contentColor = if (cell.behind) c.onWarn else c.onTile,
        modifier = modifier.heightIn(min = 48.dp).clearAndSetSemantics {
            contentDescription = spoken
            role = Role.Button
            onClick(open) { onClick(); true }
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Space.s, vertical = Space.s), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (cell.behind) stringResource(R.string.home_cell_behind, group, short) else group,
                style = MaterialTheme.typography.labelMedium.merge(Figures),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (cell.today) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                BlinkingDot(if (cell.behind) c.onWarn else c.second)
                Text(stringResource(R.string.home_cell_today), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** The board's key: the warning square for behind and the blinking dot for a lesson today. On-pace cells wear their class's colour, named by the swatch beside the class. The cells say it in words for TalkBack. */
@Composable
private fun Legend(modifier: Modifier) {
    val c = Pacer.colors
    Row(modifier.clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        Box(Modifier.size(10.dp).clip(MaterialTheme.shapes.extraSmall).background(c.warn))
        Text(stringResource(R.string.home_legend_behind), style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 2)
        Spacer(Modifier.width(Space.s))
        Box(Modifier.size(6.dp).clip(CircleShape).background(c.second))
        Text(stringResource(R.string.home_cell_today), style = MaterialTheme.typography.labelSmall, color = c.muted)
    }
}

/**
 * The dot before "Today" on a board cell. It fades out and in a few times when Home opens, then stays lit; with
 * animations off it never moves.
 */
@Composable
private fun BlinkingDot(color: Color) {
    val motion = animationsEnabled()
    val fade = remember { Animatable(1f) }
    LaunchedEffect(motion) {
        if (!motion) { fade.snapTo(1f); return@LaunchedEffect }
        repeat(BLINKS) {
            fade.animateTo(0.2f, Motion.work(MotionSpec.Step.PACE))
            fade.animateTo(1f, Motion.work(MotionSpec.Step.PACE))
        }
    }
    Box(Modifier.size(6.dp).graphicsLayer { alpha = fade.value }.clip(CircleShape).background(color))
}

private const val BLINKS = 3

/** The next tests and worksheets across the year. Tapping one opens its group's Work page. Nothing to show means no card. */
@Composable
private fun UpcomingCard(year: SchoolYear, vm: PacerViewModel) {
    val today = rememberToday()
    val items = remember(year, today) { upcoming(year, today) }
    if (items.isEmpty()) return
    val context = LocalContext.current
    val c = Pacer.colors
    HomeCard(stringResource(R.string.home_upcoming), null, {}) {
        items.forEach { item ->
            val where = listOfNotNull(item.className.ifBlank { context.getString(R.string.home_class) }, item.groupName?.ifBlank { context.getString(R.string.home_group) }).joinToString(" · ")
            val kind = stringResource(if (item.kind == UpcomingKind.WORKSHEET) R.string.home_kind_worksheet else R.string.home_kind_test)
            val day = stringResource(R.string.home_upcoming_when, item.date.dayOfWeek.short(), item.date.fmtShort())
            val title = item.title.ifBlank { kind }
            val open = stringResource(R.string.home_open_tests)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .clickable(onClickLabel = open, role = Role.Button) { vm.openWork(item.classId, item.groupId) }
                    .padding(horizontal = Space.l, vertical = Space.xs)
                    .semantics(mergeDescendants = true) { contentDescription = "$kind, $title, $day, $where" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                Text(day, style = MaterialTheme.typography.labelLarge.merge(Figures), modifier = Modifier.widthIn(min = 72.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(where, style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // The chip wears its kind's colour, as the lesson rows do.
                val edge = if (item.kind == UpcomingKind.WORKSHEET) c.edgeWorksheet else c.edgeTest
                Text(kind, style = MaterialTheme.typography.labelSmall, color = c.ink, modifier = Modifier.clip(CircleShape).background(c.kindTint(edge)).border(1.dp, edge, CircleShape).padding(horizontal = Space.s, vertical = Space.xs))
            }
        }
    }
}

/** Four fields, so a dialog rather than a sheet (§6 Bottom sheet is for three or fewer). */
@Composable
internal fun NewYearDialog(years: List<SchoolYear>, vm: PacerViewModel, onDismiss: () -> Unit) {
    val today = rememberToday()
    val draft = remember { suggestYear(years, today) }
    var name by rememberSaveable { mutableStateOf(draft.name) }
    var startText by rememberSaveable { mutableStateOf(draft.start?.toString()) }
    var endText by rememberSaveable { mutableStateOf(draft.end?.toString()) }
    val start = startText?.let(LocalDate::parse)
    val end = endText?.let(LocalDate::parse)
    var holidays by rememberSaveable { mutableStateOf(true) }
    val toggle = rememberToggleHaptic()
    AddDialogFrame(stringResource(R.string.home_new_year), name.isNotBlank(), { vm.newYear(name.trim(), start, end, holidays && start != null && end != null) }, onDismiss, confirm = stringResource(R.string.home_create)) {
        CommitField(name, { name = it }, stringResource(R.string.home_name), placeholder = draft.name)
        DateField(start, { startText = it?.toString() }, stringResource(R.string.home_starts))
        DateField(end, { endText = it?.toString() }, stringResource(R.string.home_ends))
        // One row, so the words toggle it as well as the box (Role.Checkbox; the box itself is not a second target).
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = holidays, role = Role.Checkbox, onValueChange = { toggle(it); holidays = it }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Pacer.colors.accentText, checkmarkColor = Pacer.colors.paper), checked = holidays, onCheckedChange = null)
            Spacer(Modifier.width(Space.m))
            Text(stringResource(R.string.home_holidays), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The in-app logo, drawn from the launcher artwork with the chosen palette. Decorative, so TalkBack skips it (§7). */
@Composable
fun Logo(size: Dp) {
    Box(
        // The logo tile's corners are a quarter of its size (style guide §4), the one shape outside MaterialTheme.shapes.
        Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(Pacer.colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        // The artwork sits in the middle of a 108dp canvas; scaling it up crops the empty margin.
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, colorFilter = ColorFilter.tint(Pacer.colors.onAccent), modifier = Modifier.requiredSize(size * 1.5f))
    }
}
