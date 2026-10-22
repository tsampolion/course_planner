package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.seedPlan
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Supplementary books (docs/SUPPLEMENTARY_BOOKS_BRIEF.md): placed by hand, never paced. */
class SupplementaryBookTest {
    private fun d(s: String) = LocalDate.parse(s)

    private val sb = Book("a", "SB", 1, 20)
    private val wb = Book("b", "WB", 1, 10)
    private val comp = Book("c", "Companion", 12, 30, supplementary = true, include = "12-15, 30")
    private val reader = Book("r", "Reader", 1, 8, supplementary = true, include = "1-8")

    /** Mondays and Wednesdays in March 2027: 10 lessons. */
    private fun plan(books: List<Book> = listOf(sb, wb, comp), end: String = "2027-03-31", groups: List<Group>? = null, pace: Double = 3.0) = Plan(
        id = "p", name = "Test", start = d("2027-03-01"), end = d(end), mins = 60, pace = pace,
        groups = groups ?: listOf(Group("g", "G", setOf(MONDAY, WEDNESDAY), d("2027-03-01"))),
        closures = emptyList(), books = books, homework = emptyList(), events = emptyList(),
    )

    private var n = 0
    private fun ids(): () -> String = { "l${n++}" }

    /** What pacing sees: each core book's pages per row, the figures and the verdict. */
    private fun coreView(r: GroupReport): List<Any?> {
        val core = r.plan.books.withIndex().filter { !it.value.supplementary }.map { it.index }
        return listOf(
            r.rows.map { row -> row.date to core.map { row.pages[it] } },
            core.map { r.bookStats[it] }, r.pages, r.banked, r.teaching, r.rate, r.peak, r.verdict(),
            r.coverage(), r.bookLags(), r.catchUp(1), r.plan.problems(),
        )
    }

    @Test fun noSupplementaryChangesNothing() {
        val seed = seedPlan()
        val fixtures = listOf(seed, plan(listOf(sb, wb)), plan(listOf(sb, wb), pace = 1.0))
        for (p in fixtures) for (at in 0..p.books.size) {
            val with = p.copy(books = p.books.toMutableList().apply { add(at, comp) })
            for (g in p.groups) {
                assertEquals(coreView(GroupReport(p, g)), coreView(GroupReport(with, g)))
                // A custom plan made with it, too.
                val mine = g.copy(custom = GroupReport(p, g).toCustomPlan(ids()))
                val mine2 = g.copy(custom = GroupReport(with, g).toCustomPlan(ids()))
                assertEquals(coreView(GroupReport(p, mine)).drop(1), coreView(GroupReport(with, mine2)).drop(1))
            }
        }
    }

    @Test fun theProposedPlanNeverHasItsPages() {
        val p = plan()
        val r = GroupReport(p, p.groups[0])
        assertTrue(r.rows.all { it.pages[2].isEmpty() })
        assertEquals(30, r.pages)
        assertEquals(emptyList<Int>(), p.remainingPages(p.groups[0], comp))
        assertEquals(listOf(12, 13, 14, 15, 30), comp.includedPages())
        assertEquals(listOf(12, 13, 14, 15, 30), r.unplacedPages(comp))
    }

    @Test fun strictIncludeLists() {
        assertEquals(listOf(4, 5, 6, 7, 12), parseInclude("4-7, 12"))
        assertEquals(listOf(4, 5, 6, 7, 12), parseInclude("4–7; 12"))
        assertNull(parseInclude("4-7, x"))
        assertNull(parseInclude("4-"))
        assertEquals(emptyList<Int>(), parseInclude(" "))
        val bad = comp.copy(include = "")
        assertTrue(plan(listOf(sb, wb, bad)).problems().contains("Companion: list the pages you'll use."))
        assertTrue(plan(listOf(sb, wb, comp.copy(include = "12-x"))).problems().contains("Companion: list the pages you'll use."))
        assertTrue(plan().problems().none { it.startsWith("Companion") })
    }

