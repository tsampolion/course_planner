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
    val activeBooks = books.filter { !it.groupOnly || it.id in group.extraBooks }.map { b ->
        val extraSkip = group.skip[b.id].orEmpty()
        if (extraSkip.isBlank() || b.supplementary) b
        else b.copy(skip = collapse((parseRanges(b.skip) + parseRanges(extraSkip)).sorted()))
    }
    val finish = endFor(group)
    val activeLead = lead?.takeIf { id -> activeBooks.any { it.id == id && !it.supplementary } }
    return if (activeBooks == books && finish == end && activeLead == lead) this
    else copy(books = activeBooks, end = finish, lead = activeLead)
}
