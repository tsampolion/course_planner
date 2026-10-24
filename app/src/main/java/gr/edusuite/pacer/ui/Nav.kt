package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.problems
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.SchoolClass
import gr.edusuite.pacer.model.SchoolYear

enum class GroupPage { HOME, PLAN, WORK, STATS }

/** Every destination in the app. Plain Kotlin, so the back stack rules are unit-testable. */
sealed interface Screen {
    /** The open school year's dashboard: the next lesson, a tile for each part of the year, and the year picker. */
    data object Home : Screen
    data object Settings : Screen
    /** Term dates, days off, and renaming or deleting the year. */
    data object YearSettings : Screen
    /** Pacing overview: every group in every class of the year, and the year's problems. */
    data object Overview : Screen
    data object Classes : Screen
    /** The open class: lesson length and pace, books and groups. */
    data object Class : Screen
    data object Tests : Screen
    data object Homework : Screen
    /** Lead book, unit boundaries and optional links for the open class. */
    data object Units : Screen
    /** A group of the open class. */
    data class Group(val id: String, val page: GroupPage = GroupPage.HOME) : Screen
    /** Calendar and book overrides for one group. */
    data class GroupSettings(val id: String) : Screen
}

/** The group retained by a schedule or its settings destination. */
val Screen.groupId: String? get() = when (this) {
    is Screen.Group -> id
    is Screen.GroupSettings -> id
    else -> null
}

/** Shown only while a school year is open. */
val Screen.needsYear: Boolean get() = this != Screen.Home && this != Screen.Settings

/** Shown only while a class is open. */
val Screen.needsClass: Boolean
    get() = this == Screen.Class || this == Screen.Tests || this == Screen.Homework || this == Screen.Units || this is Screen.Group || this is Screen.GroupSettings

/**
 * Screens that change school-year data show Undo in their top bar (design decision 18). Settings, Pacing overview and
 * Home have none; their snackbars still offer Undo for a delete.
 */
val Screen.showsUndo: Boolean
    get() = this == Screen.Class || this == Screen.Classes || this == Screen.YearSettings ||
        this == Screen.Tests || this == Screen.Homework || this == Screen.Units || this is Screen.Group || this is Screen.GroupSettings

/** The two panes of a tablet window: the [list] screen, and the open [detail] group beside it if there is one. */
data class Panes(val list: Screen, val detail: Screen?)

/** A group of the open year, by its class: what the group switcher's Recent list remembers. */
data class GroupRef(val classId: String, val groupId: String)

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

    /** Rendering a tablet home as Plan must not change the logical screen used for toolbar sheets. */
    fun groupPage(screen: Screen.Group, layout: WindowLayout): GroupPage =
        if (layout != WindowLayout.COMPACT && screen.page == GroupPage.HOME) GroupPage.PLAN else screen.page

    /** The stack to start with for a saved [AppData.view]. A group reopens under its class, as if opened from Home's Classes tile. */
    fun fromView(view: String): List<Screen> =
        if (view == AppData.VIEW_SETUP || view.isBlank()) root
        else listOf(Screen.Home, Screen.Classes, Screen.Class, Screen.Group(view))

    /** What to save in [AppData.view] for this stack. */
    fun viewOf(stack: List<Screen>): String =
        stack.asReversed().firstNotNullOfOrNull { it.groupId } ?: AppData.VIEW_SETUP

    /** Opens [screen]. Opening a group from a group swaps it rather than stacking groups. */
    fun push(stack: List<Screen>, screen: Screen): List<Screen> = when {
        screen == Screen.Home -> root
        stack.last() == screen -> stack
        screen is Screen.Group && stack.last().groupId != null && stack.last().groupId != screen.id ->
            stack.filterNot { it.groupId != null } + screen
        else -> stack + screen
    }

    fun pop(stack: List<Screen>): List<Screen> = if (stack.size > 1) stack.dropLast(1) else stack

    /** The menu's destinations, top to bottom. The year's screens show only while a year is open. */
    fun drawer(hasYear: Boolean): List<Screen> =
        listOf(Screen.Home, Screen.Overview, Screen.Classes, Screen.YearSettings, Screen.Settings).filter { hasYear || !it.needsYear }

    /**
     * Opens [screen] from the menu, a search or the group switcher. A screen already on the stack is gone back to
     * rather than stacked again; anything else is pushed, so Back returns to where the teacher was.
     */
    fun goTo(stack: List<Screen>, screen: Screen): List<Screen> {
        val at = stack.indexOf(screen)
        return if (at >= 0) stack.take(at + 1) else push(stack, screen)
    }

    /**
     * What a window shows beside the menu: on a tablet ([layout] medium or wide) a list pane and, where a group is
     * open, that group in a detail pane. Pacing overview or Classes on top show as the list with nothing beside it; a
     * group on top shows Pacing overview beside it, whichever screen it was opened from. Anything else, and every
     * phone, is one pane. Back pops the stack as usual, so it closes the detail pane first.
     */
    fun panes(stack: List<Screen>, layout: WindowLayout): Panes? {
        if (layout == WindowLayout.COMPACT) return null
        return when (val top = stack.last()) {
            is Screen.Group, is Screen.GroupSettings -> Panes(Screen.Overview, top)
            Screen.Overview, Screen.Classes -> Panes(top, null)
            else -> null
        }
    }

    /** How many groups the switcher's Recent list keeps. */
    const val RECENT = 4

    /** [recent] with [group] moved to the front, at most [RECENT] long. Kept in memory for the session only. */
    fun recent(recent: List<GroupRef>, group: GroupRef): List<GroupRef> = (listOf(group) + recent.filter { it != group }).take(RECENT)

    /** The stack after another year opens: everything that showed the old year closes. */
    fun afterYearSwitch(stack: List<Screen>): List<Screen> = prune(stack, hasYear = false, hasClass = false, emptySet())

    /** The stack after another class opens: the old class's screens close, the year's stay. */
    fun afterClassSwitch(stack: List<Screen>): List<Screen> = prune(stack, hasYear = true, hasClass = false, emptySet())

    /** Drops screens for a year, class or group that no longer exists, for example after a delete or an undo. */
    fun prune(stack: List<Screen>, hasYear: Boolean, hasClass: Boolean, groupIds: Set<String>): List<Screen> {
        val kept = stack.filter {
            (hasYear || !it.needsYear) && (hasClass || !it.needsClass) && (it.groupId == null || it.groupId in groupIds)
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
        plan.groups.any { msg.startsWith("${it.name}'s plan") } -> Screen.Group(plan.groups.first { msg.startsWith("${it.name}'s plan") }.id)
        plan.groups.any { msg.startsWith("${it.name} has") } -> Screen.Class
        else -> null
    }
}
