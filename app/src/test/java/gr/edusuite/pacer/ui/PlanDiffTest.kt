package gr.edusuite.pacer.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanDiffTest {
    private val day = LocalDate.of(2026, 10, 26)
    private fun lesson(vararg cells: String, id: String? = "l1", done: Boolean = false, blocked: Boolean = false, extra: String? = null) =
        Lesson(1, day, emptyList(), blocked, cells.toList(), done, lessonId = id, extraId = extra)

    @Test fun identicalPagesNoDifference() {
        assertEquals(emptySet<Int>(), differingBooks(lesson("1-2", "5"), lesson("1-2", "5")))
    }

    @Test fun onlyOneBookDiffers() {
        assertEquals(setOf(1), differingBooks(lesson("1-2", "6"), lesson("1-2", "5")))
    }

    @Test fun noProposedLessonNoColour() {
        assertEquals(emptySet<Int>(), differingBooks(lesson("1-2", "6"), null))
    }

    @Test fun supplementaryPagesIgnored() {
        assertEquals(setOf(0), differingBooks(lesson("3", "9"), lesson("1", "-"), supplementary = listOf(false, true)))
    }

    @Test fun onlyCustomPlanLessonsCount() {
        val proposed = lesson("1")
        assertEquals(emptySet<Int>(), differingBooks(lesson("2", id = null), proposed))
        assertEquals(emptySet<Int>(), differingBooks(lesson("2", done = true), proposed))
        assertEquals(emptySet<Int>(), differingBooks(lesson("2", blocked = true), proposed))
        assertEquals(emptySet<Int>(), differingBooks(lesson("2", extra = "e"), proposed))
    }
}
