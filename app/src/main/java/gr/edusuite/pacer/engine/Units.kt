package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.*
import java.time.LocalDate

/** Stored group choices are inert while the class master switch is off. */
fun Plan.unitsActive(g: Group): Boolean = unitsEnabled && g.unitsOverride != false

/** Strict feature validation deliberately leaves legacy [unitStarts] parsing unchanged. */
fun Book.validUnitStarts(): List<Int>? {
    if (units.isBlank()) return null
    val parts = units.split(',', ';').map { it.trim().toIntOrNull() ?: return null }
    return parts.takeIf { it.firstOrNull() == first && it.all { p -> p in first..last } && it.zipWithNext().all { (a, b) -> a < b } }
}

enum class UnitIssueKind { INVALID_UNITS, COUNT_MISMATCH, INSUFFICIENT_WINDOW, DEADLINE, REVISION_UNSCHEDULED, BOUNDARY_VIOLATION, BEHIND }
data class UnitIssue(val kind: UnitIssueKind, val message: String, val unitIndex: Int? = null, val bookId: String? = null)
data class UnitWindow(val index: Int, val dates: List<LocalDate>, val pages: Map<String, List<Int>>) {
    val start: LocalDate? get() = dates.firstOrNull()
    val end: LocalDate? get() = dates.lastOrNull()
}
data class UnitPlan(val windows: List<UnitWindow>, val issues: List<UnitIssue>, val linkedBookIds: Set<String>)
data class UnitProgress(val index: Int, val total: Int, val remaining: Map<String, Int>, val complete: Boolean)

/** Connected links containing the lead share its unit boundaries. Other books retain legacy pacing. */
private fun Plan.unitBooks(): List<Book> {
    val lead = leadBook() ?: return emptyList()
    val ids = linkedSetOf(lead.id)
    var changed: Boolean
    do {
        changed = false
        for (link in bookLinks) if (link.any { it in ids }) for (id in link) if (ids.add(id)) changed = true
    } while (changed)
    return books.filter { !it.supplementary && it.id in ids }
}

private fun Book.unitPages(index: Int, starts: List<Int>, pool: List<Int>): List<Int> =
    pool.filter { it >= starts[index] && it < (starts.getOrNull(index + 1) ?: (last + 1)) }

/** Pure scheduling foundation: no GroupReport construction or row evaluation. */
fun unitPlan(plan: Plan, g: Group): UnitPlan? {
    if (!plan.unitsActive(g)) return null
    val p = plan.forGroup(g)
    val dates = p.lessonsFor(g).filter { d -> p.eventsOn(g, d).none { it.consumes } && p.activeExtras(g).none { it.date == d } }
    val units = buildUnitPlan(p, g, dates) { p.remainingPages(g, it) }
    return units.copy(windows = units.windows.map { w ->
        if (w.pages.values.any { it.isNotEmpty() }) w else w.copy(dates = g.done.filter { d ->
            g.unitRevisions.none { r -> r.assignmentId != null && g.assignments.any { it.id == r.assignmentId && it.due == d.date } } &&
            p.books.any { b -> b.id in units.linkedBookIds && b.validUnitStarts()?.let { starts ->
                d.pages[b.id].orEmpty().any { page -> starts.indexOfLast { it <= page } == w.index }
            } == true }
        }.map { it.date }.sorted())
    })
}

