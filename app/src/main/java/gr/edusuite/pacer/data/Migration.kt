package gr.edusuite.pacer.data

import gr.edusuite.pacer.model.ALL_GROUPS
import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.model.MAX_PAGE
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.SchoolClass
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.newId

/** Name for the one class that each plan from an older file becomes. */
const val MIGRATED_CLASS_NAME = "Main class"

/**
 * Turns the flat plan list of version-1 files (and the web page's export) into school years:
 * each plan becomes a year with the plan's name, term and days off, holding one class with its
 * books, groups, tests and homework. Group ids are kept, so days off and [view] still point at them.
 * Pure Kotlin, so it is unit-tested without org.json.
 */
fun migratePlans(plans: List<Plan>, activePlan: String, view: String): AppData {
    var activeYear = ""
    var activeClass = ""
    val years = plans.map { p ->
        val c = SchoolClass(newId(), MIGRATED_CLASS_NAME, p.mins, p.pace, p.groups, p.books, p.homework, p.events)
        if (p.id == activePlan) {
            activeYear = p.id
            activeClass = c.id
        }
        SchoolYear(p.id, p.name, p.start, p.end, p.closures, listOf(c))
    }
    // A saved group only reopens if it belongs to the plan that was open.
    val groupOpen = years.firstOrNull { it.id == activeYear }?.classes?.first()?.groups?.any { it.id == view } == true
    return AppData(years, activeYear, activeClass, if (groupOpen) view else AppData.VIEW_SETUP)
}

/**
 * Converts legacy work once, consuming the class lists so repeating the migration cannot duplicate it.
 * The codec applies this on load/import/save; the app edits only group assignments.
 * Page endpoints are bounded before expansion, just as legacy files are sanitised before scheduling.
 */
fun migrateAssignments(c: SchoolClass): SchoolClass = c.copy(
    groups = c.groups.map { g ->
        val used = g.assignments.mapTo(HashSet()) { it.id }
        fun id(preferred: String): String {
            var id = preferred
            while (id.isBlank() || !used.add(id)) id = newId()
            return id
        }
        val homework = c.homework.filter { it.groupId == g.id }.map { h ->
            val from = h.from.coerceIn(0, MAX_PAGE)
            val to = h.to.coerceIn(0, MAX_PAGE)
            Assignment(id(h.id), AssignmentType.HOMEWORK, "", setOn = h.setOn,
                pages = mapOf(h.bookId to (minOf(from, to)..maxOf(from, to)).toList()))
        }
        val tests = c.events.filter { it.who == ALL_GROUPS || it.who == g.id }.map { e ->
            Assignment(id(e.id), AssignmentType.TEST, e.name, due = e.date, takesLesson = e.consumes)
        }
        g.copy(assignments = g.assignments + homework + tests)
    },
    homework = emptyList(),
    events = emptyList(),
)
