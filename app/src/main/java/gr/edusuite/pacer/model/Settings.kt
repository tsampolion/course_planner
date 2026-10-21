package gr.edusuite.pacer.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        /** Unknown or missing values fall back to following the system. */
        fun parse(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * App-level preferences. They live outside the school years, so undo never touches them.
 * The new-class defaults match the class the app used to start with (75-minute lessons, 4 pages each).
 */
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Lesson length, in minutes, for new classes. */
    val lessonMins: Int = DEFAULT_MINS,
    /** Sustainable pace, in pages per lesson, for new classes. */
    val pace: Double = DEFAULT_PACE,
    /** Schedules show one month at a time, in tabs, instead of every month in one list. */
    val scheduleByMonth: Boolean = false,
    /** The welcome guide was shown and closed, so a launch with no school years doesn't show it again. */
    val welcomeSeen: Boolean = false,
    /** The teacher's name for Home's greeting. Empty means Home says "Hello" alone. */
    val name: String = "",
    /** The "show once" hints already shown, by their stable [Hints] ids. "Show tips again" empties it. */
    val seenTips: Set<String> = emptySet(),
) {
    /** Keeps stored values usable whatever was saved. */
    fun sanitised(): Settings = copy(
        lessonMins = lessonMins.takeIf { it in 1..MAX_MINS } ?: DEFAULT_MINS,
        pace = pace.takeIf { it.isFinite() && it >= 0 } ?: DEFAULT_PACE,
        name = cleanName(name),
        seenTips = seenTips.filterTo(mutableSetOf()) { it.isNotBlank() },
    )

    companion object {
        const val DEFAULT_MINS = 75
        const val DEFAULT_PACE = 4.0
        const val MAX_MINS = 600
        const val MAX_NAME = 40

        /** A name as typed: one line, no edge spaces, at most [MAX_NAME] characters. */
        fun cleanName(text: String): String =
            text.replace(Regex("\\s+"), " ").trim().take(MAX_NAME).trimEnd()

        /**
         * A pace as typed, such as "4", "3.5" or "3,5". Null for anything else, including
         * "Infinity" and "1e400", which the JSON file can't store.
         */
        fun parsePace(text: String): Double? =
            text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    }
}

/**
 * "Show once" hints, remembered for the whole app in [Settings.seenTips]. Each hint has a stable id that never changes
 * once shipped, since stored ids outlive the code that shows them. Add new ids here.
 */
object Hints {
    /** The group's Plan tab: long-press a lesson for its menu. */
    const val LESSON_MENU = "group.lessonMenu"

    /** The group's Stats tab: what the pace track's tick means, and tapping a book. */
    const val PACE_TRACK = "group.paceTrack"

    /** True once the hint [id] was shown and dismissed, until "Show tips again". */
    fun seen(settings: Settings, id: String): Boolean = id in settings.seenTips

    /** [settings] with [id] remembered as seen. Unchanged if it already was. */
    fun markSeen(settings: Settings, id: String): Settings =
        if (id.isBlank() || seen(settings, id)) settings else settings.copy(seenTips = settings.seenTips + id)

    /** [settings] with every hint showing again. */
    fun reset(settings: Settings): Settings = settings.copy(seenTips = emptySet())
}
