package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.DoneLesson
import gr.edusuite.pacer.model.ExtraLesson
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.model.TestEvent
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

/**
 * The pages typed for [b] as taught in one lesson, such as "5-8, 10", in order. Blank is none.
 * Null when a part is not a page or a range of pages, or reaches outside the book.
 */
fun lessonPages(text: String, b: Book): List<Int>? {
    val out = sortedSetOf<Int>()
    for (part in text.split(',', ';')) {
        val t = part.trim()
        if (t.isEmpty()) continue
        val m = RANGE.matchEntire(t)
        val lo: Int
        val hi: Int
        if (m != null) {
            val a = m.groupValues[1].toIntOrNull() ?: return null
            val c = m.groupValues[2].toIntOrNull() ?: return null
            lo = minOf(a, c)
            hi = maxOf(a, c)
        } else {
            lo = t.takeIf { SINGLE.matches(it) }?.toIntOrNull() ?: return null
            hi = lo
        }
        if (lo < b.first || hi > b.last || hi - lo > MAX_RANGE) return null
        for (p in lo..hi) out += p
    }
    return out.toList()
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
    val out = groups.firstOrNull { it.id == groupId }?.assignments.orEmpty()
        .filter { it.type == AssignmentType.HOMEWORK }.flatMapTo(HashSet()) { it.pages[bookId].orEmpty() }
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

/** Pages of this book the group covered in lessons marked done. */
fun Group.taughtPages(bookId: String): Set<Int> = done.flatMapTo(HashSet()) { it.pages[bookId].orEmpty() }

/**
 * Pages this group still has to cover in this book, in order: from its position to the last page,
 * minus skipped pages, homework, and pages covered in lessons marked done. None for a supplementary book: it is never
 * paced, only placed by hand (engine/Supplementary.kt).
 */
fun Plan.remainingPages(g: Group, b: Book): List<Int> {
    if (b.supplementary || (b.groupOnly && b.id !in g.extraBooks)) return emptyList()
    val skip = parseRanges(b.skip) + parseRanges(g.skip[b.id].orEmpty())
    val hw = homeworkPages(g.id, b.id)
    val taught = g.taughtPages(b.id)
    val pinned = activeExtras(g).flatMapTo(HashSet()) { it.pages[b.id].orEmpty() }
    val out = ArrayList<Int>()
    for (p in position(g, b)..b.last) if (p !in skip && p !in hw && p !in taught && p !in pinned) out += p
    return out
}

/** Pages still ahead that were set for homework, so no lesson has to teach them. */
fun Plan.bankedPages(g: Group, b: Book): Int {
    if (b.supplementary || (b.groupOnly && b.id !in g.extraBooks)) return 0
    val skip = parseRanges(b.skip) + parseRanges(g.skip[b.id].orEmpty())
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

/**
 * Every lesson date left for this group: its weekdays, from its next lesson to term end,
 * minus closures and lessons marked done.
 */
fun Plan.lessonsFor(g: Group): List<LocalDate> {
    val end = endFor(g) ?: return emptyList()
    var d = g.from ?: start ?: return emptyList()
    val closed = closedDates(g.id)
    val taught = g.done.mapTo(HashSet()) { it.date }
    val out = ArrayList<LocalDate>()
    while (!d.isAfter(end)) {
        if (d.dayOfWeek in g.days && d !in closed && d !in taught) out += d
        d = d.plusDays(1)
    }
    return (out + activeExtras(g).map { it.date }).distinct().sorted()
}

/** Extras outside the remaining term, already taught, or blocked by a whole-lesson test do not reserve pages. */
fun Plan.activeExtras(g: Group): List<ExtraLesson> {
    val first = g.from ?: start ?: return emptyList()
    val last = endFor(g) ?: return emptyList()
    val taught = g.done.mapTo(HashSet()) { it.date }
    return g.extras.filter { it.date in first..last && it.date !in taught && eventsOn(g, it.date).none { e -> e.consumes } }
}

fun Plan.eventsOn(g: Group, date: LocalDate): List<TestEvent> =
    events.filter { it.date == date && (it.who == ALL_GROUPS || it.who == g.id) } +
        g.assignments.filter { it.type != AssignmentType.HOMEWORK && it.due == date }
            .map { TestEvent(it.id, it.title, date, g.id, it.takesLesson) }

/** Work set on or due on this lesson, once when both dates coincide. */
fun assignmentsOn(g: Group, date: LocalDate): List<Assignment> =
    g.assignments.filter { it.setOn == date || it.due == date }

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
    /** The custom plan's note for this lesson; blank in the proposed plan. */
    val note: String = "",
    /** The custom plan's lesson on this day, or null (the proposed plan, a test, or a day the custom plan leaves free). */
    val lessonId: String? = null,
    val extraId: String? = null,
    val assignments: List<Assignment> = emptyList(),
) {
    val cells: List<String> get() = if (blocked) pages.map { "" } else pages.map(::collapse)
}

/** Which plan a [GroupReport] lays out. */
enum class Source {
    /** The group's custom plan when it is active, otherwise the proposed one. */
    ACTIVE,
    /** The engine's plan, the web page's rule, whatever the group has. */
    PROPOSED,
    /** The group's custom plan, active or not; the proposed plan when it has none. */
    CUSTOM,
}

/**
 * Everything the group screen, the print sheet and the error list need for one group,
 * computed in a single pass. [source] picks the plan its rows follow; the figures that don't
 * depend on the rows (lessons, pages left, the verdict) are the same for both.
 */
class GroupReport(base: Plan, val group: Group, val source: Source = Source.ACTIVE) {
    val plan: Plan = base.forGroup(group)
    /** The custom plan the rows follow, or null for the proposed plan. */
    val custom: CustomPlan? = when (source) {
        Source.PROPOSED -> null
        Source.CUSTOM -> group.custom
        Source.ACTIVE -> group.custom?.takeIf { it.active }
    }
    val lessons: List<LocalDate> = plan.lessonsFor(group)
    internal val eventsByLesson: List<List<TestEvent>> = lessons.map { plan.eventsOn(group, it) }
    internal val blockedByLesson: List<Boolean> = eventsByLesson.map { list -> list.any { it.consumes } }
    internal val extrasByDate = plan.activeExtras(group).associateBy { it.date }
    val teachingDates: List<LocalDate> = lessons.filterIndexed { i, date -> !blockedByLesson[i] && date !in extrasByDate }

    // A supplementary book keeps its place in the list, so indices match plan.books, with nothing to pace.
    val bookStats: List<BookStats> = plan.books.map { b ->
        if (b.supplementary) BookStats(0, 0) else BookStats(plan.remainingPages(group, b).size, bookLessons(b, teachingDates))
    }

    val pages: Int = bookStats.sumOf { it.pages }
    val banked: Int = plan.books.sumOf { plan.bankedPages(group, it) }
    val teaching: Int get() = teachingDates.size
    val blocked: Int get() = blockedByLesson.count { it }
    val rate: Double get() = if (teaching > 0) pages.toDouble() / teaching else 0.0

    val rows: List<Row> by lazy {
        custom?.let { return@lazy customRows(it) }
        val alloc = proposedAllocation(teachingDates)
        var k = 0
        lessons.mapIndexed { i, date ->
            val blocked = blockedByLesson[i]
            val extra = extrasByDate[date]
            val pages = when {
                blocked -> plan.books.map { emptyList() }
                extra != null -> plan.books.map { b -> if (b.supplementary) emptyList() else extra.pages[b.id].orEmpty() }
                else -> alloc.map { it.getOrElse(k) { emptyList() } }
            }
            if (!blocked && extra == null) k++
            Row(i + 1, date, eventsByLesson[i], blocked, pages, extraId = extra?.id, assignments = assignmentsOn(group, date))
        }
    }

    /** The proposed allocation over ordinary slots, also used for a floating extra's pre-fill. */
    internal fun proposedAllocation(dates: List<LocalDate>): List<MutableList<List<Int>>> =
        plan.books.map { b ->
            var m = if (b.supplementary) 0 else bookLessons(b, dates)
            if (m == 0 && dates.isNotEmpty()) m = 1
            allocatePages(plan.remainingPages(group, b), m).toMutableList().apply {
                while (size < dates.size) add(emptyList())
            }
        }.let { followLead(it) }

    /** Most pages, summed over books, any single lesson carries. */
    val peak: Int by lazy {
        // The core load: supplementary pages placed in a lesson don't count.
        val core = plan.books.indices.filter { !plan.books[it].supplementary }
        rows.maxOfOrNull { r -> core.sumOf { r.pages[it].size } } ?: 0
    }

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

/**
 * The group with the upcoming lessons on [dates] marked done, each keeping the pages the schedule
 * gives it now. Lessons left undone keep their place, and the remaining pages re-spread over them.
 * Marking lessons in order leaves the same schedule as [afterNextLesson], the web page's rule.
 */
fun GroupReport.markDone(dates: Set<LocalDate>): Group {
    val fresh = rows.filter { it.date in dates }.map { r -> DoneLesson(r.date, pagesByBook(r)) }
    if (fresh.isEmpty()) return group
    return plan.settleNextLesson(group.copy(done = (group.done + fresh).sortedBy { it.date }, custom = plan.takeSlots(group, dates)))
}

/**
 * [g] with its next lesson moved to the day after the last lesson ticked in order, that is, the last
 * done lesson before the first lesson still to teach. Those lessons are in the past: adding a teaching
 * day or removing a day off later must not bring back the dates between them as lessons to teach.
 * Lessons ticked out of order leave it where it is, so the earlier ones keep their place. Never moves it back.
 * In order this is the web page's rule, which moved the next lesson on with every lesson taught.
 */
fun Plan.settleNextLesson(g: Group): Group {
    val from = g.from ?: start ?: return g
    val next = lessonsFor(g).firstOrNull()
    val last = g.done.lastOrNull { next == null || it.date.isBefore(next) }?.date ?: return g
    return if (last.isBefore(from)) g else g.copy(from = last.plusDays(1))
}

/** A row's pages as book id to pages, leaving out books it gives none. */
internal fun GroupReport.pagesByBook(r: Row): Map<String, List<Int>> =
    plan.books.zip(r.pages).filter { it.second.isNotEmpty() }.associate { (b, p) -> b.id to p }

/**
 * The pages each book gets on [date] if that lesson is taught as planned: what the schedule gives it,
 * or for a done lesson, what the schedule would give it if it were put back. Null when [date] is not
 * one of the group's lessons.
 */
fun GroupReport.plannedPages(date: LocalDate): Map<String, List<Int>>? {
    val report = if (group.done.any { it.date == date }) GroupReport(plan, plan.unmarkLesson(group, date), source) else this
    return report.rows.firstOrNull { it.date == date }?.let { pagesByBook(it) }
}

/**
 * The group with the lesson on [date] marked done as having covered [pages] (book id to pages),
 * whatever the schedule gave it. Pages it left out stay to be taught and pages beyond the plan leave
 * the schedule; either way the pages still to teach re-spread over the lessons left, as when ticking.
 * Recording the planned pages is the same as ticking. Null when [date] is not one of the group's lessons.
 */
fun GroupReport.recordLesson(date: LocalDate, pages: Map<String, List<Int>>): Group? {
    val old = group.done.firstOrNull { it.date == date }
    val planned = plannedPages(date)
    if (planned == null && old == null) return null
    val books = plan.books.mapTo(HashSet()) { it.id }
    val taught = pages.filterKeys { it in books }.mapValues { it.value.distinct().sorted() }.filterValues { it.isNotEmpty() }
    // Saving a done lesson's pages unchanged keeps its flag, even if the plan has moved since it was ticked.
    val edited = if (old != null && taught == old.pages) old.edited else taught != planned
    val lesson = DoneLesson(date, taught, edited)
    val custom = plan.takeSlots(group, setOf(date))
    return plan.settleNextLesson(group.copy(done = (group.done.filter { it.date != date } + lesson).sortedBy { it.date }, custom = custom))
}

/**
 * Marks done every upcoming lesson on or before [date], one at a time, as if each were ticked
 * after it was taught. The pages re-spread after each, so this matches [afterNextLesson] repeated.
 */
fun GroupReport.markDoneUpTo(date: LocalDate): Group {
    var report = this
    while (true) {
        val next = report.rows.firstOrNull()?.takeIf { !it.date.isAfter(date) } ?: return report.group
        report = GroupReport(plan, report.markDone(setOf(next.date)), source)
    }
}

/** Returns the lesson on [date] to the schedule, moving the next lesson back to it if ticking had moved it past. */
fun Group.unmarkDone(date: LocalDate): Group = copy(done = done.filter { it.date != date }, from = from?.let { minOf(it, date) })

/** Returns every done lesson on or after [date] to the schedule, moving the next lesson back to [date] if needed. */
fun Group.unmarkDoneFrom(date: LocalDate): Group = copy(done = done.filter { it.date.isBefore(date) }, from = from?.let { minOf(it, date) })

sealed interface Verdict {
    data class Fits(val spare: Double, val sustain: Double) : Verdict
    data class Short(val by: Double, val sustain: Double, val needRate: Double, val cutPages: Int) : Verdict
}

/** Lessons of slack treated as none when judging a plan short; see [verdict]. */
private const val FIT_TOLERANCE = 1e-9

/** Measures the required pace against the pace the teacher can sustain. Null when there is nothing to measure. */
fun GroupReport.verdict(): Verdict? {
    val sustain = plan.pace
    if (teaching == 0 || sustain <= 0) return null
    val gap = teaching - pages / sustain
    // A plan that fits exactly can come out a hair short in floating point (21 pages at 1.4 is 15.000000000000002
    // lessons), which read "Short by 0.00 lessons". Far below anything the verdict shows, so it counts as fitting.
    return if (gap < -FIT_TOLERANCE) {
        Verdict.Short(-gap, sustain, rate, kotlin.math.ceil(pages - teaching * sustain).toInt())
    } else {
        Verdict.Fits(gap.coerceAtLeast(0.0), sustain)
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
    msgs += leadProblems()
    books.forEachIndexed { i, b ->
        if (b.supplementary) {
            if (parseInclude(b.include).isNullOrEmpty()) msgs += "${b.name}: list the pages you'll use."
            return@forEachIndexed
        }
        if (b.last < b.first) msgs += "${b.name}: the last page is before the first."
        if (b.due != null && e != null && b.due.isAfter(e)) {
            msgs += "${b.name}: the finish-by date falls after the term ends, so it has no effect."
        }
        if (b.due != null) for (r in reports) {
            val effectiveIndex = r.plan.books.indexOfFirst { it.id == b.id }
            if (effectiveIndex < 0) continue
            val st = r.bookStats[effectiveIndex]
            if (st.crammed) {
                msgs += "${b.name}: ${r.group.name} has no lesson left before the finish-by date. " +
                    "All ${st.pages} pages land in the next lesson."
            }
        }
    }
    for (g in groups) {
        if (g.custom?.active != true) continue
        val cov = GroupReport(this, g).coverage() ?: continue
        val n = cov.missingCount
        if (n > 0) msgs += "${g.name}'s plan leaves $n ${if (n == 1) "page" else "pages"} out."
        if (cov.overflow > 0) msgs += "${g.name}'s plan has ${cov.overflow} ${if (cov.overflow == 1) "lesson" else "lessons"} after the term ends."
    }
    return msgs
}
