package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Verdict
import gr.edusuite.pacer.engine.followLead
import gr.edusuite.pacer.engine.parseRanges
import gr.edusuite.pacer.engine.position
import gr.edusuite.pacer.engine.taughtPages
import gr.edusuite.pacer.model.Book
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/*
 * What the group's Plan and Stats tabs lay out (design decisions 8, 11 and 22), in plain Kotlin so it is unit-testable.
 * Nothing here changes a figure the engine works out; it only groups, shortens and rounds them for display.
 */

/** A single-word name this long or shorter stays whole: "Reader", "Grammar". */
private const val WHOLE_WORD = 7

/** How much of a long single word is kept: "Vocabulary" → "Voc". */
private const val WORD_PREFIX = 3

/**
 * A book's name shortened for a one-line lesson row: the first letter of each word, with numbers kept whole
 * ("Student's Book" → "SB", "Book 2" → "B2"). A single word keeps its capitals ("WorkBook" → "WB"), a "…book" becomes
 * two letters ("Workbook" → "WB"), and a short word stays whole ("Reader"). TalkBack and the sheets use the full name.
 */
fun bookInitials(name: String): String {
    val words = name.trim().split(Regex("[^\\p{L}\\p{N}']+")).map { it.trim('\'') }.filter { it.isNotEmpty() }
    if (words.isEmpty()) return name.trim()
    if (words.size > 1) return words.joinToString("") { w -> if (w.all(Char::isDigit)) w else w.first().uppercase(Locale.ROOT) }
    val w = words.single()
    val capitals = w.filter(Char::isUpperCase)
    if (capitals.length > 1) return capitals
    val lower = w.lowercase(Locale.ROOT)
    if (lower.length > BOOK.length && lower.endsWith(BOOK)) return w.first().uppercase(Locale.ROOT) + "B"
    if (w.length <= WHOLE_WORD) return w
    return w.take(WORD_PREFIX).replaceFirstChar { it.uppercase(Locale.ROOT) }
}

private const val BOOK = "book"

/** [bookInitials] for every book of a plan, in order; two books that would read the same keep their full names. */
fun shortBookNames(names: List<String>): List<String> {
    val short = names.map(::bookInitials)
    val counts = short.groupingBy { it.lowercase(Locale.ROOT) }.eachCount()
    return names.mapIndexed { i, n -> if ((counts[short[i].lowercase(Locale.ROOT)] ?: 0) > 1) n else short[i] }
}

/** How a week section is headed: "This week", "Next week", or only its dates. */
enum class WeekLabel { THIS, NEXT, DATES }

/** A Monday-to-[end] week of the lesson list and what falls in it, in date order. */
data class WeekSection<T>(val start: LocalDate, val end: LocalDate, val label: WeekLabel, val items: List<T>)

/**
 * [items] in Monday-first weeks, in date order. The week holding [today] reads "This week" and the one after
 * "Next week"; every other week, past or later, reads as its dates. A week runs to Friday, or to Sunday when one of its
 * items falls on the weekend.
 */
fun <T> weekSections(items: List<T>, today: LocalDate, dateOf: (T) -> LocalDate): List<WeekSection<T>> {
    val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return items.sortedBy(dateOf).groupBy { dateOf(it).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
        .map { (monday, list) ->
            val weekend = list.any { dateOf(it).dayOfWeek >= DayOfWeek.SATURDAY }
            val label = when (monday) {
                thisWeek -> WeekLabel.THIS
                thisWeek.plusWeeks(1) -> WeekLabel.NEXT
                else -> WeekLabel.DATES
            }
            WeekSection(monday, monday.plusDays(if (weekend) 6 else 4), label, list)
        }
}

/** "19–23 Oct", or "30 Nov – 4 Dec" across a month's end. */
fun weekRange(start: LocalDate, end: LocalDate): String {
    val month = { d: LocalDate -> d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
    return if (start.month == end.month) "${start.dayOfMonth}–${end.dayOfMonth} ${month(end)}"
    else "${start.dayOfMonth} ${month(start)} – ${end.dayOfMonth} ${month(end)}"
}

/** Every page a book holds, in order: first to last without the skipped ones. Empty for a supplementary book. */
fun Book.allPages(): List<Int> {
    if (supplementary || last < first) return emptyList()
    val skip = parseRanges(skip)
    return (first..last).filter { it !in skip }
}

/**
 * Where each book that follows the lead book would be if it were in step with it: book id to the next page it would
 * teach. It is the engine's own [followLead] rule laid over the whole book instead of the pages left, with the lead
 * book's taught pages as one lesson, so it adds no metric of its own. Empty with no lead book; the lead book, books with
 * a finish-by date and supplementary books are left out, as [followLead] leaves them alone.
 */
fun GroupReport.inStepPages(): Map<String, Int> {
    val li = plan.books.indexOfFirst { it.id == plan.lead && !it.supplementary }
    if (li < 0) return emptyMap()
    val lead = plan.books[li]
    val leadAll = lead.allPages()
    val taught = group.taughtPages(lead.id)
    val pos = position(group, lead)
    val rest = leadAll.filter { it >= pos && it !in taught }
    val done = leadAll.filter { it !in rest.toSet() }
    val alloc = plan.books.mapIndexed { i, _ ->
        if (i == li) mutableListOf(done, rest) else mutableListOf<List<Int>>(emptyList(), emptyList())
    }
    val followed = followLead(alloc) { it.allPages() }
    return plan.books.withIndex().mapNotNull { (i, b) ->
        if (i == li || b.supplementary || b.due != null) return@mapNotNull null
        val all = b.allPages()
        if (all.isEmpty()) return@mapNotNull null
        val k = followed[i].firstOrNull()?.size ?: return@mapNotNull null
        b.id to (all.getOrNull(k) ?: (b.last + 1))
    }.toMap()
}

/** How many more pages a lesson a short group has to teach to finish, rounded up like a lesson count (0.19 → 1). */
fun Verdict.Short.pagesMore(): Int = wholeLessons(needRate - sustain)

/** Calendar sections for the Plan tab. */
data class MonthSection<T>(val month: java.time.YearMonth, val thisMonth: Boolean, val items: List<T>)
fun <T> monthSections(items: List<T>, today: LocalDate, dateOf: (T) -> LocalDate): List<MonthSection<T>> =
    items.sortedBy(dateOf).groupBy { java.time.YearMonth.from(dateOf(it)) }.map { (month, list) ->
        MonthSection(month, month == java.time.YearMonth.from(today), list)
    }

fun <T> filterMonth(items: List<T>, month: java.time.YearMonth, dateOf: (T) -> LocalDate): List<T> =
    items.filter { java.time.YearMonth.from(dateOf(it)) == month }

/** Count taught lessons and untaught lessons before today, never days off. */
fun <T> earlierSummary(items: List<T>, today: LocalDate, dateOf: (T) -> LocalDate, done: (T) -> Boolean): Int =
    items.count { done(it) || dateOf(it).isBefore(today) }
