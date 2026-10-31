package gr.edusuite.pacer.ui

import gr.edusuite.pacer.data.PlanFile
import gr.edusuite.pacer.data.PlanStore
import gr.edusuite.pacer.data.SettingsPort
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.bankedPages
import gr.edusuite.pacer.engine.remainingPages
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Brick
import gr.edusuite.pacer.model.Closure
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Module
import gr.edusuite.pacer.model.SchoolClass
import gr.edusuite.pacer.model.RevisionKind
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.update.UpdateChecker
import java.io.File
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.PriorityQueue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A dispatcher whose clock only moves when a test says so, so the save debounce is tested without waiting.
 * (kotlinx-coroutines-test is not an approved dependency.)
 */
@OptIn(InternalCoroutinesApi::class)
private class ManualDispatcher : CoroutineDispatcher(), Delay {
    private class Task(val time: Long, val seq: Long, val run: Runnable)

    private var now = 0L
    private var seq = 0L
    private val queue = PriorityQueue<Task>(compareBy<Task>({ it.time }, { it.seq }))

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue += Task(now, seq++, block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val task = Task(now + timeMillis, seq++) { continuation.resumeWith(Result.success(Unit)) }
        queue += task
        continuation.invokeOnCancellation { queue.remove(task) }
    }

    /** Runs everything due within [ms] more milliseconds, in time order. */
    fun advanceBy(ms: Long) {
        val target = now + ms
        while (queue.isNotEmpty() && queue.peek().time <= target) {
            val task = queue.poll()
            now = maxOf(now, task.time)
            task.run.run()
        }
        now = target
    }
}

class PacerViewModelTest {
    private fun d(v: String) = LocalDate.parse(v)

    private val sb = Book("sb", "Student's Book", 1, 100)
    private val wb = Book("wb", "Workbook", 1, 80)
    private val group = Group("g1", "Group A", setOf(DayOfWeek.MONDAY), null)
    private val year = SchoolYear(
        "y", "Year", d("2026-09-01"), d("2027-06-01"),
        listOf(
            Closure("own", "Trip", d("2026-10-01"), d("2026-10-01"), "g1"),
            Closure("all", "Holiday", d("2026-10-03"), d("2026-10-03"), "all"),
        ),
        listOf(SchoolClass("c", "Class", 60, 4.0, listOf(group), listOf(sb, wb), emptyList(), emptyList())),
    )

    private class FakeFile : PlanFile {
        override val base = File("plans.json")
        val writes = mutableListOf<ByteArray>()
        /** Set to make [read] fail like a locked file; [write] fails while [failWrites] is true. */
        var readError: IOException? = null
        var failWrites = false
        /** Runs in the middle of a write that is about to succeed: an edit that arrives while the disk is busy. */
        var duringWrite: () -> Unit = {}
        override fun read() = readError?.let { throw it } ?: "{}".toByteArray()
        override fun write(bytes: ByteArray) {
            if (failWrites) throw IOException("disk full")
            duringWrite()
            writes += bytes
        }
        override fun warn(message: String, error: Throwable) = Unit
    }

    private class FakeSettings : SettingsPort {
        var saved = Settings()
        override fun load() = saved
        override fun save(s: Settings) { saved = s }
    }

    private inner class Rig(locked: Boolean = false, start: AppData? = null) {
        val file = FakeFile().also { if (locked) it.readError = IOException("busy") }
        /** What the plans file holds once it can be read again. */
        var disk = start ?: AppData(listOf(year), "y", "c")
        val dispatcher = ManualDispatcher()
        var time = 0L
        var date = d("2026-10-24")
        val settings = FakeSettings()
        val vm = PacerViewModel(
            PlanStore(file, { disk }, { it.hashCode().toString() }),
            settings,
            UpdateChecker({ null }, "0.4.2.0", enabled = false),
            CoroutineScope(SupervisorJob() + dispatcher),
            dispatcher,
            { time },
            { date },
        )
        val group get() = vm.state.value.data.schoolClass!!.groups.single()
        val years get() = vm.state.value.data.years
        val undoDepth get() = vm.state.value.undoDepth
    }

