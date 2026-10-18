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
 * can't be reached from the phone, so "Save a copy…" puts it somewhere the teacher can, such as Drive.
 * The dialog stays open until the copy is saved, so the picker's result always has somewhere to land.
 */
@Composable
fun SetAsideDialog(file: File, toast: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saveTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { file.readBytes() }.getOrNull()?.let { writeFile(context, uri, it) } == true }
            toast(if (saved) "Saved a copy of the unreadable file." else "Couldn't save the file. Try another place.")
            if (saved) onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your school years couldn't be read") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                Tile(color = Pacer.colors.tileApricot) {
                    Text("Your original file is safe", style = MaterialTheme.typography.titleMedium)
                    Text("$APP_NAME started with no school years because the saved file could not be read. The original file is kept on this phone as it was.",
                        style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.onTileMuted)
                }
                Text("Save a copy to keep it somewhere you can reach. You can also restore an earlier export from Settings → Back up and restore.", style = MaterialTheme.typography.bodyMedium)
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = {
            PrimaryButton("Save a copy…", { openPicker(toast) { saveTo.launch("course-pacer-unreadable-${LocalDate.now()}.json") } })
        },
        dismissButton = { TertiaryButton("Close", onDismiss) },
    )
}
