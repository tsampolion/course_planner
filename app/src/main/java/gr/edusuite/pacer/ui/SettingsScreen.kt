package gr.edusuite.pacer.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import gr.edusuite.pacer.engine.plain
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.data.ImportResult
import gr.edusuite.pacer.data.JsonCodec
import gr.edusuite.pacer.data.MAX_IMPORT_BYTES
import gr.edusuite.pacer.data.backupFileName
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.ThemeMode
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val THEMES = listOf(
    ThemeMode.SYSTEM to "Follow the system",
    ThemeMode.LIGHT to "Light",
    ThemeMode.DARK to "Dark",
)

/**
 * App-level preferences. None of these are edits to a school year, so undo does not cover them;
 * an import is, and its snackbar offers undo because this screen has no undo button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    yearCount: Int,
    vm: PacerViewModel,
    padding: PaddingValues,
    toast: (String) -> Unit,
    undoToast: (String) -> Unit,
) {
    val context = LocalContext.current
    var guide by rememberSaveable { mutableStateOf(false) }
    var defaults by rememberSaveable { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = Space.l, end = Space.l, top = padding.calculateTopPadding() + Space.m, bottom = padding.calculateBottomPadding() + Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        item(key = "theme") {
            Section("Theme") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    THEMES.forEach { (mode, label) ->
                        FilterChip(
                            modifier = Modifier.semantics { role = Role.RadioButton },
                            selected = settings.theme == mode,
                            onClick = { vm.setTheme(mode) },
                            label = { Text(label) },
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                    }
                }
            }
        }
        item(key = "defaults") {
            Section("New classes") {
                LinkRow("Class defaults", "${settings.lessonMins} min · ${settings.pace.plain()} pages per lesson") { defaults = true }
                Hint("Used when you add a class. Existing classes keep their own values, which you change on the class screen.")
            }
        }
        item(key = "backup") { BackupSection(yearCount, vm, toast, undoToast) }
        item(key = "help") {
            Section("Help") {
                Column { LinkRow("Getting started", "What the app does, and the steps to a first schedule") { guide = true } }
            }
        }
        item(key = "about") {
            Section("About") {
                Text("$APP_NAME $version", style = MaterialTheme.typography.bodyLarge)
                Hint("School years are saved on this device and included in Android backups, along with these settings.")
                Hint("Part of the EduSuite apps, alongside Course Invoicer.")
                Hint("Outfit typeface: © The Outfit Project Authors, SIL Open Font License 1.1. AndroidX: Apache License 2.0.")
                Hint("© 2026 Dimitrios Tsampallas. All rights reserved.")
            }
        }
    }

    if (defaults) {
        SheetFrame("New class defaults", "Done", true, {}, { defaults = false }) {
            IntField(settings.lessonMins, vm::setDefaultMins, "Lesson length (min)", Modifier.fillMaxWidth(), max = Settings.MAX_MINS)
            PaceField(settings.pace, vm::setDefaultPace, Modifier.fillMaxWidth())
            Hint("Changes are saved as you type and apply to new classes only.")
        }
    }
    if (guide) WelcomeDialog(onStart = null) { guide = false }
}

/** Export saves every school year to a file the teacher picks; Import reads one back after asking Add or Replace all. */
@Composable
private fun BackupSection(yearCount: Int, vm: PacerViewModel, toast: (String) -> Unit, undoToast: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf<List<SchoolYear>?>(null) }

    val exportTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = vm.exportText()
        val years = count(yearCount, "school year")
        scope.launch {
            val saved = withContext(Dispatchers.IO) { writeFile(context, uri, text.encodeToByteArray()) }
            val name = withContext(Dispatchers.IO) { displayName(context, uri) }
            toast(if (saved) "Saved $years${name?.let { " to $it" }.orEmpty()}" else "Couldn't save the file. Try another place.")
        }
    }
    val importFrom = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            when (val result = withContext(Dispatchers.IO) { readFile(context, uri) }) {
                is ImportResult.Years -> importing = result.years
                ImportResult.NoYears -> toast("That file holds no school years.")
                ImportResult.Newer -> toast("That file comes from a newer version of $APP_NAME. Update the app, then import it.")
                ImportResult.Unreadable -> toast("Couldn't read that file. Choose one exported from $APP_NAME or from the Coursebook Pacer web page.")
            }
        }
    }

    Section("Back up and restore") {
        SecondaryButton("Export…", { openPicker(toast) { exportTo.launch(backupFileName(LocalDate.now())) } }, Modifier.fillMaxWidth(), enabled = yearCount > 0)
        // Any type: phones label JSON files inconsistently, and a wrong pick is refused with a message.
        SecondaryButton("Import…", { openPicker(toast) { importFrom.launch(arrayOf("*/*")) } }, Modifier.fillMaxWidth())
        Hint("Export saves every school year to a file, in Downloads, Google Drive or wherever your phone offers. Import reads it back, on this phone or another, and also reads the Coursebook Pacer web page's export. Settings aren't included.")
        Hint("Export before you uninstall the app or change phones: uninstalling deletes the school years.")
    }

    importing?.let { years ->
        ImportDialog(
            years, yearCount,
            onAdd = { vm.addImportedYears(years); undoToast("Added ${count(years.size, "school year")}") },
            onReplace = { vm.replaceAllYears(years); undoToast("Replaced your school years with ${years.size} from the file") },
            onDismiss = { importing = null },
        )
    }
}

@Composable
private fun ImportDialog(years: List<SchoolYear>, current: Int, onAdd: () -> Unit, onReplace: () -> Unit, onDismiss: () -> Unit) {
    val names = years.take(5).joinToString(", ") { it.name } + if (years.size > 5) " and ${years.size - 5} more" else ""
    val here = if (current == 0) "There are no school years on this phone yet." else
        "Add puts them beside the ${count(current, "school year")} on this phone. Replace all deletes those and keeps only the file's."
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import ${count(years.size, "school year")}?") },
        text = { Text("$names.\n\n$here You can undo it straight after.") },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = {
            Column {
                if (current > 0) TertiaryButton("Replace all", { onReplace(); onDismiss() }, color = Pacer.colors.warn)
                PrimaryButton("Add", { onAdd(); onDismiss() })
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Opens a system picker; a phone without one says so instead of crashing. */
internal fun openPicker(toast: (String) -> Unit, open: () -> Unit) {
    try {
        open()
    } catch (e: android.content.ActivityNotFoundException) {
        toast("This phone has no file picker.")
    }
}

/** Writes and truncates, so saving over an older export leaves none of it behind. Some providers only take "w". */
internal fun writeFile(context: Context, uri: Uri, bytes: ByteArray): Boolean = runCatching {
    val out = runCatching { context.contentResolver.openOutputStream(uri, "wt") }.getOrNull()
        ?: context.contentResolver.openOutputStream(uri, "w")
    requireNotNull(out).use { it.write(bytes) }
}.isSuccess

/** Reads at most [MAX_IMPORT_BYTES]; anything larger can't be an export. */
private fun readFile(context: Context, uri: Uri): ImportResult {
    try {
        val input = context.contentResolver.openInputStream(uri) ?: return ImportResult.Unreadable
        val bytes = input.use { s ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                if (out.size() > MAX_IMPORT_BYTES) return ImportResult.Unreadable
            }
            out.toByteArray()
        }
        return JsonCodec.readImport(bytes.decodeToString().removePrefix("\uFEFF"))
    } catch (e: Exception) {
        return ImportResult.Unreadable
    }
}

private fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()
