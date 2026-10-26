package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.SchoolYear
import java.time.LocalDate

/**
 * What Print can put on paper, in one place for the Group, Class and Year scopes. A group is printable exactly when
 * the report [PrintSheet] renders for it has rows, so a scope offered here never opens a blank print job.
 * A schedule with only tests has rows, so it stays printable.
 */
internal object PrintScopes {
    fun printable(plan: Plan, group: Group): Boolean = GroupReport(plan, group).rows.isNotEmpty()

    /** [plan] cut down to the groups that have something to print, or null when none has. */
    fun printablePlan(plan: Plan): Plan? =
        plan.groups.filter { printable(plan, it) }.takeIf { it.isNotEmpty() }?.let { plan.copy(groups = it) }

    /** The scopes with something to print, in the dialog's order. Empty means Print has nothing to offer. */
    fun available(year: SchoolYear, today: LocalDate, classId: String?, groupId: String?): List<PrintScope> {
        val cls = classId?.let { id -> year.classes.firstOrNull { it.id == id } }
        val group = cls?.groups?.firstOrNull { it.id == groupId }
        return buildList {
            if (cls != null && group != null && printable(year.planFor(cls, today), group)) add(PrintScope.GROUP)
            if (cls != null && printablePlan(year.planFor(cls, today)) != null) add(PrintScope.CLASS)
            if (year.classes.any { printablePlan(year.planFor(it, today)) != null }) add(PrintScope.YEAR)
        }
    }

    /** The plans to hand to [PrintSheet] for [scope]: only printable groups, and no plan without any. */
    fun plans(year: SchoolYear, today: LocalDate, scope: PrintScope, classId: String?, groupId: String?): List<Plan> {
        val cls = classId?.let { id -> year.classes.firstOrNull { it.id == id } }
        return when {
            scope == PrintScope.GROUP && cls != null -> {
                val plan = year.planFor(cls, today)
                listOfNotNull(plan.groups.firstOrNull { it.id == groupId }?.let { g -> plan.copy(groups = listOf(g)) }).let { one ->
                    one.mapNotNull(::printablePlan)
                }
            }
            scope == PrintScope.CLASS && cls != null -> listOfNotNull(printablePlan(year.planFor(cls, today)))
            scope == PrintScope.YEAR -> year.classes.mapNotNull { printablePlan(year.planFor(it, today)) }
            else -> emptyList()
        }
    }
}
