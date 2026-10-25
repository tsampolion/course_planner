package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Verdict
import gr.edusuite.pacer.engine.verdict
import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A single date drives work ordering; due dates take precedence over set-on dates. */
fun Assignment.workDate(): LocalDate? = due ?: setOn

enum class WorkPeriod { THIS_WEEK, LATER, UNDATED, EARLIER }
data class WorkSection(val period: WorkPeriod, val assignments: List<Assignment>)

fun dueSoon(report: GroupReport, today: LocalDate): List<Assignment> = report.group.assignments
    .filter { it.workDate()?.let { d -> d >= today } == true }
    .sortedWith(compareBy<Assignment> { it.workDate() }.thenBy { it.title }.thenBy { it.id }).take(4)

fun workSections(work: List<Assignment>, today: LocalDate, type: AssignmentType? = null): List<WorkSection> {
    val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val grouped = work.filter { type == null || it.type == type }.groupBy {
        val date = it.workDate()
        when {
            date == null -> WorkPeriod.UNDATED
            date < today -> WorkPeriod.EARLIER
            date <= end -> WorkPeriod.THIS_WEEK
            else -> WorkPeriod.LATER
        }
    }
    return WorkPeriod.entries.mapNotNull { period ->
        grouped[period]?.let { list ->
            val ordered = list.sortedWith(compareBy<Assignment> { it.workDate() }.thenBy { it.title }.thenBy { it.id })
            WorkSection(period, if (period == WorkPeriod.EARLIER) ordered.reversed() else ordered)
        }
    }
}

/** The same strict page rules as lesson editing, including a supplementary book's include list. */
fun assignmentPages(text: String, book: gr.edusuite.pacer.model.Book): List<Int>? =
    gr.edusuite.pacer.engine.lessonPages(text, book)?.takeIf { pages ->
        !book.supplementary || gr.edusuite.pacer.engine.parseInclude(book.include).orEmpty().containsAll(pages)
    }

/** Preserve pages from deselected optional books; only visible fields can clear their pages. */
fun assignmentEditedPages(existing: Map<String, List<Int>>, editable: Map<String, List<Int>>): Map<String, List<Int>> =
    (existing - editable.keys) + editable.filterValues { it.isNotEmpty() }

/** Undated work is allowed; reserving a lesson requires a date. */
fun assignmentDatesValid(a: Assignment): Boolean =
    (a.due == null || a.setOn == null || a.due >= a.setOn) &&
        !(a.type != AssignmentType.HOMEWORK && a.takesLesson && a.due == null)

/** Preview choice belongs to Plan; the home and its actions always use the active schedule. */
fun groupPageReport(report: GroupReport, page: GroupPage, showMine: Boolean): GroupReport =
    if (page != GroupPage.PLAN || report.group.custom == null) report else
        GroupReport(report.plan, report.group, if (showMine) gr.edusuite.pacer.engine.Source.CUSTOM else gr.edusuite.pacer.engine.Source.PROPOSED)

fun groupPageCanTick(report: GroupReport, page: GroupPage, showMine: Boolean): Boolean =
    page != GroupPage.PLAN || report.group.custom?.let { showMine == it.active } != false

/** The pages group home's pills open. Settings is its own screen, so this is not [GroupPage]. */
enum class GroupDestination {
    PLAN, WORK, STATS, SETTINGS;

    fun screen(groupId: String): Screen = when (this) {
        PLAN -> Screen.Group(groupId, GroupPage.PLAN)
        WORK -> Screen.Group(groupId, GroupPage.WORK)
        STATS -> Screen.Group(groupId, GroupPage.STATS)
        SETTINGS -> Screen.GroupSettings(groupId)
    }
}

enum class ProgressStatus { ON_PACE, SHORT, UNMEASURED }

/** The group home progress strip: where the group is in its lessons, and whether the plan fits. */
data class ProgressLine(val lesson: Int, val total: Int, val status: ProgressStatus, val shortBy: Int, val fraction: Float)

/** "Lesson 14 of 40": taught lessons plus the one coming, over taught plus left. With no lessons at all, [ProgressLine.total] is 0. */
internal fun progressLine(report: GroupReport): ProgressLine {
    val done = report.group.done.size
    val left = report.lessons.size
    val total = done + left
    val shortBy = report.lessonsShort() ?: 0
    val status = when (report.verdict()) {
        is Verdict.Short -> ProgressStatus.SHORT
        is Verdict.Fits -> ProgressStatus.ON_PACE
        null -> ProgressStatus.UNMEASURED
    }
    return ProgressLine(if (left == 0) done else done + 1, total, status, shortBy, if (total == 0) 0f else done.toFloat() / total)
}
