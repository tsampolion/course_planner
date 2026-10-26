package gr.edusuite.pacer.ui

import gr.edusuite.pacer.update.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.withContext
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import gr.edusuite.pacer.data.JsonCodec
import gr.edusuite.pacer.data.PlanStore
import gr.edusuite.pacer.data.SaveResult
import gr.edusuite.pacer.data.SettingsPort
import gr.edusuite.pacer.data.SettingsStore
import gr.edusuite.pacer.data.addYears
import gr.edusuite.pacer.engine.pendingUnitRevisions
import gr.edusuite.pacer.engine.withUnitRevision
import gr.edusuite.pacer.engine.withoutUnitRevision
import gr.edusuite.pacer.model.UnitRevision
import gr.edusuite.pacer.model.RevisionKind
import gr.edusuite.pacer.engine.withExtra
import gr.edusuite.pacer.engine.withAssignment
import gr.edusuite.pacer.engine.withLinkedAssignment
import gr.edusuite.pacer.engine.withoutAssignment
import gr.edusuite.pacer.engine.withoutExtra
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.LessonSave
import gr.edusuite.pacer.engine.Source
import gr.edusuite.pacer.engine.collapse
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
import gr.edusuite.pacer.engine.customAtRate
import gr.edusuite.pacer.engine.toCustomPlan
import gr.edusuite.pacer.engine.unmarkLesson
import gr.edusuite.pacer.engine.unmarkLessonsFrom
import gr.edusuite.pacer.engine.greekHolidays
import gr.edusuite.pacer.engine.markDone
import gr.edusuite.pacer.engine.markDoneUpTo
import gr.edusuite.pacer.engine.UntickedGroup
import gr.edusuite.pacer.engine.recordNotTaught
import gr.edusuite.pacer.engine.settlePast
import gr.edusuite.pacer.engine.unticked
import gr.edusuite.pacer.engine.recordLesson
import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.BookType
import gr.edusuite.pacer.model.Closure
import gr.edusuite.pacer.model.withBookDue
import gr.edusuite.pacer.model.withBookRemoved
import gr.edusuite.pacer.model.withClosureEdited
import gr.edusuite.pacer.model.withType
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.ExtraLesson
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.MAX_LAG_LESSONS
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.SchoolClass
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.model.ClassCollection
import gr.edusuite.pacer.model.ClassOrder
import gr.edusuite.pacer.model.withCollection
import gr.edusuite.pacer.model.withoutCollection
import gr.edusuite.pacer.model.PlanGrouping
import gr.edusuite.pacer.model.PaletteChoice
import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.ThemeMode
import gr.edusuite.pacer.model.newId
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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

/** Why an edit may not be on disk. */
enum class SaveProblem {
    /** A write failed; Retry, the next edit and leaving the app try again. */
    NotSaved,
    /** The file couldn't be read at launch, so nothing is written over it. */
    ReadLocked,
    /** As [ReadLocked], and the file can be read now, but edits made meanwhile would overwrite it. */
    LockedWithEdits,
}

/** Session-only feedback from an explicit update check. */
enum class UpdateCheckState { IDLE, CHECKING, UP_TO_DATE, FAILED }

/** What undo restores: every year, and which year and class were open. */
private data class Snapshot(val years: List<SchoolYear>, val activeYear: String, val activeClass: String)

/**
 * Holds the school years, applies every edit, and keeps an undo history.
 * Edits to the same field within [COALESCE_MS] collapse into one undo step,
 * so typing a page number is one step, not six. Moving between screens is not an edit,
 * and opening another school year starts a fresh history.
 */
