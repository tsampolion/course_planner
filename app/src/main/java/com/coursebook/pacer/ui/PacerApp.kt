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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.coursebook.pacer.engine.GroupReport
import com.coursebook.pacer.engine.asTsv
import com.coursebook.pacer.engine.lessonsFor
import com.coursebook.pacer.model.AppData
import com.coursebook.pacer.ui.theme.Pacer
import kotlinx.coroutines.launch

/** The app's name as the screens show it; the launcher label lives in strings.xml. */
const val APP_NAME = "Course Pacer"

private fun Screen.title(d: AppData): String = when (this) {
    Screen.Home -> APP_NAME
    Screen.Settings -> "Settings"
    Screen.Year -> d.year?.name?.ifBlank { null } ?: "School year"
    Screen.YearSettings -> "School year settings"
    Screen.Overview -> "Overview"
    Screen.Classes -> "Classes"
    Screen.Class -> d.schoolClass?.name?.ifBlank { null } ?: "Class"
    Screen.Tests -> "Tests & worksheets"
    Screen.Homework -> "Homework already assigned"
    is Screen.Group -> d.schoolClass?.groups?.firstOrNull { it.id == id }?.name?.ifBlank { null } ?: "Group"
}

/** The small line above the title: where this screen sits. */
private fun Screen.context(d: AppData): String = when (this) {
    Screen.Home, Screen.Settings -> APP_NAME
    Screen.Year -> "School year"
    Screen.YearSettings, Screen.Overview, Screen.Classes, Screen.Class -> d.year?.name.orEmpty()
    Screen.Tests, Screen.Homework, is Screen.Group -> listOfNotNull(d.year?.name, d.schoolClass?.name).joinToString(" · ")
}

/** Keys each screen's saved state (scroll position, open dialogs) while it stays on the back stack. */
private fun Screen.key(): String = toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacerApp(vm: PacerViewModel) {
    val ui by vm.state.collectAsState()
    val settings by vm.settings.collectAsState()
    val screen = ui.screen
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) } }
    // Deleting a year lands on Home, which has no undo button, so the snackbar offers it once.
    val undoToast: (String) -> Unit = { msg ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(msg, actionLabel = "Undo", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) vm.undo()
        }
    }

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
            // Home is just the logo, the year picker and Settings, with no bar.
            if (screen != Screen.Home) ScreenBar(screen, ui.data, vm, undoDepth = if (screen == Screen.Settings) null else ui.undoDepth)
        },
    ) { padding ->
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "screen",
        ) { s ->
            live += s.key()
            saved.SaveableStateProvider(s.key()) {
                ScreenContent(s, ui.data, settings, vm, padding, toast, undoToast, context)
            }
        }
    }
}

@Composable
private fun ScreenContent(
    s: Screen,
    data: AppData,
    settings: com.coursebook.pacer.model.Settings,
    vm: PacerViewModel,
    padding: PaddingValues,
    toast: (String) -> Unit,
    undoToast: (String) -> Unit,
    context: Context,
) {
    // Each is null for a moment while the screen of something just deleted fades out.
    val year = data.year
    val plan = data.plan
    when (s) {
        Screen.Home -> HomeScreen(data, vm, padding, onDeleted = { undoToast("Deleted “$it”") })
        Screen.Settings -> SettingsScreen(settings, data.years.size, vm, padding, toast, undoToast)
        Screen.Year -> YearScreen(year ?: return, vm, padding)
        Screen.YearSettings -> YearSettingsScreen(year ?: return, vm, padding, toast, onDeleted = { undoToast("Deleted “$it”") })
        Screen.Overview -> OverviewScreen(year ?: return, vm, padding)
        Screen.Classes -> ClassesScreen(year ?: return, vm, padding)
        Screen.Class -> ClassScreen(plan ?: return, vm, padding)
        Screen.Tests -> TestsScreen(plan ?: return, vm, padding)
        Screen.Homework -> HomeworkScreen(plan ?: return, vm, padding)
        is Screen.Group -> {
            if (plan == null) return
            val group = plan.groups.firstOrNull { it.id == s.id } ?: return
            val report = remember(plan, group) { GroupReport(plan, group) }
            GroupScreen(
                report, settings.scheduleByMonth, vm, padding,
                onCopy = {
                    copy(context, report.asTsv())
                    // Android 13+ shows its own copy confirmation.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast("Copied. Paste into a spreadsheet or Word.")
                },
                onShare = { share(context, "${plan.name} — ${report.group.name}", report.asTsv()) },
            )
        }
    }
}

@Composable
private fun UndoButton(vm: PacerViewModel, depth: Int) {
    TextButton(onClick = vm::undo, enabled = depth > 0) {
        Text(if (depth > 0) "Undo ($depth)" else "Undo")
    }
}

/**
 * Every other screen's bar: back, where the screen sits over its title, and undo where edits happen
 * ([undoDepth] null hides it). On a group, the title switches between the class's groups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenBar(screen: Screen, data: AppData, vm: PacerViewModel, undoDepth: Int?) {
    var groups by remember { mutableStateOf(false) }
    val plan = data.plan
    val switchable = screen is Screen.Group && plan != null && plan.groups.size > 1
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
                        Text(screen.context(data), fontSize = 11.sp, color = Pacer.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            screen.title(data) + if (switchable) " ▾" else "",
                            fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                if (switchable) TextButton(onClick = { groups = true }) { heading() } else heading()
                if (plan != null) DropdownMenu(expanded = groups, onDismissRequest = { groups = false }) {
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
