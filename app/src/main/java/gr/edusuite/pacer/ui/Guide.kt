package gr.edusuite.pacer.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import gr.edusuite.pacer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.ui.theme.Mono
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

/** The four steps to a first schedule, as the welcome guide lists them. */
private val STEPS = listOf(
    R.string.guide_step_1_title to R.string.guide_step_1_detail,
    R.string.guide_step_2_title to R.string.guide_step_2_detail,
    R.string.guide_step_3_title to R.string.guide_step_3_detail,
    R.string.guide_step_4_title to R.string.guide_step_4_detail,
)

/**
 * What the app does and the steps to a first schedule. Home shows it once, on a launch with
 * no school years; Settings shows it again. With [onStart] it offers to create a school year.
 */
@Composable
fun WelcomeDialog(onStart: (() -> Unit)?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.guide_welcome, stringResource(R.string.app_name))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                Text(
                    stringResource(R.string.guide_intro, stringResource(R.string.app_name)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(stringResource(R.string.guide_four_steps), style = MaterialTheme.typography.bodyMedium)
                STEPS.forEachIndexed { i, (title, detail) ->
                    Tile(color = if (i % 2 == 0) Pacer.colors.tileLilac else Pacer.colors.tileSky) {
                        Surface(shape = CircleShape, color = Pacer.colors.accentSoft, contentColor = Pacer.colors.onAccentSoft) {
                            Box(Modifier.sizeIn(minWidth = Space.xxl, minHeight = Space.xxl).padding(Space.xs), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.guide_step_number, i + 1), style = MaterialTheme.typography.labelMedium, fontFamily = Mono)
                            }
                        }
                        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        Text(stringResource(detail), style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.onTileMuted)
                    }
                }
                Hint(stringResource(R.string.guide_hint))
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = {
            if (onStart != null) PrimaryButton(stringResource(R.string.guide_create_year), { onStart(); onDismiss() })
            else PrimaryButton(stringResource(R.string.guide_got_it), onDismiss)
        },
        dismissButton = if (onStart != null) ({ TertiaryButton(stringResource(R.string.guide_not_now), onDismiss) }) else null,
    )
}

/**
 * A next-step card for a screen that is still empty. It says what to do and goes away
 * once that is done, so nothing about it is stored. [action] points to another screen or dialog;
 * leave it out when the screen's own add button sits right below.
 */
@Composable
fun GuideCard(title: String, text: String, modifier: Modifier = Modifier, action: Pair<String, () -> Unit>? = null, onDismiss: (() -> Unit)? = null) {
    val c = Pacer.colors
    Tile(color = c.tileSky, modifier = modifier) {
        Text(stringResource(R.string.guide_next_step), style = MaterialTheme.typography.labelMedium, color = c.onTileMuted)
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.onTileMuted)
        action?.let { (label, onClick) ->
            TertiaryButton(label, onClick, Modifier.padding(top = Space.s), color = c.onTile)
        }
        onDismiss?.let { TertiaryButton(stringResource(R.string.guide_dismiss), it, color = c.onTile) }
    }
}

/** What the schedule is: arithmetic, to be checked against the classroom. */
private const val ARITHMETIC =
    "The schedule is arithmetic, not advice: it spreads each book's pages evenly over the lessons left. " +
        "It can't know how your class is really doing, so go by what you actually teach"

/** Printed under every group's heading on the printed sheet. */
const val PRINT_NOTE = "$ARITHMETIC."

/** A quiet note with a thin bar for the schedule explanation. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(Pacer.colors.line))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted, modifier = Modifier.padding(start = Space.m, top = 2.dp, bottom = 2.dp))
    }
}
