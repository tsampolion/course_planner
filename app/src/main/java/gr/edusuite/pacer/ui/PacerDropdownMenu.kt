package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import gr.edusuite.pacer.ui.theme.PacerMenuShape

/** Menus cannot use the theme's pill-shaped extraSmall default: it clips corner rows. */
@Composable
internal fun PacerDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = PacerMenuShape,
        content = content,
    )
}
