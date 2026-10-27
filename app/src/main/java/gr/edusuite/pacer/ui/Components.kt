package gr.edusuite.pacer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import gr.edusuite.pacer.R
import gr.edusuite.pacer.ui.theme.Motion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Rhythm
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
    val interaction = remember { MutableInteractionSource() }
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.xs), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, interactionSource = interaction, modifier = modifier.pressFeedback(interaction).fillMaxWidth(), shape = MaterialTheme.shapes.large, color = color, contentColor = contentColor, content = body)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = color, contentColor = contentColor, content = body)
    }
}

/** A primary pill button (§6 Buttons): accent fill, 48dp tall. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, completed: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val motion = animationsEnabled()
    val savedLabel = stringResource(R.string.component_saved)
    Button(onClick = onClick, interactionSource = interaction, enabled = enabled, modifier = modifier.pressFeedback(interaction).heightIn(min = 48.dp), shape = CircleShape) {
        AnimatedContent(completed, transitionSpec = {
            if (motion) fadeIn(Motion.content()) togetherWith fadeOut(Motion.content())
            else EnterTransition.None togetherWith ExitTransition.None
        }, label = "save completion") { done ->
            if (done) AppIcon(AppIcons.Check, contentDescription = savedLabel)
            else Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A secondary pill button: surfaceContainer fill with ink text. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick, interactionSource = interaction, enabled = enabled, modifier = modifier.pressFeedback(interaction).heightIn(min = 48.dp), shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Pacer.colors.surfaceContainer, contentColor = Pacer.colors.ink),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** A tertiary button: text only, in the accent (or [color]). */
@Composable
fun TertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = Color.Unspecified) {
    val interaction = remember { MutableInteractionSource() }
    PaletteTextButton(onClick = onClick, interactionSource = interaction, enabled = enabled, modifier = modifier.pressFeedback(interaction).heightIn(min = 48.dp), shape = CircleShape) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/**
 * An icon button on a 40dp surface pill inside a 48dp touch target (§6 Buttons). [label] is what TalkBack reads; a
 * button that isn't [enabled] draws its symbol in `outline`.
 */
@Composable
fun PillIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier.pressFeedback(interaction)
            .size(48.dp)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = Pacer.colors.surface, contentColor = if (enabled) Pacer.colors.ink else Pacer.colors.outline, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}

/**
 * Shown when a list has nothing yet (§6 EmptyState): a 120dp circle in a tile tint holding a symbol, a title, a muted
 * line and an optional pill action.
 */
@Composable
fun EmptyState(symbol: ImageVector, title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        Surface(shape = CircleShape, color = Pacer.colors.tileSky, contentColor = Pacer.colors.onTile, modifier = Modifier.size(120.dp)) {
            Box(contentAlignment = Alignment.Center) { AppIcon(symbol, Modifier.size(48.dp)) }
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
    isValid: (String) -> Boolean = { true },
    errorMessage: String = stringResource(R.string.component_invalid_value),
    error: Boolean = false,
) {
    var text by remember { mutableStateOf(value) }
    var shown by remember { mutableStateOf(value) }
    if (value != shown) {
        shown = value
        if (!sameValue(text, value)) text = value
    }
    OutlinedTextField(
            colors = paletteFieldColors(),
        value = text,
        onValueChange = { text = it; if (isValid(it)) onCommit(it) },
        label = { Text(label) },
        isError = error || !isValid(text),
        supportingText = if (error || !isValid(text)) { { Text(errorMessage) } } else null,
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        textStyle = if (numeric) MaterialTheme.typography.bodyLarge.merge(Figures) else MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                decimal -> KeyboardType.Decimal
                numeric -> KeyboardType.Number
                else -> KeyboardType.Text
            },
        ),
        modifier = modifier.fieldFeedback(error || !isValid(text)),
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
        isValid = { it.trim().toIntOrNull()?.let { n -> n in min..max } == true },
        errorMessage = stringResource(R.string.component_integer_range, min, max),
    )
}

