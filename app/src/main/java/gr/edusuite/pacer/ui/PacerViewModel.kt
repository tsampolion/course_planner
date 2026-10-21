package gr.edusuite.pacer.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import gr.edusuite.pacer.data.JsonCodec
import gr.edusuite.pacer.data.PlanStore
import gr.edusuite.pacer.data.SettingsStore
import gr.edusuite.pacer.data.addYears
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.LessonSave
import gr.edusuite.pacer.engine.Source
import gr.edusuite.pacer.engine.parseInclude
import gr.edusuite.pacer.engine.saveLesson
import gr.edusuite.pacer.engine.catchUpPlan
import gr.edusuite.pacer.engine.foldBacklog
import gr.edusuite.pacer.engine.copyCustomPlan
import gr.edusuite.pacer.engine.editLesson
import gr.edusuite.pacer.engine.fitToTerm
import gr.edusuite.pacer.engine.insertLesson
import gr.edusuite.pacer.engine.moveLesson
import gr.edusuite.pacer.engine.removeLesson
import gr.edusuite.pacer.engine.repaceFrom
import gr.edusuite.pacer.engine.tidy
import gr.edusuite.pacer.engine.toCustomPlan
import gr.edusuite.pacer.engine.unmarkLesson
import gr.edusuite.pacer.engine.unmarkLessonsFrom
import gr.edusuite.pacer.engine.greekHolidays
import gr.edusuite.pacer.engine.markDone
import gr.edusuite.pacer.engine.markDoneUpTo
import gr.edusuite.pacer.engine.recordLesson
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Closure
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Homework
import gr.edusuite.pacer.model.MAX_LAG_LESSONS
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.SchoolClass
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.model.ThemeMode
import gr.edusuite.pacer.model.newId
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [stack] is the back stack; its last entry is the screen on show. [historyStep] changes whenever the undo history
 * does (a step added, undone or cleared), so a snackbar's Undo can tell whether its change is still the latest.
 */
data class UiState(val data: AppData, val undoDepth: Int, val stack: List<Screen> = Nav.root, val historyStep: Long = 0) {
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
    private val loaded = store.load()
    private val _state = MutableStateFlow(withDefaultYear(loaded.data).let { d -> stateFor(d, Nav.fromView(d.view), 0) })
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Where an unreadable plans file was moved at launch, until the teacher closes the notice about it. */
    private val _setAside = MutableStateFlow(loaded.setAside)
    val setAside: StateFlow<File?> = _setAside.asStateFlow()

    fun dismissSetAside() {
        _setAside.value = null
    }

    /** Groups opened this session, latest first, for the group switcher. Not saved: a new session starts empty. */
    private val _recent = MutableStateFlow(recentAfter(emptyList(), _state.value))
    val recent: StateFlow<List<GroupRef>> = _recent.asStateFlow()

    private fun recentAfter(recent: List<GroupRef>, ui: UiState): List<GroupRef> {
        val g = ui.screen as? Screen.Group ?: return recent
        return Nav.recent(recent, GroupRef(ui.data.activeClass, g.id))
    }

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val undoStack = ArrayDeque<Snapshot>()
    private var lastTag: String? = null
    private var lastTime = 0L
    private var saveJob: Job? = null
    private var historyStep = 0L

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
            historyStep++
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

    /**
     * Home always shows a year when there is one: with none open (a fresh import, the open year deleted), the one that
     * starts last opens ([defaultYearId]).
     */
    private fun withDefaultYear(d: AppData): AppData =
        if (d.year != null || d.years.isEmpty()) d else d.copy(activeYear = defaultYearId(d.years).orEmpty(), activeClass = "")

