package gr.edusuite.pacer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.plain
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.ui.theme.Mono
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A group on a screen (§6 Section): an Eyebrow, then the content on a surface list group with 16dp corners.
 * [trailing] is an optional text action next to the eyebrow, such as "See all".
 */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Eyebrow(title) }
            trailing?.invoke()
        }
        ListGroup { Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) { content() } }
    }
}

/** The surface rows sit on (§6 List row): 16dp corners, no border; rows inside are split by hairline `line` dividers. */
@Composable
fun ListGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = Pacer.colors.surface, content = content)
}

/** The label above a group (§6 Eyebrow): `labelMedium`, sentence case, muted, and a heading for TalkBack. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Pacer.colors.muted,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * The guide's base block (§6 Tile): a tile colour, 24dp corners, 16dp padding, no border. With [onClick] the whole
 * tile is one touch target.
 */
@Composable
fun Tile(
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentColor: Color = Pacer.colors.onTile,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.xs), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = color, contentColor = contentColor, content = body)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = color, contentColor = contentColor, content = body)
    }
}

/** A primary pill button (§6 Buttons): accent fill, 48dp tall. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = CircleShape) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** A secondary pill button: surfaceContainer fill with ink text. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Pacer.colors.surfaceContainer, contentColor = Pacer.colors.ink),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** A tertiary button: text only, in the accent (or [color]). */
@Composable
fun TertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = Color.Unspecified) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = CircleShape) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** An icon button on a 40dp surface pill inside a 48dp touch target (§6 Buttons). [label] is what TalkBack reads. */
@Composable
fun PillIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = Pacer.colors.surface, contentColor = Pacer.colors.ink, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}

/**
 * Shown when a list has nothing yet (§6 EmptyState): a 120dp circle in a tile tint holding a symbol, a title, a muted
 * line and an optional pill action.
 */
@Composable
fun EmptyState(symbol: String, title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        Surface(shape = CircleShape, color = Pacer.colors.tileSky, contentColor = Pacer.colors.onTile, modifier = Modifier.size(120.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(symbol, style = MaterialTheme.typography.displaySmall) }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.muted, textAlign = TextAlign.Center)
        if (action != null && onAction != null) PrimaryButton(action, onAction)
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, color: Color = Pacer.colors.muted) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, modifier = modifier)
}

/**
 * A text field that keeps its own text while typing and hands each accepted value up.
 * When [value] changes it takes the new text only if [sameValue] says the two differ: an undo or
 * another screen replaces what is typed, but a commit that only reformats it (pace "1." saved and
 * shown as "1") leaves the typing alone, so the next keystroke doesn't land on the wrong text.
 */
