package com.coursebook.pacer.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coursebook.pacer.data.PlanStore
import com.coursebook.pacer.data.SettingsStore
import com.coursebook.pacer.engine.GroupReport
import com.coursebook.pacer.engine.greekHolidays
import com.coursebook.pacer.engine.markDone
import com.coursebook.pacer.engine.markDoneUpTo
import com.coursebook.pacer.engine.unmarkDone
import com.coursebook.pacer.engine.unmarkDoneFrom
import com.coursebook.pacer.model.ALL_GROUPS
import com.coursebook.pacer.model.AppData
import com.coursebook.pacer.model.Book
import com.coursebook.pacer.model.Closure
import com.coursebook.pacer.model.Group
import com.coursebook.pacer.model.Homework
import com.coursebook.pacer.model.SchoolClass
import com.coursebook.pacer.model.SchoolYear
import com.coursebook.pacer.model.Settings
import com.coursebook.pacer.model.TestEvent
import com.coursebook.pacer.model.ThemeMode
import com.coursebook.pacer.model.newId
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [stack] is the back stack; its last entry is the screen on show. */
data class UiState(val data: AppData, val undoDepth: Int, val stack: List<Screen> = Nav.root) {
    val screen: Screen get() = stack.last()
}

/** What undo restores: every year, and which year and class were open. */
private data class Snapshot(val years: List<SchoolYear>, val activeYear: String, val activeClass: String)

/**
 * Holds the school years, applies every edit, and keeps an undo history.
 * Edits to the same field within [COALESCE_MS] collapse into one undo step,
 * so typing a page number is one step, not six. Moving between screens is not an edit,
 * and opening another school year starts a fresh history.
 */
class PacerViewModel(private val store: PlanStore, private val settingsStore: SettingsStore) : ViewModel() {
    private val _state = MutableStateFlow(store.load().let { d -> stateFor(d, Nav.fromView(d.view), 0) })
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val undoStack = ArrayDeque<Snapshot>()
    private var lastTag: String? = null
    private var lastTime = 0L
    private var saveJob: Job? = null

    private val data get() = _state.value.data

    // ---- plumbing ----------------------------------------------------------

    private fun edit(tag: String? = null, change: (AppData) -> AppData) {
        val before = data
        val after = change(before)
        if (after == before) return
        val now = SystemClock.elapsedRealtime()
        if (!(tag != null && tag == lastTag && now - lastTime < COALESCE_MS)) {
            undoStack.addLast(Snapshot(before.years, before.activeYear, before.activeClass))
            if (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
        }
        lastTag = tag
        lastTime = now
        publish(after)
    }

    /** Edits the open year. Does nothing when no year is open. */
    private fun editYear(tag: String? = null, change: (SchoolYear) -> SchoolYear) = edit(tag) { d ->
        d.copy(years = d.years.map { if (it.id == d.activeYear) change(it) else it })
    }

    /** Edits the open class. Does nothing when no class is open. */
    private fun editClass(tag: String? = null, change: (SchoolClass) -> SchoolClass) = editYear(tag) { y ->
        y.copy(classes = y.classes.map { if (it.id == data.activeClass) change(it) else it })
    }

    private fun editGroup(id: String, tag: String? = null, change: (Group) -> Group) =
        editClass(tag) { c -> c.copy(groups = c.groups.map { if (it.id == id) change(it) else it }) }

    private fun editBook(id: String, tag: String? = null, change: (Book) -> Book) =
        editClass(tag) { c -> c.copy(books = c.books.map { if (it.id == id) change(it) else it }) }

    /** Opening another year or class, by hand or through undo, closes the screens that showed the old one. */
    private fun publish(next: AppData, stack: List<Screen> = _state.value.stack) {
        val s = when {
            next.activeYear != data.activeYear -> Nav.afterYearSwitch(stack)
            next.activeClass != data.activeClass -> Nav.afterClassSwitch(stack)
            else -> stack
        }
        _state.value = stateFor(next, s, undoStack.size)
        scheduleSave()
    }

    /** Keeps the open screens pointing at things that still exist. */
    private fun stateFor(d: AppData, stack: List<Screen>, undoDepth: Int): UiState {
        val c = d.schoolClass
        val s = Nav.prune(stack, d.year != null, c != null, c?.groups.orEmpty().mapTo(HashSet()) { it.id })
        val view = Nav.viewOf(s)
        return UiState(if (view == d.view) d else d.copy(view = view), undoDepth, s)
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(SAVE_DEBOUNCE_MS)
            store.save(data)
        }
    }

