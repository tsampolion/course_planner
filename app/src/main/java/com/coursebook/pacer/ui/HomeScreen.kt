package com.coursebook.pacer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coursebook.pacer.R
import com.coursebook.pacer.engine.fmt
import com.coursebook.pacer.model.AppData
import com.coursebook.pacer.model.SchoolYear
import com.coursebook.pacer.model.suggestYear
import java.time.LocalDate

/** "3 classes", "1 class", or [none] for zero. */
fun count(n: Int, one: String, many: String = one + "s", none: String = "None yet") =
    if (n == 0) none else "$n ${if (n == 1) one else many}"

/** "9 Sep 2026 – 31 May 2027", or a prompt when either date is missing. */
fun SchoolYear.dates(): String {
    val s = start
    val e = end
    return if (s != null && e != null) "${s.fmt()} – ${e.fmt()}" else "Dates not set"
}

/** The start screen: the logo at the top, the school-year picker in the middle, and Settings at the bottom right. */
@Composable
fun HomeScreen(data: AppData, vm: PacerViewModel, padding: PaddingValues) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val years = remember(data.years) { data.years.sortedByDescending { it.start } }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 40.dp, bottom = padding.calculateBottomPadding() + 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Logo()
            Spacer(Modifier.height(16.dp))
            Text(
                APP_NAME,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            // The picker takes the space left and sits in its middle, scrolling once there are many years.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Eyebrow("School year")
                    if (years.isEmpty()) Hint("No school years yet. Add one to start planning.")
                    years.forEach { y ->
                        CardRow(y.name.ifBlank { "School year" }, "${y.dates()} · ${count(y.classes.size, "class", "classes", "no classes")}", { vm.openYear(y.id) })
                    }
                    AddButton("New school year") { adding = true }
                }
            }
        }
        TextButton(
            onClick = { vm.open(Screen.Settings) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = padding.calculateBottomPadding() + 12.dp)
                .size(56.dp)
                .semantics { contentDescription = "Settings" },
            contentPadding = PaddingValues(0.dp),
        ) {
            // U+FE0E asks for the text glyph rather than an emoji.
            Text("⚙︎", fontSize = 28.sp, color = MaterialTheme.colorScheme.onBackground)
        }
    }

    if (adding) NewYearDialog(data.years, vm) { adding = false }
}

@Composable
private fun NewYearDialog(years: List<SchoolYear>, vm: PacerViewModel, onDismiss: () -> Unit) {
    val draft = remember { suggestYear(years, LocalDate.now()) }
    var name by rememberSaveable { mutableStateOf(draft.name) }
    var start by remember { mutableStateOf<LocalDate?>(draft.start) }
    var end by remember { mutableStateOf<LocalDate?>(draft.end) }
    var holidays by rememberSaveable { mutableStateOf(true) }
    AddDialogFrame("New school year", name.isNotBlank(), { vm.newYear(name.trim(), start, end, holidays && start != null && end != null) }, onDismiss, confirm = "Create") {
        CommitField(name, { name = it }, "Name", placeholder = draft.name)
        DateField(start, { start = it }, "Starts")
        DateField(end, { end = it }, "Ends")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = holidays, onCheckedChange = { holidays = it })
            Text("Add the Greek public holidays as days off", style = MaterialTheme.typography.bodyMedium)
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
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = "$APP_NAME logo", modifier = Modifier.requiredSize(132.dp))
    }
}
