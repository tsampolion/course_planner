package com.coursebook.pacer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coursebook.pacer.engine.GroupReport
import com.coursebook.pacer.engine.asTsv
import com.coursebook.pacer.engine.lessonsFor
import com.coursebook.pacer.model.AppData
import com.coursebook.pacer.ui.theme.Pacer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacerApp(vm: PacerViewModel) {
    val ui by vm.state.collectAsState()
    val data = ui.data
    val plan = data.plan
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) } }

    var menu by remember { mutableStateOf(false) }
    var plans by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val group = plan.groups.firstOrNull { it.id == data.view }
    val report = remember(plan, group) { group?.let { GroupReport(plan, it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
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
                        TextButton(onClick = vm::undo, enabled = ui.undoDepth > 0) {
                            Text(if (ui.undoDepth > 0) "Undo (${ui.undoDepth})" else "Undo")
                        }
                        Box {
                            TextButton(onClick = { menu = true }) { Text("⋮", fontSize = 20.sp) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Print or save PDF") }, onClick = { menu = false; PrintSheet.print(context, plan) })
                                DropdownMenuItem(text = { Text("Rename plan") }, onClick = { menu = false; renaming = true })
                                DropdownMenuItem(text = { Text("New plan") }, onClick = { menu = false; vm.newPlan() })
                                DropdownMenuItem(
                                    text = { Text("Delete plan") },
                                    enabled = data.plans.size > 1,
                                    onClick = { menu = false; confirmDelete = true },
                                )
                            }
                        }
                    },
                )
                ScrollableTabRow(
                    selectedTabIndex = if (group == null) 0 else plan.groups.indexOf(group) + 1,
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    Tab(selected = group == null, onClick = { vm.openView(AppData.VIEW_SETUP) }, text = { Text("Setup") })
                    plan.groups.forEach { g ->
                        val count = remember(plan, g) { plan.lessonsFor(g).size }
                        Tab(
                            selected = g.id == data.view,
                            onClick = { vm.openView(g.id) },
                            text = {
                                Row {
                                    Text(g.name.ifBlank { "Group" })
                                    Text("  $count", fontFamily = Mono, color = Pacer.colors.muted)
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (report == null) {
            SetupScreen(plan, vm, padding, toast)
        } else {
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