    /**
     * Writes now, on the calling thread. Called when the app goes to the background,
     * where the process may be killed before a debounced save runs. The file is a few kilobytes.
     */
    fun flush() {
        if (saveJob?.isActive != true) return
        saveJob?.cancel()
        store.save(data)
    }

    /** The open year and class stay open if they still exist after the undo; otherwise the ones open at the time return. */
    fun undo() {
        val snap = undoStack.removeLastOrNull() ?: return
        lastTag = null
        val d = data
        val year = snap.years.firstOrNull { it.id == d.activeYear }
        val keep = year != null && (d.activeClass.isEmpty() || year.classes.any { it.id == d.activeClass })
        publish(
            if (keep) d.copy(years = snap.years)
            else d.copy(years = snap.years, activeYear = snap.activeYear, activeClass = snap.activeClass),
        )
    }

    private fun clearHistory() {
        undoStack.clear()
        lastTag = null
    }

    // ---- navigation (not undoable) -----------------------------------------

    fun open(screen: Screen) {
        val stack = Nav.push(_state.value.stack, screen)
        if (stack != _state.value.stack) publish(data, stack)
    }

    /** Returns false when already at Home, so the system can close the app. */
    fun back(): Boolean {
        val stack = _state.value.stack
        if (stack.size < 2) return false
        publish(data, Nav.pop(stack))
        return true
    }

    /** Opens a school year from Home. Another year's edits can no longer be undone from here. */
    fun openYear(id: String) {
        if (id != data.activeYear) {
            clearHistory()
            publish(data.copy(activeYear = id, activeClass = ""), Nav.root)
        }
        open(Screen.Year)
    }

    fun openClass(id: String) {
        if (id != data.activeClass) publish(data.copy(activeClass = id))
        open(Screen.Class)
    }

    /** Opens a group of any class in the open year, from Overview. */
    fun openGroup(classId: String, groupId: String) {
        if (classId != data.activeClass) publish(data.copy(activeClass = classId))
        open(Screen.Group(groupId))
    }

    // ---- settings (not undoable, saved at once) ----------------------------

    private fun editSettings(change: (Settings) -> Settings) {
        val next = change(_settings.value).sanitised()
        if (next == _settings.value) return
        _settings.value = next
        settingsStore.save(next)
    }

    fun setTheme(v: ThemeMode) = editSettings { it.copy(theme = v) }
    fun setDefaultMins(v: Int) = editSettings { it.copy(lessonMins = v) }
    fun setDefaultPace(v: Double) = editSettings { it.copy(pace = v) }
    fun setScheduleByMonth(v: Boolean) = editSettings { it.copy(scheduleByMonth = v) }

    // ---- school years ------------------------------------------------------

    /** Creates a year with no classes and opens it. With [holidays], the Greek public holidays it touches come as days off. */
    fun newYear(name: String, start: LocalDate?, end: LocalDate?, holidays: Boolean) {
        val days = if (holidays && start != null && end != null) {
            greekHolidays(start, end).map { (d, l) -> Closure(newId(), l, d, d, ALL_GROUPS) }
        } else {
            emptyList()
        }
        val y = SchoolYear(newId(), name.ifBlank { "School year" }, start, end, days, emptyList())
        clearHistory()
        edit { it.copy(years = it.years + y, activeYear = y.id, activeClass = "") }
        open(Screen.Year)
    }

    /** Renames any year, from Home or from its settings. */
    fun renameYear(id: String, name: String) = edit("yname:$id") { d ->
        d.copy(years = d.years.map { if (it.id == id) it.copy(name = name) else it })
    }

