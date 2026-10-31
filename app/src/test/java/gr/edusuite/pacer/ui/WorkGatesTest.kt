package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.model.Brick
import gr.edusuite.pacer.model.Features
import org.junit.Assert.*
import org.junit.Test

class WorkGatesTest {
    private val core = Features(emptySet())
    private val homework = Features(emptySet(), setOf(Brick.HOMEWORK))
    private val tests = Features(emptySet(), setOf(Brick.TESTS))

    @Test fun everythingKeepsTheExistingSheetAndChipOrders() {
        assertEquals(listOf(AssignmentType.HOMEWORK, AssignmentType.WORKSHEET, AssignmentType.TEST),
            workAssignmentTypes(Features.ALL))
        assertEquals(listOf(null, AssignmentType.HOMEWORK, AssignmentType.TEST, AssignmentType.WORKSHEET),
            workFilterTypes(Features.ALL, emptyList()))
    }

    @Test fun pacingOnlyOffersNoNewAssignmentTypes() {
        assertTrue(workAssignmentTypes(core).isEmpty())
        assertEquals(listOf<AssignmentType?>(null), workFilterTypes(core, emptyList()))
        assertNull(workDefaultType(workAssignmentTypes(core), AssignmentType.HOMEWORK))
    }

    @Test fun homeworkInUseOffersOnlyHomework() {
        assertEquals(listOf(AssignmentType.HOMEWORK), workAssignmentTypes(homework))
        assertEquals(listOf(null, AssignmentType.HOMEWORK), workFilterTypes(homework, emptyList()))
    }

    @Test fun testsInUseOfferTestsAndWorksheetsWithoutHomework() {
        assertEquals(listOf(AssignmentType.WORKSHEET, AssignmentType.TEST), workAssignmentTypes(tests))
        assertEquals(listOf(null, AssignmentType.TEST, AssignmentType.WORKSHEET), workFilterTypes(tests, emptyList()))
    }

    @Test fun existingTestsStayFilterableEvenWithoutTheirModule() {
        val saved = listOf(Assignment("test", AssignmentType.TEST, "Unit 3"))
        assertEquals(listOf(null, AssignmentType.HOMEWORK, AssignmentType.TEST), workFilterTypes(homework, saved))
        assertEquals(listOf("test"), workSections(saved, java.time.LocalDate.of(2026, 10, 30), AssignmentType.TEST)
            .flatMap { it.assignments }.map { it.id })
    }

    @Test fun existingWorksheetsStayFilterableEvenWithoutTheirModule() {
        val saved = listOf(Assignment("sheet", AssignmentType.WORKSHEET, "Practice"))
        assertEquals(listOf(null, AssignmentType.WORKSHEET), workFilterTypes(core, saved))
    }

    @Test fun existingHomeworkKeepsItsEditorTypeWithoutItsModule() {
        assertEquals(listOf(AssignmentType.HOMEWORK), workAssignmentTypes(core, AssignmentType.HOMEWORK))
        assertEquals(listOf(AssignmentType.HOMEWORK, AssignmentType.WORKSHEET, AssignmentType.TEST),
            workAssignmentTypes(tests, AssignmentType.HOMEWORK))
    }

    @Test fun existingTestsKeepTheirEditorTypeWithoutOfferingOtherDisabledTypes() {
        assertEquals(listOf(AssignmentType.HOMEWORK, AssignmentType.TEST),
            workAssignmentTypes(homework, AssignmentType.TEST))
        assertEquals(listOf(AssignmentType.TEST), workAssignmentTypes(core, AssignmentType.TEST))
    }

    @Test fun existingWorksheetsKeepTheirEditorTypeWithoutTheirModule() {
        assertEquals(listOf(AssignmentType.HOMEWORK, AssignmentType.WORKSHEET),
            workAssignmentTypes(homework, AssignmentType.WORKSHEET))
    }

    @Test fun newWorkFallsBackToAnAvailableTypeInsteadOfHiddenHomework() {
        assertEquals(AssignmentType.WORKSHEET, workDefaultType(workAssignmentTypes(tests), AssignmentType.HOMEWORK))
        assertEquals(AssignmentType.TEST, workDefaultType(workAssignmentTypes(tests), AssignmentType.TEST))
        assertEquals(AssignmentType.HOMEWORK, workDefaultType(workAssignmentTypes(homework), AssignmentType.TEST))
    }

    @Test fun anUnavailableFilterFallsBackToAllInsteadOfAnotherType() {
        val filters = workFilterTypes(homework, emptyList())
        assertNull(workSelectedFilter(filters, AssignmentType.TEST))
        assertEquals(AssignmentType.HOMEWORK, workSelectedFilter(filters, AssignmentType.HOMEWORK))
    }
}
