package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.seedPlan
import org.junit.Assert.assertEquals
import org.junit.Test

class PrintSheetTest {
    @Test fun everyPrintedGroupCarriesTheNote() {
        val plan = seedPlan()
        val html = PrintSheet.html(listOf(plan))
        assertEquals(plan.groups.size, html.split("<section>").size - 1)
        assertEquals(plan.groups.size, html.split("<div class=note>$PRINT_NOTE</div>").size - 1)
    }
}
