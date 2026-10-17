package gr.edusuite.pacer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            Text(
                "The saved file is damaged, so $APP_NAME started with no school years. The file is kept on this phone as it was. " +
                    "Save a copy to keep it somewhere you can reach, or import a backup you exported earlier from Settings → Back up and restore.",
            )
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        dismissButton = {
            TextButton(onClick = { openPicker(toast) { saveTo.launch("course-pacer-unreadable-${LocalDate.now()}.json") } }) {
                Text("Save a copy…")
            }
        },
    )
}
