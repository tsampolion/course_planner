package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class HomeworkLessonTest {
    private val date = LocalDate.of(2027, 2, 1)
    private val lesson = Lesson(1, date, emptyList(), false, emptyList(), false)
    private fun hw(id: String) = Assignment(id, AssignmentType.HOMEWORK, "Read", setOn = date)

    @Test fun everyLessonOffersHomeworkIncludingPreviewAndBlockedLessons() {
        for (l in listOf(lesson, lesson.copy(done = true), lesson.copy(blocked = true))) {
            assertTrue(MenuItemKind.ADD_HOMEWORK in lessonMenuItems(l, true, false, false))
        }
    }
    @Test fun setOrDueHomeworkOffersEditAndDeleteWithoutDuplicatingSameDate() {
        val h = hw("h").copy(due = date)
        val due = hw("due").copy(setOn = date.minusDays(2), due = date)
        val unrelated = hw("other").copy(setOn = date.plusDays(1))
        val test = hw("test").copy(type = AssignmentType.TEST)
        val l = lesson.copy(assignments = listOf(h, due, unrelated, test))
        assertEquals(listOf("h", "due"), homeworkForLesson(l).map { it.id })
        val menu = lessonMenuItems(l, true, false, true)
        assertTrue(MenuItemKind.EDIT_HOMEWORK in menu)
        assertTrue(MenuItemKind.DELETE_HOMEWORK in menu)
        assertFalse(MenuItemKind.EDIT_HOMEWORK in lessonMenuItems(lesson, true, false, true))
    }
    @Test fun rowPagesUseEffectiveBookIdsAndNames() {
        val a = hw("h").copy(pages = mapOf("wb" to listOf(2, 3), "hidden" to listOf(9)))
        assertEquals(listOf("Workbook 2–3"), homeworkPageCells(a, listOf("core", "wb"), listOf("Coursebook", "Workbook")))
    }
    @Test fun notesRemainVisibleWithRequiredTitleAndDueDate() {
        val a = hw("h").copy(title = "Homework", note = "Read chapter 3", due = date.plusDays(7))
        val parts = homeworkTextParts(a, emptyList(), emptyList())
        assertTrue("Homework" in parts)
        assertTrue("Read chapter 3" in parts)
    }
    @Test fun duplicateTitlesHaveDistinctActionsEvenWhenContentMatches() {
        val a = hw("a").copy(note = "Read", pages = mapOf("wb" to listOf(2, 3)), due = date.plusDays(7))
        val b = a.copy(id = "b", note = "Write")
        val names = homeworkActionNames(listOf(a, b), listOf("wb"), listOf("Workbook"))
        assertEquals(2, names.distinct().size)
        assertTrue(names[0].contains("Workbook 2–3"))
        assertTrue(names[0].contains("Read"))
        assertEquals(2, homeworkActionNames(listOf(a, a.copy(id = "same")), listOf("wb"), listOf("Workbook")).distinct().size)
    }
}
