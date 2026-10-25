package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Plan
import java.time.LocalDate

/** An override can finish earlier, never extend a term with an end date. */
fun Plan.endFor(group: Group): LocalDate? = when {
    end == null -> group.end
    group.end == null -> end
    else -> minOf(end, group.end)
}

/** One consistent book list and calendar for all row/stat/output indices in this group's report. */
fun Plan.forGroup(group: Group): Plan {
    val finish = endFor(group)
    val activeBooks = books.filter(group::usesBook).map { b ->
        if (b.supplementary) return@map b
        val extraSkip = group.skip[b.id].orEmpty()
        val skipped = if (extraSkip.isBlank()) b else b.copy(skip = collapse((parseRanges(b.skip) + parseRanges(extraSkip)).sorted()))
        // The group's own date replaces the class's, but never runs past the group's end.
        val due = group.bookDue[b.id]?.let { d -> if (finish != null && d.isAfter(finish)) finish else d }
        if (due == null) skipped else skipped.copy(due = due)
    }
    val activeLead = lead?.takeIf { id -> activeBooks.any { it.id == id && !it.supplementary } }
    return if (activeBooks == books && finish == end && activeLead == lead) this
    else copy(books = activeBooks, end = finish, lead = activeLead)
}