    @Test fun setupRejectsInvalidBookAndDayOffWithoutUndo() {
        val r = Rig()
        val before = r.vm.state.value.data
        val depth = r.undoDepth
        r.vm.addBook("Invalid", 20, 10)
        r.vm.setFirstPage("sb", 0)
        r.vm.addClosure("Invalid", d("2026-11-20"), d("2026-11-10"), ALL_GROUPS)
        assertEquals(before, r.vm.state.value.data)
        assertEquals(depth, r.undoDepth)
    }

    @Test fun addingGroupStoresChosenDaysInOneUndoStep() {
        val r = Rig()
        val depth = r.undoDepth
        val days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        r.vm.addGroup("New group", days)
        val added = r.vm.state.value.data.schoolClass!!.groups.last()
        assertEquals("New group", added.name)
        assertEquals(days, added.days)
        assertEquals(depth + 1, r.undoDepth)
    }

    @Test fun todayFlowAdvancesWithClock() {
        val r = Rig()
        assertEquals(d("2026-10-24"), r.vm.today.value)
        r.date = d("2026-10-25")
        assertEquals(d("2026-10-24"), r.vm.today.value)
        r.vm.refreshToday()
        assertEquals(d("2026-10-25"), r.vm.today.value)
    }

    private fun nextLessonDate(r: Rig): LocalDate {
        val plan = r.vm.state.value.data.plan(r.vm.today.value)!!
        return GroupReport(plan, plan.groups.single()).rows.first().date
    }

    @Test fun pastLessonsAreAskedAboutBeforeAnythingIsTicked() {
        val r = Rig()
        r.vm.refreshToday()
        val asked = r.vm.pastLessons.value!!.single()
        assertTrue(asked.count > 0)
        assertTrue(r.group.done.isEmpty())
        assertEquals(null, r.settings.saved.autoTickFrom)
    }

    @Test fun yesTicksEveryLessonBeforeTodayAndNothingElse() {
        val r = Rig()
        r.vm.refreshToday()
        r.vm.answerPastLessons(true)
        assertEquals(null, r.vm.pastLessons.value)
        assertTrue(r.group.done.isNotEmpty())
        assertTrue(r.group.done.all { it.date.isBefore(d("2026-10-24")) })
        assertFalse(nextLessonDate(r).isBefore(d("2026-10-24")))
        assertEquals(0, r.undoDepth)
    }

    @Test fun leavingThemAloneKeepsTheOldBacklogByHand() {
        val r = Rig()
        r.vm.refreshToday()
        r.vm.answerPastLessons(false)
        assertEquals(d("2026-10-24"), r.settings.saved.autoTickFrom)
        r.date = d("2026-11-03")
        r.vm.refreshToday()
        assertTrue(r.group.done.isEmpty())
    }

    @Test fun aLessonTicksItselfWhenItsDayHasPassedAndNotBefore() {
        val r = Rig()
        r.date = d("2026-08-30")
        r.vm.refreshToday()
        assertEquals(null, r.vm.pastLessons.value)
        r.date = d("2026-09-07")
        r.vm.refreshToday()
        assertTrue(r.group.done.isEmpty())
        r.date = d("2026-09-08")
        r.vm.refreshToday()
        assertEquals(listOf(d("2026-09-07")), r.group.done.map { it.date })
        assertEquals(0, r.undoDepth)
        // Undoing an edit never un-ticks a lesson whose day has passed.
        assertTrue(r.vm.markNextLessonDone("g1"))
        r.vm.undo()
        assertEquals(listOf(d("2026-09-07")), r.group.done.map { it.date })
    }

    @Test fun aLessonThatDidntHappenIsDoneWithNoPagesAndIsNotADayOff() {
        val r = Rig()
        r.date = d("2026-08-30")
        r.vm.refreshToday()
        r.date = d("2026-09-07")
        r.vm.refreshToday()
        assertTrue(r.vm.lessonNotTaught("g1", d("2026-09-07")))
        val done = r.group.done.single()
        assertEquals(d("2026-09-07"), done.date)
        assertTrue(done.pages.isEmpty() && done.edited)
        assertEquals(year.closures, r.years.single().closures)
        val plan = r.vm.state.value.data.plan(r.vm.today.value)!!
        assertEquals(100, plan.remainingPages(plan.groups.single(), sb).size)
        assertTrue(nextLessonDate(r).isAfter(d("2026-09-07")))
    }

