package com.coursebook.pacer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coursebook.pacer.engine.plain
import com.coursebook.pacer.model.Settings
import com.coursebook.pacer.model.ThemeMode

private val THEMES = listOf(
    ThemeMode.SYSTEM to "Follow the system",
    ThemeMode.LIGHT to "Light",
    ThemeMode.DARK to "Dark",
)

/** App-level preferences. None of these are plan edits, so undo does not cover them. */
@Composable
fun SettingsScreen(settings: Settings, vm: PacerViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "theme") {
            Section("Theme") {
                Column(Modifier.selectableGroup()) {
                    THEMES.forEach { (mode, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = settings.theme == mode, role = Role.RadioButton) { vm.setTheme(mode) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.theme == mode, onClick = null)
                            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        }
        item(key = "defaults") {
            Section("New plans") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IntField(settings.lessonMins, vm::setDefaultMins, "Lesson length (min)", Modifier.weight(1f))
                    CommitField(
                        settings.pace.plain(),
                        { s -> s.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }?.let(vm::setDefaultPace) },
                        "Pace you can sustain",
                        Modifier.weight(1f),
                        numeric = true,
                        decimal = true,
                    )
                }
                Hint("Used when you create a plan. Existing plans keep their own values, which you change under Term.")
            }
        }
        item(key = "about") {
            Section("About") {
                Text("Coursebook Pacer $version", style = MaterialTheme.typography.bodyLarge)
                Hint("Plans are saved on this device and included in Android backups, along with these settings.")
                Hint("© 2026 Dimitrios Tsampallas. All rights reserved.")
            }
        }
    }
}
