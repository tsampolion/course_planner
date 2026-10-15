package com.coursebook.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.coursebook.pacer.engine.WEEK
import com.coursebook.pacer.engine.fmt
import com.coursebook.pacer.engine.lessonsFor
import com.coursebook.pacer.engine.parseRanges
import com.coursebook.pacer.engine.plain
import com.coursebook.pacer.engine.short
import com.coursebook.pacer.model.ALL_GROUPS
import com.coursebook.pacer.model.Book
import com.coursebook.pacer.model.Group
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.ui.theme.Pacer
import java.time.LocalDate

/**
 * The scrolling body every class-editing screen shares, with the problems this screen fixes on top.
 * The class screen also lists term problems, linked to School year settings. [hide] is a problem
 * a "Next step" card already explains.
 */
@Composable
private fun EditList(plan: Plan, screen: Screen, vm: PacerViewModel, padding: PaddingValues, hide: String? = null, content: LazyListScope.() -> Unit) {
    val problems = remember(plan, hide) {
        Nav.problems(plan).filter { (it.fix == screen || (screen == Screen.Class && it.fix == Screen.YearSettings)) && it.text != hide }
    }
    ScreenList(padding) {
        items(problems) { p -> ErrorBox(p.text, onFix = p.fix?.takeIf { it != screen }?.let { f -> { vm.open(f) } }) }
        content()
    }
}

/** The open class: its name, lesson length and pace, books and groups, with tests and homework a tap away. */
@Composable
fun ClassScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues) {
    var addingBook by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    // A new class has a group but no books; the card replaces the red "Add at least one book." problem.
    val hide = when {
        plan.books.isEmpty() -> NO_BOOKS
        plan.groups.isEmpty() -> NO_GROUPS
        else -> null
    }
    EditList(plan, Screen.Class, vm, padding, hide) {
        when (hide) {
            NO_BOOKS -> item(key = "guide") {
                GuideCard(
                    "Add the books you teach from",
                    "Add each coursebook with its first and last page, like Student's Book pp. 1–160. Each book is paced on its own. " +
                        "Then check each group's lesson days below and tap Open schedule.",
                    action = "Add a book" to { addingBook = true },
                )
            }
            NO_GROUPS -> item(key = "guide") {
                GuideCard(
                    "Add a group",
                    "A group is a set of students taught from these books, with its own lesson days and place in the books.",
                    action = "Add group" to vm::addGroup,
                )
            }
        }
        item(key = "class") {
            Section("Class") {
                CommitField(plan.name, vm::renameClass, "Name", Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IntField(plan.mins, vm::setMins, "Lesson length (min)", Modifier.weight(1f))
                    CommitField(
                        plan.pace.plain(),
                        { s -> s.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }?.let(vm::setPace) },
                        "Pace you can sustain",
                        Modifier.weight(1f),
                        numeric = true,
                        decimal = true,
                    )
                }
                Hint("Pages per lesson. It does not drive the schedule; it is the benchmark the verdict measures against.")
            }
        }

        item(key = "bookshead") { Heading("Books") }
        item(key = "bookshint") {
            Hint(
                "Pages are paced per book, never pooled, so each title runs at its own rate and all of them land on the final lesson. " +
                    "Skipped pages are structural and apply to every group. Use homework for pages one group covers outside the lesson.",
            )
        }
        if (plan.books.isEmpty()) item(key = "nobooks") { Hint("No books yet.") }
        items(plan.books, key = { it.id }) { b -> BookEditor(b, vm) }
        item(key = "addbook") { AddButton("Add a book") { addingBook = true } }

        item(key = "groupshead") { Heading("Groups") }
        if (plan.groups.isEmpty()) item(key = "nogroups") { Hint("No groups yet. Add one for each set of students taught from these books.") }
        items(plan.groups, key = { it.id }) { g -> GroupEditor(plan, g, vm) }
        item(key = "groupactions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::addGroup) { Text("Add group") }
                OutlinedButton(onClick = vm::resetPositions, enabled = plan.groups.isNotEmpty()) { Text("Reset all positions") }
            }
        }

        item(key = "more") {
            Section("Tests and homework") {
                Column {
                    LinkRow("Tests & worksheets", count(plan.events.size, "entry", "entries", "None scheduled")) { vm.open(Screen.Tests) }
                    LinkRow("Homework already assigned", count(plan.homework.size, "assignment", none = "Nothing assigned")) { vm.open(Screen.Homework) }
                }
            }
        }
        item(key = "delete") {
            OutlinedButton(onClick = { deleting = true }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Delete this class", color = Pacer.colors.warn)
            }
        }
    }
    if (addingBook) AddBookDialog(vm) { addingBook = false }
    if (deleting) {
        ConfirmDelete(plan.name.ifBlank { "Class" }, "Its groups, books, tests and homework go too. Undo brings them back.", vm::deleteClass) { deleting = false }
    }
}

