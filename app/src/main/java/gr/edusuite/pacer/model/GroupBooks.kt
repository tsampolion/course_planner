package gr.edusuite.pacer.model

import java.time.LocalDate

/*
 * Per-group edits to the class's books and the group's own days off, as plain functions so the rules are unit-tested
 * without the view model.
 */

/**
 * The group with the default book [bookId] removed (or added back), or null when that isn't allowed: an unknown book,
 * an optional one (those are selected, not removed), or a removal that would leave the group no paced book.
 * Removing keeps the group's positions and taught pages, so adding the book back picks up where it was.
 */
fun Group.withBookRemoved(books: List<Book>, bookId: String, removed: Boolean): Group? {
    val book = books.firstOrNull { it.id == bookId && !it.groupOnly } ?: return null
    if (removed == (bookId in removedBooks)) return this
    val next = copy(removedBooks = if (removed) removedBooks + bookId else removedBooks - bookId)
    if (removed && books.none { !it.supplementary && next.usesBook(it) }) return null
    return next
}

/** The group with its own finish-by date for [bookId] set, or cleared with null so the class's date applies again. */
fun Group.withBookDue(books: List<Book>, bookId: String, date: LocalDate?): Group {
    if (books.none { it.id == bookId && !it.supplementary }) return this
    return copy(bookDue = if (date == null) bookDue - bookId else bookDue + (bookId to date))
}

/**
 * The year with the day off [id] changed, when it is one of group [groupId]'s own (class and whole-year days off are
 * edited elsewhere). The year is returned unchanged for any other id or for dates the wrong way round.
 */
fun SchoolYear.withClosureEdited(id: String, groupId: String, label: String, from: LocalDate, to: LocalDate?): SchoolYear {
    val end = to ?: from
    if (end.isBefore(from) || closures.none { it.id == id && it.who == groupId }) return this
    return copy(closures = closures.map { if (it.id == id) it.copy(label = label.ifBlank { "Day off" }, from = from, to = end) else it })
}
