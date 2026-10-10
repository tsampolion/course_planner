package com.coursebook.pacer.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coursebook.pacer.data.PlanStore
import com.coursebook.pacer.engine.GroupReport
import com.coursebook.pacer.engine.greekHolidays
import com.coursebook.pacer.model.ALL_GROUPS
import com.coursebook.pacer.model.AppData
import com.coursebook.pacer.model.Book
import com.coursebook.pacer.model.Closure
import com.coursebook.pacer.model.Group
import com.coursebook.pacer.model.Homework
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.model.TestEvent
import com.coursebook.pacer.model.newId
import com.coursebook.pacer.model.seedPlan
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(val data: AppData, val undoDepth: Int)

/**
 * Holds the plans, applies every edit, and keeps an undo history.
 * Edits to the same field within [COALESCE_MS] collapse into one undo step,
 * so typing a page number is one step, not six. Switching tabs is not an edit.
 */
class PacerViewModel(private val store: PlanStore) : ViewModel() {
    private val _state = MutableStateFlow(UiState(store.load(), 0))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<Pair<List<Plan>, String>>()
    private var lastTag: String? = null
    private var lastTime = 0L
    private var saveJob: Job? = null

    private val data get() = _state.value.data
    val plan: Plan get() = data.plan

    // ---- plumbing ----------------------------------------------------------

