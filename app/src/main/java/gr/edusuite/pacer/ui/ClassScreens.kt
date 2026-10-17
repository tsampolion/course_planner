package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.fmtShort
import gr.edusuite.pacer.engine.WEEK
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.lessonsFor
import gr.edusuite.pacer.engine.position
import gr.edusuite.pacer.engine.parseRanges
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.ui.theme.Mono
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
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
fun ClassScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues, undoToast: (String) -> Unit) {
    var addingBook by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf<String?>(null) }
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
                LinkRow("Name", plan.name) { settings = "name" }
                LinkRow("Lessons", "${plan.mins} min · ${plan.pace} pages per lesson") { settings = "lessons" }
            }
        }

        item(key = "books") {
            Section("Books", trailing = { TertiaryButton("Add", { addingBook = true }) }) {
                Hint("Each book is paced on its own. Skipped pages apply to every group; homework applies to one group.")
                if (plan.books.isEmpty()) Hint("No books yet.")
                TileGrid(plan.books) { book, modifier -> BookEditor(book, vm, undoToast, modifier) }
            }
        }
        item(key = "groups") {
            Section("Groups", trailing = { TertiaryButton("Add", vm::addGroup) }) {
                if (plan.groups.isEmpty()) Hint("Add a group for each set of students taught from these books.")
                TileGrid(plan.groups) { group, modifier -> GroupEditor(plan, group, vm, undoToast, modifier) }
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
            MoreOptions {
                TertiaryButton("Reset all positions", {
                    vm.resetPositions(); undoToast("Positions reset")
                }, enabled = plan.groups.isNotEmpty(), color = Pacer.colors.warn)
                TertiaryButton("Delete this class", {
                    val name = plan.name.ifBlank { "Class" }
                    vm.deleteClass(); undoToast("Deleted “$name”")
                }, color = Pacer.colors.warn)
            }
        }
    }
    if (settings != null) SheetFrame("Class settings", "Done", true, {}, { settings = null }) {
        when (settings) {
            "name" -> CommitField(plan.name, vm::renameClass, "Name", Modifier.fillMaxWidth())
            "lessons" -> {
                IntField(plan.mins, vm::setMins, "Lesson length (min)", Modifier.fillMaxWidth(), max = Settings.MAX_MINS)
                PaceField(plan.pace, vm::setPace, Modifier.fillMaxWidth())
                Hint("Pages per lesson is the benchmark the verdict measures against; it does not drive the schedule.")
            }
        }
    }

    if (addingBook) AddBookSheet(vm) { addingBook = false }
}

/** The problems ([Plan.problems]) that the class screen's "Next step" card covers. */
private const val NO_BOOKS = "Add at least one book."
private const val NO_GROUPS = "Add at least one group."

