package gr.edusuite.pacer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import gr.edusuite.pacer.ui.theme.Space
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import gr.edusuite.pacer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.ui.theme.Pacer
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Says once that the saved school years couldn't be read and were set aside as [file]. Files in app storage
 * can't be reached from the phone, so stringResource(R.string.recovery_save) puts it somewhere the teacher can, such as Drive.
 * The dialog stays open until the copy is saved, so the picker's result always has somewhere to land.
 */
@Composable
fun SetAsideDialog(file: File, toast: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = rememberToday()
    val saveTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { file.readBytes() }.getOrNull()?.let { writeFile(context, uri, it) } == true }
            toast(if (saved) context.getString(R.string.recovery_saved) else context.getString(R.string.settings_save_error))
            if (saved) onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recovery_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                Tile(color = Pacer.colors.tile3) {
                    Text(stringResource(R.string.recovery_original_safe), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.recovery_explanation, stringResource(R.string.app_name)),
                        style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.onTileMuted)
                }
                Text(stringResource(R.string.recovery_hint), style = MaterialTheme.typography.bodyMedium)
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = {
            PrimaryButton(stringResource(R.string.recovery_save), { openPicker(context, toast) { saveTo.launch("course-pacer-unreadable-$today.json") } })
        },
        dismissButton = { TertiaryButton(stringResource(R.string.recovery_close), onDismiss) },
    )
}
