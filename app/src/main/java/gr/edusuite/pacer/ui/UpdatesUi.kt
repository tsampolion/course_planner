package gr.edusuite.pacer.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import gr.edusuite.pacer.R
import gr.edusuite.pacer.update.RELEASE_LATEST_URL
import gr.edusuite.pacer.update.shareText
import gr.edusuite.pacer.ui.theme.Figures

/** The available release, daily-check preference and explicit-check feedback. */
@Composable
fun UpdatesSection(vm: PacerViewModel, toast: (String) -> Unit, modifier: Modifier = Modifier) {
    val update by vm.update.collectAsState()
    val settings by vm.settings.collectAsState()
    val check by vm.updateCheck.collectAsState()
    val context = LocalContext.current
    Section(stringResource(R.string.settings_updates), modifier) {
        update?.let { u ->
            Text(stringResource(R.string.updates_available, u.version), style = MaterialTheme.typography.titleMedium)
            if (u.notes.isNotBlank()) Text(u.notes, style = MaterialTheme.typography.bodyMedium)
            PrimaryButton(stringResource(R.string.updates_download), { openLink(context, u.apkUrl, toast) }, Modifier.fillMaxWidth())
            Hint(stringResource(R.string.updates_first_time))
            TertiaryButton(stringResource(R.string.updates_see_changes), { openLink(context, u.pageUrl, toast) })
        }
        SwitchRow(stringResource(R.string.updates_auto), settings.autoUpdateCheck, vm::setAutoUpdateCheck)
        Hint(stringResource(R.string.updates_auto_hint))
        SecondaryButton(stringResource(R.string.updates_check_now), vm::checkForUpdates, enabled = check != UpdateCheckState.CHECKING)
        Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            when (check) {
                UpdateCheckState.CHECKING -> Hint(stringResource(R.string.updates_checking))
                UpdateCheckState.UP_TO_DATE -> Hint(stringResource(R.string.updates_up_to_date))
                UpdateCheckState.FAILED -> ErrorBox(stringResource(R.string.updates_failed))
                UpdateCheckState.IDLE -> Unit
            }
        }
        Text(stringResource(R.string.updates_installed, vm.installedVersion), style = MaterialTheme.typography.bodyMedium.merge(Figures))
    }
}

/** Opens the browser, or copies the link and explains when no browser is installed. */
fun openLink(context: Context, url: String, toast: (String) -> Unit) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        copyLink(context, url)
        toast(context.getString(R.string.updates_no_browser))
    }
}

private fun copyLink(context: Context, url: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.updates_link_label), url))
}

/** Offers the app to another teacher through the share sheet, or copies the link when nothing can take it. */
fun shareApp(context: Context, toast: (String) -> Unit) {
    val title = context.getString(R.string.share_app_title)
    if (!share(context, title, shareText(context.getString(R.string.share_app_message)))) {
        copyLink(context, RELEASE_LATEST_URL)
        toast(context.getString(R.string.share_app_copied))
    }
}