    @Test fun midnightMovesPlansAndRevisionDatesTogether() {
        val lead = sb.copy(units = "1, 26, 51, 76")
        val linked = wb.copy(units = "1, 21, 41, 61")
        val homework = Assignment("hw", AssignmentType.HOMEWORK, "Read", due = d("2026-10-26"), pages = mapOf("sb" to listOf(2, 3)))
        val cls = SchoolClass(
            "c", "Class", 60, 4.0, listOf(group.copy(assignments = listOf(homework))), listOf(lead, linked), emptyList(), emptyList(),
            lead = "sb", unitsEnabled = true, bookLinks = listOf(setOf("sb", "wb")),
        )
        val r = Rig(start = AppData(listOf(year.copy(classes = listOf(cls))), "y", "c"))
        fun plan() = r.vm.state.value.data.plan(r.vm.today.value)!!
        val target = UnitRevisionTarget("c", "g1", 0)

        // 23:59 on Saturday: the homework is not due yet, but its pages are already out of pacing.
        val before = plan()
        val beforeGroup = before.groups.single()
        assertEquals(d("2026-10-24"), before.today)
        assertEquals(2, before.bankedPages(beforeGroup, lead))
        assertFalse(2 in before.remainingPages(beforeGroup, lead))
        val monday = revisionDates(GroupReport(before, beforeGroup), 0, before.today).first()
        assertTrue(monday > before.today)

        // The day that revision date arrives: the plan reads the new date, the pages stay banked and the date has passed.
        r.date = monday
        r.vm.refreshToday()
        val after = plan()
        val afterGroup = after.groups.single()
        assertEquals(monday, after.today)
        assertEquals(2, after.bankedPages(afterGroup, lead))
        assertFalse(2 in after.remainingPages(afterGroup, lead))
        val later = revisionDates(GroupReport(after, afterGroup), 0, after.today)
        assertFalse(monday in later)
        assertFalse(r.vm.saveUnitRevision(target, RevisionKind.TEST, monday, emptyMap(), "Unit 1 revision"))
        assertTrue(r.vm.saveUnitRevision(target, RevisionKind.TEST, later.first(), emptyMap(), "Unit 1 revision"))
        assertEquals(afterGroup.assignments.size + 1, r.group.assignments.size)
    }