class PacerViewModel(
    private val store: PlanStore,
    private val settingsStore: SettingsPort,
    private val updates: UpdateChecker,
    /** Where background work runs; tests pass their own, the app uses [viewModelScope]. */
    scopeOverride: CoroutineScope? = null,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    /** A millisecond clock for merging edits to one field into one undo step. */
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    /** The calendar date; tests pass their own so a day can pass without waiting. */
    private val dateNow: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val scope: CoroutineScope = scopeOverride ?: viewModelScope
    private val loaded = store.load()
    private val _today = MutableStateFlow(dateNow())
    /** The current date, for every screen and for the engine's callers. Kept fresh by [refreshToday]. */
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** Re-reads the date: on resume, at midnight, and when the system changes the date or time zone. */
    fun refreshToday() {
        _today.value = dateNow()
        settleNow()
    }

    private val _pastLessons = MutableStateFlow<List<UntickedGroup>?>(null)
    /** The groups with lessons left unticked in the past, while the first-launch question is open. Null otherwise. */
    val pastLessons: StateFlow<List<UntickedGroup>?> = _pastLessons.asStateFlow()

    /**
     * Lessons tick themselves once their day has passed (`docs/AUTO_TICK_BRIEF.md`). Until the teacher has answered the
     * first-launch question nothing is ticked; a year with no lesson left unticked in the past never needs to ask.
     * Ticking this way is not an undo step: it is what the schedule says, not an edit.
     */
    private fun settleNow() {
        val year = data.year ?: return
        if (_settings.value.autoTickFrom == null) {
            val left = year.unticked(_today.value)
            if (left.isEmpty()) editSettings { it.copy(autoTickFrom = AUTO_TICK_ALL) } else _pastLessons.value = left
            if (left.isNotEmpty()) return
        }
        _pastLessons.value = null
        val next = settled(data)
        if (next != data) publish(next)
    }

    /** [d] with the open year's past lessons ticked, as far as the teacher's first-launch answer allows. */
    private fun settled(d: AppData): AppData {
        val from = _settings.value.autoTickFrom ?: return d
        val year = d.year ?: return d
        val next = year.settlePast(_today.value, from)
        return if (next === year) d else d.copy(years = d.years.map { if (it.id == year.id) next else it })
    }

    /** Answers the first-launch question: tick every past lesson, or leave the ones already there and tick from today on. */
    fun answerPastLessons(tickThem: Boolean) {
        editSettings { it.copy(autoTickFrom = if (tickThem) AUTO_TICK_ALL else _today.value) }
        settleNow()
    }

    private val _state = MutableStateFlow(withDefaultYear(loaded.data).let { d -> stateFor(d, Nav.launch, 0) })
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Where an unreadable plans file was moved at launch, until the teacher closes the notice about it. */
    private val _setAside = MutableStateFlow(loaded.setAside)
    val setAside: StateFlow<File?> = _setAside.asStateFlow()

    fun dismissSetAside() {
        _setAside.value = null
    }

    /** Why the latest edits may not be on disk, until a write succeeds or the file is read again. Null when all is saved. */
    private val _saveProblem = MutableStateFlow(if (loaded.readFailed) SaveProblem.ReadLocked else null)
    val saveProblem: StateFlow<SaveProblem?> = _saveProblem.asStateFlow()

    /** Groups opened this session, latest first, for the group switcher. Not saved: a new session starts empty. */
    private val _recent = MutableStateFlow(recentAfter(emptyList(), _state.value))
    val recent: StateFlow<List<GroupRef>> = _recent.asStateFlow()

    private fun recentAfter(recent: List<GroupRef>, ui: UiState): List<GroupRef> {
        val id = ui.screen.groupId ?: return recent
        return Nav.recent(recent, GroupRef(ui.data.activeClass, id))
    }

    private val _settings = MutableStateFlow(settingsStore.load().let { before ->
        updates.atLaunch(before).also { after -> if (after !== before) settingsStore.save(after) }
    })
    val settings: StateFlow<Settings> = _settings.asStateFlow()
    val update: StateFlow<Update?> = settings.map(updates::pending)
        .stateIn(scope, SharingStarted.Eagerly, updates.pending(_settings.value))
    private val _updateCheck = MutableStateFlow(UpdateCheckState.IDLE)
    val updateCheck: StateFlow<UpdateCheckState> = _updateCheck.asStateFlow()
    val updatesEnabled: Boolean = updates.enabled
    val installedVersion: String = updates.installed

    private val undoStack = ArrayDeque<Snapshot>()
    private var lastTag: String? = null
    private var lastTime = 0L
    private var saveJob: Job? = null
    /** Counts edits; written on the main thread only. An edit is on disk once [savedRevision] has reached its number. */
    @Volatile private var revision = 0L
    private var savedRevision = 0L
    private val saveLock = Any()
    private var historyStep = 0L

    private val data get() = _state.value.data

    private val revisionPrompts = UnitRevisionPrompts()
    private val _unitRevision = MutableStateFlow<UnitRevisionTarget?>(null)
    val unitRevision: StateFlow<UnitRevisionTarget?> = _unitRevision.asStateFlow()

    fun dismissUnitRevision(target: UnitRevisionTarget) {
        revisionPrompts.dismiss(target)
        _unitRevision.value = revisionPrompts.current
    }
    fun openUnitRevision(groupId: String, unitIndex: Int) {
        revisionPrompts.open(UnitRevisionTarget(data.activeClass, groupId, unitIndex))
        _unitRevision.value = revisionPrompts.current
    }
    fun saveUnitRevision(target: UnitRevisionTarget, kind: RevisionKind, date: LocalDate?, pages: Map<String, List<Int>>, title: String): Boolean {
        val y = data.year ?: return false
        val c = y.classes.firstOrNull { it.id == target.classId } ?: return false
        val g = c.groups.firstOrNull { it.id == target.groupId } ?: return false
        val report = GroupReport(y.planFor(c, today.value), g)
        if (kind != RevisionKind.NONE && date != null && date !in revisionDates(report, target.unitIndex, report.plan.today)) return false
        if (kind == RevisionKind.PAGES && !validRevisionPages(report.plan, pages)) return false
        if (pages.any { (id, ps) -> report.plan.books.firstOrNull { it.id == id }?.let { assignmentPages(ps.joinToString(","), it) == null } != false }) return false
        val old = g.unitRevisions.firstOrNull { it.unitIndex == target.unitIndex }?.assignmentId?.let { id -> g.assignments.firstOrNull { it.id == id } }
        val assignment = revisionAssignment(old, kind, date, pages, title)
        val changed = editYear { year -> year.withUnitRevision(target, UnitRevision(target.unitIndex, kind, pages = pages), assignment) }
        dismissUnitRevision(target)
        return changed
    }
    fun removeUnitRevision(target: UnitRevisionTarget): Boolean {
        val changed = editYear { y -> y.copy(classes = y.classes.map { c -> if (c.id != target.classId) c else c.copy(groups = c.groups.map { g -> if (g.id == target.groupId) g.withoutUnitRevision(target.unitIndex) else g }) }) }
        dismissUnitRevision(target)
        return changed
    }

    init {
        if (updates.shouldAutoCheck(_settings.value)) scope.launch {
            val fetched = withContext(io) { updates.fetch() }
            editSettings { updates.applied(it, fetched) }
        }
    }

    // ---- plumbing ----------------------------------------------------------

    private fun edit(tag: String? = null, change: (AppData) -> AppData): Boolean {
        val before = data
        val after = change(before)
        if (after == before) return false
        val now = clock()
        if (!(tag != null && tag == lastTag && now - lastTime < COALESCE_MS)) {
            undoStack.addLast(Snapshot(before.years, before.activeYear, before.activeClass))
            if (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
            historyStep++
        }
        lastTag = tag
        lastTime = now
        publish(after)
        return true
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
        val next = settled(withDefaultYear(proposed))
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
        revision++
        saveJob?.cancel()
        saveJob = scope.launch(io) {
            delay(SAVE_DEBOUNCE_MS)
            saveNow()
        }
    }

    /**
     * Writes the newest state if it isn't on disk yet. The revision is read before the data, and the state is
     * published before the revision is bumped, so the data is never older than the revision it is recorded under;
     * a success then clears only that revision, never a newer edit that arrived during the write.
     */
    private fun saveNow() = synchronized(saveLock) {
        val rev = revision
        if (rev <= savedRevision) return@synchronized
        when (store.save(data)) {
            SaveResult.Saved -> {
                savedRevision = rev
                if (savedRevision >= revision && _saveProblem.value == SaveProblem.NotSaved) _saveProblem.value = null
            }
            SaveResult.Failed -> _saveProblem.value = SaveProblem.NotSaved
            SaveResult.Locked -> if (_saveProblem.value == null) _saveProblem.value = SaveProblem.ReadLocked
        }
    }

    /**
     * Writes now, on the calling thread, whatever the state of the debounced save. Called when the app goes to the
     * background, where the process may be killed before a debounced save runs. The file is a few kilobytes.
     */
    fun flush() {
        saveJob?.cancel()
        saveNow()
    }

    /** The Retry of the "not saved" notice. */
    fun retrySave() = flush()

    /**
     * The "Try reading again" of the locked-read notice. If the file reads and nothing was edited since launch,
     * its data opens and saving resumes. If edits were made meanwhile, saving stays off: either the file or those
     * edits would be lost, so the teacher exports a copy and reopens the app.
     */
    fun retryRead() {
        val problem = _saveProblem.value
        if (problem != SaveProblem.ReadLocked && problem != SaveProblem.LockedWithEdits) return
        val again = store.reload()
        if (again.readFailed) return
        synchronized(saveLock) {
            if (revision != savedRevision) {
                _saveProblem.value = SaveProblem.LockedWithEdits
                return
            }
            store.unlock()
            _saveProblem.value = null
        }
        again.setAside?.let { _setAside.value = it }
        _state.value = stateFor(withDefaultYear(again.data), Nav.launch, 0).copy(historyStep = historyStep)
        _recent.value = recentAfter(emptyList(), _state.value)
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

    fun setAutoUpdateCheck(on: Boolean) = editSettings { it.copy(autoUpdateCheck = on) }

    /** Settings → Check now ignores the daily throttle and switch. */
    fun checkForUpdates() {
        if (!updates.enabled || _updateCheck.value == UpdateCheckState.CHECKING) return
        _updateCheck.value = UpdateCheckState.CHECKING
        scope.launch {
            val fetched = withContext(io) { updates.fetch() }
            editSettings { updates.applied(it, fetched) }
            _updateCheck.value = when (fetched) {
                is Fetched.Found -> UpdateCheckState.IDLE
                Fetched.UpToDate -> UpdateCheckState.UP_TO_DATE
                Fetched.Failed -> UpdateCheckState.FAILED
            }
        }
    }

    fun setClassOrder(v: ClassOrder) = editSettings { it.copy(classOrder = v) }

    fun setPlanGrouping(v: PlanGrouping) = editSettings { it.copy(planGrouping = v) }

    fun setPalette(v: PaletteChoice) = editSettings { it.copy(palette = v) }
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

    /** Changes one of the group's own days off in a single undo step. False when it isn't the group's or the dates are reversed. */
    fun editClosure(id: String, groupId: String, label: String, from: LocalDate, to: LocalDate?): Boolean =
        editYear { y -> y.withClosureEdited(id, groupId, label, from, to) }

    fun removeClosure(id: String) = editYear { y -> y.copy(closures = y.closures.filter { it.id != id }) }

    /** Adds the Greek public holidays the term touches. Returns how many were new. */
    fun addGreekHolidays(groupId: String? = null): Int {
        val y = data.year ?: return 0
        val start = y.start ?: return 0
        val end = y.end ?: return 0
        val c = data.schoolClass
        if (groupId != null && c?.groups?.none { it.id == groupId } != false) return 0
        val scopes = if (groupId == null) setOf(ALL_GROUPS) else setOf(ALL_GROUPS, c!!.id, groupId)
        val have = y.closures.filter { it.who in scopes }.map { it.from to it.label }.toHashSet()
        val fresh = greekHolidays(start, end).filter { have.add(it) }
        if (fresh.isNotEmpty()) {
            editYear { it.copy(closures = it.closures + fresh.map { (d, l) -> Closure(newId(), l, d, d, groupId ?: ALL_GROUPS) }) }
        }
        return fresh.size
    }

    // ---- classes -----------------------------------------------------------

    /** Adds a class with one group and the lesson length and pace from Settings, and opens it. */
    fun newClass(name: String, unitsEnabled: Boolean = false) {
        val y = data.year ?: return
        val s = _settings.value
        val group = Group(newId(), "Group A", setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), y.start)
        val c = SchoolClass(newId(), name.ifBlank { "Class ${y.classes.size + 1}" }, s.lessonMins, s.pace, listOf(group), emptyList(), emptyList(), emptyList(), unitsEnabled = unitsEnabled)
        edit { d -> d.copy(years = d.years.map { if (it.id == y.id) it.copy(classes = it.classes + c) else it }, activeClass = c.id) }
        open(Screen.Class)
    }

    fun addCollection(name: String, classIds: Set<String>): Boolean {
        if (name.isBlank()) return false
        val value = ClassCollection(newId(), name, classIds)
        return editYear { it.withCollection(value) }
    }

    fun updateCollection(id: String, name: String, classIds: Set<String>): Boolean {
        if (name.isBlank()) return false
        return editYear { y -> if (y.collections.none { it.id == id }) y else y.withCollection(ClassCollection(id, name, classIds)) }
    }

    fun removeCollection(id: String) = editYear { it.withoutCollection(id) }

    fun renameClass(name: String) = editClass("cname") { it.copy(name = name) }

    /** Deletes the open class with the days off aimed only at it or its groups. */
    fun deleteClass() = edit { d ->
        val c = d.schoolClass ?: return@edit d
        val gone = c.groups.mapTo(hashSetOf(c.id)) { it.id }
        d.copy(
            years = d.years.map { y ->
                if (y.id != d.activeYear) y
                else y.copy(classes = y.classes.filter { it.id != c.id }, closures = y.closures.filter { it.who !in gone },
                    collections = y.collections.map { it.copy(classIds = it.classIds - c.id) })
            },
            activeClass = "",
        )
    }

    fun setMins(v: Int) = editClass("class:mins") { it.copy(mins = v) }
    /** Ignores a pace the file can't store (infinite or not a number). */
    fun setPace(v: Double) {
        if (v.isFinite() && v >= 0) editClass("class:pace") { it.copy(pace = v) }
    }

    fun setUnitsEnabled(value: Boolean) = editClass { it.copy(unitsEnabled = value) }

    fun setGroupUnitsOverride(groupId: String, value: Boolean?) = editGroup(groupId) { it.copy(unitsOverride = value) }

    fun setBookLinks(links: List<Set<String>>) = editClass { it.copy(bookLinks = links) }

    fun saveUnitStarts(bookId: String, rows: List<String>): Boolean {
        val book = data.schoolClass?.books?.firstOrNull { it.id == bookId && !it.supplementary } ?: return false
        val starts = validateUnitStarts(book.first, book.last, rows) ?: return false
        return editBook(bookId) { it.copy(units = starts.joinToString(", ")) }
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

    fun setGroupEnd(id: String, date: LocalDate?) = editGroup(id, "gend:$id") { it.copy(end = date) }

    fun setGroupSkip(groupId: String, bookId: String, value: String) = editGroup(groupId, "gskip:$groupId:$bookId") { g ->
        if (data.schoolClass?.books?.none { it.id == bookId } != false) g
        else g.copy(skip = if (value.isBlank()) g.skip - bookId else g.skip + (bookId to value))
    }

    fun setGroupExtraBook(groupId: String, bookId: String, selected: Boolean) = editGroup(groupId) { g ->
        if (data.schoolClass?.books?.none { it.id == bookId && it.groupOnly } != false) g
        else g.copy(extraBooks = if (selected) g.extraBooks + bookId else g.extraBooks - bookId)
    }

    /**
     * Stops [groupId] using the default class book [bookId], or brings it back. Its taught pages and position stay.
     * False when refused (an optional book, or the last paced book) or when nothing changed.
     */
    fun setGroupBookRemoved(groupId: String, bookId: String, removed: Boolean): Boolean {
        val c = data.schoolClass ?: return false
        val next = c.groups.firstOrNull { it.id == groupId }?.withBookRemoved(c.books, bookId, removed) ?: return false
        return editGroup(groupId) { next }
    }

    /** Sets this group's own finish-by date for a book, or with null returns it to the class's date. */
    fun setGroupBookDue(groupId: String, bookId: String, date: LocalDate?): Boolean =
        editGroup(groupId) { g -> g.withBookDue(data.schoolClass?.books.orEmpty(), bookId, date) }

    /** Adds one shared optional definition and selects it only for the intended group, in one undo step. */
    fun addGroupBook(groupId: String, name: String, first: Int, last: Int): Boolean {
        if (name.isBlank() || first !in 1..MAX_PAGE || last !in first..MAX_PAGE) return false
        val book = Book(newId(), name.trim(), first, last, groupOnly = true)
        return editClass { c ->
            if (c.groups.none { it.id == groupId }) c
            else c.copy(books = c.books + book, groups = c.groups.map { if (it.id == groupId) it.copy(extraBooks = it.extraBooks + book.id) else it })
        }
    }

    fun setPosition(groupId: String, bookId: String, page: Int) =
        editGroup(groupId, "pos:$groupId:$bookId") { it.copy(pos = it.pos + (bookId to page.coerceAtMost(MAX_PAGE + 1))) }

    /** Starts every group over: first pages, the year's start, and no lessons marked done. */
    fun resetPositions() = editClass { c ->
        c.copy(groups = c.groups.map { it.copy(pos = emptyMap(), from = data.year?.start, done = emptyList()) })
    }

    // ---- done lessons ------------------------------------------------------

    /** The open class as the engine sees it on the app's shared [today]. */
    private fun openPlan(): Plan? = data.plan(today.value)

    /** Applies [change] to a group of the open class, given its current schedule. */
    private fun editLessons(groupId: String, change: (GroupReport, Group) -> Group): Boolean {
        val plan = openPlan() ?: return false
        val g = plan.groups.firstOrNull { it.id == groupId } ?: return false
        val next = change(GroupReport(plan, g), g)
        val changed = editGroup(groupId) { next }
        return changed
    }

    /** Records the lesson on [date] as not taught: it counts as done with no pages, and its pages move to later lessons. */
    fun lessonNotTaught(groupId: String, date: LocalDate) = editLessons(groupId) { r, g -> r.recordNotTaught(date) ?: g }

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

    // ---- date-pinned extra lessons -----------------------------------------

    fun addExtra(groupId: String, date: LocalDate, pages: Map<String, List<Int>>): Boolean {
        val plan = openPlan() ?: return false
        return editGroup(groupId) { g ->
            if (g.extras.any { it.date == date } || g.done.any { it.date == date }) g
            else plan.withExtra(g, ExtraLesson(newId(), date, pages))
        }
    }

    fun saveExtra(groupId: String, extraId: String, pages: Map<String, List<Int>>): Boolean {
        val plan = openPlan() ?: return false
        return editGroup(groupId) { g ->
            g.extras.firstOrNull { it.id == extraId }?.let { plan.withExtra(g, it.copy(pages = pages)) } ?: g
        }
    }

    fun removeExtra(groupId: String, extraId: String): Boolean {
        val plan = openPlan() ?: return false
        return editGroup(groupId) { plan.withoutExtra(it, extraId) }
    }

    // ---- custom plans ------------------------------------------------------

    /** Applies [change] to the group's custom plan, laid out by a report that follows it. Does nothing when it has none. */
    private fun editCustom(groupId: String, tag: String? = null, change: (GroupReport, CustomPlan) -> CustomPlan?): Boolean {
        val plan = openPlan() ?: return false
        val g = plan.groups.firstOrNull { it.id == groupId } ?: return false
        val c = g.custom ?: return false
        val next = change(GroupReport(plan, g, Source.CUSTOM), c) ?: return false
        return editGroup(groupId, tag) { it.copy(custom = next) }
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
        val plan = openPlan() ?: return false
        val g = plan.groups.firstOrNull { it.id == groupId } ?: return false
        val next = GroupReport(plan, g, if (g.custom != null) Source.CUSTOM else Source.PROPOSED).saveLesson(slot, save) ?: return false
        editGroup(groupId) { it.copy(custom = next) }
        return true
    }

    fun removePlannedLesson(groupId: String, lessonId: String) = editCustom(groupId) { _, c -> c.removeLesson(lessonId) }

    fun movePlannedLesson(groupId: String, lessonId: String, to: Int) = editCustom(groupId) { _, c -> c.moveLesson(lessonId, to) }

    fun repaceFrom(groupId: String, from: Int) = editCustom(groupId) { r, _ -> r.repaceFrom(from) }

    /** Applies a chosen pace in one undo step, creating and activating the custom plan when needed. */
    fun applyOwnPace(groupId: String, rate: Double): Boolean {
        if (!rate.isFinite() || rate <= 0.0) return false
        return editLessons(groupId) { r, g ->
            try { g.copy(custom = r.customAtRate(rate)) } catch (_: IllegalArgumentException) { g }
        }
    }

    /** Fits overflow first, then fills missing pages, preserving touched lessons in one undo step. */
    fun repaceCustomPlanToFit(groupId: String) = editCustom(groupId) { r, _ ->
        r.fitToTerm()?.let { fitted ->
            GroupReport(r.plan, r.group.copy(custom = fitted), Source.CUSTOM).repaceFrom(0)
        }
    }

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
    fun copyCustomPlan(from: String, to: String): Boolean {
        val plan = openPlan() ?: return false
        val c = plan.groups.firstOrNull { it.id == from }?.custom ?: return false
        val target = plan.groups.firstOrNull { it.id == to } ?: return false
        return editGroup(to) { it.copy(custom = plan.copyCustomPlan(c, target)) }
    }

    /** Home's Mark done: the same rule as [markNextLessonDone], for a group of any class in the open year. */
    fun markNextLessonDoneIn(classId: String, groupId: String): Boolean {
        val before = data.year?.classes?.firstOrNull { it.id == classId }?.groups?.firstOrNull { it.id == groupId } ?: return false
        val changed = editYear { it.markNextLessonDone(classId, groupId, today.value) }
        return changed
    }

    // ---- assignments -------------------------------------------------------

    fun saveAssignment(groupId: String, assignment: Assignment): Boolean = editGroup(groupId) { g ->
        g.withAssignment(assignment)
    }

    fun saveAssignmentPair(groupId: String, originalId: String, companion: Assignment): Boolean = editGroup(groupId) {
        it.withLinkedAssignment(originalId, companion)
    }

    fun removeAssignment(groupId: String, id: String): Boolean = editGroup(groupId) { g ->
        g.withoutAssignment(id)
    }

    // ---- books -------------------------------------------------------------

    /** Adds a supplementary book with the pages in [include]; its first and last pages are the lowest and highest. Ignores an unreadable list. */
    fun addSupplementaryBook(name: String, include: String) {
        val pages = parseInclude(include)?.takeIf { it.isNotEmpty() } ?: return
        editClass { c ->
            c.copy(books = c.books + Book(newId(), name.ifBlank { "Book ${c.books.size + 1}" }, pages.first(), pages.last(), supplementary = true, include = include.trim(), type = BookType.SUPPLEMENTARY))
        }
    }

    /** Sets a supplementary book's pages, with its first and last pages. Ignores an empty or unreadable list. */
    fun setInclude(id: String, v: String) {
        val pages = parseInclude(v)?.takeIf { it.isNotEmpty() } ?: return
        editBook(id, "binclude:$id") { it.copy(include = v.trim(), first = pages.first(), last = pages.last()) }
    }

    fun setCountInLesson(id: String, v: Boolean) = editBook(id, "bcount:$id") { it.copy(countInLesson = v) }

    fun addBook(name: String, first: Int, last: Int, type: BookType = BookType.UNCLASSIFIED) = editClass { c ->
        c.copy(books = c.books + Book(newId(), name.ifBlank { "Book ${c.books.size + 1}" }, first.coerceAtMost(MAX_PAGE), last.coerceAtMost(MAX_PAGE), type = type))
    }

    /**
     * Sets a book's type. Choosing Supplementary makes it one (its pages are hand-placed), listing its current pages as
     * the ones to use; choosing anything else makes it an ordinary paced book again.
     */
    fun setBookType(id: String, type: BookType) = editBook(id) { b ->
        val typed = b.withType(type)
        val pages = b.allPages()
        if (typed.supplementary && typed.include.isBlank() && pages.isNotEmpty()) typed.copy(include = collapse(pages)) else typed
    }

    fun removeBook(id: String) = editClass { c ->
        c.copy(
            lead = c.lead?.takeIf { it != id },
            books = c.books.filter { it.id != id },
            homework = c.homework.filter { it.bookId != id },
            groups = c.groups.map { g ->
                g.copy(
                    pos = g.pos - id,
                    skip = g.skip - id,
                    extraBooks = g.extraBooks - id,
                    done = g.done.map { it.copy(pages = it.pages - id) },
                    extras = g.extras.map { it.copy(pages = it.pages - id) },
                    assignments = g.assignments.map { it.copy(pages = it.pages - id) },
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

    class Factory(private val store: PlanStore, private val settings: SettingsStore, private val updates: UpdateChecker) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PacerViewModel(store, settings, updates) as T
    }

    private companion object {
        /** [gr.edusuite.pacer.model.Settings.autoTickFrom] when every past lesson may be ticked. */
        val AUTO_TICK_ALL: LocalDate = LocalDate.of(1970, 1, 1)
        const val COALESCE_MS = 1200L
        const val UNDO_LIMIT = 60
        const val SAVE_DEBOUNCE_MS = 400L
    }
}
