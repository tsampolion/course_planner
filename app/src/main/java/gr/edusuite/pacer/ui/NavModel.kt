package gr.edusuite.pacer.ui

import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.UnitIssue
import gr.edusuite.pacer.engine.unitIssues
import gr.edusuite.pacer.engine.BookLag
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Source
import gr.edusuite.pacer.engine.fitToTerm
import gr.edusuite.pacer.engine.tidy
import gr.edusuite.pacer.engine.Verdict
import gr.edusuite.pacer.engine.activeExtras
import gr.edusuite.pacer.engine.bookLags
import gr.edusuite.pacer.engine.shortfallCoverage
import gr.edusuite.pacer.engine.coverage
import gr.edusuite.pacer.engine.verdict
import gr.edusuite.pacer.model.SchoolYear
import java.time.LocalDate
import java.text.Normalizer
import java.util.Locale
import kotlin.math.ceil

/*
 * What the menu, the group switcher and the Warnings sheet show, in plain Kotlin so it is unit-testable
 * (docs/PACER_DESIGN.md: one verdict and one rounding helper feed every status label).
 */

/** Slack below anything a lesson count shows, so 3.0000000000004 lessons still reads 3. */
private const val WHOLE_TOLERANCE = 1e-9

/** A lesson count as the app shows it: rounded up, and anything above 0 at least 1 (3.33 → 4, 0.3 → 1). 0 stays 0. */
fun wholeLessons(x: Double): Int = if (x <= WHOLE_TOLERANCE) 0 else maxOf(1, ceil(x - WHOLE_TOLERANCE).toInt())

/** Whole lessons short of the term, from [verdict]: 0 on pace, null when there is nothing to measure. */
fun GroupReport.lessonsShort(): Int? = when (val v = verdict()) {
    null -> null
    is Verdict.Fits -> 0
    is Verdict.Short -> wholeLessons(v.by)
}

/** Every group of the year by id, with [lessonsShort] for the plan it follows. */
fun yearStatus(year: SchoolYear, today: LocalDate): Map<String, Int?> = year.classes.flatMap { c ->
    val plan = year.planFor(c, today)
    c.groups.map { it.id to GroupReport(plan, it).lessonsShort() }
}.toMap()

/** How many groups of the year are behind: the count by Pacing overview in the menu. */
fun groupsBehind(year: SchoolYear, today: LocalDate): Int = yearStatus(year, today).values.count { (it ?: 0) > 0 }

/** A group found by [searchGroups], with its class. */
data class GroupHit(val classId: String, val className: String, val groupId: String, val groupName: String)

/** Lower case without accents, so "ά" finds "α" and "Fce" finds "FCE". */
private fun fold(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT)

/**
 * "Find a class or group": every group whose class name and group name together hold each word of [query], in the
 * year's order. A class's name finds all its groups. A blank query finds nothing.
 */
