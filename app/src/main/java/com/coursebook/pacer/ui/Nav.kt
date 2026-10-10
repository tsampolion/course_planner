package com.coursebook.pacer.ui

import com.coursebook.pacer.engine.problems
import com.coursebook.pacer.model.AppData
import com.coursebook.pacer.model.Plan

/** Every destination in the app. Plain Kotlin, so the back stack rules are unit-testable. */
sealed interface Screen {
    data object Home : Screen
    data class Group(val id: String) : Screen
    data object Term : Screen
    data object Groups : Screen
    data object DaysOff : Screen
    data object Tests : Screen
    data object Books : Screen
    data object Homework : Screen
    data object Settings : Screen
}

/** A plan problem and the screen that fixes it. */
data class Problem(val text: String, val fix: Screen)

/**
 * Back stack rules. The stack always starts at [Screen.Home].
 *
 * Only the open group is persisted, through [AppData.view], whose stored meaning is unchanged:
 * a group id, or [AppData.VIEW_SETUP] for "no group open" (which now lands on Home).
 */
object Nav {
    val root: List<Screen> = listOf(Screen.Home)

    /** The stack to start with for a saved [AppData.view]. */
    fun fromView(view: String): List<Screen> =
        if (view == AppData.VIEW_SETUP || view.isBlank()) root else listOf(Screen.Home, Screen.Group(view))

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

    /** Drops screens for groups that no longer exist, for example after a delete or an undo. */
    fun prune(stack: List<Screen>, groupIds: Set<String>): List<Screen> {
        val kept = stack.filter { it !is Screen.Group || it.id in groupIds }
        return if (kept.firstOrNull() == Screen.Home) kept else root + kept.filter { it != Screen.Home }
    }

    /** [Plan.problems], each paired with the screen where it gets fixed. */
    fun problems(plan: Plan): List<Problem> = plan.problems().map { Problem(it, fixFor(plan, it)) }

    private fun fixFor(plan: Plan, msg: String): Screen = when {
        msg.startsWith("Set both a term") || msg.startsWith("The term ends") -> Screen.Term
        msg.startsWith("Add at least one group") -> Screen.Groups
        msg.startsWith("Add at least one book") -> Screen.Books
        plan.books.any { msg.startsWith("${it.name}:") } -> Screen.Books
        plan.groups.any { msg.startsWith("${it.name} has") } -> Screen.Groups
        else -> Screen.Home
    }
}
