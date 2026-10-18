package gr.edusuite.pacer.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate

/** What deleting a year takes with it, wherever it is deleted from. */
const val YEAR_DELETE_DETAIL = "Its classes, groups, books and days off go too. You can undo it on the home screen."

/** One tile on the year screen: where it goes, a one-line summary of what is there, and its tile colour. */
private data class YearTile(val title: String, val detail: String, val color: Color, val warn: Boolean = false, val onClick: () -> Unit)

/**
 * The open school year: a button for each part of it. In portrait the buttons stack, full width,
 * so they stay readable on small phones; in landscape they sit in one row.
 */
@Composable
fun YearScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues) {
    var printing by rememberSaveable { mutableStateOf(false) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val problems = remember(year) { Nav.problems(year).size }
    val groups = year.classes.sumOf { it.groups.size }
    val c = Pacer.colors
    // At most three tile hues side by side (style guide §2.2); Print sits on the neutral container.
    val tiles = listOf(
        YearTile("Classes", count(year.classes.size, "class", "classes"), c.tileSky) { vm.open(Screen.Classes) },
        YearTile("School year settings", "${year.dates()} · ${count(year.closures.size, "day off", "days off", "no days off")}", c.tileLilac) {
            vm.open(Screen.YearSettings)
        },
        YearTile("Overview", if (problems > 0) count(problems, "problem") else count(groups, "group"), c.tileMint, warn = problems > 0) {
            vm.open(Screen.Overview)
        },
        YearTile("Print or save PDF", "Choose classes and margins", c.surfaceContainer) { printing = true },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Space.l, end = Space.l, top = padding.calculateTopPadding() + Space.l, bottom = padding.calculateBottomPadding() + Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (year.classes.isEmpty()) {
            GuideCard(
                "Add your first class",
                "A class is a set of books taught to one or more groups, like B1 Senior. " +
                    "Check the term dates and days off in School year settings too.",
                Modifier.widthIn(max = 480.dp).padding(bottom = Space.m),
                action = "Open Classes" to { vm.open(Screen.Classes) },
            )
        }
        // Centred and capped so tablets don't stretch the buttons.
        if (landscape) {
            Row(
                Modifier.widthIn(max = 960.dp).fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                tiles.forEach { t -> TileButton(t, Modifier.weight(1f).fillMaxHeight()) }
            }
        } else {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                tiles.forEach { t -> TileButton(t, Modifier.fillMaxWidth(), minHeight = 72.dp) }
            }
        }
    }

    if (printing) PrintDialog(year) { printing = false }
}

/** Picks which classes to print, all by default, and the page margins. */
@Composable
private fun PrintDialog(year: SchoolYear, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val chosen = remember(year.id) { mutableStateListOf(*year.classes.map { it.id }.toTypedArray()) }
    var margin by rememberSaveable { mutableStateOf(PrintMargin.NORMAL) }
    AddDialogFrame(
        "Print or save PDF",
        canAdd = chosen.isNotEmpty(),
        onAdd = { PrintSheet.print(context, year.name, year.classes.filter { it.id in chosen }.map(year::planFor), margin) },
        onDismiss = onDismiss,
        confirm = "Print",
    ) {
        Eyebrow("Classes")
        if (year.classes.isEmpty()) Hint("No classes yet.")
        year.classes.forEach { c ->
            val on = c.id in chosen
            Row(
                Modifier.fillMaxWidth().toggleable(value = on, role = Role.Checkbox) { if (on) chosen.remove(c.id) else chosen.add(c.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = on, onCheckedChange = null)
                Text(c.name.ifBlank { "Class" }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = Space.m, top = Space.s, bottom = Space.s))
            }
        }
        Eyebrow("Margins")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            PrintMargin.entries.forEach { m ->
                FilterChip(selected = margin == m, onClick = { margin = m }, label = { Text(m.label) }, shape = MaterialTheme.shapes.extraSmall)
            }
        }
        Hint("${margin.label}: ${margin.mm} mm on every side. Each group starts on a new page; the system print dialog then offers paper size, orientation and Save as PDF.")
    }
}

