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
)

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

/** Everything the app stores. [view] is "setup" or a group id. */
data class AppData(
    val plans: List<Plan>,
    val activePlan: String,
    val view: String = VIEW_SETUP,
) {
    val plan: Plan get() = plans.firstOrNull { it.id == activePlan } ?: plans.first()

    companion object {
        const val VIEW_SETUP = "setup"
    }
}
