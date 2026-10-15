package com.coursebook.pacer.model

import java.time.DayOfWeek
import java.time.LocalDate

/** Closures and tests aimed at every group use this in place of a group id. */
const val ALL_GROUPS = "all"

data class Group(
    val id: String,
    val name: String,
    val days: Set<DayOfWeek>,
    /** The group's next lesson. Null falls back to the term start. */
    val from: LocalDate?,
    /** Book id to the next page this group teaches in that book. */
    val pos: Map<String, Int> = emptyMap(),
    /** Lessons marked as taught, in date order. Their dates and pages leave the schedule. */
    val done: List<DoneLesson> = emptyList(),
)

/** A lesson marked as taught: its date, and the pages it covered in each book (book id to pages). */
data class DoneLesson(val date: LocalDate, val pages: Map<String, List<Int>> = emptyMap())

data class Closure(
    val id: String,
    val label: String,
    val from: LocalDate,
    val to: LocalDate,
    /** [ALL_GROUPS] or a group id. */
    val who: String,
)

data class Book(
    val id: String,
    val name: String,
    val first: Int,
    val last: Int,
    /** Free text such as "8, 24-27". Skipped pages apply to every group. */
    val skip: String = "",
    /** Finish-by date. Only this book re-paces to meet it. */
    val due: LocalDate? = null,
)

data class Homework(
    val id: String,
    val groupId: String,
    val bookId: String,
    val from: Int,
    val to: Int,
    val setOn: LocalDate?,
)

data class TestEvent(
    val id: String,
    val name: String,
    val date: LocalDate,
    /** [ALL_GROUPS] or a group id. */
    val who: String,
    /** True when the test takes the whole lesson, so the pages move forward. */
    val consumes: Boolean,
)

/** What the engine paces: one class over one term. The app builds it with [SchoolYear.planFor]. */
data class Plan(
    val id: String,
    val name: String,
    val start: LocalDate?,
    val end: LocalDate?,
    val mins: Int,
    /** Pages per lesson the teacher can sustain. The benchmark the verdict measures against. */
    val pace: Double,
    val groups: List<Group>,
    val closures: List<Closure>,
    val books: List<Book>,
    val homework: List<Homework>,
    val events: List<TestEvent>,
)

/** A class: books taught to one or more groups, each with its own days and positions. */
data class SchoolClass(
    val id: String,
    val name: String,
    val mins: Int,
    /** Pages per lesson the teacher can sustain. The benchmark the verdict measures against. */
    val pace: Double,
    val groups: List<Group>,
    val books: List<Book>,
    val homework: List<Homework>,
    val events: List<TestEvent>,
)

/** A school year: the calendar every one of its classes shares. */
data class SchoolYear(
    val id: String,
    val name: String,
    val start: LocalDate?,
    val end: LocalDate?,
    /** [Closure.who] is [ALL_GROUPS], a class id (every group in it) or a group id. */
    val closures: List<Closure>,
    val classes: List<SchoolClass>,
) {
    /**
     * The [Plan] the engine paces for [c]: this year's term, the days off that reach the class,
     * and the class's own books and groups. Days off for a whole class become [ALL_GROUPS];
     * those for other classes' groups are left out.
     */
    fun planFor(c: SchoolClass): Plan {
        val groupIds = c.groups.mapTo(HashSet()) { it.id }
        val days = closures.mapNotNull { k ->
            when (k.who) {
                ALL_GROUPS -> k
                c.id -> k.copy(who = ALL_GROUPS)
                in groupIds -> k
                else -> null
            }
        }
        return Plan(c.id, c.name, start, end, c.mins, c.pace, c.groups, days, c.books, c.homework, c.events)
    }
}

/**
 * Everything the app stores. [activeYear] and [activeClass] are the open year and class (blank or
 * missing when none is). [view] is the open group's id, or "setup" when no group is open
 * (the app then starts on Home). The name is kept so files from older versions still read.
 */
data class AppData(
    val years: List<SchoolYear>,
    val activeYear: String = "",
    val activeClass: String = "",
    val view: String = VIEW_SETUP,
) {
    val year: SchoolYear? get() = years.firstOrNull { it.id == activeYear }
    val schoolClass: SchoolClass? get() = year?.classes?.firstOrNull { it.id == activeClass }
    /** The open class as the engine sees it. */
    val plan: Plan? get() {
        val y = year ?: return null
        return schoolClass?.let(y::planFor)
    }

    companion object {
        const val VIEW_SETUP = "setup"
    }
}
