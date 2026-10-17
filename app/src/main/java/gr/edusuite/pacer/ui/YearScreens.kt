package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Mono
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate

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
    var editing by rememberSaveable(year.id) { mutableStateOf<String?>(null) }
    val problems = remember(year) { Nav.problems(year).filter { it.fix == Screen.YearSettings } }

    ScreenList(padding) {
        items(problems) { ErrorBox(it.text) }
        item(key = "year") {
            Section("School year") {
                LinkRow("Name", year.name.ifBlank { "School year" }) { editing = "name" }
                val datesSet = year.start != null && year.end != null
                val validDates = datesSet && !year.end!!.isBefore(year.start)
                LinkRow("Term", if (datesSet) "${year.start!!.fmt()} – ${year.end!!.fmt()}" else "Dates not set") { editing = "term" }
                if (!validDates) Hint("Set a start and end date, with the end on or after the start.", color = Pacer.colors.warn)
            }
        }
        item(key = "offhead") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Heading("Days off", Modifier.weight(1f))
                TertiaryButton("Add", { adding = true })
            }
        }
        if (year.closures.isEmpty()) item(key = "empty") { Hint("No days off yet.") }
        items(year.closures.sortedBy { it.from }, key = { it.id }) { c ->
            val detail = if (c.to == c.from) "${c.from.fmt()} · ${c.from.dayOfWeek.short()}" else "${c.from.fmt()} – ${c.to.fmt()}"
            val title = c.label.ifBlank { "Day off" }
            val remove = { vm.removeClosure(c.id); undoToast("Removed “$title”") }
            SwipeRow(title, remove) { ListItemRow(title, detail, whoTag(year, c.who), onRemove = remove) }
        }

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
            MoreOptions {
                TertiaryButton("Delete this school year", {
                    val name = year.name.ifBlank { "School year" }
                    vm.deleteYear(year.id)
                    onDeleted(name)
                }, color = Pacer.colors.warn)
            }
        }
    }

    if (adding) AddClosureSheet(year, vm) { adding = false }
    editing?.let { field ->
        SheetFrame(if (field == "name") "School year name" else "Term", "Done", true, {}, { editing = null }) {
            if (field == "name") CommitField(year.name, { vm.renameYear(year.id, it) }, "Name", Modifier.fillMaxWidth())
            else {
                DateField(year.start, vm::setStart, "Starts", Modifier.fillMaxWidth(), clearable = true)
                DateField(year.end, vm::setEnd, "Ends", Modifier.fillMaxWidth(), clearable = true)
                Hint("Every class paces up to the end date. Each group tracks its own next lesson; the start is only the default.")
                if (year.start != null && year.end != null && year.end!!.isBefore(year.start)) ErrorBox("Set a start and end date, with the end on or after the start.")
            }
        }
    }
}

/** Reason and From up front; the end date and who it applies to sit behind More options (§14 Progressive disclosure). */
@Composable
private fun AddClosureSheet(year: SchoolYear, vm: PacerViewModel, onDismiss: () -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var fromText by rememberSaveable { mutableStateOf<String?>(null) }
    var toText by rememberSaveable { mutableStateOf<String?>(null) }
    val from = fromText?.let(LocalDate::parse)
    val to = toText?.let(LocalDate::parse)
    var who by rememberSaveable { mutableStateOf(ALL_GROUPS) }
    SheetFrame("Add a day off", "Add", from != null, { vm.addClosure(label.trim(), from!!, to, who) }, onDismiss) {
        CommitField(label, { label = it }, "Reason", Modifier.fillMaxWidth(), placeholder = "Christmas break")
        DateField(from, { fromText = it?.toString() }, "From", Modifier.fillMaxWidth())
        MoreOptions {
            DateField(to, { toText = it?.toString() }, "To (optional)", Modifier.fillMaxWidth(), clearable = true)
            Picker("Applies to", whoOptions(year), who, { who = it }, Modifier.fillMaxWidth())
        }
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
            item(key = "empty") {
                EmptyState("+", "Add your first class", "Give it a name, then add books and set your groups’ lesson days.", "New class", { adding = true })
            }
        }
        item(key = "classes") {
            TileGrid(year.classes) { c, tileModifier ->
                val n = problems[c.id] ?: 0
                val lessons = remember(year, c) { val plan = year.planFor(c); c.groups.sumOf { GroupReport(plan, it).lessons.size } }
                Tile(Pacer.colors.tileSky, modifier = tileModifier.semantics(mergeDescendants = true) {}, onClick = { vm.openClass(c.id) }) {
                    if (n > 0) Tag(count(n, "problem"), warn = true, onTile = true)
                    Text(c.name.ifBlank { "Class" }, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(c.groups.joinToString(", ") { it.name }.ifEmpty { "No groups" } + " · " + count(c.books.size, "book", none = "no books"), style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.onTileMuted)
                    Text(lessons.toString(), style = MaterialTheme.typography.titleLarge, fontFamily = Mono)
                    Text("Lessons left", style = MaterialTheme.typography.bodySmall, color = Pacer.colors.onTileMuted)
                }
            }
        }
        item(key = "add") { PrimaryButton("New class", { adding = true }, Modifier.fillMaxWidth()) }
        item(key = "foot") { Hint("New classes take the lesson length and pace from Settings.") }
    }

    if (adding) {
        var name by rememberSaveable { mutableStateOf("") }
        SheetFrame("New class", "Add", true, { vm.newClass(name.trim()) }, { adding = false }) {
            CommitField(name, { name = it }, "Name", Modifier.fillMaxWidth(), placeholder = "B1 Senior")
        }
    }
}
