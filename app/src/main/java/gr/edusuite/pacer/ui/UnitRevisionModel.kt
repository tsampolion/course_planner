package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.*
import gr.edusuite.pacer.model.*
import java.time.LocalDate

/** Prompt identity includes the class: Home may tick a group outside the selected class. */
data class UnitRevisionTarget(val classId: String, val groupId: String, val unitIndex: Int)

internal fun revisionBooks(plan: Plan): List<Book> = plan.books

internal fun validRevisionPages(plan: Plan, pages: Map<String, List<Int>>): Boolean =
    pages.isNotEmpty() && pages.values.any { it.isNotEmpty() } &&
        pages.all { (id, ps) -> revisionBooks(plan).firstOrNull { it.id == id }?.let { assignmentPages(ps.joinToString(","), it) != null } == true }

internal fun revisionDates(report: GroupReport, unitIndex: Int, today: LocalDate): List<LocalDate> {
    val group = report.group.withoutUnitRevision(unitIndex)
    val candidate = GroupReport(report.plan, group)
    val end = unitPlan(candidate.plan, group)?.windows?.firstOrNull { it.index == unitIndex }?.end
    val originalEnd = unitPlan(report.plan, report.group)?.windows?.firstOrNull { it.index == unitIndex }?.end
    val assignmentId = report.group.unitRevisions.firstOrNull { it.unitIndex == unitIndex }?.assignmentId
    val reserved = report.group.assignments.firstOrNull { it.id == assignmentId }?.due
    val originalRows = report.rows.associateBy { it.date }
    val owned = report.custom?.lessons?.associateBy { it.id }.orEmpty()
    // Removing the reservation shifts custom slots. Check ownership on the existing calendar,
    // so the following manual lesson neither claims the reservation nor loses its own date.
    return candidate.rows.filter { row ->
        val boundary = if (row.date == reserved) originalEnd else end
        val original = originalRows[row.date]
        row.date > today && (boundary == null || row.date > boundary) &&
            group.done.none { it.date == row.date } && !row.blocked && row.extraId == null &&
            owned[original?.lessonId]?.let { !it.touched && it.inLesson.isEmpty() && it.note.isBlank() } != false
    }.map { it.date }.distinct()
}

internal fun revisionAssignment(old: Assignment?, kind: RevisionKind, date: LocalDate?, pages: Map<String, List<Int>>, title: String): Assignment? {
    if (kind == RevisionKind.NONE) return null
    return (old ?: Assignment(newId(), AssignmentType.WORKSHEET, title)).copy(
        type = if (kind == RevisionKind.TEST) AssignmentType.TEST else AssignmentType.WORKSHEET,
        due = date, pages = pages, takesLesson = true)
}

/** Keeps cross-class writes local to the target without selecting its class. */
internal fun SchoolYear.withUnitRevision(target: UnitRevisionTarget, revision: UnitRevision, assignment: Assignment?): SchoolYear =
    copy(classes = classes.map { cls -> if (cls.id != target.classId) cls else cls.copy(groups = cls.groups.map { group ->
        if (group.id != target.groupId) group else group.withUnitRevision(revision, assignment)
    }) })

/** Dismissal is session-only; persisted NONE decisions are filtered by pendingUnitRevisions. */
internal class UnitRevisionPrompts {
    private val seen = mutableSetOf<UnitRevisionTarget>()
    private val queue = ArrayDeque<UnitRevisionTarget>()
    var current: UnitRevisionTarget? = null
        private set
    fun open(target: UnitRevisionTarget) { current = target }
    fun offer(targets: List<UnitRevisionTarget>) {
        targets.filter { seen.add(it) }.forEach { target ->
            if (current == null) current = target else queue.addLast(target)
        }
    }
    fun dismiss(target: UnitRevisionTarget) {
        if (current == target) current = queue.removeFirstOrNull()
    }
}

data class UnitLessonPosition(val unitIndex: Int, val lesson: Int, val total: Int)

/** Count actual taught dates as well as remaining windows, so Done does not reset a unit to lesson one. */
internal fun unitLessonPositions(report: GroupReport): Map<LocalDate, UnitLessonPosition> {
    val units = unitPlan(report.plan, report.group) ?: return emptyMap()
    val books = report.plan.books.filter { it.id in units.linkedBookIds }
    val revisionDates = report.group.unitRevisions.mapNotNull { revision ->
        report.group.assignments.firstOrNull { it.id == revision.assignmentId }?.due
    }.toSet()
    val out = linkedMapOf<LocalDate, UnitLessonPosition>()
    units.windows.forEach { window ->
        val taught = report.group.done.filter { done -> done.date !in revisionDates && books.any { book ->
            val starts = book.validUnitStarts().orEmpty()
            done.pages[book.id].orEmpty().any { page -> starts.indexOfLast { it <= page } == window.index }
        } }.map { it.date }
        val dates = (taught + window.dates).distinct().sorted()
        dates.forEachIndexed { i, date -> out[date] = UnitLessonPosition(window.index, i + 1, dates.size) }
    }
    return out
}
