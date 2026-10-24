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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import gr.edusuite.pacer.engine.catchUp
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.asTsv
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.R
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.Space
import gr.edusuite.pacer.ui.theme.Pacer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long a delete's Undo stays on show before accessibility adjustments (style guide §14). */
private const val UNDO_MILLIS = gr.edusuite.pacer.ui.theme.MotionSpec.UNDO_MILLIS

private fun Screen.title(d: AppData, context: Context): String = when (this) {
    Screen.Home -> context.getString(R.string.app_name)
    Screen.Settings -> context.getString(R.string.app_settings)
    Screen.YearSettings -> context.getString(R.string.app_year_settings)
    Screen.Overview -> context.getString(R.string.app_overview)
    Screen.Classes -> context.getString(R.string.app_classes)
    Screen.Class -> d.schoolClass?.name?.ifBlank { null } ?: context.getString(R.string.app_class)
    // "FCE · Tests & worksheets", as the locked mock-ups have it.
    Screen.Tests -> classTitle(d, context, context.getString(R.string.app_tests))
    Screen.Units -> context.getString(R.string.units_title)
    Screen.Homework -> classTitle(d, context, context.getString(R.string.app_homework_short))
    is Screen.GroupSettings -> context.getString(R.string.group_settings_title)
    is Screen.Group -> {
        val name = d.schoolClass?.groups?.firstOrNull { it.id == id }?.name?.ifBlank { null } ?: context.getString(R.string.app_group)
        val part = when (page) {
            GroupPage.HOME -> null
            GroupPage.PLAN -> R.string.group_tab_plan
            GroupPage.WORK -> R.string.work_title
            GroupPage.STATS -> R.string.group_tab_stats
        }
        if (part == null) name else context.getString(R.string.nav_class_group, name, context.getString(part))
    }
}

/** [what] after the open class's name. */
private fun classTitle(d: AppData, context: Context, what: String): String =
    d.schoolClass?.name?.ifBlank { null }?.let { context.getString(R.string.nav_class_group, it, what) } ?: what

/** The small line above the title: where this screen sits. */
private fun Screen.context(d: AppData, context: Context): String = when (this) {
    Screen.Home, Screen.Settings -> context.getString(R.string.app_name)
    Screen.YearSettings, Screen.Overview, Screen.Classes, Screen.Class -> d.year?.name.orEmpty()
    Screen.Tests, Screen.Homework, Screen.Units -> d.year?.name.orEmpty()
    is Screen.Group -> listOfNotNull(d.year?.name, d.schoolClass?.name).joinToString(" · ")
    is Screen.GroupSettings -> listOfNotNull(d.schoolClass?.name, d.schoolClass?.groups?.firstOrNull { it.id == id }?.name).joinToString(" · ")
}