@Composable
private fun GroupEditor(plan: Plan, g: Group, vm: PacerViewModel, undoToast: (String) -> Unit, modifier: Modifier = Modifier) {
    val name = g.name.ifBlank { "Group" }
    // Its ticked lessons, homework, and the days off and tests aimed only at it go too; Undo brings them back.
    var editing by rememberSaveable(g.id) { mutableStateOf(false) }
    val report = remember(plan, g) { GroupReport(plan, g) }
    Tile(Pacer.colors.tileLilac, modifier, onClick = { vm.open(Screen.Group(g.id)) }) {
        Text(name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text("${report.rows.size} lessons left" + (report.rows.firstOrNull()?.date?.let { " · next ${it.dayOfWeek.short()} ${it.fmtShort()}" } ?: ""), style = MaterialTheme.typography.bodyMedium)
        Text(g.days.sortedBy { it.value }.joinToString(" · ") { it.short() }, color = Pacer.colors.onTileMuted)
        SecondaryButton("Edit", { editing = true })
    }
    if (editing) SheetFrame(name, "Done", true, {}, { editing = false }) {
        GroupFields(plan, g, vm) { editing = false; vm.removeGroup(g.id); undoToast("Removed “$name”") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.GroupFields(plan: Plan, g: Group, vm: PacerViewModel, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommitField(g.name, { vm.renameGroup(g.id, it) }, "Name", Modifier.weight(1f))

    }
    // Seven chips need about 330dp; the row wraps its last chips on very narrow phones.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        WEEK.forEach { day ->
            FilterChip(
                selected = day in g.days,
                onClick = { vm.toggleDay(g.id, day) },
                label = { Text(day.short()) },
                shape = MaterialTheme.shapes.extraSmall,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
    val lessons = remember(plan, g) { plan.lessonsFor(g) }
    // The first lesson still to teach; once lessons are ticked, the stored date is the day after the last one taught.
    DateField(lessons.firstOrNull() ?: g.from ?: plan.start, { vm.setNextLesson(g.id, it) }, "Next lesson", Modifier.fillMaxWidth())
    MoreOptions { TertiaryButton("Remove group", onRemove, color = Pacer.colors.warn) }
    val count = lessons.size
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Hint("$count ${if (count == 1) "lesson" else "lessons"} left")
        TextButton(onClick = { vm.open(Screen.Group(g.id)) }) { Text("Open schedule ›") }
    }
}

@Composable
fun TestsScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues, undoToast: (String) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val lessonDays = remember(plan) { plan.groups.associate { g -> g.id to (plan.lessonsFor(g) + g.done.map { it.date }).toHashSet() } }
    EditList(plan, Screen.Tests, vm, padding) {
        if (plan.events.isEmpty()) item(key = "empty") {
            EmptyState("✎", "No tests yet", "Plan a test or worksheet for your next lesson.", "Add a test") { adding = true }
        }
        item(key = "upcoming") {
            Section("Upcoming") {
                val upcoming = plan.events.filter { it.date >= today }.sortedBy { it.date }
                if (upcoming.isEmpty() && plan.events.isNotEmpty()) Hint("No upcoming tests.")
                upcoming.forEach { TestRow(plan, it, lessonDays, vm, undoToast) }
            }
        }
        val past = plan.events.filter { it.date < today }.sortedByDescending { it.date }
        if (past.isNotEmpty()) item(key = "past") {
            MoreOptions("Past tests (${past.size})") { past.forEach { TestRow(plan, it, lessonDays, vm, undoToast) } }
        }
        item(key = "add") { PrimaryButton("Add a test", { adding = true }) }
    }
    if (adding) AddEventDialog(plan, vm) { adding = false }
}

@Composable
private fun TestRow(plan: Plan, event: TestEvent, lessonDays: Map<String, Set<LocalDate>>, vm: PacerViewModel, undoToast: (String) -> Unit) {
    val title = event.name.ifBlank { "Test" }
    val group = if (event.who == ALL_GROUPS) "All groups" else plan.groups.firstOrNull { it.id == event.who }?.name ?: "Group"
    val hit = plan.groups.any { (event.who == ALL_GROUPS || event.who == it.id) && event.date in lessonDays[it.id].orEmpty() }
    val remove = { vm.removeEvent(event.id); undoToast("Removed “$title”") }
    SwipeRow(title, remove) {
        ListGroup {
            Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                DateBadge(event.date)
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("${event.date.dayOfWeek.short()} ${event.date.fmtShort()} · " + if (event.consumes) "takes the lesson" else "alongside the lesson", style = MaterialTheme.typography.bodyMedium)
                Tag(group)
                if (!hit) Tag("Not a lesson day", warn = true)
                TertiaryButton("Remove test", remove)
            }
        }
    }
}

@Composable
private fun BookEditor(b: Book, vm: PacerViewModel, undoToast: (String) -> Unit, modifier: Modifier = Modifier) {
    val name = b.name.ifBlank { "Book" }
    // Its positions, homework and the pages ticked lessons recorded in it go too; Undo brings them back.
    var editing by rememberSaveable(b.id) { mutableStateOf(false) }
    val skipped = remember(b) { parseRanges(b.skip).count { it in b.first..b.last } }
    Tile(Pacer.colors.tileSky, modifier, onClick = { editing = true }) {
        Text(name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text("pp. ${b.first}–${b.last}", style = MaterialTheme.typography.titleMedium, fontFamily = Mono)
        Text("${maxOf(0, b.last - b.first + 1 - skipped)} teachable pages", color = Pacer.colors.onTileMuted)
        if (b.skip.isNotBlank()) Tag("$skipped skipped", onTile = true)
        b.due?.let { Tag("by ${it.fmtShort()}", onTile = true) }
    }
    if (editing) SheetFrame(name, "Done", true, {}, { editing = false }) {
        BookFields(b, vm) { editing = false; vm.removeBook(b.id); undoToast("Removed “$name”") }
    }
}

@Composable
private fun ColumnScope.BookFields(b: Book, vm: PacerViewModel, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommitField(b.name, { vm.renameBook(b.id, it) }, "Title", Modifier.weight(1f))

    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
        IntField(b.first, { vm.setFirstPage(b.id, it) }, "First page", Modifier.fillMaxWidth(), max = MAX_PAGE)
        IntField(b.last, { vm.setLastPage(b.id, it) }, "Last page", Modifier.fillMaxWidth(), max = MAX_PAGE)
    }
    MoreOptions {
        CommitField(b.skip, { vm.setSkip(b.id, it) }, "Skip pages", Modifier.fillMaxWidth(), placeholder = "8, 24–27")
        DateField(b.due, { vm.setDue(b.id, it) }, "Finish by", Modifier.fillMaxWidth(), clearable = true)
        if (b.due != null) Hint("Only this book re-paces. The others keep their own rate.")
        TertiaryButton("Remove book", onRemove, color = Pacer.colors.warn)
    }
    val skipN = remember(b.skip, b.first, b.last) { parseRanges(b.skip).count { it in b.first..b.last } }
    Text(
        "pp. ${b.first}–${b.last} · ${maxOf(0, b.last - b.first + 1 - skipN)} teachable" +
            if (skipN > 0) " ($skipN skipped)" else "",
        style = MaterialTheme.typography.bodySmall, fontFamily = Mono, color = Pacer.colors.muted,
    )
}

@Composable
fun HomeworkScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues, undoToast: (String) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    EditList(plan, Screen.Homework, vm, padding) {
        item(key = "hint") {
            Hint("Pages set for homework leave the classroom sequence. The rest re-spread, so the pace eases and the books still finish together.")
        }
        if (plan.homework.isEmpty()) item(key = "empty") {
            EmptyState("+", "No homework assigned", "Assign pages to ease the classroom pace.",
                if (plan.groups.isNotEmpty() && plan.books.isNotEmpty()) "Add homework" else null,
                if (plan.groups.isNotEmpty() && plan.books.isNotEmpty()) ({ adding = true }) else null)
        }
        plan.homework.groupBy { it.groupId }.forEach { (groupId, assignments) ->
            item(key = "group:$groupId") {
                Section(plan.groups.firstOrNull { it.id == groupId }?.name ?: "Group") {
                    assignments.sortedBy { it.setOn ?: LocalDate.MIN }.forEach { h ->
                        val book = plan.books.firstOrNull { it.id == h.bookId }?.name ?: "Book"
                        val n = h.to - h.from + 1
                        val title = "$book pp. ${h.from}" + if (h.to != h.from) "–${h.to}" else ""
                        val detail = "$n ${if (n == 1) "page" else "pages"}" + (h.setOn?.let { " · set ${it.fmtShort()}" } ?: "")
                        val remove = { vm.removeHomework(h.id); undoToast("Removed “$title”") }
                        SwipeRow(title, remove) { ListItemRow(title, detail, onRemove = remove) }
                    }
                }
            }
        }
        item(key = "add") {
            if (plan.groups.isEmpty() || plan.books.isEmpty()) Hint("Add a group and a book first.")
            else PrimaryButton("Add homework", { adding = true })
        }
    }
    if (adding) AddHomeworkDialog(plan, vm) { adding = false }
}

