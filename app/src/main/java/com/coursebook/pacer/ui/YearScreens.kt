package com.coursebook.pacer.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coursebook.pacer.engine.fmt
import com.coursebook.pacer.engine.short
import com.coursebook.pacer.model.ALL_GROUPS
import com.coursebook.pacer.model.SchoolYear
import com.coursebook.pacer.ui.theme.Pacer
import java.time.LocalDate

/** One button on the year screen: where it goes, and a one-line summary of what is there. */
private data class Tile(val title: String, val detail: String, val warn: Boolean = false, val onClick: () -> Unit)

/** The open school year: a button for each part of it. */
@Composable
fun YearScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val problems = remember(year) { Nav.problems(year).size }
    val groups = year.classes.sumOf { it.groups.size }
    val tiles = listOf(
        Tile("Classes", count(year.classes.size, "class", "classes")) { vm.open(Screen.Classes) },
        Tile("School year settings", "${year.dates()} · ${count(year.closures.size, "day off", "days off", "no days off")}") {
            vm.open(Screen.YearSettings)
        },
        Tile("Overview", if (problems > 0) count(problems, "problem") else count(groups, "group"), warn = problems > 0) {
            vm.open(Screen.Overview)
        },
        Tile("Print or save PDF", "Every group's schedule this year") {
            PrintSheet.print(context, year.name, year.classes.map(year::planFor))
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 16.dp, bottom = padding.calculateBottomPadding() + 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Two columns, centred and capped so tablets and landscape don't stretch the buttons.
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tiles.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { t -> TileButton(t, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

@Composable
private fun TileButton(t: Tile, modifier: Modifier) {
    Surface(
        onClick = t.onClick,
        modifier = modifier.heightIn(min = 96.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (t.warn) Pacer.colors.warn else Pacer.colors.line),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                t.detail, style = MaterialTheme.typography.bodySmall,
                color = if (t.warn) Pacer.colors.warn else Pacer.colors.muted,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
        }
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

/** The year's name, term dates and days off, and deleting the year. */
@Composable
fun YearSettingsScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues, toast: (String) -> Unit, onDeleted: (String) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val problems = remember(year) { Nav.problems(year).filter { it.fix == Screen.YearSettings } }

    ScreenList(padding) {
        items(problems) { ErrorBox(it.text) }
        item(key = "year") {
            Section("School year") {
                CommitField(year.name, vm::renameYear, "Name", Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField(year.start, vm::setStart, "Starts", Modifier.weight(1f))
                    DateField(year.end, vm::setEnd, "Ends", Modifier.weight(1f))
                }
                Hint("Every class paces up to the end date. Each group tracks its own next lesson on its class; the start is only the default.")
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
        item(key = "delete") {
            OutlinedButton(onClick = { deleting = true }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Delete this school year", color = Pacer.colors.warn)
            }
        }
    }

    if (adding) AddClosureDialog(year, vm) { adding = false }
    if (deleting) {
        val name = year.name.ifBlank { "School year" }
        ConfirmDelete(name, "Its classes, groups and books go too.", { vm.deleteYear(); onDeleted(name) }) { deleting = false }
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

    ScreenList(padding, spacing = 10.dp) {
        item(key = "hint") { Hint("A class is a set of books taught to one or more groups. Each group keeps its own days and place in the books.") }
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
