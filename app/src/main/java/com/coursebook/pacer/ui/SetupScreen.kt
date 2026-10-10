package com.coursebook.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.unit.dp
import com.coursebook.pacer.engine.WEEK
import com.coursebook.pacer.engine.fmt
import com.coursebook.pacer.engine.lessonsFor
import com.coursebook.pacer.engine.parseRanges
import com.coursebook.pacer.engine.plain
import com.coursebook.pacer.engine.problems
import com.coursebook.pacer.engine.short
import com.coursebook.pacer.model.ALL_GROUPS
import com.coursebook.pacer.model.Book
import com.coursebook.pacer.model.Group
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.ui.theme.Pacer
import java.time.LocalDate

private enum class AddDialog { CLOSURE, EVENT, BOOK, HOMEWORK }

@Composable
fun SetupScreen(plan: Plan, vm: PacerViewModel, padding: PaddingValues, toast: (String) -> Unit) {
    var dialog by rememberSaveable { mutableStateOf<AddDialog?>(null) }
    val problems = remember(plan) { plan.problems() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(problems) { ErrorBox(it) }
        item(key = "term") { TermSection(plan, vm) }
        item(key = "groups") { GroupsSection(plan, vm) }
        item(key = "closures") {
            ClosuresSection(plan, vm, onAdd = { dialog = AddDialog.CLOSURE }, onGreek = {
                val n = vm.addGreekHolidays()
                toast(if (n > 0) "$n holidays added" else "Already there")
            })
        }
        item(key = "events") { EventsSection(plan, vm) { dialog = AddDialog.EVENT } }
        item(key = "books") { BooksSection(plan, vm) { dialog = AddDialog.BOOK } }
        item(key = "homework") { HomeworkSection(plan, vm) { dialog = AddDialog.HOMEWORK } }
        item(key = "foot") {
            Hint(
                "Pages are paced per book, never pooled, so each title runs at its own rate and all of them land on " +
                    "the final lesson. Greek holidays are computed from Orthodox Easter for every year the term touches. " +
                    "Check them against your school's calendar before relying on them.",
            )
        }
    }

    when (dialog) {
        AddDialog.CLOSURE -> AddClosureDialog(plan, vm) { dialog = null }
        AddDialog.EVENT -> AddEventDialog(plan, vm) { dialog = null }
        AddDialog.BOOK -> AddBookDialog(vm) { dialog = null }
        AddDialog.HOMEWORK -> AddHomeworkDialog(plan, vm) { dialog = null }
        null -> Unit
    }
}

@Composable
private fun TermSection(plan: Plan, vm: PacerViewModel) = Section("Term") {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DateField(plan.start, vm::setStart, "Term starts", Modifier.weight(1f))
        DateField(plan.end, vm::setEnd, "Term ends", Modifier.weight(1f))
    }
    Hint("Each group tracks its own next lesson below. The term start is only the default.")
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

@Composable
private fun GroupsSection(plan: Plan, vm: PacerViewModel) = Section("Groups") {
    plan.groups.forEach { g -> GroupEditor(plan, g, vm) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = vm::addGroup) { Text("Add group") }
        OutlinedButton(onClick = vm::resetPositions) { Text("Reset all positions") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupEditor(plan: Plan, g: Group, vm: PacerViewModel) {
    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }
    }
}

@Composable
private fun ClosuresSection(plan: Plan, vm: PacerViewModel, onAdd: () -> Unit, onGreek: () -> Unit) = Section("Days off") {
    if (plan.closures.isEmpty()) Hint("No days off yet.")
    plan.closures.sortedBy { it.from }.forEach { c ->
        val tag = if (c.who == ALL_GROUPS) null else plan.groups.firstOrNull { it.id == c.who }?.name ?: "one group"
        val detail = if (c.to == c.from) "${c.from.fmt()} · ${c.from.dayOfWeek.short()}" else "${c.from.fmt()} – ${c.to.fmt()}"
        ListItemRow(c.label, detail, tag) { vm.removeClosure(c.id) }
    }
    AddButton("Add a day off", onAdd)
    TextButton(onClick = onGreek) { Text("Add Greek public holidays for this term") }
}

@Composable
private fun EventsSection(plan: Plan, vm: PacerViewModel, onAdd: () -> Unit) = Section("Tests & worksheets") {
    val lessonDays = remember(plan) { plan.groups.associate { it.id to plan.lessonsFor(it).toHashSet() } }
    if (plan.events.isEmpty()) Hint("No tests or worksheets scheduled.")
    plan.events.sortedBy { it.date }.forEach { e ->
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
    AddButton("Add a test or worksheet", onAdd)
}

@Composable
private fun BooksSection(plan: Plan, vm: PacerViewModel, onAdd: () -> Unit) = Section("Books") {
    Hint("Skipped pages are structural and apply to every group. Use homework for pages one class covers outside the lesson.")
    if (plan.books.isEmpty()) Hint("No books yet.")
    plan.books.forEach { b -> BookEditor(b, vm) }
    AddButton("Add a book", onAdd)
}

@Composable
private fun BookEditor(b: Book, vm: PacerViewModel) {
    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}

@Composable
private fun HomeworkSection(plan: Plan, vm: PacerViewModel, onAdd: () -> Unit) = Section("Homework already assigned") {
    Hint("Pages set for homework leave the classroom sequence. The rest re-spread, so the pace eases and the books still finish together.")
    if (plan.homework.isEmpty()) Hint("Nothing assigned ahead yet.")
    plan.homework.sortedBy { it.setOn ?: LocalDate.MIN }.forEach { h ->
        val book = plan.books.firstOrNull { it.id == h.bookId }?.name ?: "book"
        val group = plan.groups.firstOrNull { it.id == h.groupId }?.name ?: "group"
        val n = h.to - h.from + 1
        val title = "$book pp. ${h.from}" + if (h.to != h.from) "–${h.to}" else ""
        val detail = "$n ${if (n == 1) "page" else "pages"}" + (h.setOn?.let { " · set ${it.fmt()}" } ?: "")
        ListItemRow(title, detail, group) { vm.removeHomework(h.id) }
    }
    AddButton("Add homework", onAdd)
}

// ---- add dialogs ---------------------------------------------------------

@Composable
private fun AddDialogFrame(title: String, canAdd: Boolean, onAdd: () -> Unit, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } },
        confirmButton = { TextButton(onClick = { onAdd(); onDismiss() }, enabled = canAdd) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun whoOptions(plan: Plan, allFirst: Boolean): List<Pair<String, String>> {
    val groups = plan.groups.map { it.id to it.name }
    val all = ALL_GROUPS to "All groups"
    return if (allFirst) listOf(all) + groups.map { it.first to "${it.second} only" } else groups + all
}

@Composable
private fun AddClosureDialog(plan: Plan, vm: PacerViewModel, onDismiss: () -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var from by remember { mutableStateOf<LocalDate?>(null) }
    var to by remember { mutableStateOf<LocalDate?>(null) }
    var who by rememberSaveable { mutableStateOf(ALL_GROUPS) }
    AddDialogFrame("Add a day off", from != null, { vm.addClosure(label.trim(), from!!, to, who) }, onDismiss) {
        CommitField(label, { label = it }, "Reason", placeholder = "Christmas break")
        DateField(from, { from = it }, "From")
        DateField(to, { to = it }, "To (optional)", clearable = true)
        Picker("Applies to", whoOptions(plan, allFirst = true), who, { who = it })
    }
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