/** A pace field: pages per lesson, with a comma or a point. Only finite, non-negative values are reported. */
@Composable
fun PaceField(value: Double, onCommit: (Double) -> Unit, modifier: Modifier = Modifier) {
    CommitField(
        value.plain(), { s -> Settings.parsePace(s)?.let(onCommit) }, stringResource(R.string.component_pace), modifier, numeric = true, decimal = true,
        sameValue = { a, b -> Settings.parsePace(a) == Settings.parsePace(b) },
        isValid = { Settings.parsePace(it) != null },
        errorMessage = stringResource(R.string.component_pace_invalid),
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
    val unset = stringResource(R.string.component_not_set)
    val dateDescription = stringResource(R.string.component_date_description, label, value?.fmt() ?: unset)
    Box(modifier) {
        OutlinedTextField(
            colors = paletteFieldColors(),
            value = value?.fmt() ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.component_not_set)) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        // The text field swallows taps; this layer takes them instead.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = Space.s)
                .clickable(role = Role.Button) { open = true }
                .semantics { contentDescription = dateDescription },
        )
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                PaletteTextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(LocalDate.ofEpochDay(Math.floorDiv(it, 86_400_000L))) }
                    open = false
                }) { Text(stringResource(R.string.component_ok)) }
            },
            dismissButton = {
                Row {
                    if (clearable && value != null) PaletteTextButton(onClick = { onChange(null); open = false }) { Text(stringResource(R.string.component_clear)) }
                    PaletteTextButton(onClick = { open = false }) { Text(stringResource(R.string.component_cancel)) }
                }
            },
        ) { ReadablePicker { DatePicker(state = state) } }
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
            colors = paletteFieldColors(),
            value = options.firstOrNull { it.first == selected }?.second ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { AppIcon(AppIcons.ArrowDropDown) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().padding(top = Space.s).clickable(role = Role.DropdownList) { open = true })
        PacerDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); open = false })
            }
        }
    }
}

/** A delete control drawn as a plain ×, in a 48dp touch target. */
@Composable
fun RemoveButton(what: String, onClick: () -> Unit) {
    val removeDescription = stringResource(R.string.component_remove_what, what)
    PaletteTextButton(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        shape = CircleShape,
        modifier = Modifier.size(48.dp).semantics { contentDescription = removeDescription },
    ) { AppIcon(AppIcons.Close) }
}

/** One row in a setup list (§6 List row): a title, an optional chip, a detail line in Figures, and a remove button. */
@Composable
fun ListItemRow(title: String, detail: String, tag: String? = null, warn: Boolean = false, onRemove: () -> Unit) {
    ListGroup {
        Row(Modifier.fillMaxWidth().padding(start = Space.l, top = Space.xs, bottom = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(icon, Modifier.padding(end = Space.m))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    if (tag != null) Tag(tag, Modifier.padding(start = Space.s))
                }
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall.merge(Figures),
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
    val interaction = remember { MutableInteractionSource() }
    Button(onClick = onClick, interactionSource = interaction, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).pressFeedback(interaction), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Pacer.colors.surfaceContainer, contentColor = Pacer.colors.ink)) {
        AppIcon(AppIcons.Add, Modifier.padding(end = Space.s))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
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
            AppIcon(AppIcons.Warning, Modifier.padding(end = Space.s))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (onFix != null) {
                Text(stringResource(R.string.component_fix), style = MaterialTheme.typography.labelLarge, color = Pacer.colors.warn, modifier = Modifier.padding(start = Space.s))
            }
        }
    }
}

/** A tappable row that opens another screen (§6 List row): a title, a muted summary, and a chevron. */
@Composable
fun LinkRow(title: String, detail: String, icon: ImageVector = AppIcons.Info, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(AppIcons.Info, Modifier.padding(end = Space.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted)
        }
        AppIcon(AppIcons.ChevronRight, Modifier.padding(start = Space.s))
    }
}