    /** Opening another year or class, by hand or through undo, closes the screens that showed the old one. */
    private fun publish(proposed: AppData, stack: List<Screen> = _state.value.stack) {
        val next = withDefaultYear(proposed)
        val s = when {
            next.activeYear != data.activeYear -> Nav.afterYearSwitch(stack)
            next.activeClass != data.activeClass -> Nav.afterClassSwitch(stack)
            else -> stack
        }
        _state.value = stateFor(next, s, undoStack.size).copy(historyStep = historyStep)
        _recent.value = recentAfter(_recent.value, _state.value)
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
        historyStep++
        val d = data
        val year = snap.years.firstOrNull { it.id == d.activeYear }
        // Undoing a delete of the open year opens it again, rather than staying on the year Home fell back to.
        val reopens = d.years.none { it.id == snap.activeYear } && snap.years.any { it.id == snap.activeYear }
        val keep = !reopens && year != null && (d.activeClass.isEmpty() || year.classes.any { it.id == d.activeClass })
        publish(
            if (keep) d.copy(years = snap.years)
            else d.copy(years = snap.years, activeYear = snap.activeYear, activeClass = snap.activeClass),
        )
    }

    /**
     * Undoes the latest step only if it is still the one taken at [step] ([UiState.historyStep]). Home and Settings
     * have no undo button, so their snackbar offers it; by the time it is tapped, another edit may have come after.
     */
    fun undoIfLatest(step: Long) {
        if (step == historyStep) undo()
    }

    private fun clearHistory() {
        undoStack.clear()
        lastTag = null
        historyStep++
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

    /** Shows another school year on Home. Another year's edits can no longer be undone from here. */
    fun selectYear(id: String) {
        if (id == data.activeYear || data.years.none { it.id == id }) return
        clearHistory()
        publish(data.copy(activeYear = id, activeClass = ""), Nav.root)
    }

    /** Opens another year from the School year screen's switcher, and stays on School year for it. */
    fun switchYear(id: String) {
        if (id == data.activeYear || data.years.none { it.id == id }) return
        clearHistory()
        publish(data.copy(activeYear = id, activeClass = ""))
        publish(data, Nav.push(_state.value.stack, Screen.YearSettings))
    }

    fun openClass(id: String) {
        if (id != data.activeClass) publish(data.copy(activeClass = id))
        open(Screen.Class)
    }

    /** Opens a group of any class in the open year, from Overview or Home. */
    fun openGroup(classId: String, groupId: String) {
        if (classId != data.activeClass) publish(data.copy(activeClass = classId))
        open(Screen.Group(groupId))
    }

    /** Opens [screen] from the menu ([Nav.goTo]): back to it when it is already open underneath. */
    fun goTo(screen: Screen) {
        val stack = Nav.goTo(_state.value.stack, screen)
        if (stack != _state.value.stack) publish(data, stack)
    }

    /** Opens a class from the menu. */
    fun goToClass(id: String) {
        if (id != data.activeClass) publish(data.copy(activeClass = id))
        goTo(Screen.Class)
    }

    /** Opens a group of any class in the open year from the menu's search or the group switcher. */
    fun goToGroup(classId: String, groupId: String) {
        if (classId != data.activeClass) publish(data.copy(activeClass = classId))
        goTo(Screen.Group(groupId))
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
    fun setWelcomeSeen() = editSettings { it.copy(welcomeSeen = true) }
    fun setName(v: String) = editSettings { it.copy(name = v) }
    fun markHintSeen(id: String) = editSettings { Hints.markSeen(it, id) }
    fun showTipsAgain() = editSettings { Hints.reset(it) }

    // ---- school years ------------------------------------------------------

    /** Creates a year with no classes and shows it on Home. With [holidays], the Greek public holidays it touches come as days off. */
    fun newYear(name: String, start: LocalDate?, end: LocalDate?, holidays: Boolean) {
        val days = if (holidays && start != null && end != null) {
            greekHolidays(start, end).map { (d, l) -> Closure(newId(), l, d, d, ALL_GROUPS) }
        } else {
            emptyList()
        }
        val y = SchoolYear(newId(), name.ifBlank { "School year" }, start, end, days, emptyList())
        clearHistory()
        edit { it.copy(years = it.years + y, activeYear = y.id, activeClass = "") }
    }

    /** Renames any year, from Home or from its settings. */
    fun renameYear(id: String, name: String) = edit("yname:$id") { d ->
        d.copy(years = d.years.map { if (it.id == id) it.copy(name = name) else it })
    }

    /**
     * Deletes any year, from Home or from its settings. Deleting the open year returns to Home, which shows the year
     * that starts last; Home's snackbar offers undo, which opens the deleted year again.
     */
    fun deleteYear(id: String) = edit { d ->
        val open = d.activeYear == id
        d.copy(
            years = d.years.filter { it.id != id },
            activeYear = if (open) "" else d.activeYear,
            activeClass = if (open) "" else d.activeClass,
        )
    }

    // ---- back up and restore -----------------------------------------------

    /** The file Export writes: every school year, in the format of the saved file. */
    fun exportText(): String = JsonCodec.encode(data)

    /** Puts imported years beside the ones here, in one undo step. Clashing ids and names are changed ([addYears]). */
    fun addImportedYears(years: List<SchoolYear>) = edit { d -> d.copy(years = addYears(d.years, years)) }

    /** Makes the imported years the only ones, in one undo step. No year stays open. */
    fun replaceAllYears(years: List<SchoolYear>) = edit { d -> d.copy(years = years, activeYear = "", activeClass = "") }

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
    /** Ignores a pace the file can't store (infinite or not a number). */
    fun setPace(v: Double) {
        if (v.isFinite() && v >= 0) editClass("class:pace") { it.copy(pace = v) }
    }

    // ---- groups ------------------------------------------------------------

    fun addGroup() = editClass { c ->
        val name = "Group " + ('A' + c.groups.size)
        c.copy(groups = c.groups + Group(newId(), name, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), data.year?.start))
    }

    /** Removes the group with its homework and the days off and tests aimed only at it. */
    fun removeGroup(id: String) = editYear { y ->
        y.copy(
            closures = y.closures.filter { it.who != id },
            classes = y.classes.map { c ->
                if (c.id != data.activeClass) c
                else c.copy(
                    groups = c.groups.filter { it.id != id },
                    homework = c.homework.filter { it.groupId != id },
                    events = c.events.filter { it.who != id },
                )
            },
        )
    }

    fun renameGroup(id: String, name: String) = editGroup(id, "gname:$id") { it.copy(name = name) }

    fun toggleDay(id: String, day: DayOfWeek) = editGroup(id) { g ->
        g.copy(days = if (day in g.days) g.days - day else g.days + day)
    }

    fun setNextLesson(id: String, date: LocalDate?) = editGroup(id, "gfrom:$id") { it.copy(from = date) }

    fun setPosition(groupId: String, bookId: String, page: Int) =
        editGroup(groupId, "pos:$groupId:$bookId") { it.copy(pos = it.pos + (bookId to page.coerceAtMost(MAX_PAGE + 1))) }

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
        if (g.done.any { it.date == date }) r.plan.unmarkLesson(g, date) else r.markDone(setOf(date))
    }

