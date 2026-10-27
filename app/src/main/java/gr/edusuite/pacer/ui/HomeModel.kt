package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.model.SchoolYear
import java.time.LocalDate

/*
 * What Home shows about the open school year (docs/PACER_DESIGN.md D3, D4: the Pacing overview board and the card of
 * upcoming tests and worksheets), worked out in plain Kotlin from the engine, so it is unit-testable and the screen only
 * lays it out.
 */

/** One group on Home's Pacing overview board. [short] is [lessonsShort]: 0 on pace, null with nothing to measure. */
data class BoardCell(val classId: String, val groupId: String, val groupName: String, val short: Int?, val today: Boolean = false) {
    val behind: Boolean get() = (short ?: 0) > 0
}

/** One class's line on the board: its groups, most behind first like the Pacing overview. */
data class BoardRow(val classId: String, val className: String, val cells: List<BoardCell>)

/**
 * Home's Pacing overview board: one row per class that has groups, in the year's order, from the same verdict as every
 * status label. Inside a class the groups run in the Pacing overview's default order, most behind first, groups with
 * nothing to measure last, ties in the class's own order. [BoardCell.today] marks a group with a lesson today, done or not.
 */
fun pacingBoard(year: SchoolYear, today: LocalDate): List<BoardRow> = year.classes.filter { it.groups.isNotEmpty() }.map { c ->
    val plan = year.planFor(c, today)
    val cells = plan.groups.map { g ->
        val report = GroupReport(plan, g)
        val teaches = g.done.any { it.date == today } || report.rows.any { it.date == today }
        BoardCell(c.id, g.id, g.name, report.lessonsShort(), teaches)
    }
    BoardRow(c.id, c.name, cells.sortedByDescending { it.short ?: -1 })
}

/** "16 groups · 3 behind" over the board. */
data class BoardSummary(val groups: Int, val behind: Int)

fun List<BoardRow>.summary(): BoardSummary =
    flatMap { it.cells }.let { cells -> BoardSummary(cells.size, cells.count { it.behind }) }

/**
 * Home's board in large-text mode: only the groups behind, grouped by class, then how many groups are on pace (measured
 * and not short) and how many have nothing to measure yet, which are never called on pace.
 */
data class LargeBoard(val rows: List<BoardRow>, val onPace: Int, val setUp: Int)

/** Keeps each class's groups that are behind and drops the classes with none. */
fun List<BoardRow>.behindOnly(): LargeBoard {
    val rows = mapNotNull { r -> r.cells.filter { it.behind }.takeIf { it.isNotEmpty() }?.let { r.copy(cells = it) } }
    val rest = flatMap { it.cells }.filter { !it.behind }
    return LargeBoard(rows, rest.count { it.short == 0 }, rest.count { it.short == null })
}

enum class UpcomingKind { TEST, WORKSHEET }

/** A test or worksheet still to come, for Home's card. [groupName] is null when it is for every group of the class. */
data class UpcomingItem(
    val classId: String,
    val className: String,
    val title: String,
    val date: LocalDate,
    val kind: UpcomingKind,
    val groupName: String?,
)

/**
 * The next [limit] tests and worksheets from [today] on, over every class of the year: the class's tests and the
 * groups' tests and worksheets. Earliest first; ties keep the class's order.
 */
fun upcoming(year: SchoolYear, today: LocalDate, limit: Int = 4): List<UpcomingItem> = year.classes.flatMap { c ->
    val names = c.groups.associate { it.id to it.name }
    val tests = c.events.filter { !it.date.isBefore(today) }.map { e ->
        UpcomingItem(c.id, c.name, e.name, e.date, UpcomingKind.TEST, e.who.takeIf { it != ALL_GROUPS }?.let { names[it] })
    }
    val work = c.groups.flatMap { g ->
        g.assignments.filter { it.type != AssignmentType.HOMEWORK && it.due?.isBefore(today) == false }.map { a ->
            UpcomingItem(c.id, c.name, a.title, a.due!!, if (a.type == AssignmentType.WORKSHEET) UpcomingKind.WORKSHEET else UpcomingKind.TEST, g.name)
        }
    }
    tests + work
}.sortedBy { it.date }.take(limit)

/** The year Home opens when none is open: the one that starts last (a year with no start date comes last). Null when there are none. */
fun defaultYearId(years: List<SchoolYear>): String? =
    years.maxWithOrNull(compareBy<SchoolYear, LocalDate?>(nullsFirst()) { it.start })?.id