    /**
     * Deletes any year, from Home or from its settings. Deleting the open year returns to Home.
     * Home's snackbar offers undo once.
     */
    fun deleteYear(id: String) = edit { d ->
        val open = d.activeYear == id
        d.copy(
            years = d.years.filter { it.id != id },
            activeYear = if (open) "" else d.activeYear,
            activeClass = if (open) "" else d.activeClass,
        )
    }

    // ---- term --------------------------------------------------------------

    fun setStart(v: LocalDate?) = editYear("term:start") { it.copy(start = v) }
    fun setEnd(v: LocalDate?) = editYear("term:end") { it.copy(end = v) }

    // ---- closures ----------------------------------------------------------

    fun addClosure(label: String, from: LocalDate, to: LocalDate?, who: String) = editYear { y ->
        y.copy(closures = y.closures + Closure(newId(), label.ifBlank { "Day off" }, from, to ?: from, who))
    }

    fun removeClosure(id: String) = editYear { y -> y.copy(closures = y.closures.filter { it.id != id }) }

    /** Adds the Greek public holidays the term touches. Returns how many were new. */
    fun addGreekHolidays(): Int {
        val y = data.year ?: return 0
        val start = y.start ?: return 0
        val end = y.end ?: return 0
        val have = y.closures.map { it.from to it.label }.toHashSet()
        val fresh = greekHolidays(start, end).filter { have.add(it) }
        if (fresh.isNotEmpty()) {
            editYear { it.copy(closures = it.closures + fresh.map { (d, l) -> Closure(newId(), l, d, d, ALL_GROUPS) }) }
        }
        return fresh.size
    }

    // ---- classes -----------------------------------------------------------

