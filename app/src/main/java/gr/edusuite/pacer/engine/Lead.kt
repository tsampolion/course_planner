package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Plan

/** The plan's lead book, or null when it names none of its books. */
fun Plan.leadBook(): Book? = lead?.let { id -> books.firstOrNull { it.id == id && !it.supplementary } }

/** A book's unit start pages, in order. Empty when it has none. */
fun Book.unitStarts(): List<Int> = parseRanges(units).sorted()

/**
 * [alloc] (pages per lesson for each book, in the plan's book order) with every book that has no
 * finish-by date re-paced to follow the lead book. The lead book and books with a finish-by date keep
 * their pages. With no lead book, or a lead with no pages left, [alloc] comes back unchanged.
 *
 * A follower's lesson i ends at round(c_i × n_B / n_L), halves up, where c_i is the lead's cumulative
 * pages after lesson i. When both books list the same number of units, the lead's place inside its unit
 * maps to the same fraction of the follower's unit instead. [pool] is each book's pages to place: its remaining
 * pages for the proposed plan, or what re-pacing a custom plan has left to spread.
 */
internal fun GroupReport.followLead(
    alloc: List<MutableList<List<Int>>>,
    pool: (Book) -> List<Int> = { plan.remainingPages(group, it) },
): List<MutableList<List<Int>>> {
    // A supplementary book never leads; none follows either, as it has no pages to pace.
    val li = plan.books.indexOfFirst { it.id == plan.lead && !it.supplementary }
    if (li < 0) return alloc
    val lead = plan.books[li]
    val leadAlloc = alloc[li]
    val nL = leadAlloc.sumOf { it.size }
    if (nL == 0) return followUnits(alloc, pool)
    val cum = leadAlloc.runningFold(0) { acc, l -> acc + l.size }.drop(1)
    val leadUnits = lead.unitStarts()
    return alloc.mapIndexed { bi, a ->
        val b = plan.books[bi]
        if (bi == li || b.due != null) return@mapIndexed a
        val rem = pool(b)
        if (rem.isEmpty()) return@mapIndexed a
        val bUnits = b.unitStarts()
        val ends = if (leadUnits.isNotEmpty() && leadUnits.size == bUnits.size) {
            unitEnds(lead, leadUnits, leadAlloc, cum, nL, b, bUnits, rem)
        } else {
            val nB = rem.size.toLong()
            cum.map { c -> ((2L * c * nB + nL) / (2L * nL)).toInt() }
        }
        var prev = 0
        ends.mapTo(ArrayList()) { e -> val end = maxOf(prev, e); rem.subList(prev, end).also { prev = end } }
    }.let { followUnits(it, pool) }
}

/** Where a follower's lessons end when its units are linked to the lead's: see [followLead]. */
private fun GroupReport.unitEnds(
    lead: Book, leadUnits: List<Int>, leadAlloc: List<List<Int>>, cum: List<Int>, nL: Int,
    b: Book, bUnits: List<Int>, rem: List<Int>,
): List<Int> {
    val lastWithPages = cum.indexOfFirst { it == nL }
    var next = position(group, lead)
    return leadAlloc.mapIndexed { i, pages ->
        if (pages.isNotEmpty()) next = pages.last() + 1
        if (i >= lastWithPages) return@mapIndexed rem.size
        val u = leadUnits.indexOfLast { it <= next }.coerceAtLeast(0)
        val start = leadUnits[u]
        val len = (leadUnits.getOrNull(u + 1) ?: (lead.last + 1)) - start
        val bStart = bUnits[u]
        val bLen = ((bUnits.getOrNull(u + 1) ?: (b.last + 1)) - bStart).toLong()
        val into = (next - start).coerceAtLeast(0).toLong()
        val target = if (len <= 0) bStart + bLen else bStart + (2L * into * bLen + len) / (2L * len)
        rem.count { it < target }
    }
}

/** Followers whose units can't be linked to the lead book's, so they follow page by page. */
internal fun Plan.leadProblems(): List<String> {
    val lead = leadBook() ?: return emptyList()
    val n = lead.unitStarts().size
    if (n == 0) return emptyList()
    return books.filter { it.id != lead.id && it.due == null && !it.supplementary }.mapNotNull { b ->
        val m = b.unitStarts().size
        if (m == 0 || m == n) null
        else "${b.name}: its units don't match ${lead.name} ($m vs $n), so it follows page by page."
    }
}
