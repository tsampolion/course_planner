package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.markDone
import gr.edusuite.pacer.model.SchoolYear
import java.time.LocalDate

/*
 * What Home shows about the open school year (docs/PACER_DESIGN.md D3, D4: the next-lesson bar, the Classes card and the
 * Pacing overview board), worked out in plain Kotlin from the engine, so it is unit-testable and the screen only lays it
 * out.
 */

/** The next lesson to teach in the year, whichever class and group it belongs to. */
data class NextLesson(
    val classId: String,
    val groupId: String,
    val className: String,
    val groupName: String,
    val date: LocalDate,
    /** A whole-lesson assignment reserves the slot, including a revision with selected pages. */
    val blocked: Boolean,
    /** Pages per book ("Student's Book 12-15 · Workbook 8-9"), plus the work reserving a blocked lesson. */
    val what: String,
    /** The date has passed and the lesson is still not marked done. */
    val overdue: Boolean,
    /** The custom plan's note for the lesson, or blank. */
    val note: String = "",
)

/**
 * The earliest next lesson over every group of every class, as each group's schedule has it. Ties go to the class
 * listed first, then the group listed first. Null when no group has a lesson left.
 */
fun nextLesson(year: SchoolYear, today: LocalDate): NextLesson? {
    var best: NextLesson? = null
    for (c in year.classes) {
        val plan = year.planFor(c)
        for (g in plan.groups) {
            val report = GroupReport(plan, g)
            val row = report.rows.firstOrNull() ?: continue
            val groupPlan = report.plan
            if (best != null && !row.date.isBefore(best.date)) continue
            val what = if (row.blocked) {
                (row.events.map { it.name } + groupPlan.books.indices
                    .filter { i -> row.pages[i].isNotEmpty() }
                    .map { i -> "${groupPlan.books[i].name} ${row.cells[i]}" }).joinToString(" · ")
            } else {
                // A supplementary book shows only where the lesson has its pages.
                groupPlan.books.indices.filter { i -> row.cells[i].isNotEmpty() && (!groupPlan.books[i].supplementary || row.pages[i].isNotEmpty()) }
                    .joinToString(" · ") { i -> "${groupPlan.books[i].name} ${row.cells[i]}" }
            }
            best = NextLesson(
                c.id, g.id, c.name, g.name, row.date, row.blocked, what,
                overdue = row.date.isBefore(today),
                note = row.note,
            )
        }
    }
    return best
}

/**
 * Marks the next lesson of group [groupId] in class [classId] done: the same rule as ticking it on the group screen
 * (`PacerViewModel.markNextLessonDone`), for a class that need not be the open one. Unknown ids change nothing.
 */
fun SchoolYear.markNextLessonDone(classId: String, groupId: String): SchoolYear {
    val c = classes.firstOrNull { it.id == classId } ?: return this
    val plan = planFor(c)
    val g = plan.groups.firstOrNull { it.id == groupId } ?: return this
    val r = GroupReport(plan, g)
    val next = r.rows.firstOrNull()?.let { r.markDone(setOf(it.date)) } ?: return this
    return copy(classes = classes.map { k -> if (k.id != classId) k else k.copy(groups = k.groups.map { if (it.id == groupId) next else it }) })
}

/** Today's lessons over the whole year: how many are still to teach, out of all of them (done ones included). */
data class TodayCount(val left: Int, val total: Int)

/** Every group's lessons dated [today], as each group's schedule has them, plus those already marked done today. */
fun lessonsToday(year: SchoolYear, today: LocalDate): TodayCount {
    var left = 0
    var done = 0
    for (c in year.classes) {
        val plan = year.planFor(c)
        for (g in plan.groups) {
            // A done lesson's date leaves the schedule, so the two never count the same lesson.
            left += GroupReport(plan, g).rows.count { it.date == today }
            done += g.done.count { it.date == today }
        }
    }
    return TodayCount(left, left + done)
}

/** One group on Home's Pacing overview board. [short] is [lessonsShort]: 0 on pace, null with nothing to measure. */
data class BoardCell(val classId: String, val groupId: String, val groupName: String, val short: Int?) {
    val behind: Boolean get() = (short ?: 0) > 0
}

/** One class's line on the board: its groups in the class's order. */
data class BoardRow(val classId: String, val className: String, val cells: List<BoardCell>)

/** Home's Pacing overview board: one row per class that has groups, in the year's order, from the same verdict as every status label. */
fun pacingBoard(year: SchoolYear): List<BoardRow> = year.classes.filter { it.groups.isNotEmpty() }.map { c ->
    val plan = year.planFor(c)
    BoardRow(c.id, c.name, plan.groups.map { BoardCell(c.id, it.id, it.name, GroupReport(plan, it).lessonsShort()) })
}

/** "16 groups · 3 behind" over the board. */
data class BoardSummary(val groups: Int, val behind: Int)

fun List<BoardRow>.summary(): BoardSummary =
    flatMap { it.cells }.let { cells -> BoardSummary(cells.size, cells.count { it.behind }) }

/** Home's board in large-text mode: only the groups behind, grouped by class, and how many groups are on pace. */
data class LargeBoard(val rows: List<BoardRow>, val onPace: Int)

/** Keeps each class's groups that are behind and drops the classes with none; every other group counts as on pace. */
fun List<BoardRow>.behindOnly(): LargeBoard {
    val rows = mapNotNull { r -> r.cells.filter { it.behind }.takeIf { it.isNotEmpty() }?.let { r.copy(cells = it) } }
    return LargeBoard(rows, sumOf { r -> r.cells.count { !it.behind } })
}

/** The year Home opens when none is open: the one that starts last (a year with no start date comes last). Null when there are none. */
fun defaultYearId(years: List<SchoolYear>): String? =
    years.maxWithOrNull(compareBy<SchoolYear, LocalDate?>(nullsFirst()) { it.start })?.id
