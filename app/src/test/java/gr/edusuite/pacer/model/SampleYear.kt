package gr.edusuite.pacer.model

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

/**
 * The sample year the app used to start with, kept as a test fixture: the Greek 2026-27 school year with one class
 * of two groups and three books. Since 1.7.0 a fresh install starts with no years.
 */
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
