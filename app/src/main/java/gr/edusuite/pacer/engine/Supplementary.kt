package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.newId

/*
 * Supplementary books (docs/SUPPLEMENTARY_BOOKS_BRIEF.md): a companion or a reader the teacher uses in chosen lessons.
 * They are never paced: remainingPages has none for them, so the proposed plan, the figures, the verdict, the lead book
 * and the warnings leave them out. Their pages live in a custom plan's lessons (placed by hand, on top of the lesson or
 * in it) and in done lessons, keyed by book id like any book.
 */

/** The books the engine paces, in the plan's order. */
val Plan.coreBooks: List<Book> get() = books.filter { !it.supplementary }

/** The books placed only by hand, in the plan's order. */
val Plan.supplementaryBooks: List<Book> get() = books.filter { it.supplementary }

/** The ids of the plan's supplementary books. */
internal val Plan.supplementaryIds: Set<String> get() = books.filter { it.supplementary }.mapTo(HashSet()) { it.id }

private val RANGE = Regex("""^(\d+)\s*[-–—]\s*(\d+)$""")
private val SINGLE = Regex("""^\d+$""")

/**
 * A supplementary book's pages as typed, such as "4-7, 12", sorted. Blank is none. Null when a part is not a page or a
 * range of pages (strict, like [lessonPages]), or reaches past [MAX_PAGE].
 */
fun parseInclude(text: String): List<Int>? {
    val out = sortedSetOf<Int>()
    for (part in text.split(',', ';')) {
        val t = part.trim()
        if (t.isEmpty()) continue
        val m = RANGE.matchEntire(t)
        val lo: Int
        val hi: Int
        if (m != null) {
            val a = m.groupValues[1].toIntOrNull() ?: return null
            val b = m.groupValues[2].toIntOrNull() ?: return null
            lo = minOf(a, b)
            hi = maxOf(a, b)
        } else {
            lo = t.takeIf { SINGLE.matches(it) }?.toIntOrNull() ?: return null
            hi = lo
        }
        if (hi > MAX_PAGE) return null
        for (p in lo..hi) out += p
    }
    return out.toList()
}

/** The pages a supplementary book includes; none for a core book or an unreadable list. */
fun Book.includedPages(): List<Int> = if (supplementary) parseInclude(include).orEmpty() else emptyList()

/** Whether [l] holds pages of any supplementary book. */
internal fun Plan.hasSupplementary(l: PlannedLesson): Boolean = l.pages.keys.any { it in supplementaryIds }

/** [pages] without the supplementary books' entries. */
internal fun Plan.coreOf(pages: Map<String, List<Int>>): Map<String, List<Int>> = pages.filterKeys { it !in supplementaryIds }

/** [pages] with only the supplementary books' entries. */
internal fun Plan.supplementaryOf(pages: Map<String, List<Int>>): Map<String, List<Int>> = pages.filterKeys { it in supplementaryIds }

/** Pages of supplementary book [b] this group has taught. */
fun GroupReport.taughtPages(b: Book): Set<Int> = group.taughtPages(b.id).intersect(b.includedPages().toSet())

/** Pages of supplementary book [b] the group's custom plan holds, in any lesson (active or not, in the term or not). */
fun GroupReport.placedPages(b: Book): Set<Int> =
    group.custom?.lessons.orEmpty().flatMapTo(HashSet()) { it.pages[b.id].orEmpty() }.intersect(b.includedPages().toSet())

/** Pages of supplementary book [b] neither taught nor placed in any lesson yet, for this group. */
fun GroupReport.unplacedPages(b: Book): List<Int> {
    val used = group.taughtPages(b.id) + group.custom?.lessons.orEmpty().flatMap { it.pages[b.id].orEmpty() }
    return b.includedPages().filter { it !in used }
}

/** Everything one "Plan this lesson" save writes: the core pages, the supplementary pages, which count in the lesson, the note. */
data class LessonSave(
    val core: Map<String, List<Int>>,
    val supplementary: Map<String, List<Int>>,
    val inLesson: Set<String>,
    val note: String,
)

/**
 * Saves lesson [slot] of the group's custom plan, made from the proposed plan if it has none ([toCustomPlan]); a slot
 * past its last lesson adds one. The save is whole or nothing: null when a page can't go there. Then:
 * - Supplementary pages must be included, not taught, and not in another lesson.
 * - Core pages must be the lesson's own (kept even if another lesson also has them), pages no lesson has yet, or pages
 *   of later lessons nobody touched.
 * - The first in-lesson placement keeps the lesson's core pages as its [PlannedLesson.replacementBase]; the base goes
 *   when no in-lesson placement is left (the caller then passes the base back as [LessonSave.core] to restore it).
 * - The lesson becomes touched. When its core pages or what counts in it changed, the lessons after it re-pace once,
 *   so pages it gave up land in the next untouched lessons and pages it took leave them.
 * The plan comes back active.
 */
fun GroupReport.saveLesson(slot: Int, save: LessonSave, id: () -> String = ::newId): CustomPlan? {
    val base = group.custom ?: toCustomPlan(id)
    val lessons = base.lessons.toMutableList()
    val at = if (slot in lessons.indices) slot else { lessons += PlannedLesson(id()); lessons.lastIndex }
    val old = lessons[at]
    val suppIds = plan.supplementaryIds
    val clean = { m: Map<String, List<Int>> -> m.mapValues { it.value.distinct().sorted() }.filterValues { it.isNotEmpty() } }
    val core = clean(save.core.filterKeys { k -> plan.books.any { it.id == k } && k !in suppIds })
    val supp = clean(save.supplementary.filterKeys { it in suppIds })
    val others = lessons.withIndex().filter { it.index != at }

    for ((bookId, pages) in supp) {
        val b = plan.books.first { it.id == bookId }
        val taken = group.taughtPages(bookId) + others.flatMap { it.value.pages[bookId].orEmpty() }
        val included = b.includedPages().toHashSet()
        if (pages.any { it !in included || it in taken }) return null
    }
    for ((bookId, pages) in core) {
        val b = plan.books.first { it.id == bookId }
        val held = others.filter { it.index < at || it.value.touched }.flatMapTo(HashSet()) { it.value.pages[bookId].orEmpty() }
        // The lesson may always keep what it has, even pages another lesson also holds.
        val allowed = plan.remainingPages(group, b).filterTo(HashSet()) { it !in held } + old.pages[bookId].orEmpty()
        if (pages.any { it !in allowed }) return null
    }

    val inLesson = save.inLesson.filterTo(HashSet()) { it in supp.keys }
    val oldCore = plan.coreOf(old.pages)
    val replacementBase = when {
        inLesson.isEmpty() -> null
        old.inLesson.isEmpty() -> oldCore
        else -> old.replacementBase
    }
    lessons[at] = old.copy(pages = core + supp, note = save.note.trim(), touched = true, inLesson = inLesson, replacementBase = replacementBase)
    val saved = CustomPlan(true, lessons)
    if (core == oldCore && inLesson == old.inLesson) return saved
    return GroupReport(plan, group.copy(custom = saved), Source.CUSTOM).repaceFrom(at + 1)
}
