package gr.edusuite.pacer.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import gr.edusuite.pacer.ui.theme.Pacer

/** Material primary is a fill; text-only buttons use the palette's readable foreground instead. */
@Composable
fun PaletteTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(contentColor = Pacer.colors.accentText),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.TextButton(
    onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors,
    contentPadding = contentPadding, interactionSource = interactionSource, content = content,
)

/** Focus labels, outlines and cursors stay visible even when primary is a pale saffron fill. */
@Composable
fun paletteFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pacer.colors.accentText,
    focusedLabelColor = Pacer.colors.accentText,
    cursorColor = Pacer.colors.accentText,
)

/** Date picker's internal today/year/navigation text also uses a foreground, rather than fill-primary. */
@Composable
fun ReadablePicker(content: @Composable () -> Unit) {
    val c = Pacer.colors
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = c.accentText, onPrimary = c.paper), content = content)
}
