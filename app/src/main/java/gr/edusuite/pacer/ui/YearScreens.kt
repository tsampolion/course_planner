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
fun YearSettingsScreen(year: SchoolYear, vm: PacerViewModel, padding: PaddingValues, toast: (String) -> Unit, undoToast: (String) -> Unit, onDeleted: (String) -> Unit) {
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
