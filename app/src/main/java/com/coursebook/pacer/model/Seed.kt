package com.coursebook.pacer.model

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import kotlin.random.Random

private const val ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"

fun newId(): String = (1..7).map { ID_CHARS[Random.nextInt(ID_CHARS.length)] }.joinToString("")

/** The starting year: the Greek 2026-27 school year with one class of two groups and three books. */
fun seedYear(): SchoolYear {
    fun d(s: String) = LocalDate.parse(s)
    fun off(label: String, from: String, to: String = from, who: String = ALL_GROUPS) =
        Closure(newId(), label, d(from), d(to), who)

    val start = d("2026-09-09")
    return SchoolYear(
        id = newId(),
        name = "2026–27",
        start = start,
        end = d("2027-05-31"),
        closures = listOf(
            off("Ohi Day", "2026-10-28"),
            off("Polytechnic Day", "2026-11-17"),
            off("Agios Nikolaos (Kos)", "2026-12-06"),
            off("Report cards 1", "2026-12-11", who = "ga"),
            off("Report cards 1", "2026-12-12", who = "gb"),
            off("Christmas break", "2026-12-20", "2027-01-07"),
            off("Clean Monday", "2027-03-15"),
            off("Report cards 2", "2027-03-19", who = "ga"),
            off("Report cards 2", "2027-03-20", who = "gb"),
            off("Independence Day", "2027-03-25"),
            off("Easter break", "2027-04-25", "2027-05-09"),
            off("Report cards 3", "2027-05-28", who = "ga"),
            off("Report cards 3", "2027-05-29", who = "gb"),
        ),
        classes = listOf(
            SchoolClass(
                id = newId(),
                name = "Sample class",
                mins = 75,
                pace = 4.0,
                groups = listOf(
                    Group("ga", "Group A", setOf(MONDAY, WEDNESDAY, FRIDAY), start),
                    Group("gb", "Group B", setOf(TUESDAY, THURSDAY, SATURDAY), start),
                ),
                books = listOf(
                    Book(newId(), "Student's Book", 1, 160),
                    Book(newId(), "Workbook", 1, 120),
                    Book(newId(), "Grammar", 1, 103),
                ),
                homework = emptyList(),
                events = emptyList(),
            ),
        ),
    )
}

/** The sample class as the engine sees it. */
fun seedPlan(): Plan = seedYear().let { it.planFor(it.classes.first()) }

fun seedData(): AppData = AppData(years = listOf(seedYear()))

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
