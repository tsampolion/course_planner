package com.coursebook.pacer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coursebook.pacer.engine.fmt
import com.coursebook.pacer.ui.theme.Pacer
import java.time.LocalDate
import java.time.ZoneOffset

val Mono = FontFamily.Monospace
private val CardShape = RoundedCornerShape(6.dp)

/** A titled white panel, like the page's setup panels. */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, Pacer.colors.line),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow(title)
            content()
        }
    }
}

@Composable
fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = Pacer.colors.muted,
    )
}

@Composable
fun Hint(text: String, color: androidx.compose.ui.graphics.Color = Pacer.colors.muted) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}

/** A text field that keeps its own text while typing and hands each accepted value up. */
@Composable
fun CommitField(
    value: String,
    onCommit: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    decimal: Boolean = false,
    placeholder: String? = null,
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onCommit(it) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        textStyle = if (numeric) MaterialTheme.typography.bodyLarge.copy(fontFamily = Mono) else MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                decimal -> KeyboardType.Decimal
                numeric -> KeyboardType.Number
                else -> KeyboardType.Text
            },
        ),
        modifier = modifier,
    )
}

/** An integer field that only reports positive whole numbers; blanks and junk wait for more typing. */
@Composable
fun IntField(value: Int, onCommit: (Int) -> Unit, label: String, modifier: Modifier = Modifier) {
    CommitField(value.toString(), { s -> s.trim().toIntOrNull()?.takeIf { it > 0 }?.let(onCommit) }, label, modifier, numeric = true)
}

/** A read-only field that opens a date picker. [clearable] adds a Clear button to the picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    clearable: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value?.fmt() ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text("Not set") },
            modifier = Modifier.fillMaxWidth(),
        )
        // The text field swallows taps; this layer takes them instead.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clickable(role = Role.Button) { open = true }
                .semantics { contentDescription = "$label, ${value?.fmt() ?: "not set"}" },
        )
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(LocalDate.ofEpochDay(Math.floorDiv(it, 86_400_000L))) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (clearable && value != null) TextButton(onClick = { onChange(null); open = false }) { Text("Clear") }
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                }
            },
        ) { DatePicker(state = state) }
    }
}

/** An outlined dropdown for short option lists (group, book, "applies to"). */
@Composable
fun Picker(
    label: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { Text("▾", color = Pacer.colors.muted) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().padding(top = 8.dp).clickable(role = Role.DropdownList) { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); open = false })
            }
        }
    }
}

/** A delete control drawn as a plain ×, as on the page. */
@Composable
fun RemoveButton(what: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(40.dp).semantics { contentDescription = "Remove $what" },
    ) { Text("×", fontSize = 22.sp, color = Pacer.colors.muted) }
}

/** One row in a setup list: title, optional tag, a mono detail line, and a remove button. */
@Composable
fun ListItemRow(title: String, detail: String, tag: String? = null, warn: Boolean = false, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, Pacer.colors.line)) {
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
                    if (tag != null) Tag(tag, Modifier.padding(start = 6.dp))
                }
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = Mono,
                    color = if (warn) Pacer.colors.warn else Pacer.colors.muted,
                )
            }
            RemoveButton(title, onRemove)
        }
    }
}

@Composable
fun Tag(text: String, modifier: Modifier = Modifier, warn: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        color = if (warn) Pacer.colors.warnSoft else Pacer.colors.accentSoft,
        contentColor = if (warn) Pacer.colors.warn else MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/** The dashed "Add a …" button at the foot of each list. */
@Composable
fun AddButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, Pacer.colors.line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "+ $text",
            style = MaterialTheme.typography.labelLarge,
            color = Pacer.colors.muted,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** A plan problem. With [onFix] it becomes a link to the screen that fixes it. */
@Composable
fun ErrorBox(text: String, onFix: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Pacer.colors.warnSoft,
        contentColor = Pacer.colors.warn,
        border = BorderStroke(1.dp, Pacer.colors.warn),
        modifier = Modifier.fillMaxWidth().then(if (onFix != null) Modifier.clickable(role = Role.Button, onClick = onFix) else Modifier),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (onFix != null) Text("Fix ›", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** A tappable row that opens another screen: a title, a muted summary, and a chevron. */
@Composable
fun LinkRow(title: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted)
        }
        Text("›", fontSize = 22.sp, color = Pacer.colors.muted, modifier = Modifier.padding(start = 8.dp))
    }
}