// ---- add dialogs and sheets ---------------------------------------------------------

private fun whoOptions(plan: Plan, allFirst: Boolean): List<Pair<String, String>> {
    val groups = plan.groups.map { it.id to it.name }
    val all = ALL_GROUPS to "All groups"
    return if (allFirst) listOf(all) + groups.map { it.first to "${it.second} only" } else groups + all
}

@Composable
private fun AddEventDialog(plan: Plan, vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(plan.groups.firstOrNull()?.let { GroupReport(plan, it).rows.firstOrNull()?.date }?.toString()) }
    val date = dateText?.let(LocalDate::parse)
    var who by rememberSaveable { mutableStateOf(plan.groups.firstOrNull()?.id ?: ALL_GROUPS) }
    var consumes by rememberSaveable { mutableStateOf(true) }
    SheetFrame("Add a test or worksheet", "Add", date != null, { vm.addEvent(name.trim(), date!!, who, consumes) }, onDismiss) {
        CommitField(name, { name = it }, "Name", placeholder = "Unit 3 test")
        Picker("Group", whoOptions(plan, allFirst = false), who, { selected ->
            who = selected
            dateText = plan.groups.filter { selected == ALL_GROUPS || it.id == selected }
                .mapNotNull { GroupReport(plan, it).rows.firstOrNull()?.date }.minOrNull()?.toString()
        })
        DateField(date, { dateText = it?.toString() }, "Date")
        MoreOptions {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                Text("Takes the whole lesson", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = consumes, onCheckedChange = { consumes = it })
            }
        }
    }
}

