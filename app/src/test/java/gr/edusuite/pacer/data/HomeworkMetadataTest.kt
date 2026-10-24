package gr.edusuite.pacer.data

import gr.edusuite.pacer.model.*
import org.junit.Assert.*
import org.junit.Test

class HomeworkMetadataTest {
    private fun clean(vararg work: Assignment): List<Assignment> {
        val y = seedYear()
        val c = y.classes.first()
        return sanitiseYears(listOf(y.copy(classes = listOf(c.copy(groups = listOf(c.groups.first().copy(assignments = work.toList())))))))
            .single().classes.single().groups.single().assignments
    }
    @Test fun preservesValidPairAndContent() {
        val h = Assignment("h", AssignmentType.HOMEWORK, "Practice", contentType = AssignmentType.WORKSHEET, linkedId = "w")
        val w = Assignment("w", AssignmentType.WORKSHEET, "Practice", linkedId = "h")
        assertEquals(listOf(h, w), clean(h, w))
    }
    @Test fun removesSelfDanglingAndSameKindLinksAndInvalidContent() {
        val a = Assignment("a", AssignmentType.HOMEWORK, "Read", contentType = AssignmentType.HOMEWORK, linkedId = "a")
        val b = Assignment("b", AssignmentType.TEST, "Quiz", contentType = AssignmentType.WORKSHEET, linkedId = "missing")
        val c = Assignment("c", AssignmentType.TEST, "Other", linkedId = "b")
        val out = clean(a, b, c)
        assertTrue(out.all { it.linkedId == null && it.contentType == null })
    }
    @Test fun conflictingImportedLinksKeepOnlyTheReciprocalPair() {
        val h1 = Assignment("h1", AssignmentType.HOMEWORK, "One", linkedId = "w")
        val h2 = Assignment("h2", AssignmentType.HOMEWORK, "Two", linkedId = "w")
        val w = Assignment("w", AssignmentType.WORKSHEET, "Class", linkedId = "h2")
        assertEquals(listOf(h1.copy(linkedId = null), h2, w), clean(h1, h2, w))
        assertTrue(clean(h1, w.copy(linkedId = null)).all { it.linkedId == null })
    }
}
