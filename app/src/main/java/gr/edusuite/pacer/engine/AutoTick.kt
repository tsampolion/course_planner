package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.DoneLesson
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.SchoolYear
import java.time.LocalDate

/**
 * Lessons tick themselves when their day has passed (`docs/AUTO_TICK_BRIEF.md`). [g] with every lesson before [today]
 * marked done as planned, one at a time like [markDoneUpTo], so it equals the teacher ticking each lesson in order.
 *
 * Nothing changes while the group's first lesson still to teach is before [from]: those are lessons the teacher chose to
 * leave unticked, and ticking later ones around them would break the order. A group with no lesson before [today] is returned as it is.
 */
fun GroupReport.settlePast(today: LocalDate, from: LocalDate): Group {
    val first = rows.firstOrNull()?.date ?: return group
    if (!first.isBefore(today) || first.isBefore(from)) return group
    return markDoneUpTo(today.minusDays(1))
}

/** [year] with [settlePast] applied to every group of every class. The same instance when nothing needed ticking. */
fun SchoolYear.settlePast(today: LocalDate, from: LocalDate): SchoolYear {
    var changed = false
    val settled = classes.map { c ->
        val plan = planFor(c, today)
        val groups = c.groups.map { g ->
            val next = GroupReport(plan, g).settlePast(today, from)
            if (next == g) g else next.also { changed = true }
        }
        if (groups == c.groups) c else c.copy(groups = groups)
    }
    return if (changed) copy(classes = settled) else this
}

/** A group's lessons before today that no one ticked, for the first-launch question. */
data class UntickedGroup(val className: String, val groupName: String, val count: Int)

/** The lessons before [today] that nobody ticked, per group of the year. */
fun SchoolYear.unticked(today: LocalDate): List<UntickedGroup> = classes.flatMap { c ->
    val plan = planFor(c, today)
    c.groups.mapNotNull { g ->
        val past = GroupReport(plan, g).rows.count { it.date.isBefore(today) }
        if (past > 0) UntickedGroup(c.name, g.name, past) else null
    }
}

/** True for a lesson that was recorded with no pages taught, as "this lesson didn't happen". */
val DoneLesson.notTaught: Boolean get() = edited && pages.isEmpty()

/**
 * The group with the lesson on [date] recorded as not taught: it is done, no pages count as taught, and the date is not
 * a day off. What the lesson would have taught goes back to the pool and spreads over the lessons left.
 * Null when [date] is not one of the group's lessons.
 */
fun GroupReport.recordNotTaught(date: LocalDate): Group? = recordLesson(date, emptyMap())
