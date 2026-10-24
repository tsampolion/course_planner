package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.model.Group
import java.time.LocalDate

private fun Assignment.references(other: Assignment): Boolean =
    other.title.isNotBlank() && title.contains(other.title.trim(), ignoreCase = true)

fun homeworkContent(report: GroupReport, a: Assignment): AssignmentType? =
    a.contentType?.takeIf { it != AssignmentType.HOMEWORK }
        ?: report.group.assignments.firstOrNull { it.type != AssignmentType.HOMEWORK && a.references(it) }?.type
        ?: when {
            Regex("\\bworksheet\\b", RegexOption.IGNORE_CASE).containsMatchIn(a.title) -> AssignmentType.WORKSHEET
            Regex("\\btest\\b", RegexOption.IGNORE_CASE).containsMatchIn(a.title) -> AssignmentType.TEST
            else -> null
        }

private fun GroupReport.hasPair(a: Assignment): Boolean = group.assignments.any {
    it.id != a.id && it.id == a.linkedId && it.linkedId == a.id &&
        (it.type == AssignmentType.HOMEWORK) != (a.type == AssignmentType.HOMEWORK)
}

/** Remaining lessons that can hold new class work; report calendars already include group overrides. */
fun scheduleChoices(report: GroupReport, a: Assignment): List<LocalDate> {
    val first = a.setOn ?: report.lessons.firstOrNull() ?: return emptyList()
    return report.lessons.filterIndexed { i, date -> date >= first && (a.due == null || date < a.due) && !report.blockedByLesson[i] }
}

fun homeworkChoices(report: GroupReport, a: Assignment): List<LocalDate> {
    val due = a.due ?: return emptyList()
    return report.lessons.filterIndexed { i, date -> date < due && !report.blockedByLesson[i] }
}

fun suggestSchedule(report: GroupReport, a: Assignment): LocalDate? {
    if (a.type != AssignmentType.HOMEWORK || homeworkContent(report, a) == null || report.hasPair(a)) return null
    val alreadyScheduled = report.group.assignments.any { other -> other.type != AssignmentType.HOMEWORK &&
        a.references(other) && other.due?.let { it >= (a.setOn ?: LocalDate.MIN) && (a.due == null || it <= a.due) } == true }
    return if (alreadyScheduled) null else scheduleChoices(report, a).firstOrNull()
}

fun suggestHomework(report: GroupReport, a: Assignment): Pair<LocalDate, LocalDate>? {
    if (a.type == AssignmentType.HOMEWORK || a.due == null || report.hasPair(a)) return null
    if (report.group.assignments.any { it.type == AssignmentType.HOMEWORK && it.references(a) && it.due == a.due }) return null
    return homeworkChoices(report, a).lastOrNull()?.let { it to a.due }
}

/** Import and live edits share the same metadata rules; page sanitising belongs to the data layer. */
fun normaliseAssignmentMetadata(work: List<Assignment>): List<Assignment> {
    val byId = work.associateBy { it.id }
    return work.map { a -> a.copy(
        contentType = a.contentType?.takeIf { a.type == AssignmentType.HOMEWORK && it != AssignmentType.HOMEWORK },
        linkedId = a.linkedId?.takeIf { id -> id != a.id && byId[id]?.let {
            it.linkedId == a.id && (it.type == AssignmentType.HOMEWORK) != (a.type == AssignmentType.HOMEWORK)
        } == true },
    ) }
}

private fun List<Assignment>.upsert(a: Assignment): List<Assignment> =
    if (any { it.id == a.id }) map { if (it.id == a.id) a else it } else this + a

fun Group.withAssignment(a: Assignment): Group = copy(assignments = normaliseAssignmentMetadata(assignments.upsert(a)))

/** Pairing is one structural edit; replacing a pair releases the previous counterpart. */
fun Group.withLinkedAssignment(originalId: String, companion: Assignment): Group {
    val original = assignments.firstOrNull { it.id == originalId } ?: return this
    if (originalId == companion.id || (original.type == AssignmentType.HOMEWORK) == (companion.type == AssignmentType.HOMEWORK)) return this
    val cleared = copy(assignments = assignments.map {
        if (it.linkedId == originalId || it.linkedId == companion.id) it.copy(linkedId = null) else it
    })
    return cleared.copy(assignments = normaliseAssignmentMetadata(cleared.assignments
        .upsert(original.copy(linkedId = companion.id)).upsert(companion.copy(linkedId = originalId))))
}

fun Group.withoutAssignment(id: String): Group = copy(assignments = normaliseAssignmentMetadata(assignments.filterNot { it.id == id }))