/** Keys each screen's saved state (scroll position, open dialogs) while it stays on the back stack. */
private fun Screen.key(): String = toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacerApp(vm: PacerViewModel) {
    val ui by vm.state.collectAsState()
    val revisionTarget by vm.unitRevision.collectAsState()
    val settings by vm.settings.collectAsState()
    val update by vm.update.collectAsState()
    val setAside by vm.setAside.collectAsState()
    val screen = ui.screen
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoGate = remember { UndoGate() }
    var undoToken by remember { mutableLongStateOf(-1L) }
    var undoStep by remember { mutableLongStateOf(-1L) }
    val toast: (String) -> Unit = { msg ->
        undoGate.close(undoToken)
        undoToken = -1L
        snackbar.currentSnackbarData?.dismiss()
        scope.launch { snackbar.showSnackbar(msg) }
    }
    val accessibility = LocalAccessibilityManager.current
    val undoMillis = accessibility?.calculateRecommendedTimeoutMillis(UNDO_MILLIS, containsText = true, containsControls = true) ?: UNDO_MILLIS
    val undoToast: (String) -> Unit = { msg ->
        val step = vm.state.value.historyStep
        val token = undoGate.open()
        undoToken = token
        undoStep = step
        snackbar.currentSnackbarData?.dismiss()
        scope.launch {
            if (undoGate.isCurrent(token)) {
                try {
                    val result = snackbar.showSnackbar(UndoNotice(msg, context.getString(R.string.app_undo), token, undoMillis))
                    if (result == SnackbarResult.ActionPerformed && undoGate.isCurrent(token)) vm.undoIfLatest(step)
                } finally {
                    if (undoGate.close(token)) undoToken = -1L
                }
            }
        }
    }
    // A later edit or year switch invalidates the notification's Undo action.
    LaunchedEffect(ui.historyStep, undoToken) {
        if (undoToken >= 0 && undoStep != ui.historyStep) {
            undoGate.close(undoToken)
            undoToken = -1L
            snackbar.currentSnackbarData?.dismiss()
        }
    }

    BackHandler(enabled = ui.stack.size > 1) { vm.back() }
    // ☰ opens the menu on every screen; Back closes it before it pops a screen.
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val openMenu: () -> Unit = { scope.launch { drawer.open() } }
    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
    // The group switcher and the Warnings sheet belong to the screen they opened on.
    var switching by rememberSaveable(screen.key()) { mutableStateOf(false) }
    var warningsOpen by rememberSaveable(screen.key()) { mutableStateOf(false) }
    // What the group's ⋮ menu opened: Term weeks or Tips on the screen, or Print over it.
    var groupSheet by rememberSaveable(screen.key()) { mutableStateOf<GroupSheet?>(null) }
    var printing by rememberSaveable(screen.key()) { mutableStateOf(false) }
    val large = rememberLargeText()
    val groupReport = remember(screen, ui.data.plan) {
        val plan = ui.data.plan
        val group = (screen as? Screen.Group)?.let { s -> plan?.groups?.firstOrNull { it.id == s.id } }
        if (plan == null || group == null) null else GroupReport(plan, group)
    }
    val warnings = remember(groupReport) { groupReport?.let(::groupWarnings).orEmpty() }
    // Pacing overview's warning button counts the year's problems instead (decision 7).
    val yearProblems = remember(screen, ui.data.year) { if (screen == Screen.Overview) ui.data.year?.let { Nav.problems(it).size } ?: 0 else 0 }
    // The class screen's warning button counts the class's problems (the locked Class mock-up, D7).
    val classProblems = remember(screen, ui.data.plan) { if (screen == Screen.Class) ui.data.plan?.let { screenProblems(it, Screen.Class).size } ?: 0 else 0 }
    var problemsOpen by rememberSaveable(screen.key()) { mutableStateOf(false) }
    val groupMenu = groupReport?.let { report ->
        GroupMenu(
            hasRows = report.rows.isNotEmpty(),
            hasCustom = report.group.custom != null,
            large = large,
            others = report.plan.groups.filter { it.id != report.group.id }.map { it.id to it.name },
            repace = { if (vm.repaceFrom(report.group.id, 0)) undoToast(context.getString(R.string.custom_repaced)) else toast(context.getString(R.string.toast_nothing_changed)) },
            copyPlan = { id, name -> if (vm.copyCustomPlan(report.group.id, id)) undoToast(context.getString(R.string.custom_copied, name)) else toast(context.getString(R.string.toast_nothing_changed)) },
            deletePlan = { if (vm.deleteCustomPlan(report.group.id)) undoToast(context.getString(R.string.custom_deleted)) },
            catchUp = report.group.custom != null && report.catchUp(1) != null,
            copy = {
                copy(context, report.asTsv())
                // Android 13+ shows its own copy confirmation.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast(context.getString(R.string.app_copied))
            },
            share = { share(context, "${report.plan.name} — ${report.group.name}", report.asTsv()) },
            print = { printing = true },
            open = { groupSheet = it },
            settings = { vm.open(Screen.GroupSettings(report.group.id)) },
            customPlan = if (large && report.group.custom == null && report.rows.isNotEmpty()) ({
                vm.createCustomPlan(report.group.id)
                toast(context.getString(R.string.custom_created))
            }) else null,
        )
    }
    val recent by vm.recent.collectAsState()

    // Screens keep their scroll position while they are on the stack, and forget it once popped.
    val saved = rememberSaveableStateHolder()
    val live = remember { HashSet<String>() }
    LaunchedEffect(ui.stack) {
        val keys = ui.stack.mapTo(HashSet()) { it.key() }
        (live - keys).forEach(saved::removeState)
        live.retainAll(keys)
    }

    // A tablet 840dp wide or more keeps the menu open beside the screens; a smaller one slides it over them. From 600dp,
    // Pacing overview and an open group share the window as a list pane and a detail pane (decision 17).
    val layout = rememberWindowLayout()
    val permanent = layout == WindowLayout.EXPANDED
    val panes = Nav.panes(ui.stack, layout)
    // One set of actions per window: on a tablet they sit in the detail pane's bar, which has ☰ only where the menu isn't always open.
    val bar: @Composable (Boolean) -> Unit = { titled ->
        ScreenBar(
            screen, ui.data, vm,
            undoDepth = if (screen.showsUndo) ui.undoDepth else null,
            warnings = when (screen) {
                Screen.Overview -> yearProblems
                Screen.Class -> classProblems
                else -> warnings.size
            },
            showMenu = !permanent,
            menuBadge = update != null,
            titled = titled,
            onMenu = openMenu,
            onSwitch = { switching = true },
            onWarnings = { if (screen == Screen.Overview || screen == Screen.Class) problemsOpen = true else warningsOpen = true },
            groupMenu = groupMenu,
        )
    }
    val screenContent: @Composable (Screen, PaddingValues) -> Unit = { s, padding ->
        live += s.key()
        saved.SaveableStateProvider(s.key()) {
            ScreenContent(
                s, ui.data, settings, setAside == null, vm, padding, toast, undoToast, context,
                onMenu = openMenu.takeIf { !permanent }, warningsOpen = warningsOpen && s == screen, onWarningsClose = { warningsOpen = false },
                groupSheet = groupSheet.takeIf { s == screen }, onGroupSheetClose = { groupSheet = null },
                selectedGroup = panes?.detail?.groupId,
                menuBadge = update != null,
            )
        }
    }
    val body: @Composable () -> Unit = {
        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    val motion = animationsEnabled()
                    val visible = remember(data) { MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(visibleState = visible,
                        enter = if (motion) slideInVertically(Motion.content()) { it } else EnterTransition.None,
                        exit = if (motion) slideOutVertically(Motion.content()) { it } else ExitTransition.None,
                    ) { UndoSnackbar(data, undoGate) }
                }
            },
            topBar = {
                // Home has its own header, with ☰ at its start; on a tablet the panes each have their own.
                if (screen != Screen.Home && panes == null) bar(true)
            },
        ) { padding ->
            if (panes != null) {
                val below = PaddingValues(bottom = padding.calculateBottomPadding())
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(0.4f).fillMaxHeight()) {
                        // The list pane has no ☰, Undo or warning button: only its title.
                        Text(
                            stringResource(if (panes.list == Screen.Classes) R.string.app_classes else R.string.app_overview),
                            style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = Space.l, end = Space.l, top = padding.calculateTopPadding() + Space.m, bottom = Space.xs).semantics { heading() },
                        )
                        Box(Modifier.weight(1f)) { screenContent(panes.list, below) }
                    }
                    VerticalDivider(color = Pacer.colors.line)
                    Column(Modifier.weight(0.6f).fillMaxHeight()) {
                        bar(panes.detail != null)
                        Box(Modifier.weight(1f)) {
                            val detail = panes.detail
                            if (detail != null) screenContent(detail, below)
                            else Hint(stringResource(R.string.panes_pick_group), Modifier.padding(Space.l))
                        }
                    }
                }
                return@Scaffold
            }
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
            ) { (s, _) -> screenContent(s, padding) }
        }
    }
    if (permanent) Row(Modifier.fillMaxSize()) {
        AppDrawer(ui.data, screen, vm, onClose = {}, permanent = true, updateBadge = update != null)
        Box(Modifier.weight(1f)) { body() }
    } else ModalNavigationDrawer(
        drawerContent = { AppDrawer(ui.data, screen, vm, onClose = { scope.launch { drawer.close() } }, updateBadge = update != null) },
        drawerState = drawer,
        // Swiping only closes it, so an edge swipe never fights the month pager or the system Back gesture.
        gesturesEnabled = drawer.isOpen,
    ) { body() }
    val year = ui.data.year
    if (switching && screen is Screen.Group && year != null) {
        GroupSwitcherSheet(year, ui.data.activeClass, screen.id, recent, vm) { switching = false }
    }
    if (problemsOpen && screen == Screen.Overview && year != null) {
        YearProblemsSheet(year, vm) { problemsOpen = false }
    }
    val plan = ui.data.plan
    if (problemsOpen && screen == Screen.Class && plan != null) {
        ClassProblemsSheet(plan, vm) { problemsOpen = false }
    }
    if (printing && year != null && groupReport != null) {
        PrintDialog(year, only = ui.data.activeClass to groupReport.group.id) { printing = false }
    }
    revisionTarget?.let { target ->
        val year = ui.data.year
        val cls = year?.classes?.firstOrNull { it.id == target.classId }
        val group = cls?.groups?.firstOrNull { it.id == target.groupId }
        if (year != null && cls != null && group != null) androidx.compose.runtime.key(target) { UnitRevisionSheet(GroupReport(year.planFor(cls), group), target, vm, undoToast) }
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
    onMenu: (() -> Unit)?,
    warningsOpen: Boolean,
    onWarningsClose: () -> Unit,
    groupSheet: GroupSheet?,
    onGroupSheetClose: () -> Unit,
    /** The group open in the detail pane of a tablet, highlighted in the list beside it. */
    selectedGroup: String? = null,
    menuBadge: Boolean = false,
) {
    // Each is null for a moment while the screen of something just deleted fades out.
    val year = data.year
    val plan = data.plan
    when (s) {
        Screen.Home -> HomeScreen(
            data, settings.name, vm, padding,
            // Only for a first launch: someone who deletes every year later doesn't need it again.
            welcome = canWelcome && !settings.welcomeSeen && data.years.isEmpty(),
            undoToast = undoToast,
            onMenu = onMenu,
            menuBadge = menuBadge,
        )
        Screen.Settings -> SettingsScreen(settings, data.years.size, vm, padding, toast, undoToast)
        Screen.YearSettings -> YearSettingsScreen(data.years, year ?: return, vm, padding, toast, undoToast, onDeleted = { undoToast(context.getString(R.string.app_deleted, it)) })
        Screen.Overview -> OverviewScreen(year ?: return, vm, padding, selectedGroup)
        Screen.Classes -> ClassesScreen(year ?: return, vm, padding, order = settings.classOrder, undoToast = undoToast)
        Screen.Class -> ClassScreen(plan ?: return, vm, padding, undoToast)
        Screen.Units -> UnitsSettingsScreen(plan ?: return, vm, padding)
        Screen.Tests -> MovedWorkScreen(plan ?: return, vm, padding)
        Screen.Homework -> MovedWorkScreen(plan ?: return, vm, padding)
        is Screen.GroupSettings -> {
            if (plan == null || year == null) return
            val group = plan.groups.firstOrNull { it.id == s.id } ?: return
            GroupSettingsScreen(year, plan, group, vm, padding, undoToast, toast)
        }
        is Screen.Group -> {
            if (plan == null) return
            val group = plan.groups.firstOrNull { it.id == s.id } ?: return
            val report = remember(plan, group) { GroupReport(plan, group) }
            GroupScreen(
                report, vm, padding,
                undoToast = undoToast,
                toast = toast,
                warningsOpen = warningsOpen,
                onWarningsClose = onWarningsClose,
                sheet = groupSheet,
                onSheetClose = onGroupSheetClose,
                seenTips = settings.seenTips,
                grouping = settings.planGrouping,
                page = Nav.groupPage(s, rememberWindowLayout()),
            )
        }
    }
}