private fun buildUnitPlan(p: Plan, g: Group, dates: List<LocalDate>, pool: (Book) -> List<Int>): UnitPlan {
    val issues = arrayListOf<UnitIssue>()
    val books = p.unitBooks()
    val lead = p.leadBook()
    if (lead == null) return UnitPlan(emptyList(), listOf(UnitIssue(UnitIssueKind.INVALID_UNITS, "Choose a lead book to plan units.")), emptySet())
    val starts = lead.validUnitStarts()
    if (starts == null) return UnitPlan(emptyList(), listOf(UnitIssue(UnitIssueKind.INVALID_UNITS, "${lead.name}: enter ascending, distinct unit starts beginning on page ${lead.first}.", bookId = lead.id)), emptySet())
    val valid = books.filter { b ->
        val bs = b.validUnitStarts()
        when {
            bs == null -> { issues += UnitIssue(UnitIssueKind.INVALID_UNITS, "${b.name}: enter ascending, distinct unit starts beginning on page ${b.first}; ${pool(b).size} pages remain unscheduled.", bookId = b.id); false }
            bs.size != starts.size -> { issues += UnitIssue(UnitIssueKind.COUNT_MISMATCH, "${b.name}: its units don't match ${lead.name} (${bs.size} vs ${starts.size}); ${pool(b).size} pages remain unscheduled.", bookId = b.id); false }
            else -> true
        }
    }
    // Invalid linked books must not begin a later unit ahead of the valid chain.
    val unitPages = starts.indices.map { u -> valid.associate { b -> b.id to b.unitPages(u, b.validUnitStarts()!!, pool(b)) } }
    val active = starts.indices.filter { u -> unitPages[u].values.any { it.isNotEmpty() } }
    val weights = active.associateWith { u -> unitPages[u][lead.id].orEmpty().size.coerceAtLeast(1) }
    val total = weights.values.sum().coerceAtLeast(1)
    var cursor = 0
    var cumulative = 0
    var blockedByEarlierUnit = false
    val windows = starts.indices.map { u ->
        val pages = unitPages[u]
        val remainingUnits = active.count { it > u }
        // A chosen revision is a calendar boundary even when its unit is already complete.
        val previousRevision = g.unitRevisions.filter { it.unitIndex < u && it.kind != RevisionKind.NONE }
            .mapNotNull { r -> g.assignments.firstOrNull { it.id == r.assignmentId }?.due }.maxOrNull()
        while (cursor < dates.size && previousRevision != null && dates[cursor] <= previousRevision) cursor++
        val revision = g.unitRevisions.firstOrNull { it.unitIndex == u && it.kind != RevisionKind.NONE }
            ?.let { r -> g.assignments.firstOrNull { it.id == r.assignmentId }?.due }
        val n = if (blockedByEarlierUnit || u !in active || cursor >= dates.size) 0 else {
            cumulative += weights.getValue(u)
            val idealEnd = ((2L * cumulative * dates.size + total) / (2L * total)).toInt()
            if (revision != null) {
                dates.drop(cursor).takeWhile { it < revision }.size
            } else {
                val maxEnd = (dates.size - remainingUnits).coerceAtLeast(cursor + 1).coerceAtMost(dates.size)
                (idealEnd.coerceIn(cursor + 1, maxEnd) - cursor).coerceAtLeast(1)
            }
        }
        val ds = dates.subList(cursor, (cursor + n).coerceAtMost(dates.size)).toList()
        cursor += ds.size
        if (u in active && ds.isEmpty()) {
            blockedByEarlierUnit = true
            issues += UnitIssue(UnitIssueKind.INSUFFICIENT_WINDOW,
                "Unit ${u + 1}: no lesson window is available; ${pages.values.sumOf { it.size }} pages remain unscheduled. Adjust revision dates, add lessons or extend the term.", u)
        }
        for (b in valid) if (pages[b.id].orEmpty().isNotEmpty() && b.due != null && (ds.lastOrNull() == null || ds.last() > b.due))
            issues += UnitIssue(UnitIssueKind.DEADLINE, "${b.name}: unit ${u + 1} cannot finish before its finish-by date while linked unit boundaries are kept.", u, b.id)
        UnitWindow(u, ds, pages)
    }
    for (r in g.unitRevisions.filter { it.kind != RevisionKind.NONE }) {
        val a = g.assignments.firstOrNull { it.id == r.assignmentId }
        val scheduled = a != null && a.type != AssignmentType.HOMEWORK && a.takesLesson && a.due != null &&
            (g.done.any { it.date == a.due } || p.lessonsFor(g).contains(a.due))
        if (!scheduled) issues += UnitIssue(UnitIssueKind.REVISION_UNSCHEDULED, "Unit ${r.unitIndex + 1}: revision has no available lesson. Choose a future lesson.", r.unitIndex)
    }
    return UnitPlan(windows, issues, valid.mapTo(linkedSetOf()) { it.id })
}

