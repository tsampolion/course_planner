package gr.edusuite.pacer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.suggestYear
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate

/** "3 classes", "1 class", or [none] for zero. */
fun count(n: Int, one: String, many: String = one + "s", none: String = "None yet") =
    if (n == 0) none else "$n ${if (n == 1) one else many}"

/** "9 Sep 2026 – 31 May 2027", or a prompt when either date is missing. */
fun SchoolYear.dates(): String {
    val s = start
    val e = end
    return if (s != null && e != null) "${s.fmt()} – ${e.fmt()}" else "Dates not set"
}

/**
 * The start screen: the logo at the top, the school-year picker in the middle, and Settings at the bottom right.
 * With [welcome], the welcome guide opens over it.
 */
@Composable
fun HomeScreen(data: AppData, vm: PacerViewModel, padding: PaddingValues, welcome: Boolean, onDeleted: (String) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    // The year being renamed or deleted, by id.
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val years = remember(data.years) { data.years.sortedByDescending { it.start } }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = Space.l, end = Space.l, top = padding.calculateTopPadding() + Space.xxl + Space.s, bottom = padding.calculateBottomPadding() + 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Logo()
            Spacer(Modifier.height(Space.l))
            Text(
                APP_NAME,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            // The picker takes the space left and sits in its middle, scrolling once there are many years.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Eyebrow("School year")
                    if (years.isEmpty()) {
                        GuideCard(
                            "Create your school year",
                            "Give it its first and last teaching day. The Greek public holidays can come in as days off, " +
                                "and you can add your school's own days off later.",
                        )
                    }
                    years.forEach { y ->
                        YearRow(y, onOpen = { vm.selectYear(y.id) }, onRename = { renaming = y.id }, onDelete = { deleting = y.id })
                    }
                    AddButton("New school year") { adding = true }
                }
            }
        }
        TextButton(
            onClick = { vm.open(Screen.Settings) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Space.m, bottom = padding.calculateBottomPadding() + Space.m)
                .size(56.dp)
                .semantics { contentDescription = "Settings" },
            contentPadding = PaddingValues(0.dp),
        ) {
            // U+FE0E asks for the text glyph rather than an emoji.
            Text("⚙︎", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }
    }

    if (welcome) WelcomeDialog(onStart = { adding = true }, onDismiss = vm::setWelcomeSeen)
    if (adding) NewYearDialog(data.years, vm) { adding = false }
    data.years.firstOrNull { it.id == renaming }?.let { y -> RenameYearDialog(y, vm) { renaming = null } }
    data.years.firstOrNull { it.id == deleting }?.let { y ->
        val name = y.name.ifBlank { "School year" }
        ConfirmDelete(name, YEAR_DELETE_DETAIL, { vm.deleteYear(y.id); onDeleted(name) }) { deleting = null }
    }
}

/**
 * A year in the picker: tap to open it. The ⋮ button, or a long-press anywhere on the row,
 * offers Rename and Delete.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun YearRow(y: SchoolYear, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val name = y.name.ifBlank { "School year" }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onOpen, onLongClick = { menu = true }, onLongClickLabel = "Rename or delete"),
        shape = MaterialTheme.shapes.medium,
        color = Pacer.colors.surface,
    ) {
        Row(Modifier.padding(start = Space.l, top = Space.xs, bottom = Space.xs), verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "More options for $name" },
                ) { Text("⋮", style = MaterialTheme.typography.titleLarge, color = Pacer.colors.muted) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Delete", color = Pacer.colors.warn) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun RenameYearDialog(y: SchoolYear, vm: PacerViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(y.name) }
    AddDialogFrame("Rename school year", name.isNotBlank(), { vm.renameYear(y.id, name.trim()) }, onDismiss, confirm = "Save") {
        CommitField(name, { name = it }, "Name")
    }
}

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

/** The launcher icon, drawn from the same artwork: the open book on the teal tile. */
@Composable
private fun Logo() {
    Box(
        // The logo tile's corners are a quarter of its size (style guide §4), the one shape outside MaterialTheme.shapes.
        Modifier.size(88.dp).clip(RoundedCornerShape(22.dp)).background(colorResource(R.color.launcher_tile)),
        contentAlignment = Alignment.Center,
    ) {
        // The artwork sits in the middle of a 108dp canvas; scaling it up crops the empty margin.
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = "$APP_NAME logo", modifier = Modifier.requiredSize(132.dp))
    }
}