@Composable
private fun UndoButton(vm: PacerViewModel, depth: Int) {
    // A symbol, so the bar keeps room for the title; TalkBack still hears how many steps it can take back.
    val label = if (depth > 0) pluralStringResource(R.plurals.app_undo_steps, depth, depth) else stringResource(R.string.app_undo)
    PillIconButton(label, vm::undo, enabled = depth > 0) { AppIcon(AppIcons.Undo) }
}

/** What a group's ⋮ menu does: Copy and Share the schedule, Print it starting with this group, or open a sheet. */
private class GroupMenu(
    val hasCustom: Boolean, val large: Boolean, val others: List<Pair<String, String>>,
    val repace: () -> Unit, val copyPlan: (String, String) -> Unit, val deletePlan: () -> Unit, val catchUp: Boolean,
    val hasRows: Boolean, val copy: () -> Unit, val share: () -> Unit, val print: () -> Unit, val open: (GroupSheet) -> Unit,
    val settings: () -> Unit,
    /** Large text: "Make a custom plan", which the Plan tab's line offers otherwise. Null when there is nothing to offer. */
    val customPlan: (() -> Unit)? = null,
)

/** The group's ⋮: Copy, Share, Print, Term weeks and Tips. */
@Composable
private fun GroupMenuButton(menu: GroupMenu) {
    var open by remember { mutableStateOf(false) }
    var copying by remember { mutableStateOf(false) }
    val available = groupMenuItems(menu.hasRows, menu.hasCustom, menu.others.size, menu.large)
    Box {
        PillIconButton(stringResource(R.string.group_more), { open = true }) { AppIcon(AppIcons.MoreVert) }
        PacerDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.group_settings_title)) }, leadingIcon = { AppIcon(AppIcons.Settings) }, onClick = { open = false; menu.settings() })
            if (GroupMenuItem.MY_PLAN in available) {
                Text(stringResource(R.string.plan_menu_heading), style = MaterialTheme.typography.labelSmall, color = Pacer.colors.muted, modifier = Modifier.padding(horizontal = Space.l, vertical = Space.s))
                DropdownMenuItem(text = { Text(stringResource(R.string.custom_repace_all)) }, onClick = { open = false; menu.repace() })
                if (menu.catchUp) DropdownMenuItem(text = { Text(stringResource(R.string.custom_adjust_catch_up)) }, onClick = { open = false; menu.open(GroupSheet.CATCH_UP) })
                if (GroupMenuItem.COPY_PLAN in available) DropdownMenuItem(text = { Text(stringResource(R.string.custom_copy_to)) }, onClick = { open = false; copying = true })
                DropdownMenuItem(text = { Text(stringResource(R.string.custom_delete), color = Pacer.colors.warn) }, onClick = { open = false; menu.deletePlan() })
                HorizontalDivider()
            }
            if (GroupMenuItem.OWN_PACE in available) DropdownMenuItem(text = { Text(stringResource(R.string.plan_own_pace)) }, onClick = { open = false; menu.open(GroupSheet.OWN_PACE) })
            menu.customPlan?.let { make ->
                DropdownMenuItem(text = { Text(stringResource(R.string.group_make_custom_plan)) }, leadingIcon = { AppIcon(AppIcons.Edit) }, onClick = { open = false; make() })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.extra_add)) }, leadingIcon = { AppIcon(AppIcons.Calendar) }, onClick = { open = false; menu.open(GroupSheet.EXTRA) })
            DropdownMenuItem(text = { Text(stringResource(R.string.group_copy)) }, enabled = menu.hasRows, leadingIcon = { AppIcon(AppIcons.ContentCopy) }, onClick = { open = false; menu.copy() })
            DropdownMenuItem(text = { Text(stringResource(R.string.group_share)) }, enabled = menu.hasRows, leadingIcon = { AppIcon(AppIcons.Share) }, onClick = { open = false; menu.share() })
            DropdownMenuItem(text = { Text(stringResource(R.string.group_print)) }, leadingIcon = { AppIcon(AppIcons.Print) }, onClick = { open = false; menu.print() })
            DropdownMenuItem(text = { Text(stringResource(R.string.group_term_weeks)) }, leadingIcon = { AppIcon(AppIcons.Calendar) }, onClick = { open = false; menu.open(GroupSheet.TERM_WEEKS) })
            DropdownMenuItem(text = { Text(stringResource(R.string.group_tips)) }, leadingIcon = { AppIcon(AppIcons.Info) }, onClick = { open = false; menu.open(GroupSheet.TIPS) })
        }
        PacerDropdownMenu(expanded = copying, onDismissRequest = { copying = false }) {
            menu.others.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { copying = false; menu.copyPlan(id, name) }) }
        }
    }
}

