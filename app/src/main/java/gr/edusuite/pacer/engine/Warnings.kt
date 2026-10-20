package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.newId
import java.time.LocalDate
import kotlin.math.roundToLong

/*
 * Pace and left-behind warnings, and the catch-up plans that answer them. None of this changes the proposed
 * plan's rules: a catch-up is always written into a custom plan, and only lessons nobody touched change.
 */

/**
 * Splits [pages] over lessons in proportion to [weights]: lesson i ends at round(W_i * n / W), halves up, where
 * W_i is the sum of the first i weights and W the total. Every page lands exactly once, in order. With equal
 * weights this is [allocatePages]. A total of zero or less puts every page in the first lesson.
 */
fun allocateWeighted(pages: List<Int>, weights: List<Long>): List<List<Int>> {
    if (weights.isEmpty()) return emptyList()
    val n = pages.size.toLong()
    val total = weights.sum()
    if (total <= 0) return listOf(pages) + List(weights.size - 1) { emptyList() }
    val out = ArrayList<List<Int>>(weights.size)
    var prev = 0
    var w = 0L
    for (x in weights) {
        w += x
        val cum = maxOf(prev, ((2 * w * n + total) / (2 * total)).toInt().coerceAtMost(pages.size))
        out += pages.subList(prev, cum)
        prev = cum
    }
    return out
}

/**
 * How to catch up a plan that is short: [behind] pages too many for the sustainable pace, made up by teaching
 * [boost] extra pages per lesson for [lessons] lessons, done by [until]. Not [feasible] when the term has fewer lessons.
 */
data class CatchUp(val behind: Int, val boost: Int, val lessons: Int, val until: LocalDate?, val feasible: Boolean)

/** The catch-up for [boost] extra pages per lesson (at least 1). Null unless the verdict is short. */
fun GroupReport.catchUp(boost: Int): CatchUp? {
    val v = verdict() as? Verdict.Short ?: return null
    val b = boost.coerceAtLeast(1)
    val behind = v.cutPages
    val n = (behind + b - 1) / b
    val feasible = n <= teaching
    return CatchUp(behind, b, n, if (feasible && n > 0) teachingDates[n - 1] else null, feasible)
}

/** The custom plan a catch-up starts from (untouched lessons past the term dropped), and its untouched in-term lessons. */
private fun GroupReport.catchUpBase(id: () -> String): Pair<MutableList<PlannedLesson>, List<Int>> {
    val c = group.custom ?: toCustomPlan(id)
    val days = teaching
    val kept = c.lessons.filterIndexed { i, l -> i < days || l.touched }.toMutableList()
    return kept to kept.indices.filter { it < days && !kept[it].touched }
}

/** Which of [targets] a book may use: those on or before its finish-by date, else the first one. */
private fun GroupReport.bookTargets(due: LocalDate?, targets: List<Int>): List<Int> {
    if (due == null) return targets
    return targets.filter { !teachingDates[it].isAfter(due) }.ifEmpty { targets.take(1) }
}

/**
 * A custom plan that teaches [boost] extra pages per lesson until the catch-up is done ([catchUp]) and the
 * sustainable pace after. The pages still to teach that no touched lesson has spread over the untouched lessons in
 * the term by weight; touched lessons never change and untouched lessons past the term end are dropped.
 * Null when the plan is not short or has no lesson to change.
 */
fun GroupReport.catchUpPlan(boost: Int, id: () -> String = ::newId): CustomPlan? {
    val cu = catchUp(boost) ?: return null
    val (out, targets) = catchUpBase(id)
    if (targets.isEmpty()) return null
    val s100 = (plan.pace * 100).roundToLong()
    // By teaching slot, not by target order: the first cu.lessons days of the term carry the boost.
    val weightOf = targets.associateWith { if (it < cu.lessons) s100 + cu.boost * 100L else s100 }
    for (t in targets) out[t] = out[t].copy(pages = emptyMap())
    for (b in plan.books) {
        val covered = out.flatMapTo(HashSet()) { it.pages[b.id].orEmpty() }
        val pool = plan.remainingPages(group, b).filter { it !in covered }
        if (pool.isEmpty()) continue
        val ts = bookTargets(b.due, targets)
        allocateWeighted(pool, ts.map { weightOf.getValue(it) }).forEachIndexed { j, pages ->
            if (pages.isNotEmpty()) out[ts[j]] = out[ts[j]].let { it.copy(pages = it.pages + (b.id to pages)) }
        }
    }
    return CustomPlan(true, out)
}

