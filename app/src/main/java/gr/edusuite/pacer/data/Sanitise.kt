package gr.edusuite.pacer.data

import gr.edusuite.pacer.engine.parseInclude
import gr.edusuite.pacer.engine.settleNextLesson
import gr.edusuite.pacer.model.MAX_LAG_LESSONS
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
        val classIds = clean.classes.mapTo(HashSet()) { it.id }
        val assigned = HashSet<String>()
        val collections = unique(clean.collections, { it.id }) { c, id -> c.copy(id = id) }.map { c ->
            c.copy(classIds = c.classIds.filterTo(linkedSetOf()) { it in classIds && assigned.add(it) })
        }
        clean.copy(collections = collections, classes = clean.classes.map { c -> val plan = clean.planFor(c); c.copy(groups = c.groups.map(plan::settleNextLesson)) })
    }

private fun sanitiseClass(c: SchoolClass): SchoolClass = c.copy(
    pace = c.pace.takeIf { it.isFinite() && it >= 0 } ?: 0.0,
    groups = unique(c.groups, { it.id }) { g, id -> g.copy(id = id) }.map { g ->
        val books = c.books.mapTo(HashSet()) { it.id }
        val supp = c.books.filter { it.supplementary }.mapTo(HashSet()) { it.id }
        g.copy(
            skip = g.skip.filterKeys { it in books },
            extraBooks = g.extraBooks.filterTo(linkedSetOf()) { id -> c.books.any { it.id == id && it.groupOnly } },
            done = g.done.distinctBy { it.date },
            assignments = unique(g.assignments.map { a ->
                if (a.id.isBlank()) a.copy(id = newId()) else a
            }, { it.id }) { a, id -> a.copy(id = id) }.map { a ->
                a.copy(pages = a.pages.filterKeys { it in books }.mapValues { (_, ps) ->
                    ps.map(::page).distinct().sorted()
                }.filterValues { it.isNotEmpty() })
            },
            extras = unique(g.extras.distinctBy { it.date }, { it.id }) { e, id -> e.copy(id = id) }.map { e ->
                e.copy(pages = e.pages.filterKeys { it in books && it !in supp }
                    .mapValues { (_, ps) -> ps.filter { it in 0..MAX_PAGE }.distinct().sorted() }.filterValues { it.isNotEmpty() })
            },
            custom = g.custom?.let { p ->
                p.copy(catchUpBoost = p.catchUpBoost?.takeIf { it in 1..MAX_PAGE }, lessons = unique(p.lessons, { it.id }) { l, id -> l.copy(id = id) }.map { l ->
                    val pages = l.pages.filterKeys { it in books }.mapValues { (_, ps) -> ps.filter { it in 0..MAX_PAGE } }.filterValues { it.isNotEmpty() }
                    l.copy(
                        pages = pages,
                        // Only supplementary books with pages in the lesson count in it; the base keeps core books only.
                        inLesson = l.inLesson.filterTo(HashSet()) { it in supp && it in pages },
                        replacementBase = l.replacementBase?.filterKeys { it in books && it !in supp }
                            ?.mapValues { (_, ps) -> ps.filter { it in 0..MAX_PAGE } }?.filterValues { it.isNotEmpty() },
                    )
                })
            },
        )
    },
    books = unique(c.books, { it.id }) { b, id -> b.copy(id = id) }.map { b ->
        // A supplementary book's first and last pages come from its include list when it reads.
        val inc = if (b.supplementary) parseInclude(b.include).orEmpty() else emptyList()
        if (inc.isNotEmpty()) b.copy(first = inc.first(), last = inc.last()) else b.copy(first = page(b.first), last = page(b.last))
    },
    homework = unique(c.homework, { it.id }) { h, id -> h.copy(id = id) }.map { h -> h.copy(from = page(h.from), to = page(h.to)) },
    events = unique(c.events, { it.id }) { e, id -> e.copy(id = id) },
    lead = c.lead?.takeIf { id -> c.books.any { it.id == id && !it.supplementary } },
    lagLessons = c.lagLessons.coerceIn(1, MAX_LAG_LESSONS),
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
