package gr.edusuite.pacer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import gr.edusuite.pacer.R
import gr.edusuite.pacer.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Says that the latest edits may not be on disk, and stays until they are. It sits outside the toast queue, so an
 * ordinary toast can't hide it. Retry (or Try reading again) and Export a copy are the two ways out.
 */
@Composable
fun SaveNotice(problem: SaveProblem, vm: PacerViewModel, toast: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = rememberToday()
    val exportTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = vm.exportText()
        scope.launch {
            val saved = withContext(Dispatchers.IO) { writeFile(context, uri, text.encodeToByteArray()) }
            toast(context.getString(if (saved) R.string.save_exported else R.string.settings_save_error))
        }
    }
    val retry = when (problem) {
        SaveProblem.NotSaved -> R.string.save_retry to vm::retrySave
        SaveProblem.ReadLocked, SaveProblem.LockedWithEdits -> R.string.save_read_again to vm::retryRead
    }
    val message = when (problem) {
        SaveProblem.NotSaved -> R.string.save_not_saved
        SaveProblem.ReadLocked -> R.string.save_read_locked
        SaveProblem.LockedWithEdits -> R.string.save_read_locked_edits
    }
    val actionColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary)
    Snackbar(
        modifier = Modifier.padding(Space.m).semantics { liveRegion = LiveRegionMode.Polite },
        actionOnNewLine = true,
        action = {
            Row {
                PaletteTextButton(onClick = retry.second, colors = actionColors) { Text(stringResource(retry.first)) }
                PaletteTextButton(
                    onClick = { openPicker(context, toast) { exportTo.launch("course-pacer-$today.json") } },
                    colors = actionColors,
                ) { Text(stringResource(R.string.save_export)) }
            }
        },
    ) { Text(stringResource(message)) }
}