/**
 * Every other screen's bar: ☰ for the menu, where the screen sits over its title, and Undo where school-year data
 * changes ([undoDepth] null hides it). On a group, the title opens the group switcher and a warning button with the
 * count opens the Warnings sheet, and ⋮ holds the schedule's Copy, Share, Print, Term weeks and Tips. System Back still pops the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenBar(
    screen: Screen,
    data: AppData,
    vm: PacerViewModel,
    undoDepth: Int?,
    warnings: Int,
    /** False where the menu is always open (a tablet 840dp wide or more). */
    showMenu: Boolean,
    menuBadge: Boolean,
    /** False for the empty detail pane of a tablet: its bar holds the actions but no title. */
    titled: Boolean,
    onMenu: () -> Unit,
    onSwitch: () -> Unit,
    onWarnings: () -> Unit,
    groupMenu: GroupMenu?,
) {
    val context = LocalContext.current
    val switchable = screen is Screen.Group && data.year != null
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
            if (showMenu) MenuButton(onMenu, menuBadge)
        },
        title = {
            if (!titled) return@TopAppBar
            val heading = @Composable {
                Column {
                    Text(screen.context(data, context), style = MaterialTheme.typography.labelMedium, color = Pacer.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            screen.title(data, context),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f, fill = false),
                        )
                        if (switchable) AppIcon(AppIcons.ArrowDropDown)
                    }
                }
            }
            if (switchable) {
                val label = stringResource(R.string.switcher_open)
                PaletteTextButton(onClick = onSwitch, modifier = Modifier.semantics { onClick(label) { onSwitch(); true } }) { heading() }
            } else heading()
        },
        actions = {
            if (warnings > 0) WarningButton(warnings, onWarnings)
            if (undoDepth != null) UndoButton(vm, undoDepth)
            if (groupMenu != null) GroupMenuButton(groupMenu)
        },
    )
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_schedule), text))
}

