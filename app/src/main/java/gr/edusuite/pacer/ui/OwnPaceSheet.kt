package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.input.KeyboardType
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.*

/** A draft only: no group changes until Save. */
@Composable
fun OwnPaceSheet(report: GroupReport, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable(report.group.id) { mutableStateOf(report.rate.fixed2()) }
    val rate = ownPaceRate(text)
    val preview = remember(report, rate) {
        rate?.let { value ->
            runCatching {
                val custom = report.customAtRate(value)
                GroupReport(report.plan, report.group.copy(custom = custom), Source.CUSTOM)
            }.getOrNull()
        }
    }
    PacerSheet(stringResource(R.string.stats_own_pace_title), onDismiss, SheetAction(stringResource(R.string.group_save), preview != null) { if (preview != null) rate?.let(onSave) },
        dirty = rememberDraftChanged(text)) {
        Hint(stringResource(R.string.stats_own_pace_hint))
        OutlinedTextField(
            value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.stats_own_pace_field)) },
            singleLine = true, isError = preview == null, colors = paletteFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = { if (preview == null) Text(stringResource(R.string.stats_own_pace_error)) },
            modifier = Modifier.fillMaxWidth(),
        )
        preview?.let { draft ->
            val figures = statsFigures(draft)
            Text(figures.end?.let { stringResource(R.string.stats_own_pace_preview, it.fmtShort()) }
                ?: stringResource(if ((draft.coverage()?.overflow ?: 0) > 0) R.string.stats_beyond_term else R.string.stats_no_end))
            draft.shortfallCoverage()?.let { coverage -> Text(pluralStringResource(R.plurals.stats_covered_short, coverage.short, coverage.covered, coverage.short)) }
            val overflow = draft.coverage()?.overflow ?: 0
            if (overflow > 0) Hint(pluralStringResource(R.plurals.stats_own_pace_overflow, overflow, overflow))
        }
    }
}