    @Test fun onTopLeavesEveryCoreLessonAlone() {
        val p = plan()
        val g = p.groups[0]
        val before = GroupReport(p, g).rows.map { it.pages.take(2) }
        val c = GroupReport(p, g).saveLesson(1, LessonSave(core = before[1].let { mapOf("a" to it[0], "b" to it[1]) }, supplementary = mapOf("c" to listOf(12, 13)), inLesson = emptySet(), note = ""), ids())!!
        val r = GroupReport(p, g.copy(custom = c))
        assertEquals(before, r.rows.map { it.pages.take(2) })
        assertEquals(listOf(12, 13), r.rows[1].pages[2])
        assertTrue(r.coverage()!!.ok)
        assertEquals(listOf(14, 15, 30), r.unplacedPages(comp))
        assertTrue(c.active)
    }

    /** Two lessons: SB 5–6 · WB 3, then SB 7–8 · WB 4. The spec's worked example. */
    private fun small() = plan(listOf(Book("a", "SB", 5, 8), Book("b", "WB", 3, 4), comp), end = "2027-03-03")

    @Test fun theTeacherChoosesWhatTheLessonTeaches() {
        val p = small()
        val g = p.groups[0]
        val r0 = GroupReport(p, g)
        assertEquals(listOf(listOf(5, 6), listOf(3)), r0.rows[0].pages.take(2))
        // Companion 12–13 in the lesson; SB kept, WB skipped.
        val c = r0.saveLesson(0, LessonSave(mapOf("a" to listOf(5, 6)), mapOf("c" to listOf(12, 13)), setOf("c"), ""), ids())!!
        val r = GroupReport(p, g.copy(custom = c))
        assertEquals(listOf(listOf(5, 6), emptyList(), listOf(12, 13)), r.rows[0].pages)
        assertEquals(listOf(listOf(7, 8), listOf(3, 4), emptyList()), r.rows[1].pages)
        assertEquals(mapOf("a" to listOf(5, 6), "b" to listOf(3)), c.lessons[0].replacementBase)
        assertTrue(r.coverage()!!.ok)
        // Switching it to on top, with the saved pages back, restores both lessons.
        val back = r.saveLesson(0, LessonSave(c.lessons[0].replacementBase!!, mapOf("c" to listOf(12, 13)), emptySet(), ""), ids())!!
        val rb = GroupReport(p, g.copy(custom = back))
        assertEquals(listOf(listOf(5, 6), listOf(3), listOf(12, 13)), rb.rows[0].pages)
        assertEquals(listOf(listOf(7, 8), listOf(4), emptyList()), rb.rows[1].pages)
        assertNull(back.lessons[0].replacementBase)
        assertEquals(emptySet<String>(), back.lessons[0].inLesson)
    }

    @Test fun aSecondInLessonBookKeepsTheFirstBaseline() {
        val p = small().let { it.copy(books = it.books + reader) }
        val g = p.groups[0]
        val c1 = GroupReport(p, g).saveLesson(0, LessonSave(mapOf("a" to listOf(5, 6)), mapOf("c" to listOf(12)), setOf("c"), ""), ids())!!
        val g1 = g.copy(custom = c1)
        val c2 = GroupReport(p, g1, Source.CUSTOM).saveLesson(0, LessonSave(mapOf("a" to listOf(5)), mapOf("c" to listOf(12), "r" to listOf(1, 2)), setOf("c", "r"), ""), ids())!!
        assertEquals(mapOf("a" to listOf(5, 6), "b" to listOf(3)), c2.lessons[0].replacementBase)
        assertEquals(setOf("c", "r"), c2.lessons[0].inLesson)
        // Every core page still lands once.
        assertTrue(GroupReport(p, g.copy(custom = c2)).coverage()!!.ok)
    }

    @Test fun anInvalidSaveChangesNothing() {
        val p = small()
        val g = p.groups[0]
        val r = GroupReport(p, g)
        // A page the book doesn't include, a core page outside the book, and a supplementary page held by another lesson.
        assertNull(r.saveLesson(0, LessonSave(mapOf("a" to listOf(5, 6)), mapOf("c" to listOf(16)), emptySet(), ""), ids()))
        assertNull(r.saveLesson(0, LessonSave(mapOf("a" to listOf(4)), emptyMap(), emptySet(), ""), ids()))
        val c = r.saveLesson(1, LessonSave(mapOf("a" to listOf(7, 8), "b" to listOf(4)), mapOf("c" to listOf(30)), emptySet(), ""), ids())!!
        val r2 = GroupReport(p, g.copy(custom = c), Source.CUSTOM)
        assertNull(r2.saveLesson(0, LessonSave(mapOf("a" to listOf(5, 6), "b" to listOf(3)), mapOf("c" to listOf(30)), emptySet(), ""), ids()))
        // A core page held by a touched lesson can't be pulled into another.
        assertNull(r2.saveLesson(0, LessonSave(mapOf("a" to listOf(5, 6, 7)), mapOf("c" to listOf(12)), setOf("c"), ""), ids()))
    }

