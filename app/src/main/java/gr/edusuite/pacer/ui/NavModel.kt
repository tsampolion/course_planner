package gr.edusuite.pacer.ui

import gr.edusuite.pacer.engine.UnitIssue
import gr.edusuite.pacer.engine.unitIssues
import gr.edusuite.pacer.engine.BookLag
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.Source
import gr.edusuite.pacer.engine.Verdict
import gr.edusuite.pacer.engine.activeExtras
import gr.edusuite.pacer.engine.bookLags
import gr.edusuite.pacer.engine.shortfallCoverage
import gr.edusuite.pacer.engine.coverage
import gr.edusuite.pacer.engine.verdict
import gr.edusuite.pacer.model.SchoolYear
import java.text.Normalizer
import java.util.Locale
import kotlin.math.ceil

/*
 * What the menu, the group switcher and the Warnings sheet show, in plain Kotlin so it is unit-testable
 * (docs/PACER_DESIGN.md: one verdict and one rounding helper feed every status label).
 */

/** Slack below anything a lesson count shows, so 3.0000000000004 lessons still reads 3. */
private const val WHOLE_TOLERANCE = 1e-9

/** A lesson count as the app shows it: rounded up, and anything above 0 at least 1 (3.33 → 4, 0.3 → 1). 0 stays 0. */
fun wholeLessons(x: Double): Int = if (x <= WHOLE_TOLERANCE) 0 else maxOf(1, ceil(x - WHOLE_TOLERANCE).toInt())

/** Whole lessons short of the term, from [verdict]: 0 on pace, null when there is nothing to measure. */
fun GroupReport.lessonsShort(): Int? = when (val v = verdict()) {
    null -> null
    is Verdict.Fits -> 0
    is Verdict.Short -> wholeLessons(v.by)
}

/** Every group of the year by id, with [lessonsShort] for the plan it follows. */
fun yearStatus(year: SchoolYear): Map<String, Int?> = year.classes.flatMap { c ->
    val plan = year.planFor(c)
    c.groups.map { it.id to GroupReport(plan, it).lessonsShort() }
}.toMap()

/** How many groups of the year are behind: the count by Pacing overview in the menu. */
fun groupsBehind(year: SchoolYear): Int = yearStatus(year).values.count { (it ?: 0) > 0 }

/** A group found by [searchGroups], with its class. */
data class GroupHit(val classId: String, val className: String, val groupId: String, val groupName: String)

/** Lower case without accents, so "ά" finds "α" and "Fce" finds "FCE". */
private fun fold(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT)

/**
 * "Find a class or group": every group whose class name and group name together hold each word of [query], in the
 * year's order. A class's name finds all its groups. A blank query finds nothing.
 */
fun searchGroups(year: SchoolYear, query: String): List<GroupHit> {
    val words = fold(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    return year.classes.flatMap { c ->
        c.groups.filter { g -> fold("${c.name} ${g.name}").let { hay -> words.all { it in hay } } }
            .map { GroupHit(c.id, c.name, it.id, it.name) }
    }
}

/** One warning on a group and the fix the Warnings sheet offers for it. */
sealed interface GroupWarning {
    data class Units(val issue: UnitIssue) : GroupWarning
    data class ExtraUnavailable(val date: java.time.LocalDate) : GroupWarning
    /** Short of the term: Catch up. [pages] is how many pages more than the pace fits. */
    data class Behind(val lessons: Int, val pages: Int) : GroupWarning
    /** A book left behind the others: Bring in step. */
    data class BookBehind(val lag: BookLag, val lessons: Int) : GroupWarning
    /** The custom plan leaves pages out: Re-pace. */
    data class Missing(val pages: Int) : GroupWarning
    /** The custom plan runs past the term: Fit to term. */
    data class Overflow(val lessons: Int) : GroupWarning
    /** The custom plan teaches pages twice: Tidy. */
    data class Doubled(val pages: Int) : GroupWarning
    /** The custom plan holds pages already taught: Tidy. */
    data class Stale(val pages: Int) : GroupWarning
}

/**
 * Every warning on [report]'s group, as the group screen showed them before they moved into the sheet: an active
 * custom plan that fits every page into the term already answers being behind and books left behind, and a custom
 * plan's coverage problems come last.
 */
fun groupWarnings(report: GroupReport): List<GroupWarning> {
    val plan = report.plan
    val handled = report.custom != null && report.coverage()?.ok == true &&
        (report.shortfallCoverage()?.complete != false)
    val out = ArrayList<GroupWarning>()
    out += report.unitIssues().map { GroupWarning.Units(it) }
    val active = plan.activeExtras(report.group).mapTo(HashSet()) { it.id }
    report.group.extras.filter { e -> e.id !in active && report.group.done.none { it.date == e.date } }
        .forEach { out += GroupWarning.ExtraUnavailable(it.date) }
    if (!handled) {
        (report.verdict() as? Verdict.Short)?.let { out += GroupWarning.Behind(wholeLessons(it.by), it.cutPages) }
        report.bookLags(plan.lead, plan.lagLessons.toDouble()).forEach { out += GroupWarning.BookBehind(it, wholeLessons(it.lessonsBehind)) }
    }
    val cov = if (report.group.custom == null) null else GroupReport(plan, report.group, Source.CUSTOM).coverage()
    if (cov != null) {
        if (cov.missingCount > 0) out += GroupWarning.Missing(cov.missingCount)
        if (cov.overflow > 0) out += GroupWarning.Overflow(cov.overflow)
        if (cov.doubledCount > 0) out += GroupWarning.Doubled(cov.doubledCount)
        if (cov.staleCount > 0) out += GroupWarning.Stale(cov.staleCount)
    }
    return out
}
