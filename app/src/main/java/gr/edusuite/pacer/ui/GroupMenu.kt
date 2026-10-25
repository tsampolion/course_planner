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

/** What a group's ⋮ menu does: Copy and Share the schedule, Print it starting with this group, or open a sheet. */
internal class GroupMenu(
    val hasCustom: Boolean, val large: Boolean, val others: List<Pair<String, String>>,
    val repace: () -> Unit, val copyPlan: (String, String) -> Unit, val deletePlan: () -> Unit, val catchUp: Boolean,
    val hasRows: Boolean, val copy: () -> Unit, val share: () -> Unit, val print: () -> Unit, val open: (GroupSheet) -> Unit,
    /** Large text: "Make a custom plan", which the Plan tab's line offers otherwise. Null when there is nothing to offer. */
    val customPlan: (() -> Unit)? = null,
)

/** A section's name in the ⋮ menu: not a target, a heading for TalkBack. */
@Composable
private fun MenuHeading(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Pacer.colors.muted,
        modifier = Modifier.padding(horizontal = Space.l, vertical = Space.s).semantics { heading() })
}

/** The group's ⋮, in sections: Share & print, Plan, Lessons and Help. Group settings is a pill on group home. */
@Composable
internal fun GroupMenuButton(menu: GroupMenu) {
    var open by remember { mutableStateOf(false) }
    var copying by remember { mutableStateOf(false) }
    val available = groupMenuItems(menu.hasRows, menu.hasCustom, menu.others.size, menu.large)
    Box {
        PillIconButton(stringResource(R.string.group_more), { open = true }) { AppIcon(AppIcons.MoreVert) }
        PacerDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuHeading(stringResource(R.string.menu_share_print))
            DropdownMenuItem(text = { Text(stringResource(R.string.group_copy)) }, enabled = menu.hasRows, leadingIcon = { AppIcon(AppIcons.ContentCopy) }, onClick = { open = false; menu.copy() })
            DropdownMenuItem(text = { Text(stringResource(R.string.group_share)) }, enabled = menu.hasRows, leadingIcon = { AppIcon(AppIcons.Share) }, onClick = { open = false; menu.share() })
            DropdownMenuItem(text = { Text(stringResource(R.string.group_print)) }, leadingIcon = { AppIcon(AppIcons.Print) }, onClick = { open = false; menu.print() })
            HorizontalDivider()
            MenuHeading(stringResource(R.string.group_tab_plan))
            menu.customPlan?.let { make ->
                DropdownMenuItem(text = { Text(stringResource(R.string.group_make_custom_plan)) }, leadingIcon = { AppIcon(AppIcons.Edit) }, onClick = { open = false; make() })
            }
            if (GroupMenuItem.MY_PLAN in available) {
                DropdownMenuItem(text = { Text(stringResource(R.string.custom_repace_all)) }, onClick = { open = false; menu.repace() })
                if (GroupMenuItem.COPY_PLAN in available) DropdownMenuItem(text = { Text(stringResource(R.string.custom_copy_to)) }, onClick = { open = false; copying = true })
                DropdownMenuItem(text = { Text(stringResource(R.string.custom_delete), color = Pacer.colors.warn) }, onClick = { open = false; menu.deletePlan() })
            }
            if (GroupMenuItem.OWN_PACE in available) DropdownMenuItem(text = { Text(stringResource(R.string.plan_own_pace)) }, onClick = { open = false; menu.open(GroupSheet.OWN_PACE) })
            if (menu.catchUp) DropdownMenuItem(text = { Text(stringResource(R.string.custom_adjust_catch_up)) }, onClick = { open = false; menu.open(GroupSheet.CATCH_UP) })
            HorizontalDivider()
            MenuHeading(stringResource(R.string.menu_lessons))
            DropdownMenuItem(text = { Text(stringResource(R.string.extra_add)) }, leadingIcon = { AppIcon(AppIcons.Calendar) }, onClick = { open = false; menu.open(GroupSheet.EXTRA) })
            HorizontalDivider()
            MenuHeading(stringResource(R.string.menu_help))
            DropdownMenuItem(text = { Text(stringResource(R.string.group_tips)) }, leadingIcon = { AppIcon(AppIcons.Info) }, onClick = { open = false; menu.open(GroupSheet.TIPS) })
        }
        PacerDropdownMenu(expanded = copying, onDismissRequest = { copying = false }) {
            menu.others.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { copying = false; menu.copyPlan(id, name) }) }
        }
    }
}