@Composable
private fun AddBookSheet(vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var first by rememberSaveable { mutableStateOf("1") }
    var last by rememberSaveable { mutableStateOf("") }
    val lastN = last.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE }
    SheetFrame("Add a book", "Add", lastN != null, {
        vm.addBook(name.trim(), first.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE } ?: 1, lastN!!)
    }, onDismiss) {
        CommitField(name, { name = it }, "Title", Modifier.fillMaxWidth(), placeholder = "Companion")
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            CommitField(first, { first = it }, "First page", Modifier.fillMaxWidth(), numeric = true)
            CommitField(last, { last = it }, "Last page", Modifier.fillMaxWidth(), numeric = true)
        }
    }
}

@Composable
private fun AddHomeworkDialog(plan: Plan, vm: PacerViewModel, onDismiss: () -> Unit) {
    var group by rememberSaveable { mutableStateOf(plan.groups.firstOrNull()?.id) }
    var book by rememberSaveable { mutableStateOf(plan.books.firstOrNull()?.id) }
    fun currentPage(): String {
        val g = plan.groups.firstOrNull { it.id == group }
        val b = plan.books.firstOrNull { it.id == book }
        return if (g != null && b != null) position(g, b).toString() else ""
    }
    var from by rememberSaveable { mutableStateOf(currentPage()) }
    var to by rememberSaveable { mutableStateOf("") }
    var setOnText by rememberSaveable { mutableStateOf<String?>(LocalDate.now().toString()) }
    val setOn = setOnText?.let(LocalDate::parse)
    val fromN = from.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE }
    val toN = to.trim().toIntOrNull()?.takeIf { it in 1..MAX_PAGE } ?: fromN
    SheetFrame("Add homework", "Add", fromN != null && group != null && book != null, {
        vm.addHomework(group!!, book!!, fromN!!, toN!!, setOn)
    }, onDismiss) {
        Picker("Group", plan.groups.map { it.id to it.name }, group, { group = it; from = currentPage(); to = "" })
        Picker("Book", plan.books.map { it.id to it.name }, book, { book = it; from = currentPage(); to = "" })
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            CommitField(from, { from = it }, "From p.", Modifier.fillMaxWidth(), numeric = true)
            CommitField(to, { to = it }, "To p.", Modifier.fillMaxWidth(), numeric = true)
        }
        MoreOptions { DateField(setOn, { setOnText = it?.toString() }, "Set on (optional)", clearable = true) }
    }
}
