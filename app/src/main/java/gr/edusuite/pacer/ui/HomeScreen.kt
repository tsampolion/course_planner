package gr.edusuite.pacer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.suggestYear
import gr.edusuite.pacer.ui.theme.Mono
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "3 classes", "1 class", or [none] for zero. */
fun count(n: Int, one: String, many: String = one + "s", none: String = "None yet") =
    if (n == 0) none else "$n ${if (n == 1) one else many}"

/** "9 Sep 2026 – 31 May 2027", or a prompt when either date is missing. */
fun SchoolYear.dates(): String {
    val s = start
    val e = end
    return if (s != null && e != null) "${s.fmt()} – ${e.fmt()}" else "Dates not set"
}

/** "Mon 19 Oct": a lesson or test date on a tile. */
private fun LocalDate.tileDate(): String = "${dayOfWeek.short()} ${fmtShort()}"

/** Today on the Home header, such as "Saturday 17 October". */
private val HEADER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

/** Which class screen the class chooser opens for. */
private const val CHOOSE_TESTS = "tests"
private const val CHOOSE_HOMEWORK = "homework"

/**
 * The start screen (style guide §6 "Home header", "Hero tile", "Tile grid"): the header, the open school year's chip,
 * the next lesson to teach, and a tile for each section of the year. With no years it offers to create the first one.
 * With [welcome], the welcome guide opens over it. [onDeleted] and [undoToast] show a snackbar with Undo.
 */