/** The scrolling body most screens share, inset by the scaffold's [padding]: 16dp edges, [Rhythm.section] between items. */
@Composable
fun ScreenList(padding: PaddingValues, spacing: Dp = Rhythm.section, content: LazyListScope.() -> Unit) {
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
            AppIcon(AppIcons.Info, Modifier.padding(end = Space.m))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = if (warn) Pacer.colors.warn else Pacer.colors.muted)
            }
            AppIcon(AppIcons.ChevronRight, Modifier.padding(start = Space.s))
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
    confirm: String = stringResource(R.string.component_add),
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.m)) { content() } },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = { PaletteTextButton(onClick = { onAdd(); onDismiss() }, enabled = canAdd) { Text(confirm) } },
        dismissButton = { PaletteTextButton(onClick = onDismiss) { Text(stringResource(R.string.component_cancel)) } },
    )
}

/**
 * A bottom sheet for a short edit (§6 Bottom sheet, §14 Act in place): a title, the fields, and the primary pill action
 * at the bottom, which stays off until [enabled]. Used for adds and renames of three fields or fewer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetFrame(
    title: String,
    primary: String,
    enabled: Boolean,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var completed by remember { mutableStateOf(false) }
    val motion = animationsEnabled()
    val saveLabel = stringResource(R.string.component_save)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = MaterialTheme.shapes.extraLarge.let { if (it is RoundedCornerShape) it.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)) else it },
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = Space.l, end = Space.l, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            content()
            PrimaryButton(primary, {
                if (!completed) {
                    onPrimary()
                    if (primary == saveLabel && motion) {
                        completed = true
                        scope.launch { delay(600); onDismiss() }
                    } else onDismiss()
                }
            }, Modifier.fillMaxWidth(), enabled = enabled && !completed, completed = completed)
        }
    }
}

/**
 * A list row that can be swiped away (§14 Act in place): swiping from the end reveals Remove behind it. The same
 * action is in the row's long-press menu and in TalkBack's custom actions (§7), so nobody has to swipe.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SwipeRow(what: String, onRemove: () -> Unit, content: @Composable () -> Unit) {
    val removeDescription = stringResource(R.string.component_remove_what, what)
    val optionsLabel = stringResource(R.string.component_options)
    val motion = animationsEnabled()
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        if (v == SwipeToDismissBoxValue.EndToStart) onRemove()
        // The row leaves with the data; the box itself never stays dismissed.
        false
    })
    var menu by remember { mutableStateOf(false) }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val scale by animateFloatAsState(if (state.progress > .5f) 1.2f else 1f, if (motion) Motion.work(gr.edusuite.pacer.ui.theme.MotionSpec.Step.PRESS) else snap(), label = "swipe action")
            Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(Pacer.colors.warnSoft).padding(horizontal = Space.l),
                contentAlignment = Alignment.CenterEnd,
            ) { Row(verticalAlignment = Alignment.CenterVertically) { AppIcon(AppIcons.Delete, Modifier.graphicsLayer { scaleX = scale; scaleY = scale }); Text(stringResource(R.string.component_remove), style = MaterialTheme.typography.labelLarge, color = Pacer.colors.onWarnSoft) } }
        },
        modifier = Modifier.animateContentSize(if (motion) Motion.settle() else snap()).semantics { customActions = listOf(CustomAccessibilityAction(removeDescription) { onRemove(); true }) },
    ) {
        Box(Modifier.combinedClickable(onClick = {}, onLongClick = { menu = true }, onLongClickLabel = optionsLabel)) {
            content()
            PacerDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.component_remove)) }, onClick = { menu = false; onRemove() })
            }
        }
    }
}

/** One block of a screen (a heading with its rows and hint): its parts are [Rhythm.inSection] apart. */
@Composable
fun SectionColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Rhythm.inSection), content = content)
}
