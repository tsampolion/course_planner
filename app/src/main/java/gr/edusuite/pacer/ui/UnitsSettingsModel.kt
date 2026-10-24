package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.validUnitStarts

/** Draft rows must cover the book, in order, without an empty unit. */
internal fun validateUnitStarts(first: Int, last: Int, rows: List<String>): List<Int>? {
    val starts = rows.map { it.trim().toIntOrNull() ?: return null }
    return starts.takeIf { it.isNotEmpty() && it.first() == first && it.all { p -> p in first..last } && it.zipWithNext().all { (a, b) -> a < b } }
}

/** Starting points, with the remainder kept in the final unit. */
internal fun evenlySpacedUnits(first: Int, last: Int, count: Int): List<Int>? {
    val pages = last - first + 1
    if (count !in 1..pages) return null
    return List(count) { first + it * pages / count }
}

/** Use the effective book pools, so group skips and optional membership agree with the engine. */
internal fun unitCompletion(plan: gr.edusuite.pacer.model.Plan, progress: gr.edusuite.pacer.engine.UnitProgress): Float {
    val total = plan.books.filter { it.id in progress.remaining }.sumOf { book ->
        val starts = book.validUnitStarts() ?: return@sumOf 0
        val first = starts.getOrNull(progress.index) ?: return@sumOf 0
        val end = starts.getOrNull(progress.index + 1) ?: (book.last + 1)
        book.allPages().count { it in first until end }
    }
    return if (total == 0) 1f else (1f - progress.remaining.values.sum().toFloat() / total).coerceIn(0f, 1f)
}

/** Imported link sets can overlap; the settings checklist mirrors the engine's connected chain. */
internal fun linkedUnitBooks(plan: gr.edusuite.pacer.model.Plan): Set<String> {
    val lead = plan.lead ?: return emptySet()
    val linked = linkedSetOf(lead)
    do {
        val before = linked.size
        plan.bookLinks.filter { link -> link.any { it in linked } }.forEach { linked.addAll(it) }
    } while (linked.size != before)
    return linked
}
