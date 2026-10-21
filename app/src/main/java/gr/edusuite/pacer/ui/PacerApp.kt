package gr.edusuite.pacer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.asTsv
import gr.edusuite.pacer.engine.lessonsFor
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.R
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.Space
import gr.edusuite.pacer.ui.theme.Pacer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long a delete's Undo stays on show before accessibility adjustments (style guide §14). */
private const val UNDO_MILLIS = 5_000L

private fun Screen.title(d: AppData, context: Context): String = when (this) {
    Screen.Home -> context.getString(R.string.app_name)
    Screen.Settings -> context.getString(R.string.app_settings)
    Screen.YearSettings -> context.getString(R.string.app_year_settings)
    Screen.Overview -> context.getString(R.string.app_overview)
    Screen.Classes -> context.getString(R.string.app_classes)
    Screen.Class -> d.schoolClass?.name?.ifBlank { null } ?: context.getString(R.string.app_class)
    Screen.Tests -> context.getString(R.string.app_tests)
    Screen.Homework -> context.getString(R.string.app_homework)
    is Screen.Group -> d.schoolClass?.groups?.firstOrNull { it.id == id }?.name?.ifBlank { null } ?: context.getString(R.string.app_group)
}

/** The small line above the title: where this screen sits. */
private fun Screen.context(d: AppData, context: Context): String = when (this) {
    Screen.Home, Screen.Settings -> context.getString(R.string.app_name)
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
    val setAside by vm.setAside.collectAsState()
    val screen = ui.screen
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // The history step the snackbar's Undo takes back, or -1 when no Undo is on show.
    var undoStep by remember { mutableLongStateOf(-1L) }
    val toast: (String) -> Unit = { msg ->
        undoStep = -1L
        scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) }
    }
    // Deletes happen at once and offer Undo for 5 s (style guide §14 "Undo over confirm"), longer when the
    // accessibility settings ask for more time. It undoes only its own change: called right after the change, it
    // notes the step that change made.
    val accessibility = LocalAccessibilityManager.current
    val undoMillis = accessibility?.calculateRecommendedTimeoutMillis(UNDO_MILLIS, containsText = true, containsControls = true) ?: UNDO_MILLIS
    val undoToast: (String) -> Unit = { msg ->
        val step = vm.state.value.historyStep
        undoStep = step
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val timeout = launch { delay(undoMillis); if (undoStep == step) snackbar.currentSnackbarData?.dismiss() }
            val result = snackbar.showSnackbar(msg, actionLabel = context.getString(R.string.app_undo), duration = SnackbarDuration.Indefinite)
            timeout.cancel()
            if (undoStep == step) undoStep = -1L
            if (result == SnackbarResult.ActionPerformed) vm.undoIfLatest(step)
        }
    }
    // Another edit, an undo, or opening a year (which clears the history) leaves the Undo on show pointing at
    // something else, so it goes rather than undoing the wrong thing or nothing.
    LaunchedEffect(ui.historyStep) {
        if (undoStep >= 0 && undoStep != ui.historyStep) {
            undoStep = -1L
            snackbar.currentSnackbarData?.dismiss()
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
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                val motion = animationsEnabled()
                val visible = remember(data) { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(visibleState = visible,
                    enter = if (motion) slideInVertically(Motion.content()) { it } else EnterTransition.None,
                    exit = if (motion) slideOutVertically(Motion.content()) { it } else ExitTransition.None,
                ) { UndoSnackbar(data, undoStep >= 0, undoMillis) }
            }
        },
        topBar = {
            // Home is just the logo, the year picker and Settings, with no bar.
            if (screen != Screen.Home) ScreenBar(screen, ui.data, vm, undoDepth = if (screen == Screen.Settings) null else ui.undoDepth)
        },
    ) { padding ->
        val motion = animationsEnabled()
        AnimatedContent(
            targetState = screen to ui.stack.size,
            contentKey = { it.first },
            transitionSpec = {
                if (!motion) EnterTransition.None togetherWith ExitTransition.None
                else {
                    val direction = if (targetState.second < initialState.second) -1 else 1
                    (slideInHorizontally(Motion.screen()) { it / 12 * direction } + fadeIn(Motion.screen())) togetherWith
                        (slideOutHorizontally(Motion.screen()) { -it / 12 * direction } + fadeOut(Motion.screen()))
                }
            },
            label = "screen",
        ) { (s, _) ->
            live += s.key()
            saved.SaveableStateProvider(s.key()) {
                ScreenContent(s, ui.data, settings, setAside == null, vm, padding, toast, undoToast, context)
            }
        }
    }
    setAside?.let { SetAsideDialog(it, toast, vm::dismissSetAside) }
}

