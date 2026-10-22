package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.ExtraLesson
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.newId
import java.time.LocalDate

/** The proposed share on this date if it were an ordinary, unpinned teaching slot. */
fun GroupReport.proposedExtraPages(date: LocalDate): Map<String, List<Int>> {
    val first = group.from ?: plan.start ?: return emptyMap()
    val last = plan.end ?: return emptyMap()
    if (date !in first..last || group.done.any { it.date == date } || plan.eventsOn(group, date).any { it.consumes }) return emptyMap()
    val floating = GroupReport(plan, group.copy(extras = group.extras.filter { it.date != date }), Source.PROPOSED)
    val dates = (floating.teachingDates + date).distinct().sorted()
    val i = dates.indexOf(date)
    val allocation = floating.proposedAllocation(dates)
    return plan.books.indices.filter { !plan.books[it].supplementary }
        .associate { plan.books[it].id to allocation[it].getOrElse(i) { emptyList() } }.filterValues { it.isNotEmpty() }
}

/** One immutable mutation for the ViewModel's single undo/save step. Extras are always core-only. */
fun Plan.withExtra(g: Group, extra: ExtraLesson): Group {
    val core = coreBooks.associateBy { it.id }
    val pages = extra.pages.filterKeys { it in core }.mapValues { (id, ps) ->
        ps.filter { it in core.getValue(id).first..core.getValue(id).last }.distinct().sorted()
    }.filterValues { it.isNotEmpty() }
    val next = g.copy(extras = (g.extras.filter { it.id != extra.id && it.date != extra.date } + extra.copy(pages = pages)).sortedBy { it.date })
    return repaceExtras(g, next)
}

/** Deleting a pin leaves taught history alone, and returns untaught pages to the remaining pool. */
fun Plan.withoutExtra(g: Group, id: String): Group = repaceExtras(g, g.copy(extras = g.extras.filter { it.id != id }))

private fun Plan.repaceExtras(before: Group, after: Group): Group {
    if (before == after || after.custom?.active != true) return after
    val oldSlots = GroupReport(this, before).teaching
    val newSlots = GroupReport(this, after).teaching
    val custom = after.custom
    // Re-pace may drop a now-unavailable untouched slot. Removing the extra restores that slot.
    val added = (newSlots - oldSlots).coerceAtLeast(0)
    val c = custom.copy(lessons = custom.lessons + List(minOf(added, (newSlots - custom.lessons.size).coerceAtLeast(0))) { PlannedLesson(newId()) })
    val ready = after.copy(custom = c)
    return ready.copy(custom = GroupReport(this, ready).repaceFrom(0))
}