    /** Adds a class with one group and the lesson length and pace from Settings, and opens it. */
    fun newClass(name: String) {
        val y = data.year ?: return
        val s = _settings.value
        val group = Group(newId(), "Group A", setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), y.start)
        val c = SchoolClass(newId(), name.ifBlank { "Class ${y.classes.size + 1}" }, s.lessonMins, s.pace, listOf(group), emptyList(), emptyList(), emptyList())
        edit { d -> d.copy(years = d.years.map { if (it.id == y.id) it.copy(classes = it.classes + c) else it }, activeClass = c.id) }
        open(Screen.Class)
    }

    fun renameClass(name: String) = editClass("cname") { it.copy(name = name) }

    /** Deletes the open class with the days off aimed only at it or its groups. */
    fun deleteClass() = edit { d ->
        val c = d.schoolClass ?: return@edit d
        val gone = c.groups.mapTo(hashSetOf(c.id)) { it.id }
        d.copy(
            years = d.years.map { y ->
                if (y.id != d.activeYear) y
                else y.copy(classes = y.classes.filter { it.id != c.id }, closures = y.closures.filter { it.who !in gone })
            },
            activeClass = "",
        )
    }

    fun setMins(v: Int) = editClass("class:mins") { it.copy(mins = v) }
    fun setPace(v: Double) = editClass("class:pace") { it.copy(pace = v) }

    // ---- groups ------------------------------------------------------------

    fun addGroup() = editClass { c ->
        val name = "Group " + ('A' + c.groups.size)
        c.copy(groups = c.groups + Group(newId(), name, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), data.year?.start))
    }

    /** Removes the group with its homework and the days off aimed only at it. */
    fun removeGroup(id: String) = editYear { y ->
        y.copy(
            closures = y.closures.filter { it.who != id },
            classes = y.classes.map { c ->
                if (c.id != data.activeClass) c
                else c.copy(groups = c.groups.filter { it.id != id }, homework = c.homework.filter { it.groupId != id })
            },
        )
    }

    fun renameGroup(id: String, name: String) = editGroup(id, "gname:$id") { it.copy(name = name) }

    fun toggleDay(id: String, day: DayOfWeek) = editGroup(id) { g ->
        g.copy(days = if (day in g.days) g.days - day else g.days + day)
    }

    fun setNextLesson(id: String, date: LocalDate?) = editGroup(id, "gfrom:$id") { it.copy(from = date) }

    fun setPosition(groupId: String, bookId: String, page: Int) =
        editGroup(groupId, "pos:$groupId:$bookId") { it.copy(pos = it.pos + (bookId to page)) }

    /** Starts every group over: first pages, the year's start, and no lessons marked done. */
    fun resetPositions() = editClass { c ->
        c.copy(groups = c.groups.map { it.copy(pos = emptyMap(), from = data.year?.start, done = emptyList()) })
    }

    // ---- done lessons ------------------------------------------------------

    /** Applies [change] to a group of the open class, given its current schedule. */
    private fun editLessons(groupId: String, change: (GroupReport, Group) -> Group) {
        val plan = data.plan ?: return
        val g = plan.groups.firstOrNull { it.id == groupId } ?: return
        val next = change(GroupReport(plan, g), g)
        editGroup(groupId) { next }
    }

    /** Ticks or unticks the lesson on [date] on its own. */
    fun toggleLesson(groupId: String, date: LocalDate) = editLessons(groupId) { r, g ->
        if (g.done.any { it.date == date }) g.unmarkDone(date) else r.markDone(setOf(date))
    }

    fun markDoneUpTo(groupId: String, date: LocalDate) = editLessons(groupId) { r, _ -> r.markDoneUpTo(date) }

    fun unmarkDoneFrom(groupId: String, date: LocalDate) = editLessons(groupId) { _, g -> g.unmarkDoneFrom(date) }

    fun markNextLessonDone(groupId: String) = editLessons(groupId) { r, g ->
        r.rows.firstOrNull()?.let { r.markDone(setOf(it.date)) } ?: g
    }

    // ---- tests -------------------------------------------------------------

    fun addEvent(name: String, date: LocalDate, who: String, consumes: Boolean) = editClass { c ->
        c.copy(events = c.events + TestEvent(newId(), name.ifBlank { "Test" }, date, who, consumes))
    }

    fun removeEvent(id: String) = editClass { c -> c.copy(events = c.events.filter { it.id != id }) }

    // ---- books -------------------------------------------------------------

    fun addBook(name: String, first: Int, last: Int) = editClass { c ->
        c.copy(books = c.books + Book(newId(), name.ifBlank { "Book ${c.books.size + 1}" }, first, last))
    }

    fun removeBook(id: String) = editClass { c ->
        c.copy(
            books = c.books.filter { it.id != id },
            homework = c.homework.filter { it.bookId != id },
            groups = c.groups.map { g -> g.copy(pos = g.pos - id, done = g.done.map { it.copy(pages = it.pages - id) }) },
        )
    }

    fun renameBook(id: String, name: String) = editBook(id, "bname:$id") { it.copy(name = name) }
    fun setFirstPage(id: String, v: Int) = editBook(id, "bfirst:$id") { it.copy(first = v) }
    fun setLastPage(id: String, v: Int) = editBook(id, "blast:$id") { it.copy(last = v) }
    fun setSkip(id: String, v: String) = editBook(id, "bskip:$id") { it.copy(skip = v) }
    fun setDue(id: String, v: LocalDate?) = editBook(id, "bdue:$id") { it.copy(due = v) }

    // ---- homework ----------------------------------------------------------

    fun addHomework(groupId: String, bookId: String, from: Int, to: Int, setOn: LocalDate?) = editClass { c ->
        c.copy(homework = c.homework + Homework(newId(), groupId, bookId, minOf(from, to), maxOf(from, to), setOn))
    }

    fun removeHomework(id: String) = editClass { c -> c.copy(homework = c.homework.filter { it.id != id }) }

    class Factory(private val store: PlanStore, private val settings: SettingsStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PacerViewModel(store, settings) as T
    }

    private companion object {
        const val COALESCE_MS = 1200L
        const val UNDO_LIMIT = 60
        const val SAVE_DEBOUNCE_MS = 400L
    }
}