    /** Marks the lesson on [date] done as having covered [pages] (book id to pages), in place of the planned ones. */
    fun recordLesson(groupId: String, date: LocalDate, pages: Map<String, List<Int>>) = editLessons(groupId) { r, g ->
        r.recordLesson(date, pages) ?: g
    }

    fun markDoneUpTo(groupId: String, date: LocalDate) = editLessons(groupId) { r, _ -> r.markDoneUpTo(date) }

    fun unmarkDoneFrom(groupId: String, date: LocalDate) = editLessons(groupId) { r, g -> r.plan.unmarkLessonsFrom(g, date) }

    fun markNextLessonDone(groupId: String) = editLessons(groupId) { r, g ->
        r.rows.firstOrNull()?.let { r.markDone(setOf(it.date)) } ?: g
    }

    // ---- custom plans ------------------------------------------------------

    /** Applies [change] to the group's custom plan, laid out by a report that follows it. Does nothing when it has none. */
    private fun editCustom(groupId: String, tag: String? = null, change: (GroupReport, CustomPlan) -> CustomPlan?) {
        val plan = data.plan ?: return
        val g = plan.groups.firstOrNull { it.id == groupId } ?: return
        val c = g.custom ?: return
        val next = change(GroupReport(plan, g, Source.CUSTOM), c) ?: return
        editGroup(groupId, tag) { it.copy(custom = next) }
    }

