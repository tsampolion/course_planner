package com.coursebook.pacer.engine

import com.coursebook.pacer.model.ALL_GROUPS
import com.coursebook.pacer.model.Book
import com.coursebook.pacer.model.Group
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.model.TestEvent
import java.time.LocalDate

/** Ranges wider than this are typos, not page ranges; they are ignored instead of looping for ever. */
private const val MAX_RANGE = 100_000

private val RANGE = Regex("""^(\d+)\s*[-–—]\s*(\d+)$""")
private val SINGLE = Regex("""^\d+$""")

/** "8, 24-27; 30" to {8, 24, 25, 26, 27, 30}. Unreadable parts are ignored. */
fun parseRanges(text: String): Set<Int> {
    val out = HashSet<Int>()
    for (part in text.split(',', ';')) {
        val t = part.trim()
        if (t.isEmpty()) continue
        val m = RANGE.matchEntire(t)
        if (m != null) {
            val a = m.groupValues[1].toIntOrNull() ?: continue
            val b = m.groupValues[2].toIntOrNull() ?: continue
            val lo = minOf(a, b)
            val hi = maxOf(a, b)
            if (hi - lo > MAX_RANGE) continue
            for (p in lo..hi) out += p
        } else if (SINGLE.matches(t)) {
            t.toIntOrNull()?.let { out += it }
        }
    }
    return out
}

/** [1,2,3,5,7,8] to "1–3, 5, 7–8". Empty to an em dash. */
fun collapse(pages: List<Int>): String {
    if (pages.isEmpty()) return "—"
    val parts = ArrayList<String>()
    var s = pages[0]
    var p = pages[0]
    fun flush() { parts += if (s == p) "$s" else "$s–$p" }
    for (i in 1 until pages.size) {
        if (pages[i] == p + 1) p = pages[i] else { flush(); s = pages[i]; p = pages[i] }
    }
    flush()
    return parts.joinToString(", ")
}

/**
 * Splits [pages] over [lessons] so cumulative progress tracks a straight line:
 * lesson i ends at round(i * n / lessons). Every page lands exactly once.
 */
fun allocatePages(pages: List<Int>, lessons: Int): List<List<Int>> {
    val n = pages.size
    val out = ArrayList<List<Int>>(lessons)
    var prev = 0
    for (i in 1..lessons) {
        // round(i*n/L) with halves going up, in integer arithmetic.
        val cum = maxOf(prev, ((2L * i * n + lessons) / (2L * lessons)).toInt())
        out += pages.subList(prev, cum)
        prev = cum
    }
    return out
}

fun Plan.homeworkPages(groupId: String, bookId: String): Set<Int> {
    val out = HashSet<Int>()
    for (h in homework) {
        if (h.groupId != groupId || h.bookId != bookId) continue
        val lo = minOf(h.from, h.to)
        val hi = maxOf(h.from, h.to)
        if (hi - lo > MAX_RANGE) continue
        for (p in lo..hi) out += p
    }
    return out
}

/** The next page this group teaches in [b]. Never before the book's first page. */
fun position(g: Group, b: Book): Int = maxOf(b.first, g.pos[b.id] ?: b.first)

/** Pages this group still has to cover in this book, in order. */
fun Plan.remainingPages(g: Group, b: Book): List<Int> {
    val skip = parseRanges(b.skip)
    val hw = homeworkPages(g.id, b.id)
    val out = ArrayList<Int>()
    for (p in position(g, b)..b.last) if (p !in skip && p !in hw) out += p
    return out
}

/** Pages still ahead that were set for homework, so no lesson has to teach them. */
fun Plan.bankedPages(g: Group, b: Book): Int {
    val skip = parseRanges(b.skip)
    val hw = homeworkPages(g.id, b.id)
    return (position(g, b)..b.last).count { it !in skip && it in hw }
}

fun Plan.closedDates(groupId: String): Map<LocalDate, String> {
    val out = HashMap<LocalDate, String>()
    for (c in closures) {
        if (c.who != ALL_GROUPS && c.who != groupId) continue
        var d = minOf(c.from, c.to)
        val last = maxOf(c.from, c.to)
        while (!d.isAfter(last)) { out[d] = c.label; d = d.plusDays(1) }
    }
    return out
}

/** Every lesson date left for this group: its weekdays, from its next lesson to term end, minus closures. */
fun Plan.lessonsFor(g: Group): List<LocalDate> {
    val end = end ?: return emptyList()
    var d = g.from ?: start ?: return emptyList()
    if (g.days.isEmpty()) return emptyList()
    val closed = closedDates(g.id)
    val out = ArrayList<LocalDate>()
    while (!d.isAfter(end)) {
        if (d.dayOfWeek in g.days && d !in closed) out += d
        d = d.plusDays(1)
    }
    return out
}

fun Plan.eventsOn(g: Group, date: LocalDate): List<TestEvent> =
    events.filter { it.date == date && (it.who == ALL_GROUPS || it.who == g.id) }

/** How many teaching lessons a book gets. A finish-by date keeps only those on or before it. */
fun bookLessons(b: Book, teaching: List<LocalDate>): Int {
    val due = b.due ?: return teaching.size
    return teaching.count { !it.isAfter(due) }
}

data class BookStats(val pages: Int, val lessons: Int) {
    val rate: Double get() = if (lessons > 0) pages.toDouble() / lessons else 0.0
    /** Pages are left but no lesson falls before the finish-by date. */
    val crammed: Boolean get() = pages > 0 && lessons == 0
}

