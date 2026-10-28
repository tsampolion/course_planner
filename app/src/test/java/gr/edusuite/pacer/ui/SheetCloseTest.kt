package gr.edusuite.pacer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class SheetCloseTest {
    private val waysOut = listOf(SheetClose.CLOSE_BUTTON, SheetClose.BACK, SheetClose.SCRIM, SheetClose.SWIPE)

    @Test fun anUntouchedSheetClosesAtOnceEveryWay() {
        (waysOut + SheetClose.ACTION).forEach { assertEquals(it.name, SheetCloseDecision.CLOSE, sheetCloseDecision(dirty = false, via = it)) }
    }

    @Test fun aChangedDraftAsksBeforeEveryWayOut() {
        waysOut.forEach { assertEquals(it.name, SheetCloseDecision.ASK, sheetCloseDecision(dirty = true, via = it)) }
    }

    @Test fun theFilledActionNeverAsks() {
        assertEquals(SheetCloseDecision.CLOSE, sheetCloseDecision(dirty = true, via = SheetClose.ACTION))
    }

    @Test fun aFieldEditedAndEditedBackIsNotAChange() {
        val opened = draftSignature(listOf("Unit 3", 12, null, LocalDate.of(2026, 10, 28)))
        assertNotEquals(opened, draftSignature(listOf("Unit 34", 12, null, LocalDate.of(2026, 10, 28))))
        assertEquals(opened, draftSignature(listOf("Unit 3", 12, null, LocalDate.of(2026, 10, 28))))
    }

    @Test fun fieldsDoNotRunIntoEachOther() {
        assertNotEquals(draftSignature(listOf("ab", "c")), draftSignature(listOf("a", "bc")))
        assertNotEquals(draftSignature(listOf(null)), draftSignature(listOf("null")))
    }
}
