package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Source
import gr.edusuite.pacer.engine.position
import java.time.LocalDate

/** Figures describe the selected source, including undated overflow lessons in a custom plan. */
data class StatsFigures(val lessons: Int, val rate: Double, val end: LocalDate?, val custom: Boolean)

fun statsFigures(report: GroupReport): StatsFigures {
    val custom = report.custom
    if (custom == null) return StatsFigures(report.lessons.size, report.rate, report.rows.lastOrNull { !it.blocked }?.date, false)
    val coreIds = report.plan.books.filterNot { it.supplementary }.map { it.id }.toSet()
    val counts = custom.lessons.map { lesson -> lesson.pages.filterKeys { it in coreIds }.values.sumOf { it.size } }
    val extraRows = report.rows.filter { it.extraId != null }
    val lessons = custom.lessons.size + extraRows.size
    val pages = counts.sum() + extraRows.sumOf { row -> report.plan.books.indices.filter { !report.plan.books[it].supplementary }.sumOf { row.pages[it].size } }
    val end = if (custom.lessons.size > report.teachingDates.size) null else
        (report.teachingDates.take(custom.lessons.size) + extraRows.map { it.date }).maxOrNull()
    return StatsFigures(lessons, if (lessons > 0) pages.toDouble() / lessons else 0.0, end, true)
}

/** Next page after all proposed lessons strictly before today, with today's work still ahead. */
fun plannedPositionToday(report: GroupReport, bookIndex: Int, today: LocalDate): Int {
    val book = report.plan.books.getOrNull(bookIndex) ?: return 0
    if (book.supplementary) return position(report.group, book)
    val proposed = GroupReport(report.plan, report.group, Source.PROPOSED)
    val past = proposed.rows.filter { it.date < today }.flatMap { it.pages[bookIndex] }.toSet()
    val current = position(report.group, book)
    val all = book.allPages()
    return all.firstOrNull { it >= current && it !in past } ?: (book.last + 1)
}

fun ownPaceRate(text: String): Double? = text.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }

/** A separate plan line is useful only while the actual position trails the proposed one. */
fun planLinePosition(report: GroupReport, bookIndex: Int, today: LocalDate): Int? {
    val book = report.plan.books.getOrNull(bookIndex) ?: return null
    return plannedPositionToday(report, bookIndex, today).takeIf { !book.supplementary && it > position(report.group, book) }
}

/** Dated lessons by month for the selected source; overflow has no invented dates. */
fun statsMonths(report: GroupReport): List<Pair<LocalDate, Int>> {
    if (report.custom == null) return report.lessonsByMonth
    return report.rows.filter { !it.blocked && (it.lessonId != null || it.extraId != null) }
        .groupingBy { it.date.withDayOfMonth(1) }.eachCount().toList().sortedBy { it.first }
}
