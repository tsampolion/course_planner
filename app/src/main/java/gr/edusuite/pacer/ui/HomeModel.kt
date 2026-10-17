package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.markDone
import gr.edusuite.pacer.model.SchoolYear
import java.time.LocalDate

/*
 * What Home shows about the open school year (style guide §6 "Hero tile" and "Tile grid"), worked out in plain Kotlin
 * from the engine, so it is unit-testable and the screen only lays it out.
 */

/** The next lesson to teach in the year, whichever class and group it belongs to. */
data class NextLesson(
    val classId: String,
    val groupId: String,
    val className: String,
    val groupName: String,
    val date: LocalDate,
    /** A test takes the whole lesson, so [what] names the tests rather than pages. */
    val blocked: Boolean,
    /** Pages per book ("Student's Book 12-15 · Workbook 8-9"), or the tests on a blocked lesson. */
    val what: String,
    /** The date has passed and the lesson is still not marked done. */
    val overdue: Boolean,
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
            val row = GroupReport(plan, g).rows.firstOrNull() ?: continue
            if (best != null && !row.date.isBefore(best.date)) continue
            val what = if (row.blocked) {
                row.events.joinToString(" · ") { it.name }
            } else {
                plan.books.zip(row.cells).filter { it.second.isNotEmpty() }.joinToString(" · ") { (b, cell) -> "${b.name} $cell" }
            }
            best = NextLesson(
                c.id, g.id, c.name, g.name, row.date, row.blocked, what,
                overdue = row.date.isBefore(today),
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

/** The live subtitles of Home's tiles. */
data class HomeSummary(
    val classes: Int,
    val groups: Int,
    val problems: Int,
    val tests: Int,
    /** The first test on or after today, by date then name. */
    val nextTest: Pair<LocalDate, String>?,
    val homework: Int,
    val daysOff: Int,
    val datesSet: Boolean,
)

fun homeSummary(year: SchoolYear, today: LocalDate): HomeSummary {
    val events = year.classes.flatMap { it.events }
    return HomeSummary(
        classes = year.classes.size,
        groups = year.classes.sumOf { it.groups.size },
        problems = Nav.problems(year).size,
        tests = events.size,
        nextTest = events.filter { !it.date.isBefore(today) }.minWithOrNull(compareBy({ it.date }, { it.name }))?.let { it.date to it.name },
        homework = year.classes.sumOf { it.homework.size },
        daysOff = year.closures.size,
        datesSet = year.start != null && year.end != null,
    )
}

/** The year Home opens when none is open: the one that starts last (a year with no start date comes last). Null when there are none. */
fun defaultYearId(years: List<SchoolYear>): String? =
    years.maxWithOrNull(compareBy<SchoolYear, LocalDate?>(nullsFirst()) { it.start })?.id