/** The problems ([Plan.problems]) that the class screen's "Next step" card covers. */
private const val NO_BOOKS = "Add at least one book."
private const val NO_GROUPS = "Add at least one group."

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupEditor(plan: Plan, g: Group, vm: PacerViewModel) = Section(g.name.ifBlank { "Group" }) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommitField(g.name, { vm.renameGroup(g.id, it) }, "Name", Modifier.weight(1f))
        RemoveButton(g.name) { vm.removeGroup(g.id) }
    }
    // Seven chips need about 330dp; the row wraps its last chips on very narrow phones.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        WEEK.forEach { day ->
            FilterChip(
                selected = day in g.days,
                onClick = { vm.toggleDay(g.id, day) },
                label = { Text(day.short()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
    DateField(g.from ?: plan.start, { vm.setNextLesson(g.id, it) }, "Next lesson", Modifier.fillMaxWidth())
    val count = remember(plan, g) { plan.lessonsFor(g).size }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Hint("$count ${if (count == 1) "lesson" else "lessons"} left")
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { vm.open(Screen.Group(g.id)) }) { Text("Open schedule ›") }
    }
}

@Composable
fun TestsScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues) {
    var adding by rememberSaveable { mutableStateOf(false) }
    // Lessons marked done are still lesson days.
    val lessonDays = remember(plan) { plan.groups.associate { g -> g.id to (plan.lessonsFor(g) + g.done.map { it.date }).toHashSet() } }
    EditList(plan, Screen.Tests, vm, padding) {
        if (plan.events.isEmpty()) item(key = "empty") { Hint("No tests or worksheets scheduled.") }
        items(plan.events.sortedBy { it.date }, key = { it.id }) { e ->
            val tag = if (e.who == ALL_GROUPS) "all" else plan.groups.firstOrNull { it.id == e.who }?.name ?: "group"
            // Warn when the date is not a lesson for anyone it applies to.
            val hit = plan.groups.any { (e.who == ALL_GROUPS || e.who == it.id) && e.date in lessonDays[it.id].orEmpty() }
            val parts = listOfNotNull(
                "${e.date.fmt()} · ${e.date.dayOfWeek.short()}",
                if (e.consumes) "takes the lesson" else "alongside the lesson",
                if (hit) null else "not a lesson day",
            )
            ListItemRow(e.name, parts.joinToString(" · "), tag, warn = !hit) { vm.removeEvent(e.id) }
        }
        item(key = "add") { AddButton("Add a test or worksheet") { adding = true } }
    }
    if (adding) AddEventDialog(plan, vm) { adding = false }
}

@Composable
private fun BookEditor(b: Book, vm: PacerViewModel) = Section(b.name.ifBlank { "Book" }) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommitField(b.name, { vm.renameBook(b.id, it) }, "Title", Modifier.weight(1f))
        RemoveButton(b.name) { vm.removeBook(b.id) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IntField(b.first, { vm.setFirstPage(b.id, it) }, "First page", Modifier.weight(1f))
        IntField(b.last, { vm.setLastPage(b.id, it) }, "Last page", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CommitField(b.skip, { vm.setSkip(b.id, it) }, "Skip pages", Modifier.weight(1f), placeholder = "8, 24–27")
        DateField(b.due, { vm.setDue(b.id, it) }, "Finish by", Modifier.weight(1f), clearable = true)
    }
    if (b.due != null) Hint("Only this book re-paces. The others keep their own rate.")
    val skipN = remember(b.skip, b.first, b.last) { parseRanges(b.skip).count { it in b.first..b.last } }
    Text(
        "pp. ${b.first}–${b.last} · ${maxOf(0, b.last - b.first + 1 - skipN)} teachable" +
            if (skipN > 0) " ($skipN skipped)" else "",
        style = MaterialTheme.typography.bodySmall, fontFamily = Mono, color = Pacer.colors.muted,
    )
}