/** Apply unit windows after the legacy allocation; unlinked columns remain byte-for-byte unchanged. */
internal fun GroupReport.followUnits(
    alloc: List<MutableList<List<Int>>>,
    dates: List<LocalDate> = teachingDates,
    weights: List<Long> = List(dates.size) { 1L },
    weightedBookId: String? = null,
    pool: (Book) -> List<Int>,
): List<MutableList<List<Int>>> {
    if (!plan.unitsActive(group)) return alloc
    val size = alloc.maxOfOrNull { it.size } ?: 0
    val units = buildUnitPlan(plan, group, dates, pool)
    val out = alloc.map { it.toMutableList() }.toMutableList()
    for (b in plan.unitBooks()) {
        val i = plan.books.indexOfFirst { it.id == b.id }
        if (i < 0) continue
        // Validation errors keep all pages explicitly unscheduled, never silently proportional.
        out[i] = MutableList(size) { emptyList() }
        if (b.id !in units.linkedBookIds) continue
        for (window in units.windows) {
            val slots = window.dates.map { dates.indexOf(it) }
            allocateWeighted(window.pages[b.id].orEmpty(), slots.map { if (weightedBookId == null || weightedBookId == b.id) weights[it] else 1L }).forEachIndexed { j, ps -> out[i][slots[j]] = ps }
        }
    }
    return out
}

/** Repace generated custom content through the same linked windows without touching manual material. */
internal fun GroupReport.constrainUnitLessons(
    out: MutableList<PlannedLesson>,
    targets: List<Int>,
    weights: List<Long> = List(targets.size) { 1L },
    weightedBookId: String? = null,
) {
    if (!plan.unitsActive(group) || targets.isEmpty()) return
    val pools = plan.books.associate { b -> b.id to targets.flatMap { out[it].pages[b.id].orEmpty() }.distinct().sorted() }
    val alloc = plan.books.map { b -> targets.mapTo(arrayListOf()) { out[it].pages[b.id].orEmpty() } }
    val linked = plan.unitBooks().mapTo(hashSetOf()) { it.id }
    val constrained = followUnits(alloc, targets.map { teachingDates[it] }, weights, weightedBookId) { pools.getValue(it.id) }
    for ((bi, b) in plan.books.withIndex()) if (b.id in linked) for ((j, t) in targets.withIndex()) {
        out[t] = out[t].copy(pages = (out[t].pages - b.id) + constrained[bi][j].takeIf { it.isNotEmpty() }.let { if (it == null) emptyMap() else mapOf(b.id to it) })
    }
}

fun unitProgress(plan: Plan, g: Group): List<UnitProgress> {
    val units = unitPlan(plan, g) ?: return emptyList()
    return units.windows.map { w -> UnitProgress(w.index, units.windows.size, w.pages.mapValues { it.value.size }, w.pages.values.all { it.isEmpty() }) }
}
fun pendingUnitRevisions(plan: Plan, g: Group): List<Int> = unitProgress(plan, g)
    .filter { it.complete && g.unitRevisions.none { r -> r.unitIndex == it.index } }.map { it.index }

/** Removes only the assignment owned by this revision, releasing its homework pair safely. */
fun Group.withoutUnitRevision(unitIndex: Int): Group {
    val old = unitRevisions.firstOrNull { it.unitIndex == unitIndex } ?: return this
    val id = old.assignmentId
    val others = unitRevisions.filterNot { it.unitIndex == unitIndex }
    val cleared = if (id != null && others.none { it.assignmentId == id }) withoutAssignment(id) else this
    return cleared.copy(unitRevisions = others)
}

/** Assignment and revision change together; ordinary assignments cannot be claimed by a revision. */
fun Group.withUnitRevision(revision: UnitRevision, assignment: Assignment? = null): Group {
    if (revision.unitIndex < 0) return this
    val old = unitRevisions.firstOrNull { it.unitIndex == revision.unitIndex }
    if (assignment != null && assignments.any { it.id == assignment.id } && old?.assignmentId != assignment.id) return this
    if (assignment != null && (assignment.type == AssignmentType.HOMEWORK || !assignment.takesLesson)) return this
    val a = assignment?.takeIf { revision.kind != RevisionKind.NONE }
    // Updating the owned assignment in place keeps reciprocal homework links intact.
    // Removing it first would normalise the surviving homework and erase that pair.
    val base = if (a != null && old?.assignmentId == a.id) this else withoutUnitRevision(revision.unitIndex)
    val saved = revision.copy(assignmentId = a?.id, pages = if (revision.kind == RevisionKind.NONE) emptyMap() else revision.pages)
    val with = if (a == null) base else base.withAssignment(a)
    return with.copy(unitRevisions = (with.unitRevisions.filterNot { it.unitIndex == saved.unitIndex } + saved).sortedBy { it.unitIndex })
}

