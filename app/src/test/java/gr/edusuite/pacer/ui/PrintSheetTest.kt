package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.seedPlan
import org.junit.Assert.assertEquals
import org.junit.Test

class PrintSheetTest {
    @Test fun optionalCatalogueBooksDoNotShiftPrintedColumns() {
        val base = seedPlan()
        val optional = gr.edusuite.pacer.model.Book("optional", "Optional", 1, 20, groupOnly = true)
        val plan = base.copy(books = listOf(optional) + base.books)
        val html = PrintSheet.html(listOf(plan))
        org.junit.Assert.assertFalse(html.contains("<th>Optional</th>"))
        base.books.forEach { org.junit.Assert.assertTrue(html.contains("<th>${it.name}</th>")) }
        val selected = plan.copy(groups = plan.groups.map { it.copy(extraBooks = setOf(optional.id)) })
        org.junit.Assert.assertTrue(PrintSheet.html(listOf(selected)).contains("<th>Optional</th>"))
    }

    @Test fun everyPrintedGroupCarriesTheNote() {
        val plan = seedPlan()
        val html = PrintSheet.html(listOf(plan))
        assertEquals(plan.groups.size, html.split("<section>").size - 1)
        assertEquals(plan.groups.size, html.split("<div class=note>$PRINT_NOTE</div>").size - 1)
    }
}
