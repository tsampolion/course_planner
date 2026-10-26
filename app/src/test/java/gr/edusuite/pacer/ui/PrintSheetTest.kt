package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.*
import java.time.LocalDate
import java.time.DayOfWeek
import org.junit.Assert.assertTrue
import gr.edusuite.pacer.model.seedPlan
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.editLesson
import gr.edusuite.pacer.engine.toCustomPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    @Test fun pageRevisionPrintsItsTitleAndBookCellsWhileReservingTheLesson() {
        val day = LocalDate.of(2026, 10, 26)
        val book = Book("reader", "Reader", 1, 8, supplementary = true, include = "1-8")
        val a = Assignment("revision", AssignmentType.WORKSHEET, "Read & revise", due = day,
            takesLesson = true, pages = mapOf(book.id to listOf(2, 3)))
        val base = seedPlan()
        val g = base.groups.first().copy(from = day, days = DayOfWeek.entries.toSet(), assignments = listOf(a),
            unitRevisions = listOf(UnitRevision(0, RevisionKind.PAGES, assignmentId = a.id, pages = a.pages)))
        val p = base.copy(start = day, end = day.plusDays(2), books = listOf(book), groups = listOf(g),
            events = emptyList(), closures = emptyList())
        val html = PrintSheet.html(listOf(p))
        assertTrue(html.contains("<th>Reader</th>"))
        assertTrue(html.contains("<b>[Read &amp; revise]</b>"))
        assertTrue(html.contains("<td class=pg>2–3</td>"))
    }


    private fun withBooks(n: Int): Plan {
        val base = seedPlan()
        val books = (1..n).map { Book("b$it", "Βιβλίο μαθητή $it", 1, 60) }
        return base.copy(books = books, groups = base.groups.map { it.copy(extraBooks = emptySet()) })
    }

    @Test fun oneAndFourBooksStayPortraitAndEightTurnLandscape() {
        listOf(1, 4).forEach {
            val html = PrintSheet.html(listOf(withBooks(it)))
            assertFalse("$it books", html.contains("size:landscape"))
        }
        val eight = PrintSheet.html(listOf(withBooks(8)))
        assertTrue(eight.contains("size:landscape"))
        assertTrue(PrintSheet.landscape(listOf(withBooks(5))))
        assertFalse(PrintSheet.landscape(listOf(withBooks(4))))
    }

    @Test fun theTableReservesColumnsAndTextWraps() {
        val html = PrintSheet.html(listOf(withBooks(8)))
        assertTrue(html.contains("table-layout:fixed"))
        assertFalse(html.contains("white-space:nowrap"))
        assertTrue(html.contains("overflow-wrap:anywhere"))
        val first = html.substringAfter("<colgroup>").substringBefore("</colgroup>")
        // number, date and one column per book; no note column without notes
        assertEquals(2 + 8, first.split("<col").size - 1)
        assertTrue(first.startsWith("<col class=n><col class=date>"))
        assertEquals(8, html.substringBefore("</thead>").split("<section>")[1].split("<th>Βιβλίο").size - 1)
    }

    @Test fun longGreekNotesGetTheirOwnWrappingColumn() {
        val plan = withBooks(4)
        val long = "Επανάληψη των ασκήσεων γραμματικής και προετοιμασία για το διαγώνισμα της επόμενης εβδομάδας ".repeat(3)
        val first = plan.groups.first()
        val custom = GroupReport(plan, first).toCustomPlan()
        val edited = custom.editLesson(custom.lessons.first().id, custom.lessons.first().pages, long)
        val withNote = plan.copy(groups = listOf(first.copy(custom = edited)))
        val html = PrintSheet.html(listOf(withNote))
        assertTrue(html.contains("<col class=note>"))
        assertTrue(html.contains("<th>Note</th>"))
        assertTrue(html.contains(long.trim()))
    }
}
