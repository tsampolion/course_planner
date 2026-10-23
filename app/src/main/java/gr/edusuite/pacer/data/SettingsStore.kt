package gr.edusuite.pacer.data

import android.content.Context
import gr.edusuite.pacer.model.PaletteChoice
import gr.edusuite.pacer.model.PlanGrouping
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.ThemeMode

/** App preferences in SharedPreferences (`settings.xml`), kept apart from the plans file. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): Settings = Settings(
        planGrouping = PlanGrouping.parse(runCatching { prefs.getString(GROUPING, null) }.getOrNull()),
        theme = ThemeMode.parse(prefs.getString(THEME, null)),
        palette = PaletteChoice.parse(runCatching { prefs.getString(PALETTE, null) }.getOrNull()),
        lessonMins = prefs.getInt(MINS, Settings.DEFAULT_MINS),
        // A string, not a float, so 4.1 reads back as 4.1.
        pace = prefs.getString(PACE, null)?.toDoubleOrNull() ?: Settings.DEFAULT_PACE,
        welcomeSeen = prefs.getBoolean(WELCOME, false),
        name = prefs.getString(NAME, null).orEmpty(),
        // A copy: the set SharedPreferences returns must not be changed or kept.
        seenTips = prefs.getStringSet(TIPS, null)?.toSet().orEmpty(),
    ).sanitised()

    fun save(s: Settings) {
        prefs.edit()
            .putString(THEME, s.theme.name)
            .putString(GROUPING, s.planGrouping.name)
            .putString(PALETTE, s.palette.name)
            .putInt(MINS, s.lessonMins)
            .putString(PACE, s.pace.toString())
            // The schedule's By month view went in 3.4.0; its old setting goes too.
            .remove(BY_MONTH)
            .putBoolean(WELCOME, s.welcomeSeen)
            .putString(NAME, s.name)
            .putStringSet(TIPS, s.seenTips.toSet())
            .apply()
    }

    private companion object {
        /** Backed up by name in data_extraction_rules.xml. */
        const val FILE = "settings"
        const val GROUPING = "planGrouping"
        const val THEME = "theme"
        const val PALETTE = "palette"
        const val MINS = "lessonMins"
        const val PACE = "pace"
        /** No longer stored (3.4.0); removed from older installs on the next save. */
        const val BY_MONTH = "scheduleByMonth"
        const val WELCOME = "welcomeSeen"
        const val NAME = "name"
        const val TIPS = "seenTips"
    }
}