@Composable
private fun ScreenContent(
    s: Screen,
    data: AppData,
    settings: gr.edusuite.pacer.model.Settings,
    /** False while the notice about an unreadable file is open, so the welcome guide waits behind it. */
    canWelcome: Boolean,
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
        Screen.Home -> HomeScreen(
            data, vm, padding,
            // Only for a first launch: someone who deletes every year later doesn't need it again.
            welcome = canWelcome && !settings.welcomeSeen && data.years.isEmpty(),
            onDeleted = { undoToast(context.getString(R.string.app_deleted, it)) },
            undoToast = undoToast,
        )
        Screen.Settings -> SettingsScreen(settings, data.years.size, vm, padding, toast, undoToast)
        Screen.YearSettings -> YearSettingsScreen(year ?: return, vm, padding, toast, undoToast, onDeleted = { undoToast(context.getString(R.string.app_deleted, it)) })
        Screen.Overview -> OverviewScreen(year ?: return, vm, padding)
        Screen.Classes -> ClassesScreen(year ?: return, vm, padding)
        Screen.Class -> ClassScreen(plan ?: return, vm, padding, undoToast)
        Screen.Tests -> TestsScreen(plan ?: return, vm, padding, undoToast)
        Screen.Homework -> HomeworkScreen(plan ?: return, vm, padding, undoToast)
        is Screen.Group -> {
            if (plan == null) return
            val group = plan.groups.firstOrNull { it.id == s.id } ?: return
            val report = remember(plan, group) { GroupReport(plan, group) }
            GroupScreen(
                report, settings.scheduleByMonth, vm, padding,
                onCopy = {
                    copy(context, report.asTsv())
                    // Android 13+ shows its own copy confirmation.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast(context.getString(R.string.app_copied))
                },
                onShare = { share(context, "${plan.name} — ${report.group.name}", report.asTsv()) },
                undoToast = undoToast,
            )
        }
    }
}

@Composable
private fun UndoButton(vm: PacerViewModel, depth: Int) {
    TertiaryButton(if (depth > 0) pluralStringResource(R.plurals.app_undo_steps, depth, depth) else stringResource(R.string.app_undo), vm::undo, enabled = depth > 0)
}

/**
 * Every other screen's bar: back, where the screen sits over its title, and undo where edits happen
 * ([undoDepth] null hides it). On a group, the title switches between the class's groups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenBar(screen: Screen, data: AppData, vm: PacerViewModel, undoDepth: Int?) {
    val context = LocalContext.current
    val segment = rememberSegmentHaptic()
    var groups by remember { mutableStateOf(false) }
    val plan = data.plan
    val switchable = screen is Screen.Group && plan != null && plan.groups.size > 1
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
            PillIconButton(stringResource(R.string.app_back), { vm.back() }) { AppIcon(AppIcons.Back) }
        },
        title = {
            Box {
                val heading = @Composable {
                    Column {
                        Text(screen.context(data, context), style = MaterialTheme.typography.labelMedium, color = Pacer.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            screen.title(data, context),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f, fill = false),
                          )
                          if (switchable) AppIcon(AppIcons.ExpandMore)
                        }
                    }
                }
                if (switchable) TextButton(onClick = { groups = true }) { heading() } else heading()
                if (plan != null) DropdownMenu(expanded = groups, onDismissRequest = { groups = false }) {
                    plan.groups.forEach { g ->
                        val count = remember(plan, g) { plan.lessonsFor(g).size }
                        DropdownMenuItem(
                            text = {
                                Row {
                                    Text(g.name.ifBlank { context.getString(R.string.app_group) }, fontWeight = if (screen == Screen.Group(g.id)) FontWeight.SemiBold else FontWeight.Normal)
                                    val description = pluralStringResource(R.plurals.app_lessons_left, count, count)
                                    AnimatedFigure(count.toString(), Modifier.padding(start = Space.s).semantics { contentDescription = description }, style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.muted)
                                }
                            },
                            onClick = { groups = false; segment(); vm.open(Screen.Group(g.id)) },
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
    cm.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_schedule), text))
}

/** The Undo time limit is also expressed by the action; this optional visual never delays or controls Undo. */
@Composable
private fun UndoSnackbar(data: SnackbarData, undo: Boolean, durationMillis: Long) {
    val enabled = animationsEnabled()
    val progress = remember(data) { Animatable(1f) }
    val started = remember(data) { android.os.SystemClock.elapsedRealtime() }
    LaunchedEffect(data, enabled, undo, durationMillis) {
        val remaining = (durationMillis - (android.os.SystemClock.elapsedRealtime() - started)).coerceAtLeast(0)
        progress.snapTo(if (enabled && undo) remaining.toFloat() / durationMillis else 0f)
        if (enabled && undo && remaining > 0) progress.animateTo(0f, tween(remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), easing = LinearEasing))
    }
    Snackbar(modifier = Modifier.padding(Space.m), action = {
        data.visuals.actionLabel?.let { label ->
            TextButton(onClick = data::performAction, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary)) { Text(label) }
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Text(data.visuals.message)
            if (undo) LinearProgressIndicator(progress = { progress.value }, modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.inverseOnSurface, trackColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .24f))
        }
    }
}

private fun share(context: Context, title: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, title))
}
