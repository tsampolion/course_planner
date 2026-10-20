package gr.edusuite.pacer.data

import gr.edusuite.pacer.engine.settleNextLesson
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.SchoolClass
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.newId

/**
 * Makes years read from a file safe to show and save: page numbers from 0 to [MAX_PAGE], a finite pace,
 * one done lesson per date, custom-plan pages only for the class's books, a lead book that is one of the
 * class's books, and ids that are unique where the screens list them. A repeated id gets a
 * new one; the first item with it keeps the days off, homework and positions aimed at it.
 * It also moves each group's next lesson past the lessons ticked in order ([settleNextLesson]): files from
 * 1.4.0 to 1.7.0 left it at the term start, so a later calendar edit brought past lessons back.
 * Other files the app saved itself come through unchanged. Pure Kotlin, so it is unit-tested without org.json.
 */
fun sanitiseYears(years: List<SchoolYear>): List<SchoolYear> =
    unique(years, { it.id }) { y, id -> y.copy(id = id) }.map { y ->
        val clean = y.copy(
            closures = unique(y.closures, { it.id }) { c, id -> c.copy(id = id) },
            classes = unique(y.classes, { it.id }) { c, id -> c.copy(id = id) }.map(::sanitiseClass),
        )
        clean.copy(classes = clean.classes.map { c -> val plan = clean.planFor(c); c.copy(groups = c.groups.map(plan::settleNextLesson)) })
    }

private fun sanitiseClass(c: SchoolClass): SchoolClass = c.copy(
    pace = c.pace.takeIf { it.isFinite() && it >= 0 } ?: 0.0,
    groups = unique(c.groups, { it.id }) { g, id -> g.copy(id = id) }.map { g ->
        val books = c.books.mapTo(HashSet()) { it.id }
        g.copy(
            done = g.done.distinctBy { it.date },
            custom = g.custom?.let { p ->
                p.copy(lessons = unique(p.lessons, { it.id }) { l, id -> l.copy(id = id) }.map { l ->
                    l.copy(pages = l.pages.filterKeys { it in books }.mapValues { (_, ps) -> ps.filter { it in 0..MAX_PAGE } }.filterValues { it.isNotEmpty() })
                })
            },
        )
    },
    books = unique(c.books, { it.id }) { b, id -> b.copy(id = id) }.map { b -> b.copy(first = page(b.first), last = page(b.last)) },
    homework = unique(c.homework, { it.id }) { h, id -> h.copy(id = id) }.map { h -> h.copy(from = page(h.from), to = page(h.to)) },
    events = unique(c.events, { it.id }) { e, id -> e.copy(id = id) },
    lead = c.lead?.takeIf { id -> c.books.any { it.id == id } },
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
