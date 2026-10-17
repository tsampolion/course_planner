package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.problems
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.SchoolClass
import gr.edusuite.pacer.model.SchoolYear

/** Every destination in the app. Plain Kotlin, so the back stack rules are unit-testable. */
sealed interface Screen {
    /** The logo, the school-year picker and Settings. */
    data object Home : Screen
    data object Settings : Screen
    /** The open year: Classes, School year settings, Overview and Print. */
    data object Year : Screen
    /** Term dates, days off, and renaming or deleting the year. */
    data object YearSettings : Screen
    /** Every group in every class of the year, and the year's problems. */
    data object Overview : Screen
    data object Classes : Screen
    /** The open class: lesson length and pace, books and groups. */
    data object Class : Screen
    data object Tests : Screen
    data object Homework : Screen
    /** A group of the open class. */
    data class Group(val id: String) : Screen
}

/** Shown only while a school year is open. */
val Screen.needsYear: Boolean get() = this != Screen.Home && this != Screen.Settings

/** Shown only while a class is open. */
val Screen.needsClass: Boolean
    get() = this == Screen.Class || this == Screen.Tests || this == Screen.Homework || this is Screen.Group

/** A problem and the screen that fixes it, if any. [classId] is the class to open first. */
data class Problem(val text: String, val fix: Screen?, val classId: String? = null)

/**
 * Back stack rules. The stack always starts at [Screen.Home].
 *
 * Only the open group is persisted, through [AppData.view]: a group id, or [AppData.VIEW_SETUP]
 * for "no group open", which lands on Home.
 */
object Nav {
    val root: List<Screen> = listOf(Screen.Home)

    /** The stack to start with for a saved [AppData.view]. A group reopens under its class, as if opened from Classes. */
    fun fromView(view: String): List<Screen> =
        if (view == AppData.VIEW_SETUP || view.isBlank()) root
        else listOf(Screen.Home, Screen.Year, Screen.Classes, Screen.Class, Screen.Group(view))

    /** What to save in [AppData.view] for this stack. */
    fun viewOf(stack: List<Screen>): String =
        (stack.lastOrNull { it is Screen.Group } as Screen.Group?)?.id ?: AppData.VIEW_SETUP

    /** Opens [screen]. Opening a group from a group swaps it rather than stacking groups. */
    fun push(stack: List<Screen>, screen: Screen): List<Screen> = when {
        screen == Screen.Home -> root
        stack.last() == screen -> stack
        screen is Screen.Group && stack.last() is Screen.Group -> stack.dropLast(1) + screen
        else -> stack + screen
    }

    fun pop(stack: List<Screen>): List<Screen> = if (stack.size > 1) stack.dropLast(1) else stack

    /** The stack after another year opens: everything that showed the old year closes. */
    fun afterYearSwitch(stack: List<Screen>): List<Screen> = prune(stack, hasYear = false, hasClass = false, emptySet())

    /** The stack after another class opens: the old class's screens close, the year's stay. */
    fun afterClassSwitch(stack: List<Screen>): List<Screen> = prune(stack, hasYear = true, hasClass = false, emptySet())

    /** Drops screens for a year, class or group that no longer exists, for example after a delete or an undo. */
    fun prune(stack: List<Screen>, hasYear: Boolean, hasClass: Boolean, groupIds: Set<String>): List<Screen> {
        val kept = stack.filter {
            (hasYear || !it.needsYear) && (hasClass || !it.needsClass) && (it !is Screen.Group || it.id in groupIds)
        }
        return if (kept.firstOrNull() == Screen.Home) kept else root + kept.filter { it != Screen.Home }
    }

    /** [Plan.problems], each paired with the screen where it gets fixed. */
    fun problems(plan: Plan): List<Problem> = plan.problems().map { Problem(it, fixFor(plan, it)) }

    /**
     * Every problem in the year. Term problems are listed once; the rest name their class
     * and open it when fixed.
     */
    fun problems(year: SchoolYear): List<Problem> {
        val none = SchoolClass("", "", 0, 0.0, emptyList(), emptyList(), emptyList(), emptyList())
        val term = problems(year.planFor(none)).filter { it.fix == Screen.YearSettings }
        if (year.classes.isEmpty()) return term + Problem("Add at least one class.", Screen.Classes)
        return term + year.classes.flatMap { c ->
            problems(year.planFor(c)).filter { it.fix != Screen.YearSettings }
                .map { Problem("${c.name.ifBlank { "Class" }}: ${it.text}", it.fix, c.id) }
        }
    }

    private fun fixFor(plan: Plan, msg: String): Screen? = when {
        msg.startsWith("Set both a term") || msg.startsWith("The term ends") -> Screen.YearSettings
        msg.startsWith("Add at least one group") || msg.startsWith("Add at least one book") -> Screen.Class
        plan.books.any { msg.startsWith("${it.name}:") } -> Screen.Class
        plan.groups.any { msg.startsWith("${it.name} has") } -> Screen.Class
        else -> null
    }
}
