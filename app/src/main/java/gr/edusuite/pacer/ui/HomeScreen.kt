package gr.edusuite.pacer.ui

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.colorResource
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

/** "Mon 19 Oct": the next lesson's date when it isn't today. */
private fun LocalDate.barDate(): String = format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

/** Today on the Home header, such as "Wednesday 21 October". */
private val HEADER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())

/** From this font scale the board gives each group cell a whole line (docs/PACER_DESIGN.md, large text). */
private const val LARGE_TEXT = 1.5f

/**
 * The start screen (docs/PACER_DESIGN.md D3, D4): "Hello, name" over today's date and the open year, the next-lesson
 * bar with Done, the Classes card and the Pacing overview board. With no years it offers to create the first one.
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
    onMenu: () -> Unit = {},
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
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
            Header(name, year, today, onMenu)
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
                NextLessonBar(year, today, vm, undoToast)
                ClassesCard(year, vm)
                PacingBoard(year, vm)
            }
        }
    }

    if (welcome) WelcomeDialog(onStart = { adding = true }, onDismiss = vm::setWelcomeSeen)
    if (adding) NewYearDialog(data.years, vm) { adding = false }
}

/** D3: ☰ for the menu (which holds Settings), the logo, "Hello, name" and, under it, today's date and the open year. */
@Composable
private fun Header(name: String, year: SchoolYear?, today: LocalDate, onMenu: () -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        PillIconButton(stringResource(R.string.nav_menu), onMenu) { AppIcon(AppIcons.Menu) }
        Spacer(Modifier.width(Space.s))
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
                color = Pacer.colors.muted,
            )
        }
    }
}

/**
 * The next-lesson bar: the year's next lesson in the accent fill, with Done. Tapping the bar opens the group. Its
 * label says "today" with how many of today's lessons are left, the date, or that the lesson is overdue. A year with
 * no lesson left gets a neutral bar that opens Pacing overview.
 */
