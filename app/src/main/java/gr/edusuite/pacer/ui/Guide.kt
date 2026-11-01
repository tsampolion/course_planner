package gr.edusuite.pacer.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import gr.edusuite.pacer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.ui.theme.Figures
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
 * The welcome page (decision 20, D9): a full page with the logo, what the app does and the four steps to a first
 * schedule as a numbered list. Settings → Getting started shows it; Got it closes it. It has no background pattern.
 * A fresh install meets [FirstRunPage] first.
 */
@Composable
fun WelcomePage(onDismiss: () -> Unit) {
    val c = Pacer.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = c.paper, contentColor = c.ink) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).systemBarsPadding()
                    .padding(horizontal = Space.xl, vertical = Space.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    Logo(56.dp)
                    Text(
                        stringResource(R.string.guide_welcome, stringResource(R.string.app_name)),
                        style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = Space.s).semantics { heading() },
                    )
                    Text(stringResource(R.string.guide_intro, stringResource(R.string.app_name)), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.guide_four_steps), style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(top = Space.s))
                    Column {
                        STEPS.forEachIndexed { i, (title, detail) ->
                            HorizontalDivider(color = c.line)
                            Row(Modifier.fillMaxWidth().padding(vertical = Space.m).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                                Box(Modifier.size(28.dp).background(c.accent, CircleShape), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.guide_step_number, i + 1), style = MaterialTheme.typography.labelLarge.merge(Figures), color = c.onAccent)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(detail), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                                }
                            }
                        }
                    }
                    Hint(stringResource(R.string.guide_hint))
                    PrimaryButton(stringResource(R.string.guide_got_it), onDismiss, Modifier.fillMaxWidth().padding(top = Space.m))
                }
            }
        }
    }
}

/**
 * A next-step card for a screen that is still empty. It says what to do and goes away
 * once that is done, so nothing about it is stored. [action] points to another screen or dialog;
 * leave it out when the screen's own add button sits right below.
 */
/**
 * A show-once hint (design decision 19): a muted line with "Got it", which remembers [id] in Settings.seenTips for the
 * whole app. Nothing shows once it was seen, until Settings → Show tips again.
 */
@Composable
fun ShowOnceHint(id: String, seen: Set<String>, text: String, onSeen: (String) -> Unit, modifier: Modifier = Modifier) {
    if (id in seen) return
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        AppIcon(AppIcons.Info, Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Pacer.colors.muted, modifier = Modifier.weight(1f))
        TertiaryButton(stringResource(R.string.hint_got_it), { onSeen(id) })
    }
}

@Composable
fun GuideCard(title: String, text: String, modifier: Modifier = Modifier, action: Pair<String, () -> Unit>? = null, onDismiss: (() -> Unit)? = null) {
    val c = Pacer.colors
    Tile(color = c.tile2, modifier = modifier) {
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