    /** Makes the group's own plan from the proposed one and makes it active. The proposed plan stays as it is. */
    fun createCustomPlan(groupId: String) = editLessons(groupId) { r, g ->
        if (g.custom != null) g else g.copy(custom = GroupReport(r.plan, g, Source.PROPOSED).toCustomPlan())
    }

    /** Switches between the group's own plan ([active] true) and the proposed one, keeping both. */
    fun setCustomActive(groupId: String, active: Boolean) = editCustom(groupId) { _, c -> c.copy(active = active) }

    /** Deletes the group's own plan; the proposed one takes over. Undo brings it back. */
    fun deleteCustomPlan(groupId: String) = editGroup(groupId) { it.copy(custom = null) }

    /**
     * Saves a lesson of the custom plan with [pages] and [note], in one undo step: lesson [lessonId], or with null
     * a new lesson at position [at].
     */
    fun savePlannedLesson(groupId: String, lessonId: String?, at: Int, pages: Map<String, List<Int>>, note: String) =
        editCustom(groupId) { _, c ->
            if (lessonId != null) c.editLesson(lessonId, pages, note)
            else PlannedLesson(newId()).let { l -> c.insertLesson(at, l).editLesson(l.id, pages, note) }
        }

    /**
     * Saves teaching slot [slot] of the group's custom plan from the "Plan this lesson" sheet: core and supplementary
     * pages, which count in the lesson, and the note, making the plan from the proposed one if the group has none.
     * One undo step, re-pacing included. Returns false, changing nothing, when a page can't go there.
     */
    fun saveLesson(groupId: String, slot: Int, save: LessonSave): Boolean {
        val plan = data.plan ?: return false
        val g = plan.groups.firstOrNull { it.id == groupId } ?: return false
        val next = GroupReport(plan, g, if (g.custom != null) Source.CUSTOM else Source.PROPOSED).saveLesson(slot, save) ?: return false
        editGroup(groupId) { it.copy(custom = next) }
        return true
    }

    fun removePlannedLesson(groupId: String, lessonId: String) = editCustom(groupId) { _, c -> c.removeLesson(lessonId) }

    fun movePlannedLesson(groupId: String, lessonId: String, to: Int) = editCustom(groupId) { _, c -> c.moveLesson(lessonId, to) }

    fun repaceFrom(groupId: String, from: Int) = editCustom(groupId) { r, _ -> r.repaceFrom(from) }

    fun tidyCustomPlan(groupId: String) = editCustom(groupId) { r, _ -> r.tidy() }

    fun fitCustomPlanToTerm(groupId: String) = editCustom(groupId) { r, _ -> r.fitToTerm() }

    /**
     * Writes a catch-up of [boost] extra pages a lesson into the group's custom plan, making one from the proposed
     * plan first if it has none, in one undo step. Lessons planned by hand don't change.
     */
    fun applyCatchUp(groupId: String, boost: Int) = editLessons(groupId) { r, g -> r.catchUpPlan(boost)?.let { g.copy(custom = it) } ?: g }

    /** Brings book [bookId] back in step over the next [over] lessons of the group's custom plan, as [applyCatchUp] does. */
    fun foldBacklog(groupId: String, bookId: String, over: Int) =
        editLessons(groupId) { r, g -> r.foldBacklog(bookId, over)?.let { g.copy(custom = it) } ?: g }

    /** Gives group [to] a copy of group [from]'s plan, replacing any it had, in one undo step. */
    fun copyCustomPlan(from: String, to: String) {
        val plan = data.plan ?: return
        val c = plan.groups.firstOrNull { it.id == from }?.custom ?: return
        val target = plan.groups.firstOrNull { it.id == to } ?: return
        editGroup(to) { it.copy(custom = plan.copyCustomPlan(c, target)) }
    }

    /** Home's Mark done: the same rule as [markNextLessonDone], for a group of any class in the open year. */
    fun markNextLessonDoneIn(classId: String, groupId: String) = editYear { it.markNextLessonDone(classId, groupId) }

    // ---- tests -------------------------------------------------------------

