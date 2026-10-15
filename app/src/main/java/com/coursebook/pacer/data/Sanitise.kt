package com.coursebook.pacer.data

import com.coursebook.pacer.model.MAX_PAGE
import com.coursebook.pacer.model.SchoolClass
import com.coursebook.pacer.model.SchoolYear
import com.coursebook.pacer.model.newId

/**
 * Makes years read from a file safe to show and save: page numbers from 0 to [MAX_PAGE], a finite pace,
 * one done lesson per date, and ids that are unique where the screens list them. A repeated id gets a
 * new one; the first item with it keeps the days off, homework and positions aimed at it.
 * Files the app saved itself come through unchanged, so this only touches hand-edited or damaged ones.
 * Pure Kotlin, so it is unit-tested without org.json.
 */
fun sanitiseYears(years: List<SchoolYear>): List<SchoolYear> =
    unique(years, { it.id }) { y, id -> y.copy(id = id) }.map { y ->
        y.copy(
            closures = unique(y.closures, { it.id }) { c, id -> c.copy(id = id) },
            classes = unique(y.classes, { it.id }) { c, id -> c.copy(id = id) }.map(::sanitiseClass),
        )
    }

private fun sanitiseClass(c: SchoolClass): SchoolClass = c.copy(
    pace = c.pace.takeIf { it.isFinite() && it >= 0 } ?: 0.0,
    groups = unique(c.groups, { it.id }) { g, id -> g.copy(id = id) }.map { g -> g.copy(done = g.done.distinctBy { it.date }) },
    books = unique(c.books, { it.id }) { b, id -> b.copy(id = id) }.map { b -> b.copy(first = page(b.first), last = page(b.last)) },
    homework = unique(c.homework, { it.id }) { h, id -> h.copy(id = id) }.map { h -> h.copy(from = page(h.from), to = page(h.to)) },
    events = unique(c.events, { it.id }) { e, id -> e.copy(id = id) },
)

private fun page(p: Int) = p.coerceIn(0, MAX_PAGE)

/** [items] with every id after its first use replaced by a new one. */
private fun <T> unique(items: List<T>, id: (T) -> String, withId: (T, String) -> T): List<T> {
    val seen = HashSet<String>()
    return items.map { t ->
        if (seen.add(id(t))) {
            t
        } else {
            var fresh = newId()
            while (!seen.add(fresh)) fresh = newId()
            withId(t, fresh)
        }
    }
}