/** A year tile. With problems it turns into a warning block (warnSoft, rust summary) instead of its own hue. */
@Composable
private fun TileButton(t: YearTile, modifier: Modifier, minHeight: Dp = 96.dp) {
    val c = Pacer.colors
    Tile(
        color = if (t.warn) c.warnSoft else t.color,
        modifier = modifier.heightIn(min = minHeight),
        onClick = t.onClick,
        contentColor = if (t.warn) c.onWarnSoft else c.onTile,
    ) {
        Text(t.title, style = MaterialTheme.typography.titleMedium)
        Text(
            t.detail, style = MaterialTheme.typography.bodySmall,
            color = if (t.warn) c.warn else c.onTileMuted,
            maxLines = 3, overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Who a day off can apply to: everyone, a whole class, or one group. With a single class
 * the class level is left out, since it would mean the same as everyone.
 */
private fun whoOptions(year: SchoolYear): List<Pair<String, String>> {
    val many = year.classes.size > 1
    val out = mutableListOf(ALL_GROUPS to if (many) "All classes" else "All groups")
    for (c in year.classes) {
        if (many) out += c.id to "${c.name} · all groups"
        c.groups.forEach { g -> out += g.id to if (many) "${c.name} · ${g.name}" else "${g.name} only" }
    }
    return out
}

/** The tag a day off shows: null for everyone, else the class or group it is aimed at. */
private fun whoTag(year: SchoolYear, who: String): String? {
    if (who == ALL_GROUPS) return null
    val many = year.classes.size > 1
    for (c in year.classes) {
        if (c.id == who) return c.name
        val g = c.groups.firstOrNull { it.id == who } ?: continue
        return if (many) "${c.name} · ${g.name}" else g.name
    }
    return "removed group"
}

/** The year's name, term dates and deleting it at the top, then its days off. */
@Composable
fun YearSettingsScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues, toast: (String) -> Unit, onDeleted: (String) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val problems = remember(year) { Nav.problems(year).filter { it.fix == Screen.YearSettings } }

    ScreenList(padding) {
        items(problems) { ErrorBox(it.text) }
        item(key = "year") {
            Section("School year") {
                CommitField(year.name, { vm.renameYear(year.id, it) }, "Name", Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    DateField(year.start, vm::setStart, "Starts", Modifier.weight(1f))
                    DateField(year.end, vm::setEnd, "Ends", Modifier.weight(1f))
                }
                Hint("Every class paces up to the end date. Each group tracks its own next lesson on its class; the start is only the default.")
                // Up here, not below a long list of days off, so it is easy to find.
                OutlinedButton(onClick = { deleting = true }) { Text("Delete this school year", color = Pacer.colors.warn) }
            }
        }
        item(key = "offhead") { Heading("Days off") }
        if (year.closures.isEmpty()) item(key = "empty") { Hint("No days off yet.") }
        items(year.closures.sortedBy { it.from }, key = { it.id }) { c ->
            val detail = if (c.to == c.from) "${c.from.fmt()} · ${c.from.dayOfWeek.short()}" else "${c.from.fmt()} – ${c.to.fmt()}"
            ListItemRow(c.label, detail, whoTag(year, c.who)) { vm.removeClosure(c.id) }
        }
        item(key = "add") { AddButton("Add a day off") { adding = true } }
        item(key = "greek") {
            Column {
                TextButton(onClick = {
                    val n = vm.addGreekHolidays()
                    toast(if (n > 0) "$n holidays added" else "Already there")
                }, enabled = year.start != null && year.end != null) { Text("Add Greek public holidays for this year") }
                Hint(
                    "Greek holidays are computed from Orthodox Easter for every calendar year the school year touches. " +
                        "Check them against your school's calendar before relying on them.",
                )
            }
        }
    }

    if (adding) AddClosureDialog(year, vm) { adding = false }
    if (deleting) {
        val name = year.name.ifBlank { "School year" }
        ConfirmDelete(name, YEAR_DELETE_DETAIL, { vm.deleteYear(year.id); onDeleted(name) }) { deleting = false }
    }
}

@Composable
private fun AddClosureDialog(year: SchoolYear, vm: PacerViewModel, onDismiss: () -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var from by remember { mutableStateOf<LocalDate?>(null) }
    var to by remember { mutableStateOf<LocalDate?>(null) }
    var who by rememberSaveable { mutableStateOf(ALL_GROUPS) }
    AddDialogFrame("Add a day off", from != null, { vm.addClosure(label.trim(), from!!, to, who) }, onDismiss) {
        CommitField(label, { label = it }, "Reason", placeholder = "Christmas break")
        DateField(from, { from = it }, "From")
        DateField(to, { to = it }, "To (optional)", clearable = true)
        Picker("Applies to", whoOptions(year), who, { who = it })
    }
}

/** The year's classes. Tap one to open it. */
@Composable
fun ClassesScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val problems = remember(year) { Nav.problems(year).groupingBy { it.classId }.eachCount() }

    ScreenList(padding) {
        item(key = "hint") { Hint("A class is a set of books taught to one or more groups. Each group keeps its own days and place in the books.") }
        if (year.classes.isEmpty()) {
            item(key = "guide") {
                GuideCard(
                    "Add your first class",
                    "Tap New class below and give it a name. It starts with one group, Group A, taught on Mon, Wed and Fri; " +
                        "you add its books and change its days on the class screen.",
                )
            }
        }
        items(year.classes, key = { it.id }) { c ->
            val n = problems[c.id] ?: 0
            val groups = c.groups.joinToString(", ") { it.name }.ifEmpty { "No groups" }
            val detail = if (n > 0) count(n, "problem") else "$groups · ${count(c.books.size, "book", none = "no books")}"
            CardRow(c.name.ifBlank { "Class" }, detail, { vm.openClass(c.id) }, warn = n > 0)
        }
        item(key = "add") { AddButton("New class") { adding = true } }
        item(key = "foot") { Hint("New classes take the lesson length and pace from Settings.") }
    }

    if (adding) {
        var name by rememberSaveable { mutableStateOf("") }
        AddDialogFrame("New class", true, { vm.newClass(name.trim()) }, { adding = false }, confirm = "Create") {
            CommitField(name, { name = it }, "Name", placeholder = "B1 Senior")
        }
    }
}