    fun addEvent(name: String, date: LocalDate, who: String, consumes: Boolean) = editClass { c ->
        c.copy(events = c.events + TestEvent(newId(), name.ifBlank { "Test" }, date, who, consumes))
    }

    fun removeEvent(id: String) = editClass { c -> c.copy(events = c.events.filter { it.id != id }) }

    // ---- books -------------------------------------------------------------

    /** Adds a supplementary book with the pages in [include]; its first and last pages are the lowest and highest. Ignores an unreadable list. */
    fun addSupplementaryBook(name: String, include: String) {
        val pages = parseInclude(include)?.takeIf { it.isNotEmpty() } ?: return
        editClass { c ->
            c.copy(books = c.books + Book(newId(), name.ifBlank { "Book ${c.books.size + 1}" }, pages.first(), pages.last(), supplementary = true, include = include.trim()))
        }
    }

    /** Sets a supplementary book's pages, with its first and last pages. Ignores an empty or unreadable list. */
    fun setInclude(id: String, v: String) {
        val pages = parseInclude(v)?.takeIf { it.isNotEmpty() } ?: return
        editBook(id, "binclude:$id") { it.copy(include = v.trim(), first = pages.first(), last = pages.last()) }
    }

    fun setCountInLesson(id: String, v: Boolean) = editBook(id, "bcount:$id") { it.copy(countInLesson = v) }

    fun addBook(name: String, first: Int, last: Int) = editClass { c ->
        c.copy(books = c.books + Book(newId(), name.ifBlank { "Book ${c.books.size + 1}" }, first.coerceAtMost(MAX_PAGE), last.coerceAtMost(MAX_PAGE)))
    }

    fun removeBook(id: String) = editClass { c ->
        c.copy(
            lead = c.lead?.takeIf { it != id },
            books = c.books.filter { it.id != id },
            homework = c.homework.filter { it.bookId != id },
            groups = c.groups.map { g ->
                g.copy(
                    pos = g.pos - id,
                    done = g.done.map { it.copy(pages = it.pages - id) },
                    custom = g.custom?.let { p ->
                        p.copy(lessons = p.lessons.map { l ->
                            // The lesson keeps the core pages the teacher chose; with nothing left counting in it, its base goes.
                            val inLesson = l.inLesson - id
                            l.copy(pages = l.pages - id, inLesson = inLesson, replacementBase = if (inLesson.isEmpty()) null else l.replacementBase?.minus(id))
                        })
                    },
                )
            },
        )
    }

    fun renameBook(id: String, name: String) = editBook(id, "bname:$id") { it.copy(name = name) }
    fun setFirstPage(id: String, v: Int) = editBook(id, "bfirst:$id") { it.copy(first = v.coerceAtMost(MAX_PAGE)) }
    fun setLastPage(id: String, v: Int) = editBook(id, "blast:$id") { it.copy(last = v.coerceAtMost(MAX_PAGE)) }
    fun setSkip(id: String, v: String) = editBook(id, "bskip:$id") { it.copy(skip = v) }
    fun setDue(id: String, v: LocalDate?) = editBook(id, "bdue:$id") { it.copy(due = v) }
    fun setUnits(id: String, v: String) = editBook(id, "bunits:$id") { it.copy(units = v) }

    fun setLagLessons(v: Int) = editClass("class:lag") { it.copy(lagLessons = v.coerceIn(1, MAX_LAG_LESSONS)) }

    /** Makes book [id] the class's lead book, or with null lets every book keep its own pace. */
    fun setLead(id: String?) = editClass { it.copy(lead = id?.takeIf { b -> it.books.any { book -> book.id == b && !book.supplementary } }) }

    // ---- homework ----------------------------------------------------------

    fun addHomework(groupId: String, bookId: String, from: Int, to: Int, setOn: LocalDate?) = editClass { c ->
        // Supplementary books take no homework in this version.
        if (c.books.any { it.id == bookId && it.supplementary }) return@editClass c
        c.copy(homework = c.homework + Homework(newId(), groupId, bookId, minOf(from, to).coerceAtMost(MAX_PAGE), maxOf(from, to).coerceAtMost(MAX_PAGE), setOn))
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
