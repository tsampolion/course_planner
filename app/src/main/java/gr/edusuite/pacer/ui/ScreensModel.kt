package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.model.ClassCollection
import gr.edusuite.pacer.model.ClassOrder
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.SchoolYear
import java.text.Collator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/*
 * What Classes, School year and Pacing overview list (docs/PACER_DESIGN.md, 3.5.0), in plain Kotlin so it is
 * unit-testable. Status comes from the same verdict and rounding as everywhere else ([lessonsShort]).
 */

/** A group on Pacing overview: its status, its next lesson and how many lessons it has left. */
data class OverviewGroup(
    val classId: String,
    val groupId: String,
    val name: String,
    /** [lessonsShort]: 0 on pace, null with nothing to measure. */
    val short: Int?,
    val next: LocalDate?,
    val lessonsLeft: Int,
) {
    val behind: Boolean get() = (short ?: 0) > 0
}

/** A class on Pacing overview, with its groups in the class's order until [sortGroups] reorders them. */
data class OverviewClass(val classId: String, val name: String, val groups: List<OverviewGroup>) {
    val behind: Int get() = groups.count { it.behind }
}

/** Every class of the year, in the year's order, with each group's figures from its active plan. */
fun pacingOverview(year: SchoolYear, today: LocalDate): List<OverviewClass> = year.classes.map { c ->
    val plan = year.planFor(c, today)
    OverviewClass(c.id, c.name, plan.groups.map { g ->
        val r = GroupReport(plan, g)
        OverviewGroup(c.id, g.id, g.name, r.lessonsShort(), r.rows.firstOrNull()?.date, r.lessons.size)
    })
}

/** How Pacing overview orders the groups inside each class (design decision 7). */
enum class GroupOrder { MOST_BEHIND, NAME, NEXT_LESSON }

/**
 * Reorders the groups inside each class by [order], leaving the classes where they are. Ties keep the class's own
 * order. Most behind first puts groups with nothing to measure last; by next lesson puts groups with none left last.
 */
fun sortGroups(classes: List<OverviewClass>, order: GroupOrder): List<OverviewClass> {
    val collator = Collator.getInstance(Locale.ROOT).apply { strength = Collator.SECONDARY }
    val by: Comparator<OverviewGroup> = when (order) {
        GroupOrder.MOST_BEHIND -> compareByDescending<OverviewGroup> { it.short ?: -1 }
        GroupOrder.NAME -> Comparator { a, b -> collator.compare(a.name, b.name) }
        GroupOrder.NEXT_LESSON -> compareBy(nullsLast()) { it.next }
    }
    return classes.map { it.copy(groups = it.groups.sortedWith(by)) }
}

/** A class on Pacing overview in large-text mode: [shown] holds only its groups behind, and [onPace] are the rest. */
data class LargeClass(val shown: OverviewClass, val onPace: List<OverviewGroup>) {
    /** Groups behind the link that have nothing to measure yet; they are never called on pace. */
    val setUp: Int get() = onPace.count { it.short == null }
}

/**
 * Large-text Pacing overview: the classes with a group behind (their groups behind only, the rest one tap away) and,
 * apart, the classes whose groups are all on pace. Classes with no groups are left out.
 */
data class LargeOverview(val classes: List<LargeClass>, val allOnPace: List<OverviewClass>)

fun largeOverview(classes: List<OverviewClass>): LargeOverview {
    val behind = classes.filter { it.behind > 0 }.map { c ->
        val (late, ok) = c.groups.partition { it.behind }
        LargeClass(c.copy(groups = late), ok)
    }
    return LargeOverview(behind, classes.filter { it.behind == 0 && it.groups.isNotEmpty() })
}

/** A class on the Classes screen: its groups' names, how many books, and how many groups are behind. */
data class ClassSummary(
    val classId: String,
    val name: String,
    val groupNames: List<String>,
    val books: Int,
    val behind: Int,
    /** At least one group has a verdict, so "On pace" means something. */
    val measured: Boolean,
)

fun classSummaries(year: SchoolYear, today: LocalDate): List<ClassSummary> = pacingOverview(year, today).zip(year.classes) { o, c ->
    ClassSummary(c.id, c.name, c.groups.map { it.name }, c.books.size, o.behind, o.groups.any { it.short != null })
}

/** The term on one line: its weeks (part weeks count) and how many of its days are off for every group. */
data class TermSummary(val weeks: Int, val daysOff: Int)

/**
 * Null until both term dates are set and in order. Days off count the distinct dates inside the term that a day off
 * for every group covers, Sundays left out; a day off for one class or group doesn't count.
 */
fun termSummary(year: SchoolYear): TermSummary? {
    val start = year.start ?: return null
    val end = year.end ?: return null
    if (end.isBefore(start)) return null
    val weeks = ((ChronoUnit.DAYS.between(start, end) + 1 + 6) / 7).toInt()
    val off = HashSet<LocalDate>()
    year.closures.filter { it.who == ALL_GROUPS }.forEach { c ->
        var d = maxOf(c.from, start)
        val last = minOf(c.to, end)
        while (!d.isAfter(last)) {
            if (d.dayOfWeek != DayOfWeek.SUNDAY) off += d
            d = d.plusDays(1)
        }
    }
    return TermSummary(weeks, off.size)
}


/** Stable sorting leaves the teacher's order intact whenever the selected figures tie. */
fun sortClasses(classes: List<ClassSummary>, order: ClassOrder): List<ClassSummary> {
    val collator = Collator.getInstance(Locale.ROOT).apply { strength = Collator.SECONDARY }
    return when (order) {
        ClassOrder.YOUR_ORDER -> classes
        ClassOrder.NAME -> classes.sortedWith { a, b -> collator.compare(a.name, b.name) }
        ClassOrder.MOST_BEHIND -> classes.sortedWith(
            compareByDescending<ClassSummary> { it.behind }.thenByDescending { it.measured },
        )
    }
}

data class ClassSection(val collection: ClassCollection?, val classes: List<ClassSummary>)

/** Collections retain their order; malformed duplicate membership belongs to the first collection. */
fun classSections(classes: List<ClassSummary>, collections: List<ClassCollection>, order: ClassOrder): List<ClassSection> {
    if (collections.isEmpty()) return listOf(ClassSection(null, sortClasses(classes, order)))
    val owned = mutableSetOf<String>()
    val sections = collections.map { collection ->
        ClassSection(collection, sortClasses(classes.filter { it.classId in collection.classIds && owned.add(it.classId) }, order))
    }
    val remaining = classes.filter { it.classId !in owned }
    return if (remaining.isEmpty()) sections else sections + ClassSection(null, sortClasses(remaining, order))
}

/** Stable section keys occupy a namespace distinct from screen chrome and the uncollected section. */
fun classSectionKey(id: String?): String = if (id == null) "uncollected" else "collection:$id"

fun classSections(year: SchoolYear, classes: List<ClassSummary>, order: ClassOrder): List<ClassSection> =
    classSections(classes, year.collections, order)