data class Row(
    val n: Int,
    val date: LocalDate,
    val events: List<TestEvent>,
    /** A test takes the whole lesson, so no pages are taught. */
    val blocked: Boolean,
    /** Pages per book, in the plan's book order. */
    val pages: List<List<Int>>,
) {
    val cells: List<String> get() = if (blocked) pages.map { "" } else pages.map(::collapse)
}

/**
 * Everything the group screen, the print sheet and the error list need for one group,
 * computed in a single pass.
 */
class GroupReport(val plan: Plan, val group: Group) {
    val lessons: List<LocalDate> = plan.lessonsFor(group)
    private val eventsByLesson: List<List<TestEvent>> = lessons.map { plan.eventsOn(group, it) }
    private val blockedByLesson: List<Boolean> = eventsByLesson.map { list -> list.any { it.consumes } }
    val teachingDates: List<LocalDate> = lessons.filterIndexed { i, _ -> !blockedByLesson[i] }

    val bookStats: List<BookStats> = plan.books.map { b ->
        BookStats(plan.remainingPages(group, b).size, bookLessons(b, teachingDates))
    }

    val pages: Int = bookStats.sumOf { it.pages }
    val banked: Int = plan.books.sumOf { plan.bankedPages(group, it) }
    val teaching: Int get() = teachingDates.size
    val blocked: Int get() = lessons.size - teaching
    val rate: Double get() = if (teaching > 0) pages.toDouble() / teaching else 0.0

    val rows: List<Row> by lazy {
        val alloc = plan.books.mapIndexed { i, b ->
            var m = bookStats[i].lessons
            // Finish-by date before the next lesson: everything lands at once.
            if (m == 0 && teaching > 0) m = 1
            val a = allocatePages(plan.remainingPages(group, b), m).toMutableList()
            // Nothing after the finish-by date.
            while (a.size < teaching) a += emptyList<Int>()
            a
        }
        var k = 0
        lessons.mapIndexed { i, date ->
            val blocked = blockedByLesson[i]
            val pages = if (blocked) plan.books.map { emptyList() } else alloc.map { it.getOrElse(k) { emptyList() } }
            if (!blocked) k++
            Row(i + 1, date, eventsByLesson[i], blocked, pages)
        }
    }

    /** Most pages, summed over books, any single lesson carries. */
    val peak: Int by lazy { rows.maxOfOrNull { r -> r.pages.sumOf { it.size } } ?: 0 }

    /** Lessons per calendar month, in date order. */
    val lessonsByMonth: List<Pair<LocalDate, Int>> by lazy {
        lessons.groupingBy { it.withDayOfMonth(1) }.eachCount().toList().sortedBy { it.first }
    }

    /** Book id to its new position, and the group's new next lesson, once the next lesson is taught. */
    fun afterNextLesson(): Group? {
        val first = rows.firstOrNull() ?: return null
        val pos = group.pos.toMutableMap()
        first.pages.forEachIndexed { i, pages -> if (pages.isNotEmpty()) pos[plan.books[i].id] = pages.last() + 1 }
        val nextFrom = rows.getOrNull(1)?.date ?: (plan.end ?: first.date).plusDays(1)
        return group.copy(pos = pos, from = nextFrom)
    }
}

sealed interface Verdict {
    data class Fits(val spare: Double, val sustain: Double) : Verdict
    data class Short(val by: Double, val sustain: Double, val needRate: Double, val cutPages: Int) : Verdict
}

/** Measures the required pace against the pace the teacher can sustain. Null when there is nothing to measure. */
fun GroupReport.verdict(): Verdict? {
    val sustain = plan.pace
    if (teaching == 0 || sustain <= 0) return null
    val gap = teaching - pages / sustain
    return if (gap < 0) {
        Verdict.Short(-gap, sustain, rate, kotlin.math.ceil(pages - teaching * sustain).toInt())
    } else {
        Verdict.Fits(gap, sustain)
    }
}

/** Problems that make the schedule wrong or empty, in plain words. */
fun Plan.problems(): List<String> {
    val msgs = ArrayList<String>()
    val s = start
    val e = end
    if (s == null || e == null) msgs += "Set both a term start and a term end."
    else if (e.isBefore(s)) msgs += "The term ends before it starts."
    if (groups.isEmpty()) msgs += "Add at least one group."
    for (g in groups) {
        if (g.days.isEmpty()) msgs += "${g.name} has no teaching days selected."
        if (g.from != null && e != null && g.from.isAfter(e)) msgs += "${g.name} has finished the term."
    }
    if (books.isEmpty()) msgs += "Add at least one book."
    val reports by lazy { groups.map { GroupReport(this, it) } }
    books.forEachIndexed { i, b ->
        if (b.last < b.first) msgs += "${b.name}: the last page is before the first."
        if (b.due != null && e != null && b.due.isAfter(e)) {
            msgs += "${b.name}: the finish-by date falls after the term ends, so it has no effect."
        }
        if (b.due != null) for (r in reports) {
            val st = r.bookStats[i]
            if (st.crammed) {
                msgs += "${b.name}: ${r.group.name} has no lesson left before the finish-by date. " +
                    "All ${st.pages} pages land in the next lesson."
            }
        }
    }
    return msgs
}