    @Test fun corePagesCanComeFromLaterUntouchedLessons() {
        val p = plan()
        val g = p.groups[0]
        val r = GroupReport(p, g)
        // Lesson 1 (SB 1–2 · WB 1) also takes SB 3–4 from lesson 2.
        val c = r.saveLesson(0, LessonSave(mapOf("a" to listOf(1, 2, 3, 4), "b" to listOf(1)), mapOf("c" to listOf(12)), setOf("c"), ""), ids())!!
        val rr = GroupReport(p, g.copy(custom = c))
        assertTrue(rr.coverage()!!.toString(), rr.coverage()!!.ok)
        assertEquals(listOf(1, 2, 3, 4), rr.rows[0].pages[0])
        assertTrue(rr.rows.drop(1).flatMap { it.pages[0] }.none { it < 5 })
    }

    /** A custom plan whose untouched lesson 2 carries Companion pages on top. */
    private fun withUntouchedPlacement(p: Plan): Group {
        val g = p.groups[0]
        val c = GroupReport(p, g).toCustomPlan(ids())
        val l = c.lessons[1]
        return g.copy(custom = c.copy(lessons = c.lessons.toMutableList().apply { set(1, l.copy(pages = l.pages + ("c" to listOf(12, 13)))) }))
    }

    @Test fun savingSupplementaryPagesKeepsTheChosenCatchUp() {
        val p = plan(pace = 2.0)
        val g = p.groups[0]
        val c = GroupReport(p, g).catchUpPlan(2, ids())!!
        val report = GroupReport(p, g.copy(custom = c))
        val saved = report.saveLesson(0, LessonSave(c.lessons[0].pages, mapOf("c" to listOf(12)), emptySet(), "Reader"), ids())!!
        assertEquals(2, saved.catchUpBoost)
        assertEquals(listOf(12), saved.lessons[0].pages["c"])
        val repaced = GroupReport(p, g.copy(custom = saved)).repaceFrom(0)!!
        assertEquals(saved.lessons[0], repaced.lessons[0])
        assertEquals(2, repaced.catchUpBoost)
        assertTrue(GroupReport(p, g.copy(custom = repaced)).coverage()!!.ok)
    }

    @Test fun automaticFixesKeepSupplementaryPages() {
        val p = plan(pace = 2.0)
        val g = withUntouchedPlacement(p)
        val r = GroupReport(p, g, Source.CUSTOM)
        assertTrue(r.coverage()!!.ok)
        for (c in listOf(r.repaceFrom(0)!!, r.tidy()!!, r.catchUpPlan(1)!!, r.foldBacklog("b", 2) ?: r.repaceFrom(0)!!)) {
            assertEquals(listOf(12, 13), c.lessons[1].pages["c"])
        }
        // Re-pacing gives the core pages back unchanged.
        assertEquals(GroupReport(p, p.groups[0]).rows.map { it.pages.take(2) }, GroupReport(p, g.copy(custom = r.repaceFrom(0))).rows.map { it.pages.take(2) })
    }

    @Test fun overflowLessonsKeepTheirSupplementaryPages() {
        val p = plan()
        val g = p.groups[0]
        val c = GroupReport(p, g).toCustomPlan(ids())
        val extra = PlannedLesson("x", mapOf("a" to listOf(20), "c" to listOf(30)))
        val g2 = g.copy(custom = c.copy(lessons = c.lessons + extra))
        val r = GroupReport(p, g2, Source.CUSTOM)
        for (out in listOf(r.repaceFrom(0)!!, r.fitToTerm()!!, r.catchUpPlan(1) ?: r.repaceFrom(0)!!)) {
            assertTrue(out.lessons.any { it.pages["c"] == listOf(30) })
        }
    }

