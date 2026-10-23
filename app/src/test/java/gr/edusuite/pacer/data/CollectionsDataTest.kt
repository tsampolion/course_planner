package gr.edusuite.pacer.data

import gr.edusuite.pacer.model.*
import org.junit.Assert.*
import org.junit.Test

class CollectionsDataTest {
    private fun year(collections: List<ClassCollection> = emptyList()): SchoolYear = seedYear().let { y ->
        val c = y.classes.first()
        y.copy(classes = listOf(c.copy(id = "a"), c.copy(id = "b"), c.copy(id = "c")), collections = collections)
    }

    @Test fun updatingCollectionMovesClassesWithoutDeletingThem() {
        val y = year(listOf(ClassCollection("one", "First", setOf("a", "b")), ClassCollection("two", "Second", setOf("c"))))
        val moved = y.withCollection(ClassCollection("two", "Second renamed", setOf("b", "c")))
        assertEquals(setOf("a"), moved.collections[0].classIds)
        assertEquals(setOf("b", "c"), moved.collections[1].classIds)
        assertEquals(y.classes, moved.classes)
        assertEquals(y.classes, moved.withoutCollection("one").classes)
    }

    @Test fun sanitisingDropsUnknownIdsAndRepeatedMembership() {
        val y = year(listOf(ClassCollection("same", "First", setOf("a", "gone")), ClassCollection("same", "Second", setOf("a", "b"))))
        val clean = sanitiseYears(listOf(y)).single()
        assertEquals(setOf("a"), clean.collections[0].classIds)
        assertEquals(setOf("b"), clean.collections[1].classIds)
        assertEquals(2, clean.collections.map { it.id }.distinct().size)
    }

    @Test fun backupRemapsClashingCollectionIds() {
        val y = year(listOf(ClassCollection("co", "First", setOf("a"))))
        val result = addYears(listOf(y), listOf(y)).last()
        assertNotEquals("co", result.collections.single().id)
        assertEquals(setOf("a"), result.collections.single().classIds)
        assertEquals(y.classes, result.classes)
    }

    @Test fun groupOverridesDefaultToAbsentAndUnknownBookReferencesDrop() {
        val y = seedYear()
        val c = y.classes.first()
        val g = c.groups.first()
        assertNull(g.end)
        assertTrue(g.skip.isEmpty())
        assertTrue(g.extraBooks.isEmpty())
        val book = c.books.first().id
        val changed = g.copy(skip = mapOf(book to "1-3", "gone" to "2"), extraBooks = setOf(book, "gone"))
        val dirty = y.copy(classes = listOf(c.copy(groups = listOf(changed))))
        val clean = sanitiseYears(listOf(dirty)).single().classes.single().groups.single()
        assertEquals(mapOf(book to "1-3"), clean.skip)
        assertFalse("gone" in clean.extraBooks)
    }
}
