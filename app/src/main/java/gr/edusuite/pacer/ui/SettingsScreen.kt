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
import gr.edusuite.pacer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
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
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Space
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val THEMES = listOf(
    ThemeMode.SYSTEM to R.string.settings_theme_system,
    ThemeMode.LIGHT to R.string.settings_theme_light,
    ThemeMode.DARK to R.string.settings_theme_dark,
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
    val segmentHaptic = rememberSegmentHaptic()
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
            Section(stringResource(R.string.settings_theme), modifier = Modifier.animateItem()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    THEMES.forEach { (mode, label) ->
                        FilterChip(
                            modifier = Modifier.semantics { role = Role.RadioButton },
                            selected = settings.theme == mode,
                            onClick = { if (settings.theme != mode) { segmentHaptic(); vm.setTheme(mode) } },
                            label = { Text(stringResource(label)) },
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                    }
                }
            }
        }
        item(key = "defaults") {
            Section(stringResource(R.string.settings_new_classes), modifier = Modifier.animateItem()) {
                LinkRow(stringResource(R.string.settings_class_defaults), stringResource(R.string.settings_defaults_summary, settings.lessonMins, settings.pace.plain())) { defaults = true }
                Hint(stringResource(R.string.settings_defaults_hint))
            }
        }
        item(key = "backup") { BackupSection(yearCount, vm, toast, undoToast) }
        item(key = "help") {
            Section(stringResource(R.string.settings_help), modifier = Modifier.animateItem()) {
                Column { LinkRow(stringResource(R.string.settings_getting_started), stringResource(R.string.settings_getting_started_detail)) { guide = true } }
            }
        }
        item(key = "about") {
            Section(stringResource(R.string.settings_about), modifier = Modifier.animateItem()) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge)
                Text(version, style = MaterialTheme.typography.bodyMedium.merge(Figures))
                Hint(stringResource(R.string.settings_local_storage))
                Hint(stringResource(R.string.settings_suite))
                Hint(stringResource(R.string.settings_credits))
                Hint(stringResource(R.string.settings_copyright))
            }
        }
    }

    if (defaults) {
        SheetFrame(stringResource(R.string.settings_defaults_title), stringResource(R.string.settings_done), true, {}, { defaults = false }) {
            IntField(settings.lessonMins, vm::setDefaultMins, stringResource(R.string.settings_lesson_length), Modifier.fillMaxWidth(), max = Settings.MAX_MINS)
            PaceField(settings.pace, vm::setDefaultPace, Modifier.fillMaxWidth())
            Hint(stringResource(R.string.settings_defaults_saved))
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
        val years = context.resources.getQuantityString(R.plurals.settings_year_count, yearCount, yearCount)
        scope.launch {
            val saved = withContext(Dispatchers.IO) { writeFile(context, uri, text.encodeToByteArray()) }
            val name = withContext(Dispatchers.IO) { displayName(context, uri) }
            toast(if (saved) {
                if (name == null) context.getString(R.string.settings_export_saved, years)
                else context.getString(R.string.settings_export_saved_to, years, name)
            } else context.getString(R.string.settings_save_error))
        }
    }
    val importFrom = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            when (val result = withContext(Dispatchers.IO) { readFile(context, uri) }) {
                is ImportResult.Years -> importing = result.years
                ImportResult.NoYears -> toast(context.getString(R.string.settings_import_empty))
                ImportResult.Newer -> toast(context.getString(R.string.settings_import_newer, context.getString(R.string.app_name)))
                ImportResult.Unreadable -> toast(context.getString(R.string.settings_import_unreadable, context.getString(R.string.app_name)))
            }
        }
    }

    Section(stringResource(R.string.settings_backup)) {
        SecondaryButton(stringResource(R.string.settings_export), { openPicker(context, toast) { exportTo.launch(backupFileName(LocalDate.now())) } }, Modifier.fillMaxWidth(), enabled = yearCount > 0)
        // Any type: phones label JSON files inconsistently, and a wrong pick is refused with a message.
        SecondaryButton(stringResource(R.string.settings_import), { openPicker(context, toast) { importFrom.launch(arrayOf("*/*")) } }, Modifier.fillMaxWidth())
        Hint(stringResource(R.string.settings_backup_hint))
        Hint(stringResource(R.string.settings_uninstall_hint))
    }

    importing?.let { years ->
        ImportDialog(
            years, yearCount,
            onAdd = { vm.addImportedYears(years); undoToast(context.resources.getQuantityString(R.plurals.settings_import_added, years.size, years.size)) },
            onReplace = { vm.replaceAllYears(years); undoToast(context.resources.getQuantityString(R.plurals.settings_import_replaced, years.size, years.size)) },
            onDismiss = { importing = null },
        )
    }
}

@Composable
private fun ImportDialog(years: List<SchoolYear>, current: Int, onAdd: () -> Unit, onReplace: () -> Unit, onDismiss: () -> Unit) {
    val names = years.take(5).joinToString(stringResource(R.string.settings_name_separator)) { it.name } +
        if (years.size > 5) pluralStringResource(R.plurals.settings_more_years, years.size - 5, years.size - 5) else ""
    val here = if (current == 0) stringResource(R.string.settings_no_years) else
        pluralStringResource(R.plurals.settings_import_explanation, current, current)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pluralStringResource(R.plurals.settings_import_title, years.size, years.size)) },
        text = { Text(stringResource(R.string.settings_import_body, names, here)) },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = {
            Column {
                if (current > 0) TertiaryButton(stringResource(R.string.settings_replace), { onReplace(); onDismiss() }, color = Pacer.colors.warn)
                PrimaryButton(stringResource(R.string.settings_add), { onAdd(); onDismiss() })
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}

/** Opens a system picker; a phone without one says so instead of crashing. */
internal fun openPicker(context: Context, toast: (String) -> Unit, open: () -> Unit) {
    try {
        open()
    } catch (e: android.content.ActivityNotFoundException) {
        toast(context.getString(R.string.settings_no_picker))
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
