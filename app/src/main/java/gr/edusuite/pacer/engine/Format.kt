package gr.edusuite.pacer.engine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Monday first, the way the week reads on a school timetable. */
val WEEK: List<DayOfWeek> = DayOfWeek.entries

fun DayOfWeek.short(): String = getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

fun LocalDate.monthShort(): String = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

/** "9 Sep 2026". */
fun LocalDate.fmt(): String = "$dayOfMonth ${monthShort()} $year"

/** "9 Sep". */
fun LocalDate.fmtShort(): String = "$dayOfMonth ${monthShort()}"

fun Double.fixed2(): String = String.format(Locale.US, "%.2f", this)

/** 4.0 to "4", 4.25 to "4.25". */
fun Double.plain(): String = if (this == Math.floor(this) && !isInfinite()) toLong().toString() else toString()

/** The schedule as tab-separated text, ready to paste into a spreadsheet or Word. A custom plan's notes get a last column. */
fun GroupReport.asTsv(): String {
    val books = plan.books
    val notes = rows.any { it.note.isNotEmpty() }
    val lines = ArrayList<String>()
    lines += (listOf("#", "Date", "Day") + books.map { it.name } + if (notes) listOf("Note") else emptyList()).joinToString("\t")
    for (r in rows) {
        val names = r.events.joinToString(" / ") { it.name }
        val cells = if (r.blocked) {
            listOf(names) + books.drop(1).map { "" }
        } else {
            r.cells.mapIndexed { i, c -> if (i == 0 && names.isNotEmpty()) "$c  [$names]" else c }
        }
        lines += (listOf(r.n.toString(), r.date.fmt(), r.date.dayOfWeek.short()) + cells + if (notes) listOf(r.note.replace('\t', ' ').replace('\n', ' ')) else emptyList()).joinToString("\t")
    }
    return lines.joinToString("\n")
}
