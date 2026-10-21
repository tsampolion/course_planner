package gr.edusuite.pacer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

/*
 * The ledger rows of the list screens (3.5.0, docs/PACER_DESIGN.md D6): one line per item on hairline rows, no
 * leading icon, an edge colour where the item has a kind, and one status mark at the end.
 */

/** A list section's head: its title as a heading, and an optional text action in a 48dp target ("+ Add", or "Edit" without [plus]). */
@Composable
fun SectionHead(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null, plus: Boolean = true, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = Space.s), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Pacer.colors.ink, modifier = Modifier.weight(1f).semantics { heading() })
        trailing?.invoke()
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = Space.s)) {
                if (plus) AppIcon(AppIcons.Add, Modifier.size(18.dp).padding(end = Space.xs))
                Text(action, style = MaterialTheme.typography.labelLarge, color = Pacer.colors.accent)
            }
        }
    }
}

/** The surface a section's rows sit on, with hairlines between them. */
@Composable
fun RowGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    ListGroup(modifier) { Column(content = content) }
}

/** The hairline under every row but the last. */
@Composable
fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = Space.l), color = Pacer.colors.line)
}

/**
 * One ledger row: an optional [edge] for its kind (or a leading [icon] on a row that opens another screen), an optional [date] column in figures, the [title] over a muted
 * [detail], and [trailing] (a status, a chevron, a ⋮). With [onClick] the row is one target; TalkBack reads it as one
 * item. [warn] turns the detail rust.
 */
@Composable
fun LedgerRow(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    date: String? = null,
    edge: Color? = null,
    icon: ImageVector? = null,
    warn: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = Pacer.colors
    // Large text: the date sits over the title instead of taking a column of its own.
    val large = rememberLargeText()
    val side = date != null && !large
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (edge != null) Box(Modifier.width(4.dp).fillMaxHeight().padding(vertical = Space.s).background(edge))
        if (icon != null) AppIcon(icon, Modifier.padding(start = Space.l))
        val start = if (edge != null || icon != null) Space.m else Space.l
        if (side) Text(
            date!!, style = MaterialTheme.typography.titleSmall.merge(Figures), color = c.ink,
            modifier = Modifier.width(DATE_W).padding(start = start),
        )
        Column(
            Modifier.weight(1f).padding(start = if (side) Space.s else start, end = Space.s, top = Space.s, bottom = Space.s),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (date != null && large) Text(date, style = MaterialTheme.typography.titleSmall.merge(Figures), color = c.ink)
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!detail.isNullOrEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall.merge(Figures), color = if (warn) c.warn else c.muted)
        }
        if (trailing != null) Row(Modifier.padding(end = Space.xs), verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

/** The date column's width: room for "Wed 28 Oct" or "23 Dec – 6 Jan". */
private val DATE_W = 112.dp

/** A row's chevron, decorative: the row itself is the target. */
@Composable
fun Chevron() {
    AppIcon(AppIcons.ChevronRight, Modifier.padding(horizontal = Space.s))
}

/** A row's ⋮ with Remove: the visible way to remove it, beside the swipe and the long-press (decision 21). */
@Composable
fun RemoveMenu(what: String, onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = stringResource(R.string.rows_options, what)
    Box {
        TextButton(
            onClick = { open = true }, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(48.dp).semantics { contentDescription = label },
        ) { AppIcon(AppIcons.MoreVert) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.component_remove), color = Pacer.colors.warn) },
                leadingIcon = { AppIcon(AppIcons.Delete) },
                onClick = { open = false; onRemove() },
            )
        }
    }
}

/** The quiet actions at a screen's foot: a muted one at the start and a rust one at the end, both 48dp. */
@Composable
fun FootActions(start: Pair<String, () -> Unit>?, end: Pair<String, () -> Unit>, startEnabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(top = Space.l), verticalAlignment = Alignment.CenterVertically) {
        if (start != null) TertiaryButton(start.first, start.second, enabled = startEnabled, color = Pacer.colors.muted)
        Box(Modifier.weight(1f))
        TertiaryButton(end.first, end.second, color = Pacer.colors.warn)
    }
}