    @Test fun editPushesOneUndoStep() {
        val r = Rig()
        assertEquals(0, r.undoDepth)
        assertTrue(r.vm.toggleDay("g1", DayOfWeek.TUESDAY))
        assertEquals(1, r.undoDepth)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), r.group.days)
    }

    /** Routes that no longer exist ("tests", "homework") may still be in a saved view; the app opens Home and ignores them. */
    @Test fun aSavedViewNamingAnOldRouteOpensHome() {
        for (view in listOf("tests", "homework", "setup", "gone-group")) {
            val r = Rig(start = AppData(listOf(year), "y", "c", view))
            assertEquals(view, listOf(Screen.Home), r.vm.state.value.stack)
        }
    }

    @Test fun homeTestsOpenTheGroupsWorkPageOrTheClass() {
        val r = Rig()
        r.vm.openWork("c", "g1")
        assertEquals(Screen.Group("g1", GroupPage.WORK), r.vm.state.value.screen)
        r.vm.goTo(Screen.Home)
        r.vm.openWork("c", null)
        assertEquals(Screen.Class, r.vm.state.value.screen)
    }

    /** Create on Plan and Make a custom plan in ⋮ both call this and then offer Undo, so one tap takes either back. */
    @Test fun creatingACustomPlanIsOneUndoStep() {
        val r = Rig()
        assertTrue(r.vm.createCustomPlan("g1"))
        assertEquals(1, r.undoDepth)
        assertTrue(r.group.custom != null)
        r.vm.undo()
        assertEquals(null, r.group.custom)
        assertEquals(0, r.undoDepth)
    }

    @Test fun noChangeEditDoesNotPushUndo() {
        val r = Rig()
        assertFalse(r.vm.renameGroup("g1", "Group A"))
        assertFalse(r.vm.setGroupBookDue("g1", "sb", null))
        assertEquals(0, r.undoDepth)
    }

    @Test fun sameTagWithinWindowMerges() {
        val r = Rig()
        r.vm.renameGroup("g1", "B")
        r.time = 100
        r.vm.renameGroup("g1", "Bo")
        assertEquals(1, r.undoDepth)
        r.time = 2_000
        r.vm.renameGroup("g1", "Boo")
        assertEquals(2, r.undoDepth)
        // Another field never merges, even at once.
        r.vm.setNextLesson("g1", d("2026-10-05"))
        assertEquals(3, r.undoDepth)
    }

    @Test fun undoRestoresPreviousYears() {
        val r = Rig()
        val before = r.years
        r.vm.toggleDay("g1", DayOfWeek.TUESDAY)
        r.vm.undo()
        assertEquals(before, r.years)
        assertEquals(0, r.undoDepth)
        // Nothing left to undo is harmless.
        r.vm.undo()
        assertEquals(before, r.years)
    }

    @Test fun undoLimitIsSixty() {
        val r = Rig()
        repeat(70) { r.vm.toggleDay("g1", DayOfWeek.TUESDAY) }
        assertEquals(60, r.undoDepth)
    }

    @Test fun saveIsDebounced() {
        val r = Rig()
        r.vm.toggleDay("g1", DayOfWeek.TUESDAY)
        r.dispatcher.advanceBy(399)
        assertEquals(0, r.file.writes.size)
        // Another edit restarts the wait.
        r.vm.toggleDay("g1", DayOfWeek.TUESDAY)
        r.dispatcher.advanceBy(399)
        assertEquals(0, r.file.writes.size)
        r.dispatcher.advanceBy(1)
        assertEquals(1, r.file.writes.size)
        r.dispatcher.advanceBy(10_000)
        assertEquals(1, r.file.writes.size)
    }

    @Test fun flushSavesImmediately() {
        val r = Rig()
        r.vm.flush()
        assertEquals("nothing to save yet", 0, r.file.writes.size)
        r.vm.toggleDay("g1", DayOfWeek.TUESDAY)
        r.vm.flush()
        assertEquals(1, r.file.writes.size)
        r.dispatcher.advanceBy(10_000)
        assertEquals("the cancelled debounce doesn't save again", 1, r.file.writes.size)
    }

    @Test fun setGroupBookRemovedRefusesLastCoreBook() {
        val r = Rig()
        assertTrue(r.vm.setGroupBookRemoved("g1", "wb", true))
        assertEquals(setOf("wb"), r.group.removedBooks)
        assertFalse(r.vm.setGroupBookRemoved("g1", "sb", true))
        assertEquals(setOf("wb"), r.group.removedBooks)
        assertEquals(1, r.undoDepth)
        assertTrue(r.vm.setGroupBookRemoved("g1", "wb", false))
        assertEquals(emptySet<String>(), r.group.removedBooks)
        assertFalse("not a book of the class", r.vm.setGroupBookRemoved("g1", "nope", true))
    }

    @Test fun setGroupBookDueIsOneUndoStep() {
        val r = Rig()
        assertTrue(r.vm.setGroupBookDue("g1", "sb", d("2027-01-15")))
        assertEquals(mapOf("sb" to d("2027-01-15")), r.group.bookDue)
        assertEquals(1, r.undoDepth)
        r.vm.undo()
        assertEquals(emptyMap<String, LocalDate>(), r.group.bookDue)
        r.vm.setGroupBookDue("g1", "sb", d("2027-01-15"))
        assertTrue(r.vm.setGroupBookDue("g1", "sb", null))
        assertEquals(emptyMap<String, LocalDate>(), r.group.bookDue)
    }

    @Test fun editClosureUndoable() {
        val r = Rig()
        val before = r.years
        assertTrue(r.vm.editClosure("own", "g1", "Away", d("2026-10-02"), d("2026-10-04")))
        val edited = r.years.single().closures.first { it.id == "own" }
        assertEquals(Closure("own", "Away", d("2026-10-02"), d("2026-10-04"), "g1"), edited)
        assertEquals(1, r.undoDepth)
        r.vm.undo()
        assertEquals(before, r.years)
        // A year-wide day off, or one that isn't this group's, is left alone.
        assertFalse(r.vm.editClosure("all", "g1", "x", d("2026-10-02"), null))
        assertFalse(r.vm.editClosure("own", "other", "x", d("2026-10-02"), null))
        assertEquals(0, r.undoDepth)
    }

    @Test fun settingBookTypeSyncsTheSupplementaryFlag() {
        val r = Rig()
        r.vm.setBookType("wb", gr.edusuite.pacer.model.BookType.SUPPLEMENTARY)
        val book = r.vm.state.value.data.schoolClass!!.books.first { it.id == "wb" }
        assertTrue(book.supplementary)
        // Its current pages become the pages to use, so the book isn't left empty.
        assertEquals("1–80", book.include)
        r.vm.setBookType("wb", gr.edusuite.pacer.model.BookType.WORKBOOK)
        assertFalse(r.vm.state.value.data.schoolClass!!.books.first { it.id == "wb" }.supplementary)
    }

    private fun Rig.written(data: AppData) = data.hashCode().toString().toByteArray().toList()
    private fun Rig.lastWrite() = file.writes.last().toList()
    private fun Rig.newest() = written(vm.state.value.data)

    @Test fun aFailedSaveShowsAProblemAndKeepsTheEditInMemory() {
        val r = Rig()
        r.file.failWrites = true
        r.vm.toggleDay("g1", DayOfWeek.TUESDAY)
        r.dispatcher.advanceBy(400)
        assertEquals(SaveProblem.NotSaved, r.vm.saveProblem.value)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), r.group.days)
        assertEquals(0, r.file.writes.size)
    }

    @Test fun retryAfterAFailureWritesTheNewestStateAndClearsTheProblem() {
        val r = Rig()
        r.file.failWrites = true
        r.vm.toggleDay("g1", DayOfWeek.TUESDAY)
        r.dispatcher.advanceBy(400)
        r.vm.retrySave()
        assertEquals("still failing, still reported", SaveProblem.NotSaved, r.vm.saveProblem.value)
        r.file.failWrites = false
        r.vm.retrySave()
        assertEquals(null, r.vm.saveProblem.value)
        assertEquals(r.newest(), r.lastWrite())
    }

    @Test fun theNextEditRetriesAFailedSave() {
        val r = Rig()
        r.file.failWrites = true
        r.vm.toggleDay("g1", DayOfWeek.TUESDAY)
        r.dispatcher.advanceBy(400)
        r.file.failWrites = false
        r.vm.toggleDay("g1", DayOfWeek.WEDNESDAY)
        r.dispatcher.advanceBy(400)
        assertEquals(null, r.vm.saveProblem.value)
        assertEquals(1, r.file.writes.size)
        assertEquals(r.newest(), r.lastWrite())
    }

    @Test fun flushWritesAFailedSaveEvenThoughTheJobIsDone() {
        val r = Rig()
        r.file.failWrites = true
        r.vm.toggleDay("g1", DayOfWeek.TUESDAY)
        r.dispatcher.advanceBy(400)
        r.file.failWrites = false
        r.vm.flush()
        assertEquals(1, r.file.writes.size)
        assertEquals(null, r.vm.saveProblem.value)
        r.vm.flush()
        assertEquals("nothing is dirty any more", 1, r.file.writes.size)
    }

    @Test fun anOlderSuccessNeverClearsANewerEdit() {
        val r = Rig()
        r.file.failWrites = true
        r.vm.toggleDay("g1", DayOfWeek.TUESDAY)
        r.dispatcher.advanceBy(400)
        r.file.failWrites = false
        val older = r.newest()
        r.file.duringWrite = { r.vm.toggleDay("g1", DayOfWeek.WEDNESDAY); r.file.duringWrite = {} }
        r.vm.retrySave()
        assertEquals(older, r.lastWrite())
        assertEquals("the newer edit isn't on disk yet", SaveProblem.NotSaved, r.vm.saveProblem.value)
        r.dispatcher.advanceBy(400)
        assertEquals(r.newest(), r.lastWrite())
        assertEquals(null, r.vm.saveProblem.value)
    }

    @Test fun nothingIsWrittenAfterALockedRead() {
        val r = Rig(locked = true)
        assertEquals(SaveProblem.ReadLocked, r.vm.saveProblem.value)
        r.vm.newYear("New", d("2026-09-01"), d("2027-06-01"), false)
        r.dispatcher.advanceBy(400)
        r.vm.flush()
        r.vm.retrySave()
        assertEquals(0, r.file.writes.size)
        assertEquals(SaveProblem.ReadLocked, r.vm.saveProblem.value)
    }

    @Test fun tryingToReadAgainWhileStillUnreadableChangesNothing() {
        val r = Rig(locked = true)
        r.vm.retryRead()
        assertEquals(SaveProblem.ReadLocked, r.vm.saveProblem.value)
    }

    @Test fun tryingToReadAgainWithNoEditsLoadsTheFileAndResumesSaving() {
        val r = Rig(locked = true)
        r.file.readError = null
        r.vm.retryRead()
        assertEquals(null, r.vm.saveProblem.value)
        assertEquals(r.disk.years, r.vm.state.value.data.years)
        assertEquals("reading doesn't write", 0, r.file.writes.size)
        r.vm.newYear("New", d("2026-09-01"), d("2027-06-01"), false)
        r.dispatcher.advanceBy(400)
        assertEquals(1, r.file.writes.size)
    }

    @Test fun tryingToReadAgainAfterEditsKeepsTheLockAndTheEdits() {
        val r = Rig(locked = true)
        r.vm.newYear("New", d("2026-09-01"), d("2027-06-01"), false)
        r.file.readError = null
        r.vm.retryRead()
        assertEquals(SaveProblem.LockedWithEdits, r.vm.saveProblem.value)
        assertEquals(listOf("New"), r.years.map { it.name })
        r.dispatcher.advanceBy(400)
        r.vm.flush()
        assertEquals("the file that was just read is never overwritten", 0, r.file.writes.size)
    }

    @Test fun turnOnAddsAndNeverRemoves() {
        val r = Rig()
        r.vm.chooseModules(setOf(Module.WORK))
        r.vm.turnOn(Module.OUTPUT)
        assertEquals(setOf(Module.WORK, Module.OUTPUT), r.vm.settings.value.modules)
        assertEquals(setOf(Module.WORK, Module.OUTPUT), r.settings.saved.modules)
        r.vm.turnOn(Module.WORK)
        assertEquals(setOf(Module.WORK, Module.OUTPUT), r.vm.settings.value.modules)
    }

    @Test fun choosingModulesHappensOnceAndNeverTakesAnyAway() {
        val r = Rig()
        r.vm.chooseModules(setOf(Module.WORK, Module.OUTPUT))
        r.vm.chooseModules(setOf(Module.PLANNING))
        assertEquals(setOf(Module.WORK, Module.OUTPUT), r.vm.settings.value.modules)
    }

    @Test fun turningOnFromNothingChosenKeepsEverything() {
        val r = Rig()
        r.vm.turnOn(Module.WORK)
        assertEquals(Module.entries.toSet(), r.vm.settings.value.modules)
    }

    @Test fun undoKeepsModulesOn() {
        val r = Rig()
        r.vm.chooseModules(emptySet())
        r.vm.turnOn(Module.PLANNING)
        r.vm.addClosure("Trip", d("2026-11-02"), null, ALL_GROUPS)
        r.vm.undo()
        assertEquals(setOf(Module.PLANNING), r.vm.settings.value.modules)
    }

    @Test fun featuresAddTheOpenYearsBricksInUse() {
        val r = Rig()
        r.vm.chooseModules(emptySet())
        assertFalse(featuresOf(r.vm.settings.value, r.vm.state.value.data).has(Brick.HOMEWORK))
        r.vm.saveAssignment(r.group.id, Assignment("h", AssignmentType.HOMEWORK, "HW"))
        val f = featuresOf(r.vm.settings.value, r.vm.state.value.data)
        assertTrue(f.has(Brick.HOMEWORK))
        assertFalse(f.has(Brick.TESTS))
    }
}