/** A book [lessonsBehind] lessons, at its own rate, behind the reference progress. */
data class BookLag(val bookId: String, val lessonsBehind: Double)

/** Book index to (pages in the book, share of them taught); books with no pages are left out. */
private fun GroupReport.progress(): Map<Int, Pair<Int, Double>> = plan.books.withIndex().mapNotNull { (i, b) ->
    val skip = parseRanges(b.skip)
    val total = if (b.last < b.first) 0 else (b.first..b.last).count { it !in skip }
    if (total == 0) null else i to (total to (total - plan.remainingPages(group, b).size).toDouble() / total)
}.toMap()

/** The progress book [i] is measured against: the [reference] book's, else the median of the other books'. */
private fun GroupReport.referenceFor(i: Int, prog: Map<Int, Pair<Int, Double>>, reference: String?): Double? {
    val ref = reference?.let { r -> plan.books.indexOfFirst { it.id == r } }?.takeIf { it >= 0 && it in prog }
    if (ref != null) return if (ref == i) null else prog.getValue(ref).second
    val others = prog.filterKeys { it != i }.values.map { it.second }.sorted()
    if (others.isEmpty()) return null
    val m = others.size / 2
    return if (others.size % 2 == 1) others[m] else (others[m - 1] + others[m]) / 2
}

/**
 * Books falling behind the others: lessonsBehind = (f_ref − f) × pages / rate, where f is the share of the book
 * taught and f_ref the [reference] book's share, or the median of the other books'. Only those beyond [threshold],
 * worst first. Needs at least two books; the reference book is never listed.
 */
fun GroupReport.bookLags(reference: String? = null, threshold: Double = 2.0): List<BookLag> {
    if (plan.books.size < 2) return emptyList()
    val prog = progress()
    return prog.mapNotNull { (i, p) ->
        val rate = bookStats[i].rate
        if (rate <= 0) return@mapNotNull null
        val fRef = referenceFor(i, prog, reference) ?: return@mapNotNull null
        BookLag(plan.books[i].id, (fRef - p.second) * p.first / rate)
    }.filter { it.lessonsBehind > threshold }.sortedByDescending { it.lessonsBehind }
}

/**
 * A custom plan that front-loads [bookId]'s backlog, round((f_ref − f) × pages) with f_ref the median of the other
 * books, over its next [over] untouched lessons in the term, on top of its own rate. Other books' pages and touched
 * lessons never change. Null when the book is unknown, not behind, or has no lesson to change.
 */
fun GroupReport.foldBacklog(bookId: String, over: Int, id: () -> String = ::newId): CustomPlan? {
    val i = plan.books.indexOfFirst { it.id == bookId }
    if (i < 0 || plan.books.size < 2) return null
    val b = plan.books[i]
    val prog = progress()
    val p = prog[i] ?: return null
    val fRef = referenceFor(i, prog, null) ?: return null
    val backlog = ((fRef - p.second) * p.first).roundToLong()
    if (backlog <= 0) return null
    val (out, targets) = catchUpBase(id)
    if (targets.isEmpty()) return null
    val k = over.coerceAtLeast(1)
    val rate100 = (bookStats[i].rate * 100).roundToLong()
    val extra = (backlog * 100.0 / k).roundToLong()
    for (t in targets) out[t] = out[t].copy(pages = out[t].pages - b.id)
    val covered = out.flatMapTo(HashSet()) { it.pages[b.id].orEmpty() }
    val pool = plan.remainingPages(group, b).filter { it !in covered }
    val ts = bookTargets(b.due, targets)
    allocateWeighted(pool, ts.indices.map { j -> if (j < k) rate100 + extra else rate100 }).forEachIndexed { j, pages ->
        if (pages.isNotEmpty()) out[ts[j]] = out[ts[j]].let { it.copy(pages = it.pages + (b.id to pages)) }
    }
    return CustomPlan(true, out)
}
