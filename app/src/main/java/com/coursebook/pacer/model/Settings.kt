package com.coursebook.pacer.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        /** Unknown or missing values fall back to following the system. */
        fun parse(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * App-level preferences. They live outside the school years, so undo never touches them.
 * The new-class defaults match the sample class, so a fresh install behaves as before.
 */
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Lesson length, in minutes, for new classes. */
    val lessonMins: Int = DEFAULT_MINS,
    /** Sustainable pace, in pages per lesson, for new classes. */
    val pace: Double = DEFAULT_PACE,
    /** Schedules show one month at a time, in tabs, instead of every month in one list. */
    val scheduleByMonth: Boolean = false,
) {
    /** Keeps stored values usable whatever was saved. */
    fun sanitised(): Settings = copy(
        lessonMins = lessonMins.takeIf { it in 1..MAX_MINS } ?: DEFAULT_MINS,
        pace = pace.takeIf { it.isFinite() && it >= 0 } ?: DEFAULT_PACE,
    )

    companion object {
        const val DEFAULT_MINS = 75
        const val DEFAULT_PACE = 4.0
        const val MAX_MINS = 600
    }
}
