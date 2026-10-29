package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.model.Brick
import gr.edusuite.pacer.model.Features
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonMenuModelTest {
    private val lesson = Lesson(1, LocalDate.of(2027, 3, 1), emptyList(), false, emptyList(), false,
        lessonId = "lesson", index = 0, slot = 0)

    @Test fun futureLessonWithCustomPlanOffersPlannedPagesOnly() {
        val items = lessonMenuItems(lesson, isPast = false, hasCustomPlan = true, canTick = true)
        assertTrue(MenuItemKind.PLAN_LESSON in items)
        assertFalse(MenuItemKind.RECORD_TAUGHT in items)
    }

    @Test fun pastOrTodayLessonOffersRecordTaughtOnly() {
        for (done in listOf(false, true)) {
            val items = lessonMenuItems(lesson.copy(done = done), isPast = true, hasCustomPlan = true, canTick = true)
            assertTrue(MenuItemKind.RECORD_TAUGHT in items)
            assertFalse(MenuItemKind.PLAN_LESSON in items)
        }
    }

    @Test fun futureLessonWithoutCustomPlanOffersNeitherPagesAction() {
        val items = lessonMenuItems(lesson.copy(lessonId = null), isPast = false, hasCustomPlan = false, canTick = true)
        assertFalse(MenuItemKind.PLAN_LESSON in items)
        assertFalse(MenuItemKind.RECORD_TAUGHT in items)
    }

    @Test fun previewCannotRecordTaughtOrMarkDone() {
        val items = lessonMenuItems(lesson, isPast = true, hasCustomPlan = true, canTick = false)
        assertFalse(MenuItemKind.RECORD_TAUGHT in items)
        assertFalse(MenuItemKind.DONE_UP_TO in items)
        assertFalse(MenuItemKind.NOT_DONE_FROM in items)
    }

    @Test fun freeFutureSlotCanBePlannedWithoutMoveOrRemove() {
        val items = lessonMenuItems(lesson.copy(lessonId = null, free = true), false, true, true)
        assertTrue(MenuItemKind.PLAN_LESSON in items)
        assertFalse(MenuItemKind.REMOVE in items)
        assertFalse(MenuItemKind.MOVE_EARLIER in items)
    }

    @Test fun pastLessonOffersNotTaughtOnceAndNoNotDoneWhenItTicksItself() {
        val past = lessonMenuItems(lesson.copy(done = true), isPast = true, hasCustomPlan = false, canTick = true, ticksItself = true)
        assertTrue(MenuItemKind.NOT_TAUGHT in past)
        assertFalse(MenuItemKind.NOT_DONE_FROM in past)
        val again = lessonMenuItems(lesson.copy(done = true, notTaught = true), isPast = true, hasCustomPlan = false, canTick = true, ticksItself = true)
        assertFalse(MenuItemKind.NOT_TAUGHT in again)
        val future = lessonMenuItems(lesson, isPast = false, hasCustomPlan = false, canTick = true)
        assertFalse(MenuItemKind.NOT_TAUGHT in future)
        assertTrue(MenuItemKind.NOT_DONE_FROM in future)
    }

    private val justPacing = Features(emptySet())

    @Test fun justPacingFutureLessonOffersTickingOnly() {
        val items = lessonMenuItems(lesson, isPast = false, hasCustomPlan = false, canTick = true, features = justPacing)
        assertEquals(listOf(MenuItemKind.DONE_UP_TO, MenuItemKind.NOT_DONE_FROM), items)
    }

    @Test fun planToolsNeedThePlanEditor() {
        val items = lessonMenuItems(lesson, isPast = false, hasCustomPlan = true, canTick = true, features = justPacing)
        assertFalse(MenuItemKind.PLAN_LESSON in items)
        assertFalse(MenuItemKind.MOVE_LATER in items)
        assertFalse(MenuItemKind.ADD_SUPPLEMENTARY in items)
    }

    @Test fun existingHomeworkStaysEditable() {
        val hw = Assignment("h", AssignmentType.HOMEWORK, "HW", setOn = lesson.date)
        val withHw = lesson.copy(assignments = listOf(hw))
        val items = lessonMenuItems(withHw, false, false, true, features = Features(emptySet(), inUse = setOf(Brick.HOMEWORK)))
        assertTrue(MenuItemKind.EDIT_HOMEWORK in items)
        assertTrue(MenuItemKind.ADD_HOMEWORK in items)
    }

    @Test fun everyMenuItemNamesABrick() {
        assertTrue(MenuItemKind.entries.all { it.brick() in Brick.entries })
        assertTrue(GroupMenuItem.entries.all { it.brick() in Brick.entries })
    }
}