@Composable
fun HomeworkScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues) {
    var adding by rememberSaveable { mutableStateOf(false) }
    EditList(plan, Screen.Homework, vm, padding) {
        item(key = "hint") {
            Hint("Pages set for homework leave the classroom sequence. The rest re-spread, so the pace eases and the books still finish together.")
        }
        if (plan.homework.isEmpty()) item(key = "empty") { Hint("Nothing assigned ahead yet.") }
        items(plan.homework.sortedBy { it.setOn ?: LocalDate.MIN }, key = { it.id }) { h ->
            val book = plan.books.firstOrNull { it.id == h.bookId }?.name ?: "book"
            val group = plan.groups.firstOrNull { it.id == h.groupId }?.name ?: "group"
            val n = h.to - h.from + 1
            val title = "$book pp. ${h.from}" + if (h.to != h.from) "–${h.to}" else ""
            val detail = "$n ${if (n == 1) "page" else "pages"}" + (h.setOn?.let { " · set ${it.fmt()}" } ?: "")
            ListItemRow(title, detail, group) { vm.removeHomework(h.id) }
        }
        item(key = "add") {
            if (plan.groups.isEmpty() || plan.books.isEmpty()) Hint("Add a group and a book first.")
            else AddButton("Add homework") { adding = true }
        }
    }
    if (adding) AddHomeworkDialog(plan, vm) { adding = false }
}

// ---- add dialogs ---------------------------------------------------------

private fun whoOptions(plan: Plan, allFirst: Boolean): List<Pair<String, String>> {
    val groups = plan.groups.map { it.id to it.name }
    val all = ALL_GROUPS to "All groups"
    return if (allFirst) listOf(all) + groups.map { it.first to "${it.second} only" } else groups + all
}

@Composable
private fun AddEventDialog(plan: Plan, vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var who by rememberSaveable { mutableStateOf(plan.groups.firstOrNull()?.id ?: ALL_GROUPS) }
    var consumes by rememberSaveable { mutableStateOf(true) }
    AddDialogFrame("Add a test or worksheet", date != null, { vm.addEvent(name.trim(), date!!, who, consumes) }, onDismiss) {
        CommitField(name, { name = it }, "Name", placeholder = "Unit 3 test")
        DateField(date, { date = it }, "Date")
        Picker("Class", whoOptions(plan, allFirst = false), who, { who = it })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = consumes, onCheckedChange = { consumes = it })
            Text("Takes up the whole lesson, so the coursebook pages move forward", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AddBookDialog(vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var first by rememberSaveable { mutableStateOf("1") }
    var last by rememberSaveable { mutableStateOf("") }
    val lastN = last.trim().toIntOrNull()?.takeIf { it > 0 }
    AddDialogFrame("Add a book", lastN != null, {
        vm.addBook(name.trim(), first.trim().toIntOrNull()?.takeIf { it > 0 } ?: 1, lastN!!)
    }, onDismiss) {
        CommitField(name, { name = it }, "Title", placeholder = "Companion")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CommitField(first, { first = it }, "First page", Modifier.weight(1f), numeric = true)
            CommitField(last, { last = it }, "Last page", Modifier.weight(1f), numeric = true)
        }
    }
}

@Composable
private fun AddHomeworkDialog(plan: Plan, vm: PacerViewModel, onDismiss: () -> Unit) {
    var group by rememberSaveable { mutableStateOf(plan.groups.firstOrNull()?.id) }
    var book by rememberSaveable { mutableStateOf(plan.books.firstOrNull()?.id) }
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var setOn by remember { mutableStateOf<LocalDate?>(null) }
    val fromN = from.trim().toIntOrNull()?.takeIf { it > 0 }
    val toN = to.trim().toIntOrNull()?.takeIf { it > 0 } ?: fromN
    AddDialogFrame("Add homework", fromN != null && group != null && book != null, {
        vm.addHomework(group!!, book!!, fromN!!, toN!!, setOn)
    }, onDismiss) {
        Picker("Group", plan.groups.map { it.id to it.name }, group, { group = it })
        Picker("Book", plan.books.map { it.id to it.name }, book, { book = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CommitField(from, { from = it }, "From p.", Modifier.weight(1f), numeric = true)
            CommitField(to, { to = it }, "To p.", Modifier.weight(1f), numeric = true)
        }
        DateField(setOn, { setOn = it }, "Set on (optional)", clearable = true)
    }
}
