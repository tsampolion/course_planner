package gr.edusuite.pacer.model

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.catchUpPlan
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturesTest {
    private fun d(s: String) = LocalDate.parse(s)

    private val year = seedYear()
    private val cls = year.classes[0]

    private fun withClass(change: (SchoolClass) -> SchoolClass) = listOf(year.copy(classes = listOf(change(cls))))
    private fun withGroup(change: (Group) -> Group) = withClass { c -> c.copy(groups = listOf(change(c.groups[0])) + c.groups.drop(1)) }

    @Test fun emptyYearUsesNothing() {
        assertEquals(emptySet<Module>(), modulesInUse(listOf(year)))
        assertTrue(Brick.entries.none { it.inUse(listOf(year)) })
    }

    @Test fun assignmentMeansWork() {
        val homework = withGroup { it.copy(assignments = listOf(Assignment("h", AssignmentType.HOMEWORK, "HW"))) }
        assertTrue(Brick.HOMEWORK.inUse(homework))
        assertFalse(Brick.TESTS.inUse(homework))
        val worksheet = withGroup { it.copy(assignments = listOf(Assignment("w", AssignmentType.WORKSHEET, "WS"))) }
        assertTrue(Brick.TESTS.inUse(worksheet))
        assertEquals(setOf(Module.WORK), modulesInUse(worksheet))
    }

    @Test fun unitRevisionMeansWork() {
        val years = withGroup { it.copy(unitRevisions = listOf(UnitRevision(0, RevisionKind.PAGES))) }
        assertTrue(Brick.UNIT_REVISIONS.inUse(years))
        assertEquals(setOf(Module.WORK), modulesInUse(years))
    }

    @Test fun leadBookMeansBookTools() {
        val years = withClass { it.copy(lead = it.books[0].id) }
        assertTrue(Brick.LEAD_BOOK.inUse(years))
        assertEquals(setOf(Module.BOOK_TOOLS), modulesInUse(years))
    }

    @Test fun linkedBooksMeanUnits() {
        assertTrue(Brick.UNITS.inUse(withClass { c -> c.copy(bookLinks = listOf(setOf(c.books[0].id, c.books[1].id))) }))
    }

    @Test fun groupSkipMeansBookTools() {
        val years = withGroup { it.copy(skip = mapOf(cls.books[0].id to "5-6")) }
        assertTrue(Brick.GROUP_BOOK_RULES.inUse(years))
        assertFalse(Brick.GROUP_BOOK_RULES.inUse(withGroup { it.copy(skip = mapOf(cls.books[0].id to " ")) }))
    }

    @Test fun classSkipAndDueAreCore() {
        val years = withClass { c -> c.copy(books = c.books.map { it.copy(skip = "8-9", due = d("2027-03-01")) }) }
        assertEquals(emptySet<Module>(), modulesInUse(years))
    }

    @Test fun supplementaryBookMeansPlanning() {
        val years = withClass { c -> c.copy(books = c.books + Book("s", "Reader", 1, 10, supplementary = true)) }
        assertTrue(Brick.SUPPLEMENTARY.inUse(years))
        assertEquals(setOf(Module.PLANNING), modulesInUse(years))
    }

    @Test fun extraLessonMeansPlanning() {
        val years = withGroup { it.copy(extras = listOf(ExtraLesson("x", d("2026-10-03")))) }
        assertTrue(Brick.EXTRA_LESSONS.inUse(years))
        assertEquals(setOf(Module.PLANNING), modulesInUse(years))
    }

    /** Ten Monday and Wednesday lessons in March 2027 for 30 pages at a pace of 1: the group is short, so catch-up applies. */
    private fun catchUpCustom(): CustomPlan {
        val p = Plan(
            id = "p", name = "Test", start = d("2027-03-01"), end = d("2027-03-31"), mins = 60, pace = 1.0,
            groups = listOf(Group("g", "G", setOf(MONDAY, WEDNESDAY), d("2027-03-01"))),
            closures = emptyList(), books = listOf(Book("a", "SB", 1, 20), Book("b", "WB", 1, 10)),
            homework = emptyList(), events = emptyList(), today = TEST_TODAY,
        )
        val custom = GroupReport(p, p.groups[0]).catchUpPlan(2)
        assertNotNull(custom)
        return custom!!
    }

    @Test fun catchUpPlanIsNotPlanning() {
        val custom = catchUpCustom()
        assertFalse(custom.handPlanned())
        assertFalse(Brick.PLAN_EDITOR.inUse(withGroup { it.copy(custom = custom) }))
    }

    @Test fun oneTouchedLessonIsPlanning() {
        val custom = catchUpCustom().let { c -> c.copy(lessons = c.lessons.mapIndexed { i, l -> if (i == 0) l.copy(touched = true) else l }) }
        assertTrue(custom.handPlanned())
        assertTrue(Brick.PLAN_EDITOR.inUse(withGroup { it.copy(custom = custom) }))
        assertEquals(setOf(Module.PLANNING), modulesInUse(withGroup { it.copy(custom = custom) }))
    }

    @Test fun coreBricksAlwaysOn() {
        val none = Features(emptySet())
        assertTrue(none.has(Brick.PACING))
        assertTrue(none.has(Brick.PLAN_BASE))
        assertTrue(none.has(Brick.CATCH_UP))
        assertFalse(none.has(Brick.HOMEWORK))
        assertFalse(none.has(Brick.PRINT))
    }

    @Test fun needsAreHonoured() {
        assertFalse(Features(setOf(Module.WORK)).has(Brick.UNIT_REVISIONS))
        assertTrue(Features(setOf(Module.WORK, Module.BOOK_TOOLS)).has(Brick.UNIT_REVISIONS))
        assertFalse(Features(setOf(Module.BOOK_TOOLS)).has(Brick.UNIT_REVISIONS))
    }

    @Test fun everyNeedIsAcyclic() {
        for (b in Brick.entries) {
            val seen = mutableSetOf<Brick>()
            var frontier = b.needs
            while (frontier.isNotEmpty()) {
                assertFalse("$b needs itself", b in frontier)
                seen += frontier
                frontier = frontier.flatMap { it.needs }.toSet() - seen
            }
        }
    }

    @Test fun inUseRevealsBrick() {
        assertTrue(Features(emptySet(), inUse = setOf(Brick.HOMEWORK)).has(Brick.HOMEWORK))
        assertFalse(Features(emptySet(), inUse = setOf(Brick.HOMEWORK)).has(Brick.TESTS))
    }

    @Test fun allHasEveryBrick() {
        assertTrue(Brick.entries.all { Features.ALL.has(it) })
        assertEquals(Module.entries.toSet(), Features.ALL.on)
    }

    @Test fun parseIgnoresUnknown() {
        assertNull(Module.parse("NOPE"))
        assertNull(Module.parse(null))
        assertEquals(Module.WORK, Module.parse("WORK"))
    }
}