    private fun edit(tag: String? = null, change: (AppData) -> AppData) {
        val before = data
        val after = change(before)
        if (after == before) return
        val now = SystemClock.elapsedRealtime()
        if (!(tag != null && tag == lastTag && now - lastTime < COALESCE_MS)) {
            undoStack.addLast(before.plans to before.activePlan)
            if (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
        }
        lastTag = tag
        lastTime = now
        publish(after)
    }

    private fun editPlan(tag: String? = null, change: (Plan) -> Plan) = edit(tag) { d ->
        val p = d.plan
        d.copy(plans = d.plans.map { if (it.id == p.id) change(it) else it })
    }

    private fun editGroup(id: String, tag: String? = null, change: (Group) -> Group) =
        editPlan(tag) { p -> p.copy(groups = p.groups.map { if (it.id == id) change(it) else it }) }

    private fun editBook(id: String, tag: String? = null, change: (Book) -> Book) =
        editPlan(tag) { p -> p.copy(books = p.books.map { if (it.id == id) change(it) else it }) }

    private fun publish(next: AppData) {
        _state.value = UiState(normalise(next), undoStack.size)
        scheduleSave()
    }

    /** Keeps the open tab pointing at something that still exists. */
    private fun normalise(d: AppData): AppData {
        val p = d.plan
        val view = if (d.view == AppData.VIEW_SETUP || p.groups.any { it.id == d.view }) d.view else AppData.VIEW_SETUP
        return if (p.id == d.activePlan && view == d.view) d else d.copy(activePlan = p.id, view = view)
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

    fun undo() {
        val (plans, active) = undoStack.removeLastOrNull() ?: return
        lastTag = null
        publish(data.copy(plans = plans, activePlan = active))
    }

    // ---- navigation (not undoable) -----------------------------------------

    fun openView(view: String) {
        if (view == data.view) return
        publish(data.copy(view = view))
    }

    // ---- plans -------------------------------------------------------------

    fun selectPlan(id: String) = edit { it.copy(activePlan = id, view = AppData.VIEW_SETUP) }

    fun renamePlan(name: String) = editPlan("pname") { it.copy(name = name) }

    fun newPlan() = edit { d ->
        val p = seedPlan().copy(name = "New plan")
        d.copy(plans = d.plans + p, activePlan = p.id, view = AppData.VIEW_SETUP)
    }

    fun deletePlan() = edit { d ->
        if (d.plans.size < 2) return@edit d
        val rest = d.plans.filter { it.id != d.activePlan }
        d.copy(plans = rest, activePlan = rest.first().id, view = AppData.VIEW_SETUP)
    }

    // ---- term --------------------------------------------------------------

    fun setStart(v: LocalDate?) = editPlan("term:start") { it.copy(start = v) }
    fun setEnd(v: LocalDate?) = editPlan("term:end") { it.copy(end = v) }
    fun setMins(v: Int) = editPlan("term:mins") { it.copy(mins = v) }
    fun setPace(v: Double) = editPlan("term:pace") { it.copy(pace = v) }

    // ---- groups ------------------------------------------------------------

    fun addGroup() = editPlan { p ->
        val name = "Group " + ('A' + p.groups.size)
        p.copy(groups = p.groups + Group(newId(), name, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), p.start))
    }

    fun removeGroup(id: String) = editPlan { p ->
        p.copy(groups = p.groups.filter { it.id != id }, homework = p.homework.filter { it.groupId != id })
    }

    fun renameGroup(id: String, name: String) = editGroup(id, "gname:$id") { it.copy(name = name) }

    fun toggleDay(id: String, day: DayOfWeek) = editGroup(id) { g ->
        g.copy(days = if (day in g.days) g.days - day else g.days + day)
    }

    fun setNextLesson(id: String, date: LocalDate?) = editGroup(id, "gfrom:$id") { it.copy(from = date) }

    fun setPosition(groupId: String, bookId: String, page: Int) =
        editGroup(groupId, "pos:$groupId:$bookId") { it.copy(pos = it.pos + (bookId to page)) }

    fun resetPositions() = editPlan { p -> p.copy(groups = p.groups.map { it.copy(pos = emptyMap(), from = p.start) }) }

    fun markNextLessonDone(groupId: String) = editPlan { p ->
        val g = p.groups.firstOrNull { it.id == groupId } ?: return@editPlan p
        val next = GroupReport(p, g).afterNextLesson() ?: return@editPlan p
        p.copy(groups = p.groups.map { if (it.id == groupId) next else it })
    }

    // ---- closures ----------------------------------------------------------

    fun addClosure(label: String, from: LocalDate, to: LocalDate?, who: String) = editPlan { p ->
        p.copy(closures = p.closures + Closure(newId(), label.ifBlank { "Day off" }, from, to ?: from, who))
    }

    fun removeClosure(id: String) = editPlan { p -> p.copy(closures = p.closures.filter { it.id != id }) }

    /** Adds the Greek public holidays the term touches. Returns how many were new. */
    fun addGreekHolidays(): Int {
        val p = plan
        val start = p.start ?: return 0
        val end = p.end ?: return 0
        val have = p.closures.map { it.from to it.label }.toHashSet()
        val fresh = greekHolidays(start, end).filter { have.add(it) }
        if (fresh.isNotEmpty()) {
            editPlan { it.copy(closures = it.closures + fresh.map { (d, l) -> Closure(newId(), l, d, d, ALL_GROUPS) }) }
        }
        return fresh.size
    }

    // ---- tests -------------------------------------------------------------

    fun addEvent(name: String, date: LocalDate, who: String, consumes: Boolean) = editPlan { p ->
        p.copy(events = p.events + TestEvent(newId(), name.ifBlank { "Test" }, date, who, consumes))
    }

    fun removeEvent(id: String) = editPlan { p -> p.copy(events = p.events.filter { it.id != id }) }

    // ---- books -------------------------------------------------------------

    fun addBook(name: String, first: Int, last: Int) = editPlan { p ->
        p.copy(books = p.books + Book(newId(), name.ifBlank { "Book ${p.books.size + 1}" }, first, last))
    }

    fun removeBook(id: String) = editPlan { p ->
        p.copy(
            books = p.books.filter { it.id != id },
            homework = p.homework.filter { it.bookId != id },
            groups = p.groups.map { it.copy(pos = it.pos - id) },
        )
    }

    fun renameBook(id: String, name: String) = editBook(id, "bname:$id") { it.copy(name = name) }
    fun setFirstPage(id: String, v: Int) = editBook(id, "bfirst:$id") { it.copy(first = v) }
    fun setLastPage(id: String, v: Int) = editBook(id, "blast:$id") { it.copy(last = v) }
    fun setSkip(id: String, v: String) = editBook(id, "bskip:$id") { it.copy(skip = v) }
    fun setDue(id: String, v: LocalDate?) = editBook(id, "bdue:$id") { it.copy(due = v) }

    // ---- homework ----------------------------------------------------------

    fun addHomework(groupId: String, bookId: String, from: Int, to: Int, setOn: LocalDate?) = editPlan { p ->
        p.copy(homework = p.homework + Homework(newId(), groupId, bookId, minOf(from, to), maxOf(from, to), setOn))
    }

    fun removeHomework(id: String) = editPlan { p -> p.copy(homework = p.homework.filter { it.id != id }) }

    class Factory(private val store: PlanStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PacerViewModel(store) as T
    }

    private companion object {
        const val COALESCE_MS = 1200L
        const val UNDO_LIMIT = 60
        const val SAVE_DEBOUNCE_MS = 400L
    }
}
