package com.coursebook.pacer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coursebook.pacer.R
import com.coursebook.pacer.engine.fmtShort
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.ui.theme.Pacer

/** One button on Home: where it goes, and a one-line summary of what is there. */
private data class Tile(val title: String, val detail: String, val warn: Boolean = false, val onClick: () -> Unit)

private fun count(n: Int, one: String, many: String = one + "s", none: String = "None yet") =
    if (n == 0) none else "$n ${if (n == 1) one else many}"

/** The start screen: the logo, the app's name, the open plan, and a button for each part of the app. */
@Composable
fun HomeScreen(plan: Plan, planCount: Int, vm: PacerViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val problems = remember(plan) { Nav.problems(plan).size }
    val s = plan.start
    val e = plan.end
    val tiles = listOf(
        Tile(
            "Schedules",
            if (problems > 0) count(problems, "problem") else count(plan.groups.size, "group"),
            warn = problems > 0,
        ) { vm.open(Screen.Schedules) },
        Tile("Plans", count(planCount, "plan")) { vm.open(Screen.Plans) },
        Tile("Term", if (s != null && e != null) "${s.fmtShort()} – ${e.fmtShort()}" else "Dates not set") { vm.open(Screen.Term) },
        Tile("Groups", count(plan.groups.size, "group")) { vm.open(Screen.Groups) },
        Tile("Days off", count(plan.closures.size, "day off", "days off")) { vm.open(Screen.DaysOff) },
        Tile("Tests & worksheets", count(plan.events.size, "entry", "entries", "None scheduled")) { vm.open(Screen.Tests) },
        Tile("Books", count(plan.books.size, "book")) { vm.open(Screen.Books) },
        Tile("Homework", count(plan.homework.size, "assignment", none = "Nothing assigned")) { vm.open(Screen.Homework) },
        Tile("Print or save PDF", "Every group's schedule") { PrintSheet.print(context, plan) },
        Tile("Settings", "Theme, defaults, about") { vm.open(Screen.Settings) },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 40.dp, bottom = padding.calculateBottomPadding() + 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo()
        Spacer(Modifier.height(16.dp))
        Text(
            "Coursebook Pacer",
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            plan.name, style = MaterialTheme.typography.bodyMedium, color = Pacer.colors.muted,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(32.dp))
        // Two columns, centred and capped so tablets and landscape don't stretch the buttons.
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tiles.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { t -> TileButton(t, Modifier.weight(1f).fillMaxHeight()) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** The launcher icon, drawn from the same artwork: the open book on the teal tile. */
@Composable
private fun Logo() {
    Box(
        Modifier.size(88.dp).clip(RoundedCornerShape(22.dp)).background(colorResource(R.color.accent)),
        contentAlignment = Alignment.Center,
    ) {
        // The artwork sits in the middle of a 108dp canvas; scaling it up crops the empty margin.
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Coursebook Pacer logo", modifier = Modifier.requiredSize(132.dp))
    }
}

@Composable
private fun TileButton(t: Tile, modifier: Modifier) {
    Surface(
        onClick = t.onClick,
        modifier = modifier.heightIn(min = 76.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (t.warn) Pacer.colors.warn else Pacer.colors.line),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                t.detail, style = MaterialTheme.typography.bodySmall,
                color = if (t.warn) Pacer.colors.warn else Pacer.colors.muted,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
