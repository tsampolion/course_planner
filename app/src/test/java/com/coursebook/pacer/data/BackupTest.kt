package com.coursebook.pacer.data

import com.coursebook.pacer.model.SchoolYear
import com.coursebook.pacer.model.seedYear
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
}
