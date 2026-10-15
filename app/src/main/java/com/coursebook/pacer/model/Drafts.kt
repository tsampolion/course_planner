package com.coursebook.pacer.model

import java.time.LocalDate
import kotlin.random.Random

private const val ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"

fun newId(): String = (1..7).map { ID_CHARS[Random.nextInt(ID_CHARS.length)] }.joinToString("")

/** What the "New school year" form starts with. */
data class YearDraft(val name: String, val start: LocalDate, val end: LocalDate)

/**
 * The year after the latest one already planned, or the one under way on [today] if that is later.
 * A school year counts as under way from July. Dates run from 1 September to 31 May.
 */
fun suggestYear(years: List<SchoolYear>, today: LocalDate): YearDraft {
    val current = if (today.monthValue >= 7) today.year else today.year - 1
    val next = years.mapNotNull { it.start?.year }.maxOrNull()?.plus(1) ?: current
    val y = maxOf(current, next)
    return YearDraft("$y–" + ((y + 1) % 100).toString().padStart(2, '0'), LocalDate.of(y, 9, 1), LocalDate.of(y + 1, 5, 31))
}
