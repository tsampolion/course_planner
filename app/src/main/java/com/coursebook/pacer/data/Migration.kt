package com.coursebook.pacer.data

import com.coursebook.pacer.model.AppData
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.model.SchoolClass
import com.coursebook.pacer.model.SchoolYear
import com.coursebook.pacer.model.newId

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