/** Calendar and manual-content warnings, kept separate from the pure window builder to avoid recursion. */
fun GroupReport.unitIssues(): List<UnitIssue> {
    val units = unitPlan(plan, group) ?: return emptyList()
    val issues = units.issues.toMutableList()
    val books = plan.books.filter { it.id in units.linkedBookIds && it.validUnitStarts() != null }
    val required = books.associate { b -> b.id to (plan.remainingPages(group, b) + plan.activeExtras(group).flatMap { it.pages[b.id].orEmpty() }).toSet() }
    val seen = books.associate { it.id to hashSetOf<Int>() }
    val flagged = hashSetOf<Int>()
    val revisionConflicts = hashSetOf<Int>()
    for (row in rows) {
        // Revision pages can revisit any unit; they are not new unit teaching.
        if (group.unitRevisions.any { r -> group.assignments.any { it.id == r.assignmentId && it.due == row.date } }) continue
        val taughtUnits = books.flatMap { b ->
            row.pages[plan.books.indexOfFirst { it.id == b.id }].map { page -> b.validUnitStarts()!!.indexOfLast { it <= page } }
        }
        for (r in group.unitRevisions.filter { it.kind != RevisionKind.NONE }) {
            val date = group.assignments.firstOrNull { it.id == r.assignmentId }?.due ?: continue
            val conflicts = taughtUnits.any { (it == r.unitIndex && row.date >= date) || (it > r.unitIndex && row.date <= date) }
            if (conflicts && revisionConflicts.add(r.unitIndex)) issues += UnitIssue(UnitIssueKind.BOUNDARY_VIOLATION,
                "Unit ${r.unitIndex + 1}: teaching on ${row.date} conflicts with its revision on $date. Manual lessons are preserved; adjust lesson pages or the revision date.", r.unitIndex)
        }
        val highest = books.maxOfOrNull { b ->
            val bi = plan.books.indexOfFirst { it.id == b.id }
            row.pages[bi].maxOfOrNull { page -> b.validUnitStarts()!!.indexOfLast { it <= page } } ?: -1
        } ?: -1
        for (u in 0 until highest) {
            val unfinished = books.filter { b ->
                val starts = b.validUnitStarts()!!
                b.unitPages(u, starts, required.getValue(b.id).toList()).any { it !in seen.getValue(b.id) }
            }
            if (unfinished.isNotEmpty() && flagged.add(u)) issues += UnitIssue(UnitIssueKind.BOUNDARY_VIOLATION,
                "Unit ${u + 1}: ${unfinished.joinToString { it.name }} still has pages when a later unit starts on ${row.date}. Manual lessons are preserved; adjust their pages or dates.", u)
        }
        for (b in books) seen.getValue(b.id).addAll(row.pages[plan.books.indexOfFirst { it.id == b.id }])
    }
    for (w in units.windows) {
        val missing = w.pages.filterValues { it.isNotEmpty() }
        if (missing.isNotEmpty() && missing.size < books.size) issues += UnitIssue(UnitIssueKind.BEHIND,
            "Unit ${w.index + 1}: ${missing.keys.mapNotNull { id -> books.firstOrNull { it.id == id }?.name }.joinToString()} holds the next unit back.", w.index)
    }
    return issues.distinct()
}

/** Revision pages occupy the reserved lesson without consuming a custom teaching slot. */
internal fun GroupReport.revisionPages(date: LocalDate): List<List<Int>> {
    val pages = group.unitRevisions.filter { it.kind == RevisionKind.PAGES }.mapNotNull { r ->
        group.assignments.firstOrNull { it.id == r.assignmentId && it.due == date && it.takesLesson && it.type != AssignmentType.HOMEWORK }
    }.flatMap { it.pages.entries }.groupBy({ it.key }, { it.value }).mapValues { (_, lists) -> lists.flatten().distinct().sorted() }
    return plan.books.map { pages[it.id].orEmpty() }
}
