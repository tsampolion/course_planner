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

/** The starting plan: a Greek 2026-27 school year with two groups and three books. */
fun seedPlan(): Plan {
    fun d(s: String) = LocalDate.parse(s)
    fun off(label: String, from: String, to: String = from, who: String = ALL_GROUPS) =
        Closure(newId(), label, d(from), d(to), who)

    val start = d("2026-09-09")
    return Plan(
        id = newId(),
        name = "2026–27 main",
        start = start,
        end = d("2027-05-31"),
        mins = 75,
        pace = 4.0,
        groups = listOf(
            Group("ga", "Group A", setOf(MONDAY, WEDNESDAY, FRIDAY), start),
            Group("gb", "Group B", setOf(TUESDAY, THURSDAY, SATURDAY), start),
        ),
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
        books = listOf(
            Book(newId(), "Student's Book", 1, 160),
            Book(newId(), "Workbook", 1, 120),
            Book(newId(), "Grammar", 1, 103),
        ),
        homework = emptyList(),
        events = emptyList(),
    )
}

fun seedData(): AppData {
    val p = seedPlan()
    return AppData(plans = listOf(p), activePlan = p.id)
}
