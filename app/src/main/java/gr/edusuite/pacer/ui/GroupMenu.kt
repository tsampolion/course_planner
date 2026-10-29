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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
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

/** What a group's ⋮ menu does: Copy and Share the schedule, Print it starting with this group, or open a sheet. */
internal class GroupMenu(
    val hasCustom: Boolean, val large: Boolean, val others: List<Pair<String, String>>,
    val repace: () -> Unit, val copyPlan: (String, String) -> Unit, val deletePlan: () -> Unit, val catchUp: Boolean,
    val hasRows: Boolean, val copy: () -> Unit, val share: () -> Unit, val print: () -> Unit, val open: (GroupSheet) -> Unit,
    /** Large text: "Make a custom plan", which the Plan tab's line offers otherwise. Null when there is nothing to offer. */
    val customPlan: (() -> Unit)? = null,
    /** Whether any Print scope has lessons to print; the entry is hidden when none has. */
    val canPrint: Boolean = true,
    /** The page the menu is on: Work has no lessons, so it leaves out the sections about them. */
    val page: GroupPage = GroupPage.HOME,
)

/** Undo and the warning button, folded into ⋮ when the bar is too narrow for the title ([foldBarActions]). */
internal class FoldedActions(val warnings: Int, val onWarnings: () -> Unit, val undoDepth: Int?, val onUndo: () -> Unit)

/** A section's name in the ⋮ menu: not a target, a heading for TalkBack. */
@Composable
private fun MenuHeading(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Pacer.colors.muted,
        modifier = Modifier.padding(horizontal = Space.l, vertical = Space.s).semantics { heading() })
}

@Composable
private fun sectionTitle(section: GroupMenuSection): String = stringResource(
    when (section) {
        GroupMenuSection.SHARE_PRINT -> R.string.menu_share_print
        GroupMenuSection.PLAN -> R.string.group_tab_plan
        GroupMenuSection.LESSONS -> R.string.menu_lessons
        GroupMenuSection.HELP -> R.string.menu_help
    },
)

/** Undo and the warnings, first in the menu while the bar has folded them. */
@Composable
private fun FoldedItems(folded: FoldedActions, close: () -> Unit) {
    if (folded.warnings > 0) {
        DropdownMenuItem(
            text = { Text(pluralStringResource(R.plurals.warnings_count, folded.warnings, folded.warnings), color = Pacer.colors.warn) },
            leadingIcon = { AppIcon(AppIcons.Warning) }, onClick = { close(); folded.onWarnings() },
        )
    }
    folded.undoDepth?.let { depth ->
        DropdownMenuItem(
            text = { Text(if (depth > 0) pluralStringResource(R.plurals.app_undo_steps, depth, depth) else stringResource(R.string.app_undo)) },
            enabled = depth > 0, leadingIcon = { AppIcon(AppIcons.Undo) }, onClick = { close(); folded.onUndo() },
        )
    }
}

/**
 * The bar's ⋮. On a group it holds the schedule's tools, in the sections that apply to the page (never a heading over
 * nothing), every item with an icon. While the bar has folded Undo and the warnings ([folded]) they come first and the
 * button carries the warning count; on any other screen it is just those two.
 */
@Composable
internal fun GroupMenuButton(menu: GroupMenu?, folded: FoldedActions? = null) {
    var open by remember { mutableStateOf(false) }
    var copying by remember { mutableStateOf(false) }
    val features = LocalFeatures.current
    val available = menu?.let { m ->
        groupMenuItems(m.hasRows, m.hasCustom, m.others.size, m.large, m.catchUp, features).filter { it != GroupMenuItem.PRINT || m.canPrint }
    }.orEmpty()
    val sections = menu?.let { groupMenuSections(available, it.page) }.orEmpty()
    val warnings = folded?.warnings ?: 0
    val more = stringResource(R.string.group_more)
    val label = if (warnings > 0) stringResource(R.string.bar_more_warnings, more, pluralStringResource(R.plurals.warnings_count, warnings, warnings)) else more
    val c = Pacer.colors
    Box {
        PillIconButton(label, { open = true }) {
            BadgedBox(
                badge = { if (warnings > 0) Badge(containerColor = c.warn, contentColor = c.onWarn) { Text("$warnings") } },
                modifier = Modifier.clearAndSetSemantics {},
            ) { AppIcon(AppIcons.MoreVert) }
        }
        PacerDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val close = { open = false }
            if (folded != null) {
                FoldedItems(folded, close)
                if (sections.isNotEmpty()) HorizontalDivider()
            }
            sections.forEachIndexed { i, (section, items) ->
                if (i > 0) HorizontalDivider()
                MenuHeading(sectionTitle(section))
                items.forEach { item ->
                    if (menu != null) GroupMenuEntry(item, menu, close) { copying = true }
                }
            }
        }
        PacerDropdownMenu(expanded = copying, onDismissRequest = { copying = false }) {
            if (menu != null) menu.others.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { copying = false; menu.copyPlan(id, name) }) }
        }
    }
}

/** One item of the ⋮ menu; MY_PLAN only marks that the group has its own plan, so it shows nothing itself. */
@Composable
private fun GroupMenuEntry(item: GroupMenuItem, menu: GroupMenu, close: () -> Unit, copyingTo: () -> Unit) {
    @Composable fun entry(text: Int, icon: ImageVector, color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified, action: () -> Unit) =
        DropdownMenuItem(text = { Text(stringResource(text), color = color) }, leadingIcon = { AppIcon(icon) }, onClick = { close(); action() })
    when (item) {
        GroupMenuItem.CREATE_PLAN -> menu.customPlan?.let { make -> entry(R.string.group_make_custom_plan, AppIcons.Edit, action = make) }
        GroupMenuItem.MY_PLAN -> Unit
        GroupMenuItem.REPACE -> entry(R.string.custom_repace_all, AppIcons.Event, action = menu.repace)
        GroupMenuItem.COPY_PLAN -> entry(R.string.custom_copy_to, AppIcons.Groups, action = copyingTo)
        GroupMenuItem.DELETE_PLAN -> entry(R.string.custom_delete, AppIcons.Delete, Pacer.colors.warn, menu.deletePlan)
        GroupMenuItem.OWN_PACE -> entry(R.string.plan_own_pace, AppIcons.Overview) { menu.open(GroupSheet.OWN_PACE) }
        GroupMenuItem.CATCH_UP -> entry(R.string.custom_adjust_catch_up, AppIcons.Forward) { menu.open(GroupSheet.CATCH_UP) }
        GroupMenuItem.EXTRA -> entry(R.string.extra_add, AppIcons.Add) { menu.open(GroupSheet.EXTRA) }
        GroupMenuItem.COPY -> entry(R.string.group_copy, AppIcons.ContentCopy, action = menu.copy)
        GroupMenuItem.SHARE -> entry(R.string.group_share, AppIcons.Share, action = menu.share)
        GroupMenuItem.PRINT -> entry(R.string.group_print, AppIcons.Print, action = menu.print)
        GroupMenuItem.TIPS -> entry(R.string.group_tips, AppIcons.Info) { menu.open(GroupSheet.TIPS) }
    }
}
