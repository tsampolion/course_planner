package gr.edusuite.pacer.ui

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.ui.theme.Mono
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

/** The four steps to a first schedule, as the welcome guide lists them. */
private val STEPS = listOf(
    "School year" to "Its first and last teaching day, and the days off.",
    "Class" to "The lesson length and the pace you can sustain.",
    "Books and groups" to "Each book's first and last page, and each group's lesson days.",
    "Schedule" to "Open a group to see the pages for every lesson, and tick lessons as you teach them.",
)

/**
 * What the app does and the steps to a first schedule. Home shows it once, on a launch with
 * no school years; Settings shows it again. With [onStart] it offers to create a school year.
 */
@Composable
fun WelcomeDialog(onStart: (() -> Unit)?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Welcome to $APP_NAME") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                Text(
                    "$APP_NAME spreads your coursebook pages over the lessons left in the term, " +
                        "so you can see whether each group will finish in time.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Four steps to your first schedule:", style = MaterialTheme.typography.bodyMedium)
                STEPS.forEachIndexed { i, (title, detail) ->
                    Row {
                        Text("${i + 1}", fontFamily = Mono, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(22.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted)
                        }
                    }
                }
                Hint("Each screen says what to do next while it's empty. You can open this guide again from Settings.")
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = Pacer.colors.surface,
        tonalElevation = 0.dp,
        confirmButton = {
            if (onStart != null) TextButton(onClick = { onStart(); onDismiss() }) { Text("Create a school year") }
            else TextButton(onClick = onDismiss) { Text("Got it") }
        },
        dismissButton = if (onStart != null) ({ TextButton(onClick = onDismiss) { Text("Later") } }) else null,
    )
}

/**
 * A "Next step" card for a screen that is still empty. It says what to do and goes away
 * once that is done, so nothing about it is stored. [action] points to another screen or dialog;
 * leave it out when the screen's own add button sits right below.
 */
@Composable
fun GuideCard(title: String, text: String, modifier: Modifier = Modifier, action: Pair<String, () -> Unit>? = null) {
    val c = Pacer.colors
    // A flat accent-soft panel (style guide §4): no border or side bar.
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(c.accentSoft),
    ) {
        Column(Modifier.padding(start = Space.l, end = Space.l, top = Space.m, bottom = if (action == null) Space.m else Space.xs)) {
            Text("Next step", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                title, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 2.dp).semantics { heading() },
            )
            Text(text, style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(top = Space.xs))
            action?.let { (label, onClick) ->
                OutlinedButton(onClick = onClick, modifier = Modifier.padding(top = Space.s, bottom = Space.xs)) { Text(label) }
            }
        }
    }
}

/** What the schedule is: arithmetic, to be checked against the classroom. */
private const val ARITHMETIC =
    "The schedule is arithmetic, not advice: it spreads each book's pages evenly over the lessons left. " +
        "It can't know how your class is really doing, so go by what you actually teach"

/** Shown above every group's schedule. */
const val SCHEDULE_NOTE = "$ARITHMETIC and record it here as you go."

/** Printed under every group's heading on the printed sheet. */
const val PRINT_NOTE = "$ARITHMETIC."

/** A quiet note with a thin bar, for [SCHEDULE_NOTE]. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(Pacer.colors.line))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted, modifier = Modifier.padding(start = Space.m, top = 2.dp, bottom = 2.dp))
    }
}
