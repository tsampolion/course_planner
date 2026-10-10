package com.coursebook.pacer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coursebook.pacer.engine.GroupReport
import com.coursebook.pacer.engine.asTsv
import com.coursebook.pacer.engine.lessonsFor
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.ui.theme.Pacer
import kotlinx.coroutines.launch

private fun Screen.title(plan: Plan): String = when (this) {
    Screen.Home -> plan.name
    is Screen.Group -> plan.groups.firstOrNull { it.id == id }?.name?.ifBlank { "Group" } ?: "Group"
    Screen.Term -> "Term"
    Screen.Groups -> "Groups"
    Screen.DaysOff -> "Days off"
    Screen.Tests -> "Tests & worksheets"
    Screen.Books -> "Books"
    Screen.Homework -> "Homework already assigned"
    Screen.Settings -> "Settings"
}

/** Keys each screen's saved state (scroll position, open dialogs) while it stays on the back stack. */
private fun Screen.key(): String = toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacerApp(vm: PacerViewModel) {
    val ui by vm.state.collectAsState()
    val settings by vm.settings.collectAsState()
    val plan = ui.data.plan
    val screen = ui.screen
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) } }

    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = ui.stack.size > 1) { vm.back() }

    // Screens keep their scroll position while they are on the stack, and forget it once popped.
    val saved = rememberSaveableStateHolder()
    val live = remember { HashSet<String>() }
    LaunchedEffect(ui.stack) {
        val keys = ui.stack.mapTo(HashSet()) { it.key() }
        (live - keys).forEach(saved::removeState)
        live.retainAll(keys)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (screen == Screen.Home) {
                HomeBar(ui, vm, onPrint = { PrintSheet.print(context, plan) }, onRename = { renaming = true }, onDelete = { confirmDelete = true })
            } else {
                ScreenBar(screen, plan, vm, undoDepth = if (screen == Screen.Settings) null else ui.undoDepth)
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "screen",
        ) { s ->
            live += s.key()
            saved.SaveableStateProvider(s.key()) {
                ScreenContent(s, plan, settings, vm, padding, toast, context)
            }
        }
    }

    if (renaming) {
        var name by rememberSaveable { mutableStateOf(plan.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename plan") },
            text = { CommitField(name, { name = it }, "Plan name") },
            confirmButton = { TextButton(onClick = { vm.renamePlan(name.trim().ifEmpty { plan.name }); renaming = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${plan.name}”?") },
            text = { Text("Undo brings it back until you close the app.") },
            confirmButton = { TextButton(onClick = { vm.deletePlan(); confirmDelete = false }) { Text("Delete", color = Pacer.colors.warn) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ScreenContent(
    s: Screen,
    plan: Plan,
    settings: com.coursebook.pacer.model.Settings,
    vm: PacerViewModel,
    padding: PaddingValues,
    toast: (String) -> Unit,
    context: Context,
) {
    when (s) {
        Screen.Home -> HomeScreen(plan, vm, padding)
        is Screen.Group -> {
            // Null for a moment while a deleted group's screen fades out.
            val group = plan.groups.firstOrNull { it.id == s.id } ?: return
            val report = remember(plan, group) { GroupReport(plan, group) }
            GroupScreen(
                report, vm, padding,
                onCopy = {
                    copy(context, report.asTsv())
                    // Android 13+ shows its own copy confirmation.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast("Copied. Paste into a spreadsheet or Word.")
                },
                onShare = { share(context, "${plan.name} — ${report.group.name}", report.asTsv()) },
            )
        }
        Screen.Term -> TermScreen(plan, vm, padding)
        Screen.Groups -> GroupsScreen(plan, vm, padding)
        Screen.DaysOff -> DaysOffScreen(plan, vm, padding, toast)
        Screen.Tests -> TestsScreen(plan, vm, padding)
        Screen.Books -> BooksScreen(plan, vm, padding)
        Screen.Homework -> HomeworkScreen(plan, vm, padding)
        Screen.Settings -> SettingsScreen(settings, vm, padding)
    }
}

@Composable
private fun UndoButton(vm: PacerViewModel, depth: Int) {
    TextButton(onClick = vm::undo, enabled = depth > 0) {
        Text(if (depth > 0) "Undo ($depth)" else "Undo")
    }
}

/** Home's bar: the plan switcher as the title, undo, and the plan and app menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeBar(ui: UiState, vm: PacerViewModel, onPrint: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val data = ui.data
    val plan = data.plan
    var plans by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = {
            Box {
                TextButton(onClick = { plans = true }) {
                    Column {
                        Text("Coursebook Pacer", fontSize = 11.sp, color = Pacer.colors.muted)
                        Text(
                            "${plan.name} ▾", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.widthIn(max = 220.dp),
                        )
                    }
                }
                DropdownMenu(expanded = plans, onDismissRequest = { plans = false }) {
                    data.plans.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name, fontWeight = if (p.id == plan.id) FontWeight.SemiBold else FontWeight.Normal) },
                            onClick = { plans = false; vm.selectPlan(p.id) },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("New plan") }, onClick = { plans = false; vm.newPlan() })
                }
            }
        },
        actions = {
            UndoButton(vm, ui.undoDepth)
            Box {
                TextButton(onClick = { menu = true }, modifier = Modifier.semantics { contentDescription = "More options" }) {
                    Text("⋮", fontSize = 20.sp)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Print or save PDF") }, onClick = { menu = false; onPrint() })
                    DropdownMenuItem(text = { Text("Rename plan") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("New plan") }, onClick = { menu = false; vm.newPlan() })
                    DropdownMenuItem(
                        text = { Text("Delete plan") },
                        enabled = data.plans.size > 1,
                        onClick = { menu = false; onDelete() },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Settings") }, onClick = { menu = false; vm.open(Screen.Settings) })
                }
            }
        },
    )
}

/**
 * Every other screen's bar: back, the plan name over the screen title, and undo where plan edits happen
 * ([undoDepth] null hides it). On a group, the title switches between groups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenBar(screen: Screen, plan: Plan, vm: PacerViewModel, undoDepth: Int?) {
    var groups by remember { mutableStateOf(false) }
    val switchable = screen is Screen.Group && plan.groups.size > 1
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
            TextButton(onClick = { vm.back() }, modifier = Modifier.semantics { contentDescription = "Back" }) {
                Text("←", fontSize = 22.sp)
            }
        },
        title = {
            Box {
                val heading = @Composable {
                    Column {
                        Text(if (screen == Screen.Settings) "Coursebook Pacer" else plan.name, fontSize = 11.sp, color = Pacer.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            screen.title(plan) + if (switchable) " ▾" else "",
                            fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                if (switchable) TextButton(onClick = { groups = true }) { heading() } else heading()
                DropdownMenu(expanded = groups, onDismissRequest = { groups = false }) {
                    plan.groups.forEach { g ->
                        val count = remember(plan, g) { plan.lessonsFor(g).size }
                        DropdownMenuItem(
                            text = {
                                Row {
                                    Text(g.name.ifBlank { "Group" }, fontWeight = if (screen == Screen.Group(g.id)) FontWeight.SemiBold else FontWeight.Normal)
                                    Text("  $count", fontFamily = Mono, color = Pacer.colors.muted)
                                }
                            },
                            onClick = { groups = false; vm.open(Screen.Group(g.id)) },
                        )
                    }
                }
            }
        },
        actions = { if (undoDepth != null) UndoButton(vm, undoDepth) },
    )
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Schedule", text))
}

private fun share(context: Context, title: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, title))
}