@Composable
fun HomeScreen(
    data: AppData,
    vm: PacerViewModel,
    padding: PaddingValues,
    welcome: Boolean,
    onDeleted: (String) -> Unit,
    undoToast: (String) -> Unit = {},
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var printing by rememberSaveable { mutableStateOf(false) }
    // The year being renamed, by id, and the class screen waiting for a class to be chosen.
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var choosing by rememberSaveable { mutableStateOf<String?>(null) }
    val today = remember { LocalDate.now() }
    // The VM opens the latest year when none is open; until it has, show that one.
    val fallback = remember(data.years) { defaultYearId(data.years)?.let { id -> data.years.first { it.id == id } } }
    val year = data.year ?: fallback
    val dir = LocalLayoutDirection.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // §6: at 200% font or under 360dp the grid becomes one column.
        val oneColumn = LocalDensity.current.fontScale >= 2f || maxWidth < 360.dp
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = padding.calculateStartPadding(dir) + Space.l,
                    end = padding.calculateEndPadding(dir) + Space.l,
                    top = padding.calculateTopPadding() + Space.xxl,
                    bottom = padding.calculateBottomPadding() + Space.xxl,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xl)) {
                Header(today) { vm.open(Screen.Settings) }
                if (year == null) {
                    EmptyState(
                        "+",
                        "Create your school year",
                        "Give it its first and last teaching day. The Greek public holidays can come in as days off.",
                        "New school year",
                    ) { adding = true }
                } else {
                    YearChip(year.name.ifBlank { "School year" }) { picking = true }
                    NextLessonCard(year, today, vm, undoToast)
                    TileGrid(
                        year, today, vm, oneColumn,
                        onTests = { openForClass(year, vm, Screen.Tests) { choosing = CHOOSE_TESTS } },
                        onHomework = { openForClass(year, vm, Screen.Homework) { choosing = CHOOSE_HOMEWORK } },
                        onPrint = { printing = true },
                    )
                }
                Text(
                    "part of EduSuite apps",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = Pacer.colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (welcome) WelcomeDialog(onStart = { adding = true }, onDismiss = vm::setWelcomeSeen)
    if (adding) NewYearDialog(data.years, vm) { adding = false }
    if (picking) {
        YearPicker(
            data.years, year?.id, vm,
            onRename = { renaming = it },
            onDelete = { y ->
                val name = y.name.ifBlank { "School year" }
                vm.deleteYear(y.id)
                onDeleted(name)
            },
            onNew = { adding = true },
            onDismiss = { picking = false },
        )
    }
    data.years.firstOrNull { it.id == renaming }?.let { y -> RenameYearSheet(y, vm) { renaming = null } }
    if (printing && year != null) PrintDialog(year) { printing = false }
    if (choosing != null && year != null) {
        val screen = if (choosing == CHOOSE_TESTS) Screen.Tests else Screen.Homework
        ClassChooser(year, if (screen == Screen.Tests) "Tests for which class?" else "Homework for which class?", onDismiss = { choosing = null }) {
            vm.openClassScreen(it, screen)
        }
    }
}

/** Opens a class's Tests or Homework: straight away with one class, through [choose] with several, or Classes with none. */
private fun openForClass(year: SchoolYear, vm: PacerViewModel, screen: Screen, choose: () -> Unit) {
    when (year.classes.size) {
        0 -> vm.open(Screen.Classes)
        1 -> vm.openClassScreen(year.classes.first().id, screen)
        else -> choose()
    }
}

/** §6 "Home header": the logo, "Hello" over today's date, and Settings at the end. */
@Composable
private fun Header(today: LocalDate, onSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Logo(48.dp)
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(
                "Hello",
                style = MaterialTheme.typography.headlineSmall,
                color = Pacer.colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            Text(today.format(HEADER_DATE), style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.muted)
        }
        PillIconButton("Settings", onSettings) {
            // U+FE0E asks for the text glyph rather than an emoji.
            Text("⚙︎", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** The open year's name on a pill; tapping it opens the year picker. */
@Composable
private fun YearChip(name: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Pacer.colors.surfaceContainer,
        contentColor = Pacer.colors.ink,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Change school year", onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = Space.l, vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.labelLarge)
            Text(" ▾", style = MaterialTheme.typography.labelLarge, modifier = Modifier.clearAndSetSemantics { })
        }
    }
}

/**
 * The hero tile (§6): the year's next lesson in the accent fill, with Mark done. Tapping the tile opens the group.
 * A year with classes but no lesson left gets a neutral tile; a year with no classes gets a guide to add one.
 */
@Composable
private fun NextLessonCard(year: SchoolYear, today: LocalDate, vm: PacerViewModel, undoToast: (String) -> Unit) {
    val next = remember(year, today) { nextLesson(year, today) }
    val c = Pacer.colors
    when {
        year.classes.isEmpty() -> GuideCard(
            "Add your first class",
            "A class holds its books, lesson days and groups. Add one and its next lesson shows here.",
            action = "Add a class" to { vm.open(Screen.Classes) },
        )
        next == null -> Tile(c.surfaceContainer, contentColor = c.ink, onClick = { vm.open(Screen.Overview) }) {
            Text("No lessons left this term", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text("Open Overview to see where each group stands.", style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        else -> Tile(c.accent, contentColor = c.onAccent, onClick = { vm.openGroup(next.classId, next.groupId) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Next lesson", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f).semantics { heading() })
                if (next.overdue) Tag("Overdue", warn = true)
            }
            Text(next.date.tileDate(), style = MaterialTheme.typography.headlineSmall, fontFamily = Mono, fontWeight = FontWeight.SemiBold)
            Text("${next.className} · ${next.groupName}", style = MaterialTheme.typography.titleMedium)
            // Book names and pages read as a sentence, so they stay in sans (§3: mono is for figures only).
            Text(next.what, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = {
                    vm.markNextLessonDoneIn(next.classId, next.groupId)
                    undoToast("Lesson marked done")
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = c.surface, contentColor = c.ink),
                modifier = Modifier.padding(top = Space.s).heightIn(min = 48.dp),
            ) { Text("Mark done", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/**
 * The bento grid (§6 "Tile grid"): two columns of mixed heights, or one in [oneColumn]. Three tile hues (sky, mint,
 * lilac) plus neutral tiles, so the screen stays within §2.2's limit beside the accent hero.
 */
@Composable
private fun TileGrid(
    year: SchoolYear,
    today: LocalDate,
    vm: PacerViewModel,
    oneColumn: Boolean,
    onTests: () -> Unit,
    onHomework: () -> Unit,
    onPrint: () -> Unit,
) {
    val s = remember(year, today) { homeSummary(year, today) }
    val c = Pacer.colors
    val tall = !oneColumn

    val classes: @Composable (Modifier) -> Unit = { m ->
        HomeTile(
            "Classes",
            if (s.classes == 0) "None yet" else "${count(s.classes, "class", "classes")} · ${count(s.groups, "group", none = "no groups")}",
            c.tileSky, { vm.open(Screen.Classes) }, m, dim = s.classes == 0, tall = tall,
        )
    }
    val tests: @Composable (Modifier) -> Unit = { m ->
        val next = s.nextTest
        HomeTile(
            "Tests",
            if (next != null) "Next: ${next.first.tileDate()} · ${next.second}" else count(s.tests, "test"),
            c.tileLilac, onTests, m, dim = s.tests == 0,
        )
    }
    val print: @Composable (Modifier) -> Unit = { m ->
        HomeTile("Print or save PDF", "Choose classes and margins", c.surfaceContainer, onPrint, m, contentColor = c.ink, mutedColor = c.muted)
    }
    val overview: @Composable (Modifier) -> Unit = { m ->
        val problems = s.problems > 0
        HomeTile(
            "Overview",
            if (problems) count(s.problems, "problem") else "No problems · ${count(s.groups, "group", none = "no groups")}",
            if (problems) c.warnSoft else c.tileMint, { vm.open(Screen.Overview) }, m,
            contentColor = if (problems) c.onWarnSoft else c.onTile,
            mutedColor = if (problems) c.onWarnSoft else c.onTileMuted,
            tall = tall,
            tag = if (problems) "Needs a look" else null,
        )
    }
    val homework: @Composable (Modifier) -> Unit = { m ->
        HomeTile("Homework", count(s.homework, "assignment", none = "Nothing assigned"), c.tileSky, onHomework, m, dim = s.homework == 0)
    }
    val daysOff: @Composable (Modifier) -> Unit = { m ->
        val start = year.start
        val end = year.end
        val off = count(s.daysOff, "day off", "days off", "no days off")
        HomeTile(
            "Days off & dates",
            if (start != null && end != null) "${start.fmtShort()} – ${end.fmtShort()} · $off" else off.replaceFirstChar { it.uppercase() },
            c.surfaceContainer, { vm.open(Screen.YearSettings) }, m,
            contentColor = c.ink, mutedColor = c.muted,
            // Words, not colour alone (§7), say the term still needs its dates.
            tag = if (!s.datesSet) "Dates not set" else null,
            tagWarn = true,
        )
    }

    if (oneColumn) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf(classes, overview, tests, homework, daysOff, print).forEach { it(Modifier) }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                classes(Modifier); tests(Modifier); print(Modifier)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                overview(Modifier); homework(Modifier); daysOff(Modifier)
            }
        }
    }
}

/**
 * One section on Home's grid (§6 Tile): an optional status [tag] (warnSoft with [tagWarn]), the title (a heading) and a live subtitle, read together as
 * one touch target. With [dim] the section is empty: the fill at 30% and the text at 60%, still tappable to add the
 * first item. A [tall] tile is one of the most-used sections.
 */
@Composable
private fun HomeTile(
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Pacer.colors.onTile,
    mutedColor: Color = Pacer.colors.onTileMuted,
    dim: Boolean = false,
    tall: Boolean = false,
    tag: String? = null,
    tagWarn: Boolean = false,
) {
    Tile(
        color = if (dim) color.copy(alpha = 0.3f) else color,
        modifier = modifier.then(if (tall) Modifier.heightIn(min = 160.dp) else Modifier),
        onClick = onClick,
        contentColor = contentColor,
    ) {
        Column(Modifier.alpha(if (dim) 0.6f else 1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            // A warn tag is warnSoft on the tile; any other is a surface pill (§6 Status chip).
            tag?.let { Tag(it, warn = tagWarn, onTile = !tagWarn) }
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = mutedColor)
        }
    }
}

/** A bottom sheet for Home's pickers (§6 Bottom sheet): 28dp top corners on surface. [content] gets a close that slides it away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
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

/**
 * Every school year, newest first: tap one to show it on Home. Each row's ⋮ offers Rename and Delete; Delete happens at
 * once and the snackbar offers Undo (§14 undo over confirm).
 */
@Composable
private fun YearPicker(
    years: List<SchoolYear>,
    openId: String?,
    vm: PacerViewModel,
    onRename: (String) -> Unit,
    onDelete: (SchoolYear) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sorted = remember(years) { years.sortedByDescending { it.start } }
    HomeSheet("School year", onDismiss) { close ->
        Column(Modifier.selectableGroup()) {
            sorted.forEach { y ->
                YearRow(
                    y, selected = y.id == openId,
                    onOpen = { vm.selectYear(y.id); close() },
                    onRename = { onRename(y.id); close() },
                    onDelete = { onDelete(y); close() },
                )
            }
        }
        AddButton("New school year") { onNew(); close() }
    }
}

/** A year in the picker: a radio, its name over its dates and class count, and a ⋮ menu. */
@Composable
private fun YearRow(y: SchoolYear, selected: Boolean, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val name = y.name.ifBlank { "School year" }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = Space.m))
        Column(Modifier.weight(1f).padding(vertical = Space.s)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${y.dates()} · ${count(y.classes.size, "class", "classes", "no classes")}",
                style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted,
            )
        }
        Box {
            TextButton(
                onClick = { menu = true },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp).semantics { contentDescription = "Options for $name" },
            ) { Text("⋮", style = MaterialTheme.typography.titleLarge, color = Pacer.colors.muted) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text("Delete", color = Pacer.colors.warn) }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

/** Picks the class whose Tests or Homework to open, when the year has several. */
@Composable
private fun ClassChooser(year: SchoolYear, title: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    HomeSheet(title, onDismiss) { close ->
        year.classes.forEach { k ->
            LinkRow(k.name.ifBlank { "Class" }, count(k.groups.size, "group", none = "No groups")) { onPick(k.id); close() }
        }
    }
}

/** Renames a year in a sheet; the name is saved once, on Save. */
@Composable
private fun RenameYearSheet(y: SchoolYear, vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(y.name) }
    SheetFrame("Rename school year", "Save", name.isNotBlank(), { vm.renameYear(y.id, name.trim()) }, onDismiss) {
        CommitField(name, { name = it }, "Name", Modifier.fillMaxWidth())
    }
}

/** Four fields, so a dialog rather than a sheet (§6 Bottom sheet is for three or fewer). */
@Composable
private fun NewYearDialog(years: List<SchoolYear>, vm: PacerViewModel, onDismiss: () -> Unit) {
    val draft = remember { suggestYear(years, LocalDate.now()) }
    var name by rememberSaveable { mutableStateOf(draft.name) }
    var start by remember { mutableStateOf<LocalDate?>(draft.start) }
    var end by remember { mutableStateOf<LocalDate?>(draft.end) }
    var holidays by rememberSaveable { mutableStateOf(true) }
    AddDialogFrame("New school year", name.isNotBlank(), { vm.newYear(name.trim(), start, end, holidays && start != null && end != null) }, onDismiss, confirm = "Create") {
        CommitField(name, { name = it }, "Name", placeholder = draft.name)
        DateField(start, { start = it }, "Starts")
        DateField(end, { end = it }, "Ends")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = holidays, onCheckedChange = { holidays = it })
            Text("Add the Greek public holidays as days off", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The launcher icon, drawn from the same artwork: the open book on the teal tile. Decorative, so TalkBack skips it (§7). */
@Composable
private fun Logo(size: Dp) {
    Box(
        // The logo tile's corners are a quarter of its size (style guide §4), the one shape outside MaterialTheme.shapes.
        Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(colorResource(R.color.launcher_tile)),
        contentAlignment = Alignment.Center,
    ) {
        // The artwork sits in the middle of a 108dp canvas; scaling it up crops the empty margin.
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.requiredSize(size * 1.5f))
    }
}