    @Test fun tickingRecordsTheSupplementaryPages() {
        val p = plan()
        val g = p.groups[0]
        val c = GroupReport(p, g).saveLesson(0, LessonSave(mapOf("a" to listOf(1, 2), "b" to listOf(1)), mapOf("c" to listOf(12, 13)), emptySet(), ""), ids())!!
        val r = GroupReport(p, g.copy(custom = c))
        val ticked = r.markDone(setOf(r.rows[0].date))
        assertEquals(listOf(12, 13), ticked.done[0].pages["c"])
        assertEquals(listOf(14, 15, 30), GroupReport(p, ticked).unplacedPages(comp))
        val back = p.unmarkLesson(ticked, r.rows[0].date)
        assertEquals(listOf(12, 13), GroupReport(p, back).rows[0].pages[2])
    }

    @Test fun eachGroupPlacesItsOwnPages() {
        val two = listOf(Group("g", "G", setOf(MONDAY, WEDNESDAY), d("2027-03-01")), Group("h", "H", setOf(TUESDAY), d("2027-03-01")))
        val p = plan(groups = two)
        val c = GroupReport(p, two[0]).saveLesson(0, LessonSave(mapOf("a" to listOf(1, 2), "b" to listOf(1)), mapOf("c" to listOf(12)), emptySet(), ""), ids())!!
        val p2 = p.copy(groups = listOf(two[0].copy(custom = c), two[1]))
        assertEquals(listOf(13, 14, 15, 30), GroupReport(p2, p2.groups[0]).unplacedPages(comp))
        assertEquals(listOf(12, 13, 14, 15, 30), GroupReport(p2, p2.groups[1]).unplacedPages(comp))
        // Copying the plan to H keeps Companion 12, which H hasn't used.
        val copied = p2.copyCustomPlan(c, two[1], ids())
        assertEquals(listOf(12), copied.lessons[0].pages["c"])
    }

    @Test fun aSupplementaryBookNeverLeads() {
        val p = plan(listOf(sb, wb, comp))
        val led = p.copy(lead = "c")
        assertEquals(GroupReport(p, p.groups[0]).rows, GroupReport(led, led.groups[0]).rows)
        assertTrue(led.problems().none { it.contains("units") })
    }

    @Test fun removingAnInLessonLessonClearsItsBaseline() {
        val c = CustomPlan(true, listOf(
            PlannedLesson("x", mapOf("a" to listOf(1), "c" to listOf(12)), inLesson = setOf("c"), replacementBase = mapOf("a" to listOf(1, 2)), touched = true),
            PlannedLesson("y", mapOf("a" to listOf(2))),
        ))
        val out = c.removeLesson("x").lessons.single()
        assertEquals(mapOf("a" to listOf(1, 2), "c" to listOf(12)), out.pages)
        assertEquals(setOf("c"), out.inLesson)
        assertNull(out.replacementBase)
    }

    @Test fun theTsvShowsAColumnOnlyWhenUsed() {
        val p = plan()
        val g = p.groups[0]
        assertTrue(!GroupReport(p, g).asTsv().lines()[0].contains("Companion"))
        val c = GroupReport(p, g).saveLesson(0, LessonSave(mapOf("a" to listOf(1, 2), "b" to listOf(1)), mapOf("c" to listOf(12)), emptySet(), ""), ids())!!
        val lines = GroupReport(p, g.copy(custom = c)).asTsv().lines()
        assertEquals(listOf("#", "Date", "Day", "SB", "WB", "Companion"), lines[0].split("\t"))
        assertEquals("12", lines[1].split("\t")[5])
    }

    @Test fun aLessonCanAlwaysKeepItsOwnCorePages() {
        val p = plan()
        val g = p.groups[0]
        val c = GroupReport(p, g).toCustomPlan(ids())
        // Lesson 2 also holds SB 1, which lesson 1 has: doubled, but still its own.
        val messy = c.copy(lessons = c.lessons.toMutableList().apply { set(1, get(1).copy(pages = get(1).pages + ("a" to listOf(1, 3, 4)), touched = true)) })
        val r = GroupReport(p, g.copy(custom = messy), Source.CUSTOM)
        val out = r.saveLesson(1, LessonSave(mapOf("a" to listOf(1, 3, 4), "b" to listOf(2)), mapOf("c" to listOf(12)), emptySet(), ""), ids())
        assertEquals(listOf(12), out!!.lessons[1].pages["c"])
    }
}