/** The displayed notification carries its own token and timeout, independent of undo history. */
private class UndoNotice(
    override val message: String,
    override val actionLabel: String,
    val token: Long,
    val durationMillis: Long,
) : SnackbarVisuals {
    override val withDismissAction = false
    override val duration = SnackbarDuration.Indefinite
}

@Composable
private fun UndoSnackbar(data: SnackbarData, gate: UndoGate) {
    val notice = data.visuals as? UndoNotice
    val enabled = animationsEnabled()
    val token = notice?.token
    val durationMillis = notice?.durationMillis ?: 0L
    val progress = remember(data, token) { Animatable(1f) }
    val started = remember(data, token) { android.os.SystemClock.elapsedRealtime() }
    // Start only once the snackbar is actually displayed, including when it waited behind another snackbar.
    LaunchedEffect(data, token) {
        if (notice != null) {
            delay(durationMillis)
            if (gate.isCurrent(notice.token)) data.dismiss()
        }
    }
    LaunchedEffect(data, token, enabled) {
        val remaining = (durationMillis - (android.os.SystemClock.elapsedRealtime() - started)).coerceAtLeast(0)
        progress.snapTo(if (enabled && notice != null && durationMillis > 0) remaining.toFloat() / durationMillis else 0f)
        if (enabled && notice != null && remaining > 0) progress.animateTo(0f, tween(remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), easing = LinearEasing))
    }
    Snackbar(modifier = Modifier.padding(Space.m), action = {
        data.visuals.actionLabel?.let { label ->
            PaletteTextButton(onClick = data::performAction, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary)) { Text(label) }
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Text(data.visuals.message)
            if (notice != null) LinearProgressIndicator(progress = { progress.value }, modifier = Modifier.fillMaxWidth(),
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
