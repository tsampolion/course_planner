package gr.edusuite.pacer.model

import java.time.DayOfWeek
import java.time.LocalDate

/** Closures and tests aimed at every group use this in place of a group id. */
const val ALL_GROUPS = "all"

/**
 * The highest page number a book, a position or homework can use. The engine walks every page,
 * so a typo such as 1600000 would otherwise freeze the app; real coursebooks stop far below this.
 */
const val MAX_PAGE = 100_000

/** A book this many lessons behind the others gets a warning, unless the class sets its own number. */
const val DEFAULT_LAG_LESSONS = 2
const val MAX_LAG_LESSONS = 20

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
    /** The teacher's own plan, built from the proposed one. Null when the group has none. */
    val custom: CustomPlan? = null,
    /** Date-pinned core-book lessons, shared by both plans. */
    val extras: List<ExtraLesson> = emptyList(),
    /** Optional earlier finish date; null uses the school year's end. */
    val end: LocalDate? = null,
    /** Extra skipped page ranges by book id, added to the class defaults. */
    val skip: Map<String, String> = emptyMap(),
    /** Optional books from the class catalogue enabled for this group. */
    val extraBooks: Set<String> = emptySet(),
    /** Work owned by this group; legacy class work migrates on load/import/save. */
    val assignments: List<Assignment> = emptyList(),
    val unitsOverride: Boolean? = null,
    val unitRevisions: List<UnitRevision> = emptyList(),
    /** Default class books this group doesn't use. Its taught history stays; optional books use [extraBooks] instead. */
    val removedBooks: Set<String> = emptySet(),
    /** Book id to this group's own finish-by date, replacing the class's for that book (never past the group's end). */
    val bookDue: Map<String, LocalDate> = emptyMap(),
) {
    /** Whether this group paces [b]: an optional book only when selected, any other unless removed. */
    fun usesBook(b: Book): Boolean = if (b.groupOnly) b.id in extraBooks else b.id !in removedBooks
}

/** A saved extra lesson keeps its date and pages until the teacher edits it. */
data class ExtraLesson(val id: String, val date: LocalDate, val pages: Map<String, List<Int>> = emptyMap())

/**
 * The teacher's own plan for a group: the lessons still to teach, in order. Dates are not stored; lesson k
 * takes the group's k-th teaching day, so a day off or a test moves the content to the next lesson and keeps it.
 * [active] is true when it drives the schedule; false keeps it beside the proposed plan, unused.
 */
data class CustomPlan(
    val active: Boolean,
    val lessons: List<PlannedLesson>,
    /** Extra pages per lesson chosen for catch-up; re-pacing keeps this policy while the group is short. */
    val catchUpBoost: Int? = null,
)

/**
 * One lesson of a [CustomPlan]: the pages it teaches from each book (book id to pages) and a note.
 * [touched] is true once the teacher changed its pages, inserted it or moved it by hand; re-pacing never rewrites it.
 */
data class PlannedLesson(
    val id: String,
    val pages: Map<String, List<Int>> = emptyMap(),
    val note: String = "",
    val touched: Boolean = false,
    /** The supplementary books whose pages count in this lesson, taking the room of core pages the teacher chose. */
    val inLesson: Set<String> = emptySet(),
    /**
     * The lesson's core pages (book id to pages) just before its first in-lesson placement, so removing the last one can
     * restore them. Null when there is none to restore; an empty map means the lesson had no core pages.
     */
    val replacementBase: Map<String, List<Int>>? = null,
)

/**
 * A lesson marked as taught: its date, and the pages it covered in each book (book id to pages).
 * [edited] is true when the teacher recorded pages other than the ones the schedule gave it.
 */
data class DoneLesson(val date: LocalDate, val pages: Map<String, List<Int>> = emptyMap(), val edited: Boolean = false)

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
    /** Unit start pages as free text such as "1, 13, 25, 37". Links this book's units to the lead book's. */
    val units: String = "",
    /**
     * A supplementary book (a companion, a reader) is never paced: the teacher places its pages by hand in chosen
     * lessons. Its pages are [include], not first to last minus skipped; first and last are its lowest and highest.
     */
    val supplementary: Boolean = false,
    /** A supplementary book's pages, as free text such as "4-7, 12, 30-33". */
    val include: String = "",
    /** Whether a new placement of this supplementary book counts in the lesson (true) or comes on top (false). */
    val countInLesson: Boolean = false,
    /** Optional catalogue entry: only groups selecting this book use it. */
    val groupOnly: Boolean = false,
    /** Sets the book's abbreviation. Kept in step with [supplementary]: the Supplementary type is the supplementary switch. */
    val type: BookType = BookType.UNCLASSIFIED,
)

/** Unit indices are zero-based. NONE records an explicit decision to skip revision. */
enum class RevisionKind { TEST, WORKSHEET, PAGES, NONE }
data class UnitRevision(
    val unitIndex: Int,
    val kind: RevisionKind,
    val assignmentId: String? = null,
    val pages: Map<String, List<Int>> = emptyMap(),
)

enum class AssignmentType { HOMEWORK, TEST, WORKSHEET }

/** Group-owned work. A test or worksheet is scheduled on [due]; homework can also have a set-on date. */
data class Assignment(
    val id: String,
    val type: AssignmentType,
    val title: String,
    val setOn: LocalDate? = null,
    val due: LocalDate? = null,
    val pages: Map<String, List<Int>> = emptyMap(),
    val note: String = "",
    val takesLesson: Boolean = false,
    /** Homework can refer to a worksheet or test; null means ordinary pages/notes. */
    val contentType: AssignmentType? = null,
    /** The paired homework/class assignment, within the same group. */
    val linkedId: String? = null,
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
    /** The lead book's id: books without a finish-by date follow its pace. Null when none leads. */
    val lead: String? = null,
    /** How many lessons a book may fall behind the others before the group screen warns about it. */
    val lagLessons: Int = DEFAULT_LAG_LESSONS,
    val unitsEnabled: Boolean = false,
    val bookLinks: List<Set<String>> = emptyList(),
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
    /** The lead book's id: books without a finish-by date follow its pace. Null when none leads. */
    val lead: String? = null,
    /** How many lessons a book may fall behind the others before the group screen warns about it. */
    val lagLessons: Int = DEFAULT_LAG_LESSONS,
    val unitsEnabled: Boolean = false,
    val bookLinks: List<Set<String>> = emptyList(),
)

/** A named section on Classes. A class belongs to at most one collection. */
data class ClassCollection(val id: String, val name: String, val classIds: Set<String> = emptySet())

/** A school year: the calendar every one of its classes shares. */
data class SchoolYear(
    val id: String,
    val name: String,
    val start: LocalDate?,
    val end: LocalDate?,
    /** [Closure.who] is [ALL_GROUPS], a class id (every group in it) or a group id. */
    val closures: List<Closure>,
    val classes: List<SchoolClass>,
    val collections: List<ClassCollection> = emptyList(),
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
        return Plan(c.id, c.name, start, end, c.mins, c.pace, c.groups, days, c.books, c.homework, c.events, c.lead, c.lagLessons, c.unitsEnabled, c.bookLinks)
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
