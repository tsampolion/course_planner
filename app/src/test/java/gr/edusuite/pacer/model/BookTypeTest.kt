package gr.edusuite.pacer.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTypeTest {
    private val book = Book("b", "Book", 1, 10)

    @Test fun guessBookTypeMatchesNames() {
        val table = mapOf(
            "Student's Book" to BookType.STUDENT, "STUDENT BOOK" to BookType.STUDENT,
            "Workbook" to BookType.WORKBOOK, "Work book" to BookType.WORKBOOK, "WorkBook 2" to BookType.WORKBOOK,
            "Grammar in Use" to BookType.GRAMMAR, "Writing Book" to BookType.WRITING,
            "Skills Book" to BookType.SKILLS, "Graded Reader" to BookType.READER,
            "Mystery" to BookType.UNCLASSIFIED, "" to BookType.UNCLASSIFIED,
        )
        for ((name, type) in table) assertEquals(name, type, guessBookType(name))
    }

    @Test fun abbreviationsAreTheOwnersList() {
        assertEquals(
            listOf("SB", "WB", "GB", "WrB", "SkB", "R", "Supp", ""),
            BookType.entries.map { it.abbr },
        )
    }

    @Test fun schema14LoadsGuessedTypes() {
        // A file from before schema 15 has no stored type.
        assertEquals(BookType.STUDENT, readBookType(null, "Student's Book", false))
        assertEquals(BookType.WORKBOOK, readBookType("", "Workbook", false))
        assertEquals(BookType.UNCLASSIFIED, readBookType(null, "Mystery", false))
        assertEquals(BookType.SUPPLEMENTARY, readBookType(null, "Mystery", true))
        assertEquals(BookType.SUPPLEMENTARY, readBookType(null, "Workbook", true))
    }

    @Test fun aStoredTypeIsKeptAndNeverGuessedAgain() {
        // The teacher chose Unclassified for a book named Workbook: reading it back must not guess.
        assertEquals(BookType.UNCLASSIFIED, readBookType("UNCLASSIFIED", "Workbook", false))
        assertEquals(BookType.READER, readBookType("READER", "Workbook", false))
        assertEquals(BookType.UNCLASSIFIED, readBookType("FUTURE_TYPE", "Workbook", false))
    }

    @Test fun supplementaryFlagAndTypeAgree() {
        assertEquals(BookType.SUPPLEMENTARY, readBookType("READER", "Reader", true))
        assertEquals(book.copy(type = BookType.SUPPLEMENTARY, supplementary = true), book.copy(supplementary = true).consistent())
        assertEquals(book.copy(type = BookType.SUPPLEMENTARY, supplementary = true), book.copy(type = BookType.SUPPLEMENTARY).consistent())
        assertSame(book, book.consistent())
    }

    @Test fun withTypeSyncsSupplementaryFlag() {
        val supp = book.copy(include = "1-3", countInLesson = true).withType(BookType.SUPPLEMENTARY)
        assertTrue(supp.supplementary)
        assertEquals("1-3", supp.include)
        assertTrue(supp.countInLesson)
        val back = supp.withType(BookType.WORKBOOK)
        assertFalse(back.supplementary)
        assertEquals(BookType.WORKBOOK, back.type)
        assertEquals("1-3", back.include)
    }
}

class GroupBooksTest {
    private fun d(v: String) = LocalDate.parse(v)
    private val sb = Book("sb", "Student's Book", 1, 100)
    private val wb = Book("wb", "Workbook", 1, 80)
    private val reader = Book("r", "Reader", 1, 30, supplementary = true, include = "1-30")
    private val optional = Book("o", "Extra", 1, 10, groupOnly = true)
    private val books = listOf(sb, wb, reader, optional)
    private val g = Group("g", "Group", emptySet(), null)

    @Test fun aDefaultBookCanBeRemovedAndAddedBack() {
        val removed = g.withBookRemoved(books, "wb", true)!!
        assertEquals(setOf("wb"), removed.removedBooks)
        assertFalse(removed.usesBook(wb))
        assertTrue(removed.usesBook(sb))
        assertEquals(g, removed.withBookRemoved(books, "wb", false))
    }

    @Test fun removingNothingNewReturnsTheSameGroup() {
        assertSame(g, g.withBookRemoved(books, "wb", false))
        val removed = g.withBookRemoved(books, "wb", true)!!
        assertSame(removed, removed.withBookRemoved(books, "wb", true))
    }

    @Test fun lastCoreBookCannotBeRemoved() {
        val one = g.withBookRemoved(books, "wb", true)!!
        assertNull(one.withBookRemoved(books, "sb", true))
        // The supplementary reader doesn't count as a paced book.
        assertNull(one.withBookRemoved(listOf(sb, reader), "sb", true))
    }

    @Test fun optionalAndUnknownBooksAreNotRemoved() {
        assertNull(g.withBookRemoved(books, "o", true))
        assertNull(g.withBookRemoved(books, "nope", true))
    }

    @Test fun groupBookDueIsSetClearedAndLimitedToPacedBooks() {
        val set = g.withBookDue(books, "sb", d("2027-02-01"))
        assertEquals(mapOf("sb" to d("2027-02-01")), set.bookDue)
        assertEquals(emptyMap<String, LocalDate>(), set.withBookDue(books, "sb", null).bookDue)
        assertSame(g, g.withBookDue(books, "r", d("2027-02-01")))
        assertSame(g, g.withBookDue(books, "nope", d("2027-02-01")))
    }

    private val year = SchoolYear(
        "y", "Year", d("2026-09-01"), d("2027-06-01"),
        listOf(
            Closure("own", "Trip", d("2026-10-01"), d("2026-10-01"), "g"),
            Closure("other", "Trip", d("2026-10-02"), d("2026-10-02"), "h"),
            Closure("all", "Holiday", d("2026-10-03"), d("2026-10-03"), ALL_GROUPS),
        ),
        emptyList(),
    )

    @Test fun editClosureChangesOnlyThatClosure() {
        val out = year.withClosureEdited("own", "g", "  ", d("2026-11-02"), d("2026-11-04"))
        assertEquals(Closure("own", "Day off", d("2026-11-02"), d("2026-11-04"), "g"), out.closures[0])
        assertEquals(year.closures.drop(1), out.closures.drop(1))
        assertEquals(Closure("own", "Trip", d("2026-11-02"), d("2026-11-02"), "g"), year.withClosureEdited("own", "g", "Trip", d("2026-11-02"), null).closures[0])
    }

    @Test fun editClosureRefusesOtherGroupsClosureAndReversedDates() {
        assertSame(year, year.withClosureEdited("other", "g", "x", d("2026-11-02"), null))
        assertSame(year, year.withClosureEdited("all", "g", "x", d("2026-11-02"), null))
        assertSame(year, year.withClosureEdited("own", "g", "x", d("2026-11-05"), d("2026-11-04")))
        assertSame(year, year.withClosureEdited("missing", "g", "x", d("2026-11-02"), null))
    }
}
