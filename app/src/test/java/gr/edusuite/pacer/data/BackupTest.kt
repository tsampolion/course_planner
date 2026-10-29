package gr.edusuite.pacer.data

import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.model.Features
import gr.edusuite.pacer.model.Module
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.seedYear
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private fun year(id: String, name: String) = SchoolYear(id, name, null, null, emptyList(), emptyList())

    @Test fun fileNameCarriesTheDate() {
        assertEquals("course-pacer-2026-10-15.json", backupFileName(LocalDate.of(2026, 10, 15)))
    }

    @Test fun distinctYearsAreAddedUnchangedAfterTheCurrentOnes() {
        val current = listOf(year("a", "2025–26"))
        val incoming = listOf(year("b", "2026–27"), year("c", "2027–28"))
        assertEquals(current + incoming, addYears(current, incoming))
    }

    @Test fun aClashingIdGetsANewOneAndTheYearKeepsEverythingElse() {
        val seed = seedYear()
        val result = addYears(listOf(seed), listOf(seed))
        assertEquals(2, result.size)
        assertEquals(seed, result[0])
        val copy = result[1]
        assertNotEquals(seed.id, copy.id)
        assertEquals("${seed.name} (imported)", copy.name)
        assertEquals(seed.copy(id = copy.id, name = copy.name), copy)
    }

    @Test fun importingAClashingYearKeepsEachGroupsAssignments() {
        val y = seedYear()
        val c = y.classes.first()
        val work = Assignment("work", AssignmentType.WORKSHEET, "Revision",
            due = LocalDate.of(2026, 10, 23), note = "Bring notes")
        val incoming = y.copy(classes = listOf(c.copy(groups = listOf(
            c.groups.first().copy(assignments = listOf(work))
        ))))
        val out = addYears(listOf(y), listOf(incoming))
        assertEquals(y, out.first())
        assertNotEquals(y.id, out.last().id)
        assertEquals(listOf(work), out.last().classes.single().groups.single().assignments)
    }

    @Test fun aClashingNameAloneKeepsTheId() {
        val result = addYears(listOf(year("a", "2026–27")), listOf(year("b", "2026–27")))
        assertEquals(year("b", "2026–27 (imported)"), result[1])
    }

    @Test fun importingTheSameFileAgainNumbersTheCopies() {
        val file = listOf(year("a", "2026–27"))
        val once = addYears(file, file)
        val twice = addYears(once, file)
        assertEquals(listOf("2026–27", "2026–27 (imported)", "2026–27 (imported 2)"), twice.map { it.name })
        assertEquals(3, twice.map { it.id }.toSet().size)
    }

    @Test fun clashesInsideTheFileAreResolvedToo() {
        val result = addYears(emptyList(), listOf(year("a", "Y"), year("a", "Y")))
        assertEquals(listOf("Y", "Y (imported)"), result.map { it.name })
        assertTrue(result[0].id != result[1].id)
    }

    private fun text(n: Int) = ByteArrayInputStream("a".repeat(n).encodeToByteArray())

    @Test fun aFileAtTheLimitIsReadAndOneByteOverIsRefused() {
        assertEquals(BoundedText.Text("a".repeat(100)), readBoundedText(text(100), limit = 100))
        assertEquals(BoundedText.TooLarge(101, 100), readBoundedText(text(101), limit = 100))
    }

    @Test fun aDeclaredSizeOverTheLimitIsRefusedWithoutReading() {
        val untouched = object : InputStream() { override fun read(): Int = error("must not read") }
        assertEquals(BoundedText.TooLarge(500, 100), readBoundedText(untouched, declaredSize = 500, limit = 100))
    }

    @Test fun aWrongDeclaredSizeDoesNotLetALargeFileThrough() {
        assertEquals(BoundedText.TooLarge(101, 100), readBoundedText(text(101), declaredSize = 10, limit = 100))
    }

    @Test fun theByteOrderMarkIsDroppedAndGreekTextSurvives() {
        val bytes = "\uFEFF{\"name\":\"Επανάληψη\"}".encodeToByteArray()
        assertEquals(BoundedText.Text("{\"name\":\"Επανάληψη\"}"), readBoundedText(ByteArrayInputStream(bytes)))
    }

    @Test fun theLimitCoversTheOldOneAndTheSizeShownRoundsUp() {
        assertTrue(MAX_IMPORT_BYTES > 5L * 1024 * 1024)
        assertEquals(1L, megabytes(1))
        assertEquals(5L, megabytes(5L * 1024 * 1024))
        assertEquals(6L, megabytes(5L * 1024 * 1024 + 1))
        assertEquals(50L, megabytes(MAX_IMPORT_BYTES))
    }

    @Test fun anOversizedFileIsNotTheGenericUnreadableResult() {
        val big = ImportResult.TooLarge(60L * 1024 * 1024, MAX_IMPORT_BYTES)
        assertNotEquals(ImportResult.Unreadable, big)
        assertEquals("60 MB; the limit is 50 MB", "${megabytes(big.bytes)} MB; the limit is ${megabytes(big.limit)} MB")
    }

    private fun yearWithTests(): SchoolYear = seedYear().let { y ->
        val c = y.classes[0]
        val g = c.groups[0].copy(assignments = listOf(Assignment("t", AssignmentType.TEST, "Unit 1 test")))
        y.copy(classes = listOf(c.copy(groups = listOf(g) + c.groups.drop(1), lead = c.books[0].id)))
    }

    @Test fun noQuestionWhenEverythingIsOn() {
        assertEquals(emptySet<Module>(), modulesNeeded(listOf(yearWithTests()), Features.ALL))
    }

    @Test fun testsNeedWorkWhenOff() {
        assertEquals(setOf(Module.WORK, Module.BOOK_TOOLS), modulesNeeded(listOf(yearWithTests()), Features(emptySet())))
    }

    @Test fun onlyMissingModulesAreNamed() {
        assertEquals(setOf(Module.WORK), modulesNeeded(listOf(yearWithTests()), Features(setOf(Module.BOOK_TOOLS))))
        assertEquals(emptySet<Module>(), modulesNeeded(listOf(seedYear()), Features(emptySet())))
    }
}
