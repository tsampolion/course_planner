package com.coursebook.pacer.data

import com.coursebook.pacer.engine.GroupReport
import com.coursebook.pacer.engine.markDoneUpTo
import com.coursebook.pacer.model.Book
import com.coursebook.pacer.model.DoneLesson
import com.coursebook.pacer.model.Homework
import com.coursebook.pacer.model.MAX_PAGE
import com.coursebook.pacer.model.seedYear
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SanitiseTest {
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
}
