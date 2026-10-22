package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.*
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ExtraLessonTest {
    private val start = LocalDate.of(2027, 3, 1)
    private val p = Plan("p", "P", start, start.plusDays(27), 60, 2.0,
        listOf(Group("g", "G", setOf(MONDAY), start)), emptyList(), listOf(Book("b", "Book", 1, 20)), emptyList(), emptyList())
    private val g = p.groups.single()
    private fun extra(date: LocalDate = start.plusDays(1)) = ExtraLesson("e", date, mapOf("b" to listOf(1, 2, 3, 4)))

    @Test fun noExtrasChangesNothing() {
        val seed = seedPlan()
        seed.groups.forEach { group ->
            val before = GroupReport(seed, group)
            val after = GroupReport(seed, group.copy(extras = emptyList()))
            assertEquals(before.rows, after.rows)
            assertEquals(before.verdict(), after.verdict())
            assertEquals(before.bookStats, after.bookStats)
        }
        assertEquals(List(4) { 5 }, GroupReport(p, g).rows.map { it.pages.single().size })
    }

    @Test fun extraInsideClosureBecomesALesson() {
        val plan = p.copy(closures = listOf(Closure("off", "Break", start, start, ALL_GROUPS)))
        val group = g.copy(extras = listOf(extra(start)))
        val r = GroupReport(plan, group)
        assertEquals(4, r.rows.size)
        assertEquals("e", r.rows.first().extraId)
        assertEquals(3, r.teaching)
        assertEquals(0, r.blocked)
    }

    @Test fun extraOnWeekdayOutsideDaysBecomesALesson() {
        val r = GroupReport(p, g.copy(extras = listOf(extra())))
        assertEquals(5, r.rows.size)
        assertEquals(4, r.teaching)
        assertEquals(0, r.blocked)
        assertEquals(listOf(1, 2, 3, 4), r.rows.single { it.extraId == "e" }.pages.single())
    }

    @Test fun pinnedPagesLeaveThePool() {
        val r = GroupReport(p, g.copy(extras = listOf(extra())))
        assertEquals((5..20).toList(), p.remainingPages(r.group, p.books.single()))
        assertEquals(4.0, (r.verdict() as Verdict.Short).by, 1e-9)
        assertEquals((1..20).toList(), r.rows.flatMap { it.pages.single() }.sorted())
    }

    @Test fun extraOnConsumingTestDatePagesReturnToPool() {
        for (date in listOf(start, start.plusDays(1))) {
            val plan = p.copy(events = listOf(TestEvent("test", "Test", date, ALL_GROUPS, true)))
            val group = g.copy(extras = listOf(extra(date)))
            assertTrue(plan.activeExtras(group).isEmpty())
            assertEquals((1..20).toList(), plan.remainingPages(group, plan.books.single()))
            assertTrue(GroupReport(plan, group).rows.none { it.extraId != null })
        }
    }

    @Test fun extraBeforeFromIsIgnoredAndItsPagesReturn() {
        for (date in listOf(start.minusDays(1), p.end!!.plusDays(1))) {
            val group = g.copy(extras = listOf(extra(date)))
            assertTrue(p.activeExtras(group).isEmpty())
            assertEquals((1..20).toList(), p.remainingPages(group, p.books.single()))
        }
    }

    @Test fun emptyDaysStillHasExtraLessons() {
        val r = GroupReport(p, g.copy(days = emptySet(), extras = listOf(extra())))
        assertEquals(listOf(extra().date), r.lessons)
        assertEquals(0, r.teaching)
        assertEquals(0, r.blocked)
        assertEquals(listOf(1, 2, 3, 4), r.rows.single().pages.single())
    }

    @Test fun doneExtraStaysDoneAndUndoneComesBack() {
        val group = g.copy(extras = listOf(extra()))
        val r = GroupReport(p, group)
        val done = r.markDone(setOf(extra().date))
        assertEquals(group.extras, done.extras)
        assertTrue(p.activeExtras(done).isEmpty())
        assertEquals(listOf(1, 2, 3, 4), done.done.single().pages["b"])
        val undone = p.unmarkLesson(done, extra().date)
        assertEquals(r.rows, GroupReport(p, undone).rows)
        assertEquals(group.extras, undone.extras)
    }

    @Test fun customPlanSlotsIgnoreExtras() {
        val custom = GroupReport(p, g).toCustomPlan()
        val group = g.copy(custom = custom, extras = listOf(extra()))
        val repaced = group.copy(custom = GroupReport(p, group).repaceFrom(0))
        val r = GroupReport(p, repaced)
        assertEquals(custom.lessons.map { it.id }, r.rows.mapNotNull { it.lessonId })
        val done = r.markDone(setOf(extra().date))
        assertEquals(repaced.custom, done.custom)
        val undone = p.unmarkLesson(done, extra().date)
        assertEquals(repaced.custom, undone.custom)
        assertEquals(r.rows, GroupReport(p, undone).rows)
        assertEquals(4, r.toCustomPlan().lessons.size)
    }
    @Test fun tickingFirstExtraAdvancesFromAndUntickingRestoresItWithoutACustomSlot() {
        val custom = GroupReport(p, g).toCustomPlan()
        val group = g.copy(custom = custom, extras = listOf(extra(start)))
        val r = GroupReport(p, group)
        val done = r.markDone(setOf(start))
        assertEquals(start.plusDays(1), done.from)
        assertEquals(custom, done.custom)
        val back = p.unmarkLesson(done, start)
        assertEquals(custom, back.custom)
        assertEquals(r.rows, GroupReport(p, back).rows)
    }

    @Test fun testForAnotherGroupAndNonConsumingTestDoNotBlockExtras() {
        val date = start.plusDays(1)
        val plan = p.copy(events = listOf(
            TestEvent("other", "Other group", date, "other-group", true),
            TestEvent("short", "Short test", date, g.id, false),
        ))
        val group = g.copy(extras = listOf(extra(date)))
        assertEquals(listOf(extra(date)), plan.activeExtras(group))
        assertEquals("e", GroupReport(plan, group).rows.single { it.date == date }.extraId)
    }

    @Test fun dateRangeUsesNextLessonOrTermStartAndRejectsMissingEnd() {
        val group = g.copy(from = start.plusDays(2), extras = listOf(extra(start.plusDays(1))))
        assertTrue(p.activeExtras(group).isEmpty())
        assertEquals((1..20).toList(), p.remainingPages(group, p.books.single()))
        assertEquals(1, p.activeExtras(group.copy(from = null)).size)
        assertTrue(p.copy(end = null).activeExtras(group).isEmpty())
    }

}
