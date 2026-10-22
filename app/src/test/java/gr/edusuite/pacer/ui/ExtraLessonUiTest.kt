package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.*
import gr.edusuite.pacer.model.*
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ExtraLessonUiTest {
    private val date = LocalDate.of(2027, 1, 6)
    private val g = Group("g", "G", setOf(MONDAY), date, extras = listOf(ExtraLesson("e", date, mapOf("b" to listOf(1, 2)))))
    private val c = SchoolClass("c", "C", 60, 2.0, listOf(g), listOf(Book("b", "B", 1, 20)), emptyList(), emptyList())
    private val y = SchoolYear("y", "Y", date, date.plusDays(20), emptyList(), listOf(c))

    @Test fun extraMenuOffersEditAndDeleteWithoutCustomSlotActions() {
        val r = GroupReport(y.planFor(c), g)
        val l = lessonsOf(r).first()
        assertEquals("e", l.extraId)
        assertFalse(l.free)
        assertEquals(-1, l.slot)
        val items = lessonMenuItems(l, false, true, true)
        assertTrue(MenuItemKind.EDIT_EXTRA in items)
        assertTrue(MenuItemKind.DELETE_EXTRA in items)
        assertFalse(MenuItemKind.PLAN_LESSON in items)
        assertFalse(MenuItemKind.ADD_SUPPLEMENTARY in items)
        assertFalse(MenuItemKind.ADD_BEFORE in items)
        val done = lessonsOf(GroupReport(r.plan, r.markDone(setOf(date)))).first()
        assertEquals("e", done.extraId)
        assertFalse(MenuItemKind.EDIT_EXTRA in lessonMenuItems(done, true, true, true))
    }

    @Test fun blockedExtraWarnsAndKeepsPagesInPool() {
        val plan = y.planFor(c).copy(events = listOf(TestEvent("t", "Test", date, ALL_GROUPS, true)))
        val r = GroupReport(plan, g)
        assertTrue(groupWarnings(r).any { it is GroupWarning.ExtraUnavailable })
        assertEquals(20, r.pages)
    }

    @Test fun extraAppearsInHomePrintAndTsv() {
        assertEquals(date, nextLesson(y, date)!!.date)
        val r = GroupReport(y.planFor(c), g)
        assertTrue(r.asTsv().contains("1–2"))
        assertTrue(r.asTsv().contains("Extra"))
        val html = PrintSheet.html(listOf(r.plan))
        assertTrue(html.contains("1–2"))
        assertTrue(html.contains("Extra"))
    }
}
