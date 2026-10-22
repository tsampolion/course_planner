package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.*
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ExtraLessonEditTest {
    private val start = LocalDate.of(2027, 3, 1)
    private val g = Group("g", "G", setOf(MONDAY), start)
    private val p = Plan("p", "P", start, start.plusDays(27), 60, 2.0, listOf(g), emptyList(), listOf(Book("b", "B", 1, 20)), emptyList(), emptyList())

    @Test fun proposedExtraPagesEqualsFloatingShare() {
        val report = GroupReport(p, g)
        assertEquals(mapOf("b" to listOf(5, 6, 7, 8)), report.proposedExtraPages(start.plusDays(1)))
        assertEquals(mapOf("b" to listOf(1, 2, 3, 4, 5)), report.proposedExtraPages(start))
        assertTrue(report.proposedExtraPages(start.minusDays(1)).isEmpty())
        val closure = p.copy(closures = listOf(Closure("c", "Break", start, start, ALL_GROUPS)))
        assertEquals(mapOf("b" to listOf(1, 2, 3, 4, 5)), GroupReport(closure, g).proposedExtraPages(start))
    }

    @Test fun saveExtraRepacesRestInOneSnapshotAndKeepsTouchedPages() {
        val c = GroupReport(p, g).toCustomPlan().let { custom -> custom.copy(lessons = custom.lessons.mapIndexed { i, l -> if (i == 2) l.copy(touched = true) else l }) }
        val before = g.copy(custom = c)
        val added = p.withExtra(before, ExtraLesson("e", start.plusDays(1), mapOf("b" to listOf(1, 2))))
        val after = p.withExtra(added, added.extras.single().copy(pages = mapOf("b" to listOf(1, 2, 3, 4))))
        assertEquals(before.custom!!.lessons[2], after.custom!!.lessons[2])
        assertTrue(after.custom!!.lessons.first().pages["b"].orEmpty().none { it in 1..4 })
        assertEquals(1, after.extras.size)
        assertTrue(before.extras.isEmpty())
        assertEquals(c, before.custom)
    }

    @Test fun removeExtraRestoresPlan() {
        for (date in listOf(start, start.plusDays(1))) for (custom in listOf(false, true)) {
            val before = if (custom) g.copy(custom = GroupReport(p, g).toCustomPlan()) else g
            val extra = ExtraLesson("e", date, mapOf("b" to listOf(1, 2)))
            val added = p.withExtra(before, extra)
            val restored = p.withoutExtra(added, "e")
            assertEquals(GroupReport(p, before).rows.map { it.date to it.pages }, GroupReport(p, restored).rows.map { it.date to it.pages })
            assertTrue(restored.extras.isEmpty())
        }
    }

    @Test fun editingDoneExtraDoesNotRewriteTaughtPagesAndDeletingKeepsHistory() {
        val added = p.withExtra(g, ExtraLesson("e", start.plusDays(1), mapOf("b" to listOf(1, 2))))
        val done = GroupReport(p, added).markDone(setOf(start.plusDays(1)))
        val edited = p.withExtra(done, done.extras.single().copy(pages = mapOf("b" to listOf(9))))
        assertEquals(done.done, edited.done)
        assertEquals(done.done, p.withoutExtra(edited, "e").done)
    }
    @Test fun extraEditsPreserveSavedCatchUpAndRecalculateItsDuration() {
        val plan = p.copy(books = listOf(Book("b", "B", 1, 12)))
        val custom = GroupReport(plan, g).catchUpPlan(2)!!
        assertEquals(listOf(4, 4, 2, 2), custom.lessons.map { it.pages.values.sumOf { ps -> ps.size } })
        val before = g.copy(custom = custom)
        val added = plan.withExtra(before, ExtraLesson("e", start.plusDays(1), mapOf("b" to listOf(1, 2))))
        assertEquals(2, added.custom!!.catchUpBoost)
        assertEquals(listOf(4, 2, 2, 2), added.custom!!.lessons.map { it.pages.values.sumOf { ps -> ps.size } })
        assertEquals(2, plan.withExtra(added, added.extras.single().copy(pages = mapOf("b" to listOf(1)))).custom!!.catchUpBoost)
        val removed = plan.withoutExtra(added, "e")
        assertEquals(custom, removed.custom)
    }

}
