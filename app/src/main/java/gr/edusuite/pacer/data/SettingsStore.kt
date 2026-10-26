package gr.edusuite.pacer.data

import android.content.Context
import gr.edusuite.pacer.update.storedUpdate
import gr.edusuite.pacer.model.PaletteChoice
import gr.edusuite.pacer.model.PlanGrouping
import gr.edusuite.pacer.model.ClassOrder
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.ThemeMode
import java.time.LocalDate

/** What the view model needs from the preferences, so tests can supply their own. */
interface SettingsPort {
    fun load(): Settings
    fun save(s: Settings)
}

/** App preferences in SharedPreferences (`settings.xml`), kept apart from the plans file. */
class SettingsStore(context: Context) : SettingsPort {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun load(): Settings = Settings(
        autoUpdateCheck = runCatching { prefs.getBoolean(AUTO_UPDATE, true) }.getOrDefault(true),
        lastUpdateCheck = runCatching { prefs.getLong(LAST_UPDATE_CHECK, 0L) }.getOrDefault(0L),
        availableUpdate = storedUpdate(
            runCatching { prefs.getString(UPDATE_VERSION, null) }.getOrNull(),
            runCatching { prefs.getString(UPDATE_NOTES, null) }.getOrNull(),
            runCatching { prefs.getString(UPDATE_APK, null) }.getOrNull(),
            runCatching { prefs.getString(UPDATE_PAGE, null) }.getOrNull(),
        ),
        classOrder = ClassOrder.parse(runCatching { prefs.getString(CLASS_ORDER, null) }.getOrNull()),
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
        autoTickFrom = runCatching { LocalDate.parse(prefs.getString(AUTO_TICK_FROM, null)) }.getOrNull(),
    ).sanitised()

    override fun save(s: Settings) {
        prefs.edit()
            .putBoolean(AUTO_UPDATE, s.autoUpdateCheck)
            .putLong(LAST_UPDATE_CHECK, s.lastUpdateCheck)
            .apply {
                val u = s.availableUpdate
                if (u == null) {
                    remove(UPDATE_VERSION); remove(UPDATE_NOTES); remove(UPDATE_APK); remove(UPDATE_PAGE)
                } else {
                    putString(UPDATE_VERSION, u.version); putString(UPDATE_NOTES, u.notes)
                    putString(UPDATE_APK, u.apkUrl); putString(UPDATE_PAGE, u.pageUrl)
                }
            }
            .putString(THEME, s.theme.name)
            .putString(CLASS_ORDER, s.classOrder.name)
            .putString(GROUPING, s.planGrouping.name)
            .putString(PALETTE, s.palette.name)
            .putInt(MINS, s.lessonMins)
            .putString(PACE, s.pace.toString())
            // The schedule's By month view went in 3.4.0; its old setting goes too.
            .remove(BY_MONTH)
            .putBoolean(WELCOME, s.welcomeSeen)
            .putString(NAME, s.name)
            .putStringSet(TIPS, s.seenTips.toSet())
            .apply { if (s.autoTickFrom == null) remove(AUTO_TICK_FROM) else putString(AUTO_TICK_FROM, s.autoTickFrom.toString()) }
            .apply()
    }

    private companion object {
        /** Backed up by name in data_extraction_rules.xml. */
        const val FILE = "settings"
        /** Whether launch checks are enabled. */
        const val AUTO_UPDATE = "autoUpdateCheck"
        /** Last successful check, in epoch millis. */
        const val LAST_UPDATE_CHECK = "lastUpdateCheck"
        /** Cached release version. */
        const val UPDATE_VERSION = "updateVersion"
        /** Cached plain-text notes. */
        const val UPDATE_NOTES = "updateNotes"
        /** Cached trusted APK link. */
        const val UPDATE_APK = "updateApk"
        /** Cached trusted release page. */
        const val UPDATE_PAGE = "updatePage"
        const val CLASS_ORDER = "classOrder"
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
        /** The first day lessons tick themselves from; absent until the first-launch question is answered. */
        const val AUTO_TICK_FROM = "autoTickFrom"
    }
}
