package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.markDone
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.model.seedYear
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModelTest {
    private val seed = seedYear()
    private val sample = seed.classes.single()

    /** The seed with a second class whose group starts earlier than the sample's (Wednesday 9 September). */
    private fun twoClasses(): SchoolYear {
        val early = sample.copy(
            id = "c2", name = "Early class",
            groups = listOf(sample.groups.first().copy(id = "g2", name = "Early group", from = LocalDate.parse("2026-09-07"))),
        )
        return seed.copy(classes = listOf(sample, early))
    }

    @Test fun theEarliestLessonAcrossClassesWins() {
        val next = nextLesson(twoClasses(), LocalDate.parse("2026-09-01"))!!
        assertEquals("c2", next.classId)
        assertEquals("g2", next.groupId)
        assertEquals(LocalDate.parse("2026-09-07"), next.date)
        assertFalse(next.overdue)
        assertTrue(next.what.startsWith("Student's Book "))
    }

    @Test fun aTieGoesToTheFirstClassThenTheFirstGroup() {
        val tied = sample.copy(groups = sample.groups.map { it.copy(days = sample.groups.first().days) })
        val next = nextLesson(seed.copy(classes = listOf(tied)), LocalDate.parse("2026-09-01"))!!
        assertEquals("ga", next.groupId)
    }

    @Test fun aPastLessonIsOverdue() {
        assertTrue(nextLesson(seed, LocalDate.parse("2026-10-01"))!!.overdue)
    }

    @Test fun aLessonATestTakesNamesTheTest() {
        val day = LocalDate.parse("2026-09-09")
        val withTest = sample.copy(events = listOf(TestEvent("t", "Unit 1 test", day, "ga", consumes = true)))
        val next = nextLesson(seed.copy(classes = listOf(withTest)), day)!!
        assertTrue(next.blocked)
        assertEquals("Unit 1 test", next.what)
    }

    @Test fun noClassesOrNoLessonsGiveNothing() {
        assertNull(nextLesson(seed.copy(classes = emptyList()), LocalDate.parse("2026-09-01")))
        assertNull(nextLesson(seed.copy(start = null, end = null), LocalDate.parse("2026-09-01")))
    }

    /** Home ticks a group of any class; the result must equal ticking it on the group screen of the open class. */
    @Test fun tickingFromHomeEqualsTickingTheOpenClass() {
        val year = twoClasses()
        val ticked = year.markNextLessonDone("c2", "g2")
        val c = year.classes[1]
        val plan = year.planFor(c)
        val r = GroupReport(plan, plan.groups.single())
        val expected = r.markDone(setOf(r.rows.first().date))
        assertEquals(expected, ticked.classes[1].groups.single())
        assertEquals(year.classes[0], ticked.classes[0])
        // That group's next lesson moves on to its second one.
        val after = ticked.planFor(ticked.classes[1])
        assertEquals(r.rows[1].date, GroupReport(after, after.groups.single()).rows.first().date)
    }

    @Test fun unknownIdsChangeNothing() {
        assertEquals(seed, seed.markNextLessonDone("nope", "ga"))
        assertEquals(seed, seed.markNextLessonDone(sample.id, "nope"))
    }

    @Test fun summaryCountsTheYear() {
        val s = homeSummary(seed, LocalDate.parse("2026-09-01"))
        assertEquals(1, s.classes)
        assertEquals(2, s.groups)
        assertEquals(0, s.tests)
        assertNull(s.nextTest)
        assertEquals(seed.closures.size, s.daysOff)
        assertTrue(s.datesSet)
    }

    @Test fun theDefaultYearIsTheOneThatStartsLast() {
        val a = seed.copy(id = "a", start = LocalDate.parse("2025-09-10"))
        val b = seed.copy(id = "b", start = LocalDate.parse("2026-09-09"))
        val undated = seed.copy(id = "u", start = null)
        assertEquals("b", defaultYearId(listOf(a, undated, b)))
        assertEquals("u", defaultYearId(listOf(undated)))
        assertNull(defaultYearId(emptyList()))
    }

    @Test
    fun gridColumnsFollowTheWidth() {
        assertEquals(2, homeColumns(328f, 1f, 6)) // a 360dp phone less its 16dp edges
        assertEquals(2, homeColumns(380f, 1f, 6))
        assertEquals(4, homeColumns(768f, 1f, 6)) // a phone in landscape
        assertEquals(6, homeColumns(1248f, 1f, 6)) // never more columns than tiles
    }

    @Test
    fun gridIsOneColumnAtLargeFontOrNarrowWidth() {
        assertEquals(1, homeColumns(768f, 2f, 6))
        assertEquals(1, homeColumns(300f, 1f, 6))
    }

    @Test
    fun masonryPutsEachTileInTheShortestColumn() {
        assertEquals(
            listOf(0 to 0, 1 to 0, 1 to 108, 0 to 168),
            masonryPlacement(listOf(160, 100, 100, 100), columns = 2, gap = 8),
        )
        // Ties go to the leftmost column.
        assertEquals(listOf(0 to 0, 1 to 0, 2 to 0, 0 to 58), masonryPlacement(listOf(50, 50, 50, 50), columns = 3, gap = 8))
    }
}
