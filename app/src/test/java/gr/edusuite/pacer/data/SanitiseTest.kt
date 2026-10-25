package gr.edusuite.pacer.data

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.markDoneUpTo
import gr.edusuite.pacer.model.ExtraLesson
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.BookType
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.DoneLesson
import gr.edusuite.pacer.model.Homework
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.seedYear
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SanitiseTest {
    @Test fun unitsMetadataKeepsOverridesAndCleansUnknownLinks() {
        val y = seedYear()
        val c = y.classes.first()
        val id = c.books.first().id
        val revised = c.copy(unitsEnabled = true, bookLinks = listOf(setOf(id, "missing")),
            groups = c.groups.map { it.copy(unitsOverride = false,
                unitRevisions = listOf(gr.edusuite.pacer.model.UnitRevision(0, gr.edusuite.pacer.model.RevisionKind.NONE))) })
        val out = sanitiseYears(listOf(y.copy(classes = listOf(revised)))).single().classes.single()
        assertEquals(false, out.groups.first().unitsOverride)
        assertEquals(1, out.groups.first().unitRevisions.size)
        assertEquals(emptyList<Set<String>>(), out.bookLinks)
        assertEquals(true, y.planFor(revised).unitsEnabled)
    }

    @Test fun savedCatchUpChoicesSurviveAndInvalidChoicesAreIgnored() {
        val y = seedYear()
        val c = y.classes.first()
        val g = c.groups.first()
        for (boost in listOf(null, 1, 2, 3, 0, -1, Int.MAX_VALUE)) {
            val custom = CustomPlan(true, emptyList(), catchUpBoost = boost)
            val incoming = y.copy(classes = listOf(c.copy(groups = listOf(g.copy(custom = custom)))))
            val out = sanitiseYears(listOf(incoming)).single().classes.single().groups.single().custom!!
            assertEquals(boost?.takeIf { it in 1..MAX_PAGE }, out.catchUpBoost)
        }
    }

    @Test fun aSavedYearComesThroughUnchanged() {
        val y = seedYear()
        assertEquals(listOf(y), sanitiseYears(listOf(y)))
    }

    @Test fun pageNumbersAreCappedSoTheScheduleStaysQuick() {
        val y = seedYear()
        val c = y.classes.first()
        val huge = c.copy(
            books = listOf(Book("b", "Huge", -2_000_000_000, 2_000_000_000)),
            homework = listOf(Homework("h", c.groups.first().id, "b", 5, Int.MAX_VALUE, null)),
        )
        val out = sanitiseYears(listOf(y.copy(classes = listOf(huge)))).single().classes.single()
        assertEquals(Book("b", "Huge", 0, MAX_PAGE), out.books.single())
        assertEquals(MAX_PAGE, out.homework.single().to)
        // Pages 0–MAX_PAGE, minus the homework on 5–MAX_PAGE, leave 0–4 to teach.
        val plan = y.copy(classes = listOf(out)).planFor(out)
        assertEquals(5, GroupReport(plan, plan.groups.first()).pages)
    }

    @Test fun aPaceTheFileCantStoreBecomesZero() {
        val y = seedYear()
        for (bad in listOf(Double.POSITIVE_INFINITY, Double.NaN, -1.0)) {
            val c = y.classes.first().copy(pace = bad)
            assertEquals(0.0, sanitiseYears(listOf(y.copy(classes = listOf(c)))).single().classes.single().pace, 0.0)
        }
    }

    @Test fun repeatedIdsGetNewOnesAndTheFirstKeepsItsOwn() {
        val y = seedYear()
        val c = y.classes.first()
        val twice = c.copy(groups = c.groups + c.groups.first(), books = c.books + c.books.first())
        val out = sanitiseYears(listOf(y.copy(classes = listOf(twice, twice)), y))
        assertEquals(y.id, out[0].id)
        assertNotEquals(out[0].id, out[1].id)
        assertEquals(c.id, out[0].classes[0].id)
        assertNotEquals(out[0].classes[0].id, out[0].classes[1].id)
        for (k in out[0].classes) {
            assertEquals(k.groups.size, k.groups.map { it.id }.toSet().size)
            assertEquals(k.books.size, k.books.map { it.id }.toSet().size)
        }
        assertEquals(c.groups.first().id, out[0].classes[0].groups.first().id)
    }

    /** Files from 1.4.0 to 1.7.0 kept the term start as the next lesson after ticking. */
    @Test fun lessonsTickedInOrderMoveTheNextLessonOnRead() {
        val y = seedYear()
        val c = y.classes.first()
        val g = c.groups.first()
        val r = GroupReport(y.planFor(c), g)
        val ticked = r.markDoneUpTo(r.rows[4].date)
        val saved = ticked.copy(from = g.from)
        val read = sanitiseYears(listOf(y.copy(classes = listOf(c.copy(groups = listOf(saved) + c.groups.drop(1))))))
        assertEquals(ticked, read.single().classes.single().groups.first())
    }

    @Test fun aDateTickedTwiceKeepsItsFirstRecord() {
        val y = seedYear()
        val c = y.classes.first()
        val d = LocalDate.parse("2026-09-09")
        val first = DoneLesson(d, mapOf(c.books.first().id to listOf(1, 2)))
        val g = c.groups.first().copy(done = listOf(first, DoneLesson(d)))
        val out = sanitiseYears(listOf(y.copy(classes = listOf(c.copy(groups = listOf(g)))))).single()
        assertEquals(listOf(first), out.classes.single().groups.single().done)
    }

    @Test fun supplementaryBooksAndTheirPlacementsAreKeptConsistent() {
        val y = seedYear()
        val c = y.classes.first()
        val g = c.groups.first()
        val comp = Book("s", "Companion", 1, 1, supplementary = true, include = "12-15, 30")
        val lesson = PlannedLesson(
            "x", mapOf(c.books.first().id to listOf(1), "s" to listOf(12)), touched = true,
            inLesson = setOf("s", c.books.first().id, "gone"), replacementBase = mapOf(c.books.first().id to listOf(1, 2), "s" to listOf(13)),
        )
        val dirty = c.copy(books = c.books + comp, lead = "s", groups = listOf(g.copy(custom = CustomPlan(true, listOf(lesson)))) + c.groups.drop(1))
        val out = sanitiseYears(listOf(y.copy(classes = listOf(dirty)))).single().classes.single()
        // Its first and last pages come from the include list, and it can't lead.
        assertEquals(12, out.books.last().first)
        assertEquals(30, out.books.last().last)
        assertEquals(null, out.lead)
        val l = out.groups.first().custom!!.lessons.single()
        assertEquals(setOf("s"), l.inLesson)
        assertEquals(mapOf(c.books.first().id to listOf(1, 2)), l.replacementBase)
    }
    @Test fun extrasKeepOnePerDateAndUniqueIdsAndOnlyCoreBookPages() {
        val y = seedYear()
        val c = y.classes.first()
        val b = c.books.first().id
        val date = y.start!!
        val extras = listOf(
            ExtraLesson("same", date, mapOf(b to listOf(-1, 0, 1, MAX_PAGE, MAX_PAGE + 1), "gone" to listOf(1), "s" to listOf(2))),
            ExtraLesson("duplicate-date", date),
            ExtraLesson("same", date.plusDays(1)),
        )
        val dirty = c.copy(books = c.books + Book("s", "Reader", 1, 2, supplementary = true), groups = listOf(c.groups.first().copy(extras = extras)))
        val out = sanitiseYears(listOf(y.copy(classes = listOf(dirty)))).single().classes.single().groups.single().extras
        assertEquals(2, out.size)
        assertNotEquals(out[0].id, out[1].id)
        assertEquals(mapOf(b to listOf(0, 1, MAX_PAGE)), out[0].pages)
        val removed = dirty.copy(books = dirty.books.filter { it.id != b })
        assertEquals(emptyMap<String, List<Int>>(), sanitiseYears(listOf(y.copy(classes = listOf(removed)))).single().classes.single().groups.single().extras.first().pages)
    }


    @Test fun supplementaryFlagAndTypeAreMadeToAgree() {
        val y = seedYear()
        val c = y.classes.first()
        val books = listOf(
            Book("a", "A", 1, 5, supplementary = true, include = "1-5"),
            Book("b", "B", 1, 5, include = "1-5", type = BookType.SUPPLEMENTARY),
            Book("c", "C", 1, 5, type = BookType.WORKBOOK),
        )
        val out = sanitiseYears(listOf(y.copy(classes = listOf(c.copy(books = books))))).single().classes.single().books
        assertEquals(listOf(true, true, false), out.map { it.supplementary })
        assertEquals(listOf(BookType.SUPPLEMENTARY, BookType.SUPPLEMENTARY, BookType.WORKBOOK), out.map { it.type })
    }

    @Test fun sanitiseDropsUnknownBookIds() {
        val y = seedYear()
        val c = y.classes.first()
        val sb = c.books[0].id
        val optional = Book("opt", "Optional", 1, 5, groupOnly = true)
        val supp = Book("sup", "Reader", 1, 5, supplementary = true, include = "1-5", type = BookType.SUPPLEMENTARY)
        val g = c.groups.first().copy(
            removedBooks = setOf(sb, "gone", "opt"),
            bookDue = mapOf(sb to LocalDate.parse("2027-01-01"), "gone" to LocalDate.parse("2027-01-01"), "sup" to LocalDate.parse("2027-01-01")),
        )
        val out = sanitiseYears(listOf(y.copy(classes = listOf(c.copy(books = c.books + optional + supp, groups = listOf(g)))))).single().classes.single().groups.single()
        assertEquals(setOf(sb), out.removedBooks)
        assertEquals(mapOf(sb to LocalDate.parse("2027-01-01")), out.bookDue)
    }
}