@Composable
private fun NextLessonBar(year: SchoolYear, today: LocalDate, vm: PacerViewModel, undoToast: (String) -> Unit) {
    val context = LocalContext.current
    val press = remember { MutableInteractionSource() }
    val next = remember(year, today) { nextLesson(year, today) }
    val count = remember(year, today) { lessonsToday(year, today) }
    val c = Pacer.colors
    if (next == null) {
        Tile(c.surfaceContainer, contentColor = c.ink, onClick = { vm.goTo(Screen.Overview) }) {
            Text(stringResource(R.string.home_no_lessons), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.home_overview_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        return
    }
    val large = LocalDensity.current.fontScale >= LARGE_TEXT
    val label = when {
        next.overdue -> stringResource(R.string.home_next_overdue, next.date.barDate())
        next.date == today -> stringResource(R.string.home_next_today, count.left, count.total)
        else -> stringResource(R.string.home_next_on, next.date.barDate())
    }
    val done: @Composable (Modifier) -> Unit = { m ->
        Button(
            onClick = {
                vm.markNextLessonDoneIn(next.classId, next.groupId)
                undoToast(context.getString(R.string.home_lesson_done))
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = c.onAccent, contentColor = c.accent),
            interactionSource = press,
            modifier = m.pressFeedback(press).heightIn(min = 48.dp),
        ) {
            AppIcon(AppIcons.Check, Modifier.padding(end = Space.s))
            Text(stringResource(R.string.home_done), style = MaterialTheme.typography.labelLarge)
        }
    }
    Tile(c.accent, contentColor = c.onAccent, onClick = { vm.openGroup(next.classId, next.groupId) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium.merge(Figures), modifier = Modifier.semantics { heading() })
                Text(
                    stringResource(R.string.home_pair, next.className.ifBlank { context.getString(R.string.home_class) }, next.groupName.ifBlank { context.getString(R.string.home_group) }),
                    style = MaterialTheme.typography.titleMedium,
                )
                // Book names and pages read as a sentence, so they stay in sans.
                Text(next.what.ifBlank { context.getString(R.string.home_no_pages) }, style = MaterialTheme.typography.bodySmall)
                if (next.note.isNotEmpty()) Text(next.note, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
            if (!large) done(Modifier.padding(start = Space.s))
        }
        if (large) done(Modifier.fillMaxWidth().padding(top = Space.s))
    }
}

/** A card on Home: surface, the large corners, a title row with an optional link at its end. */
@Composable
private fun HomeCard(title: String, link: String?, onLink: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = Pacer.colors.surface, contentColor = Pacer.colors.ink) {
        Column(Modifier.padding(vertical = Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(Modifier.padding(start = Space.l, end = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                if (link != null) TertiaryButton(link, onLink, color = Pacer.colors.accent)
            }
            content()
        }
    }
}

/** The Classes card: a scrolling row of class chips (name over its group count) and "All N ›" to Classes. */
@Composable
private fun ClassesCard(year: SchoolYear, vm: PacerViewModel) {
    val context = LocalContext.current
    val n = year.classes.size
    HomeCard(stringResource(R.string.home_classes), stringResource(R.string.home_all_classes, n), { vm.goTo(Screen.Classes) }) {
        LazyRow(contentPadding = PaddingValues(horizontal = Space.l), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            items(year.classes, key = { it.id }) { k ->
                Surface(
                    onClick = { vm.openClass(k.id) },
                    shape = MaterialTheme.shapes.medium,
                    color = Pacer.colors.surfaceContainer,
                    contentColor = Pacer.colors.ink,
                    modifier = Modifier.heightIn(min = 48.dp).widthIn(max = 200.dp),
                ) {
                    Column(Modifier.padding(horizontal = Space.m, vertical = Space.s)) {
                        Text(k.name.ifBlank { context.getString(R.string.home_class) }, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(pluralStringResource(R.plurals.home_groups_count, k.groups.size, k.groups.size), style = MaterialTheme.typography.labelSmall, color = Pacer.colors.muted)
                    }
                }
            }
        }
    }
}

/**
 * The Pacing overview board (decisions 5, 12): one line per class, one cell per group. On-pace cells are neutral
 * (surfaceContainer with ink) and behind cells are rust with "−N" whole lessons. A cell opens its group, and TalkBack
 * reads it as one item ("FCE Wed–Fri, short by 2 lessons"). Under it, the legend and "See all ›" to Pacing overview.
 */
@Composable
private fun PacingBoard(year: SchoolYear, vm: PacerViewModel) {
    val context = LocalContext.current
    val board = remember(year) { pacingBoard(year) }
    val summary = remember(board) { board.summary() }
    val perLine = if (LocalDensity.current.fontScale >= LARGE_TEXT) 1 else 2
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
                Row(verticalAlignment = Alignment.Top) {
                    // Level with the first line of cells.
                    Box(Modifier.weight(1f).heightIn(min = 48.dp).padding(end = Space.s), contentAlignment = Alignment.CenterStart) {
                        Text(className, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Column(Modifier.weight(if (perLine == 1) 1.5f else 2f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        row.cells.chunked(perLine).forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                                line.forEach { cell -> BoardCellView(className, cell, Modifier.weight(1f)) { vm.openGroup(cell.classId, cell.groupId) } }
                                repeat(perLine - line.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Legend(Modifier.weight(1f))
                TertiaryButton(stringResource(R.string.home_see_all), { vm.goTo(Screen.Overview) }, color = Pacer.colors.accent)
            }
        }
    }
}

/** One group's cell: its name, and "−N" when it is behind. One touch target and one TalkBack item. */
@Composable
private fun BoardCellView(className: String, cell: BoardCell, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val c = Pacer.colors
    val group = cell.groupName.ifBlank { context.getString(R.string.home_group) }
    val short = cell.short ?: 0
    val spoken = when {
        cell.short == null -> stringResource(R.string.home_cell, className, group)
        short > 0 -> pluralStringResource(R.plurals.home_cell_short, short, className, group, short)
        else -> stringResource(R.string.home_cell_on_pace, className, group)
    }
    val open = stringResource(R.string.home_open_group)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (cell.behind) c.warn else c.surfaceContainer,
        contentColor = if (cell.behind) c.onWarn else c.ink,
        modifier = modifier.heightIn(min = 48.dp).clearAndSetSemantics {
            contentDescription = spoken
            role = Role.Button
            onClick(open) { onClick(); true }
        },
    ) {
        Box(Modifier.padding(horizontal = Space.s, vertical = Space.s), contentAlignment = Alignment.Center) {
            Text(
                if (cell.behind) stringResource(R.string.home_cell_behind, group, short) else group,
                style = MaterialTheme.typography.labelMedium.merge(Figures),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The board's key: an outlined square for on pace and a rust one for behind. The cells say it in words for TalkBack. */
@Composable
private fun Legend(modifier: Modifier) {
    val c = Pacer.colors
    Row(modifier.clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        Box(Modifier.size(10.dp).clip(MaterialTheme.shapes.extraSmall).background(c.surfaceContainer).border(1.dp, c.outline, MaterialTheme.shapes.extraSmall))
        Text(stringResource(R.string.status_on_pace), style = MaterialTheme.typography.labelSmall, color = c.muted)
        Spacer(Modifier.width(Space.s))
        Box(Modifier.size(10.dp).clip(MaterialTheme.shapes.extraSmall).background(c.warn))
        Text(stringResource(R.string.home_legend_behind), style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 2)
    }
}

/** A bottom sheet for Home's pickers, the group switcher and Warnings (§6 Bottom sheet): 28dp top corners on surface. [content] gets a close that slides it away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }; Unit }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = MaterialTheme.shapes.extraLarge.let { if (it is RoundedCornerShape) it.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)) else it },
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = Space.l, end = Space.l, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            content(close)
        }
    }
}

/** Four fields, so a dialog rather than a sheet (§6 Bottom sheet is for three or fewer). */
@Composable
internal fun NewYearDialog(years: List<SchoolYear>, vm: PacerViewModel, onDismiss: () -> Unit) {
    val draft = remember { suggestYear(years, LocalDate.now()) }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = holidays, onCheckedChange = { toggle(it); holidays = it })
            Text(stringResource(R.string.home_holidays), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The launcher icon, drawn from the same artwork: the open book on the teal tile. Decorative, so TalkBack skips it (§7). */
@Composable
fun Logo(size: Dp) {
    Box(
        // The logo tile's corners are a quarter of its size (style guide §4), the one shape outside MaterialTheme.shapes.
        Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(colorResource(R.color.launcher_tile)),
        contentAlignment = Alignment.Center,
    ) {
        // The artwork sits in the middle of a 108dp canvas; scaling it up crops the empty margin.
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.requiredSize(size * 1.5f))
    }
}