@Composable
fun CommitField(
    value: String,
    onCommit: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    decimal: Boolean = false,
    placeholder: String? = null,
    sameValue: (typed: String, value: String) -> Boolean = { a, b -> a == b },
) {
    var text by remember { mutableStateOf(value) }
    var shown by remember { mutableStateOf(value) }
    if (value != shown) {
        shown = value
        if (!sameValue(text, value)) text = value
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onCommit(it) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        textStyle = if (numeric) MaterialTheme.typography.bodyLarge.copy(fontFamily = Mono) else MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.small,
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

/**
 * An integer field that only reports whole numbers from [min] to [max]; blanks, junk and numbers out of range
 * wait for more typing, so nothing is saved that would then be clamped back under the cursor.
 */
@Composable
fun IntField(value: Int, onCommit: (Int) -> Unit, label: String, modifier: Modifier = Modifier, min: Int = 1, max: Int = Int.MAX_VALUE) {
    CommitField(
        value.toString(), { s -> s.trim().toIntOrNull()?.takeIf { it in min..max }?.let(onCommit) }, label, modifier, numeric = true,
        sameValue = { a, b -> a.trim().toIntOrNull() == b.trim().toIntOrNull() },
    )
}

/** A pace field: pages per lesson, with a comma or a point. Only finite, non-negative values are reported. */
@Composable
fun PaceField(value: Double, onCommit: (Double) -> Unit, modifier: Modifier = Modifier) {
    CommitField(
        value.plain(), { s -> Settings.parsePace(s)?.let(onCommit) }, "Pace you can sustain", modifier, numeric = true, decimal = true,
        sameValue = { a, b -> Settings.parsePace(a) == Settings.parsePace(b) },
    )
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
            shape = MaterialTheme.shapes.small,
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
            shape = MaterialTheme.shapes.small,
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

/** A delete control drawn as a plain ×, in a 48dp touch target. */
@Composable
fun RemoveButton(what: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        shape = CircleShape,
        modifier = Modifier.size(48.dp).semantics { contentDescription = "Remove $what" },
    ) { Text("×", style = MaterialTheme.typography.headlineSmall, color = Pacer.colors.muted) }
}

/** One row in a setup list (§6 List row): a title, an optional chip, a mono detail line, and a remove button. */
@Composable
fun ListItemRow(title: String, detail: String, tag: String? = null, warn: Boolean = false, onRemove: () -> Unit) {
    ListGroup {
        Row(Modifier.fillMaxWidth().padding(start = Space.l, top = Space.xs, bottom = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    if (tag != null) Tag(tag, Modifier.padding(start = Space.s))
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

/**
 * A status chip (§6): a pill in `labelSmall`, sentence case. On paper and surface it takes the accent's soft tint,
 * or warnSoft with [warn]; [onTile] puts it on a tile, as a surface pill at 70%.
 */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, warn: Boolean = false, onTile: Boolean = false) {
    val c = Pacer.colors
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = when {
            warn -> c.warnSoft
            onTile -> c.surface.copy(alpha = 0.7f)
            else -> c.accentSoft
        },
        contentColor = when {
            warn -> c.onWarnSoft
            onTile -> c.onTile
            else -> c.onAccentSoft
        },
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.padding(horizontal = Space.s, vertical = 2.dp))
    }
}

/** The "Add a …" button at the foot of each list: a secondary pill. */
@Composable
fun AddButton(text: String, onClick: () -> Unit) {
    SecondaryButton("+ $text", onClick, Modifier.fillMaxWidth())
}

/**
 * A warning banner (§6): a full-width warnSoft block with 16dp corners and on-warnSoft text. With [onFix] it links to
 * the screen that fixes the problem.
 */
@Composable
fun ErrorBox(text: String, onFix: (() -> Unit)? = null) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Pacer.colors.warnSoft,
        contentColor = Pacer.colors.onWarnSoft,
        modifier = Modifier.fillMaxWidth().then(if (onFix != null) Modifier.clickable(role = Role.Button, onClick = onFix) else Modifier),
    ) {
        Row(Modifier.padding(horizontal = Space.l, vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (onFix != null) {
                Text("Fix ›", style = MaterialTheme.typography.labelLarge, color = Pacer.colors.warn, modifier = Modifier.padding(start = Space.s))
            }
        }
    }
}

/** A tappable row that opens another screen (§6 List row): a title, a muted summary, and a chevron. */
@Composable
fun LinkRow(title: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted)
        }
        Text("›", style = MaterialTheme.typography.headlineSmall, color = Pacer.colors.muted, modifier = Modifier.padding(start = Space.s))
    }
}

/** The scrolling body most screens share, inset by the scaffold's [padding]: 16dp edges, 12dp between items. */
@Composable
fun ScreenList(padding: PaddingValues, spacing: Dp = Space.m, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(
            start = Space.l, end = Space.l,
            top = padding.calculateTopPadding() + Space.m, bottom = padding.calculateBottomPadding() + Space.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** A heading over a run of rows, such as "Books" or a class name. */
@Composable
fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground, modifier = modifier.padding(top = Space.s).semantics { heading() },
    )
}

/** A tappable row on its own list group: a title, a muted summary, and a chevron. With [warn] the summary is rust. */
@Composable
fun CardRow(title: String, detail: String, onClick: () -> Unit, warn: Boolean = false) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Pacer.colors.surface,
    ) {
        Row(Modifier.heightIn(min = 48.dp).padding(horizontal = Space.l, vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = if (warn) Pacer.colors.warn else Pacer.colors.muted)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = Pacer.colors.muted, modifier = Modifier.padding(start = Space.s))
        }
    }
}

/** The dialog behind every "Add a …" button: [content] holds the fields, and Add stays off until [canAdd]. */
@Composable
fun AddDialogFrame(
    title: String,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    confirm: String = "Add",
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.m)) { content() } },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = { TextButton(onClick = { onAdd(); onDismiss() }, enabled = canAdd) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Asks before deleting something big. Undo can still bring it back. */
@Composable
fun ConfirmDelete(name: String, detail: String, onDelete: () -> Unit, onDismiss: () -> Unit) =
    Confirm("Delete “$name”?", detail, "Delete", onDelete, onDismiss)

/** Asks before an edit that throws work away, with [action] in the warning colour. */
@Composable
fun Confirm(title: String, detail: String, action: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(detail) },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(action, color = Pacer.colors.warn) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