fun searchGroups(year: SchoolYear, query: String): List<GroupHit> {
    val words = fold(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    return year.classes.flatMap { c ->
        c.groups.filter { g -> fold("${c.name} ${g.name}").let { hay -> words.all { it in hay } } }
            .map { GroupHit(c.id, c.name, it.id, it.name) }
    }
}

/** One warning on a group and the fix the Warnings sheet offers for it. */
sealed interface GroupWarning {
    data class Units(val issue: UnitIssue) : GroupWarning
    data class ExtraUnavailable(val date: java.time.LocalDate) : GroupWarning
    /** Short of the term: Catch up. [pages] is how many pages more than the pace fits. */
    data class Behind(val lessons: Int, val pages: Int) : GroupWarning
    /** A book left behind the others: Bring in step. */
    data class BookBehind(val lag: BookLag, val lessons: Int) : GroupWarning
    /** The custom plan leaves pages out: Re-pace. */
    data class Missing(val pages: Int) : GroupWarning
    /**
     * The custom plan runs past the term: Fit to term. [edited] of the lessons are the teacher's and stay as they are;
     * with [canFit] false there is nothing Fit to term could move.
     */
    data class Overflow(val lessons: Int, val edited: Int = 0, val canFit: Boolean = true) : GroupWarning
    /** The custom plan teaches pages twice: Tidy, or, when Tidy can't fix them, [openLesson] (a lesson id) to edit one. */
    data class Doubled(val pages: Int, val openLesson: String? = null) : GroupWarning
    /** The custom plan holds pages already taught: Tidy, or [openLesson] (a lesson id) when Tidy can't remove them. */
    data class Stale(val pages: Int, val openLesson: String? = null) : GroupWarning
}

/**
 * Every warning on [report]'s group, as the group screen showed them before they moved into the sheet: an active
 * custom plan that fits every page into the term already answers being behind and books left behind, and a custom
 * plan's coverage problems come last.
 */
fun groupWarnings(report: GroupReport): List<GroupWarning> {
    val plan = report.plan
    val handled = report.custom != null && report.coverage()?.ok == true &&
        (report.shortfallCoverage()?.complete != false)
    val out = ArrayList<GroupWarning>()
    out += report.unitIssues().map { GroupWarning.Units(it) }
    val active = plan.activeExtras(report.group).mapTo(HashSet()) { it.id }
    report.group.extras.filter { e -> e.id !in active && report.group.done.none { it.date == e.date } }
        .forEach { out += GroupWarning.ExtraUnavailable(it.date) }
    if (!handled) {
        (report.verdict() as? Verdict.Short)?.let { out += GroupWarning.Behind(wholeLessons(it.by), it.cutPages) }
        report.bookLags(plan.lead, plan.lagLessons.toDouble()).forEach { out += GroupWarning.BookBehind(it, wholeLessons(it.lessonsBehind)) }
    }
    val cov = if (report.group.custom == null) null else GroupReport(plan, report.group, Source.CUSTOM).coverage()
    if (cov != null) {
        if (cov.missingCount > 0) out += GroupWarning.Missing(cov.missingCount)
        // Tidy and Fit to term leave the teacher's lessons alone, so each offers its fix only while it can change something.
        val custom = GroupReport(plan, report.group, Source.CUSTOM)
        val tidied = custom.tidy()?.let { GroupReport(plan, report.group.copy(custom = it), Source.CUSTOM).coverage() }
        if (cov.overflow > 0) out += GroupWarning.Overflow(cov.overflow, cov.overflowEdited, custom.fitToTerm() != report.group.custom)
        if (cov.doubledCount > 0) out += GroupWarning.Doubled(cov.doubledCount, if (tidied != null && tidied.doubledCount < cov.doubledCount) null else custom.lessonHolding(cov.doubled))
        if (cov.staleCount > 0) out += GroupWarning.Stale(cov.staleCount, if (tidied != null && tidied.staleCount < cov.staleCount) null else custom.lessonHolding(cov.stale))
    }
    return out
}

/** The id of the lesson to open for [pages] (book id to pages): the first the teacher touched that holds one, else the first that does. */
private fun GroupReport.lessonHolding(pages: Map<String, List<Int>>): String? {
    val holds = { l: gr.edusuite.pacer.model.PlannedLesson -> l.pages.any { (book, ps) -> pages[book]?.any { it in ps } == true } }
    val lessons = group.custom?.lessons.orEmpty()
    return (lessons.firstOrNull { it.touched && holds(it) } ?: lessons.firstOrNull(holds))?.id
}

/** The places a group has, in the order the title switcher and the ☰ menu list them. */
enum class GroupPlace {
    HOME, PLAN, WORK, STATS, SETTINGS;

    fun titleRes(): Int = when (this) {
        HOME -> R.string.group_place_home
        PLAN -> R.string.group_tab_plan
        WORK -> R.string.work_title
        STATS -> R.string.group_tab_stats
        SETTINGS -> R.string.group_settings_title
    }

    fun screen(groupId: String): Screen = when (this) {
        HOME -> Screen.Group(groupId)
        PLAN -> Screen.Group(groupId, GroupPage.PLAN)
        WORK -> Screen.Group(groupId, GroupPage.WORK)
        STATS -> Screen.Group(groupId, GroupPage.STATS)
        SETTINGS -> Screen.GroupSettings(groupId)
    }
}

/** The place this screen is, or null off a group. With [plansStandIn] (a tablet) group home is shown as, and counts as, Plan. */
fun Screen.place(plansStandIn: Boolean = false): GroupPlace? = when (this) {
    is Screen.Group -> when (page) {
        GroupPage.HOME -> if (plansStandIn) GroupPlace.PLAN else GroupPlace.HOME
        GroupPage.PLAN -> GroupPlace.PLAN
        GroupPage.WORK -> GroupPlace.WORK
        GroupPage.STATS -> GroupPlace.STATS
    }
    is Screen.GroupSettings -> GroupPlace.SETTINGS
    else -> null
}

/**
 * The places the menu and the switcher list. On a tablet group home is shown as Plan, so listing both would offer two
 * rows for one view: [plansStandIn] leaves Group home out.
 */
fun groupPlaces(plansStandIn: Boolean = false): List<GroupPlace> = GroupPlace.entries.filter { !plansStandIn || it != GroupPlace.HOME }

/** One row of the title switcher: a place of the open group, with a mark on the one the teacher is on. */
data class SwitcherPage(val place: GroupPlace, val current: Boolean)

/** The switcher's first part: every place of the open group in fixed order, the current one marked. */
fun switcherPages(current: Screen, plansStandIn: Boolean = false): List<SwitcherPage> =
    groupPlaces(plansStandIn).map { SwitcherPage(it, it == current.place(plansStandIn)) }

/** The switcher's second part: the open class's other groups. */
fun switcherGroups(groups: List<gr.edusuite.pacer.model.Group>, openGroupId: String?): List<gr.edusuite.pacer.model.Group> =
    groups.filter { it.id != openGroupId }

/**
 * Where the menu's Classes & Groups list is open: the classes, one class's groups, or one group's places. Each level
 * hides the ones above it, so the list shows only the way down and one row back up.
 */
sealed interface MenuLevel {
    data object Classes : MenuLevel
    data class Class(val classId: String) : MenuLevel
    data class Group(val classId: String, val groupId: String) : MenuLevel
}

/** The level that holds [screen] ("You are here"), or null on a screen outside Classes & Groups. */
fun menuLevelFor(screen: Screen, activeClass: String): MenuLevel? = when {
    screen is Screen.Group || screen is Screen.GroupSettings -> MenuLevel.Group(activeClass, screen.groupId.orEmpty())
    screen == Screen.Class || screen == Screen.Units -> MenuLevel.Class(activeClass)
    screen == Screen.Classes -> MenuLevel.Classes
    else -> null
}

/** Where Classes & Groups is open: the [level] it shows and whether the list is [expanded]. */
data class MenuState(val level: MenuLevel, val expanded: Boolean)

/** The state each time the menu opens: at the teacher's own place, or folded away on a screen outside Classes & Groups. */
fun menuStateOnOpen(here: MenuLevel?): MenuState = MenuState(here ?: MenuLevel.Classes, here != null)

/**
 * The state after the screen changes. Inside Classes & Groups the list follows the teacher; elsewhere (Home, Pacing
 * overview, Settings) it stays as it is, so a pinned menu doesn't fold up each time another item is tapped.
 */
fun MenuState.followScreen(here: MenuLevel?): MenuState = if (here != null) MenuState(here, true) else this

/** What the open level of Classes & Groups lists. Names are as stored, so the screen supplies stand-ins for blank ones. */
sealed interface MenuRow {
    /** One step back up the list; [name] is what it goes back to, null for the list of classes. Doesn't leave the screen. */
    data class Up(val to: MenuLevel, val name: String?) : MenuRow
    /** The open class or group, over its rows. */
    data class Heading(val name: String, val isClass: Boolean) : MenuRow
    /** Opens that class's groups in the list. */
    data class ClassItem(val classId: String, val name: String, val groups: Int) : MenuRow
    /** Opens the Classes screen. */
    data class AllClasses(val count: Int, val current: Boolean) : MenuRow
    /** Opens the Class screen. */
    data class ClassSettings(val classId: String, val current: Boolean) : MenuRow
    data class GroupItem(val classId: String, val groupId: String, val name: String, val current: Boolean) : MenuRow
    data class PlaceItem(val groupId: String, val place: GroupPlace, val current: Boolean) : MenuRow
}

/**
 * The rows under Classes & Groups at [level]. A class or group that no longer exists falls back to the classes, so a
 * delete never leaves the menu pointing at nothing.
 */
fun menuRows(year: SchoolYear, level: MenuLevel, screen: Screen, plansStandIn: Boolean = false): List<MenuRow> {
    val cl = when (level) {
        MenuLevel.Classes -> null
        is MenuLevel.Class -> year.classes.firstOrNull { it.id == level.classId }
        is MenuLevel.Group -> year.classes.firstOrNull { it.id == level.classId }
    }
    if (level is MenuLevel.Group && cl != null) {
        val g = cl.groups.firstOrNull { it.id == level.groupId }
        if (g != null) {
            val here = screen.place(plansStandIn).takeIf { screen.groupId == g.id }
            return listOf(MenuRow.Up(MenuLevel.Class(cl.id), cl.name), MenuRow.Heading(g.name, isClass = false)) +
                groupPlaces(plansStandIn).map { MenuRow.PlaceItem(g.id, it, it == here) }
        }
    }
    if (cl != null && level !is MenuLevel.Classes) {
        return listOf(MenuRow.Up(MenuLevel.Classes, null), MenuRow.Heading(cl.name, isClass = true), MenuRow.ClassSettings(cl.id, screen == Screen.Class)) +
            cl.groups.map { MenuRow.GroupItem(cl.id, it.id, it.name, screen.groupId == it.id) }
    }
    return year.classes.map { MenuRow.ClassItem(it.id, it.name, it.groups.size) } +
        (if (year.classes.isEmpty()) emptyList() else listOf(MenuRow.AllClasses(year.classes.size, screen == Screen.Classes)))
}

/** A long year name for the menu: "2026–2027" → "2026–27", anything still over 12 characters cut with an ellipsis. */
fun yearShort(name: String): String {
    val n = name.trim().replace(Regex("^(\\d{4})\\s*[-–—/]\\s*\\d{2}(\\d{2})\\b"), "$1–$2")
    return if (n.length > 12) n.take(11).trimEnd() + "…" else n
}

/** Width of an average character of the bar's title, in dp at the default text size. */
private const val TITLE_CHAR_DP = 12f

/**
 * Whether the top bar should fold Undo and the warning button into ⋮ so the title keeps its room. [buttons] is how many
 * buttons follow the title now; [hasMenu] is false where ☰ isn't there (a tablet beside the permanent drawer). An estimate
 * from the title's length, the bar's width and the text size: close enough to decide, and never cuts a title that fits.
 */
fun foldBarActions(barDp: Float, fontScale: Float, titleChars: Int, buttons: Int, hasMenu: Boolean, hasSwitcher: Boolean): Boolean {
    val needs = titleChars * TITLE_CHAR_DP * fontScale + (if (hasSwitcher) 24f else 0f)
    val room = barDp - (if (hasMenu) 56f else 16f) - 48f * buttons - 16f
    return needs > room
}
