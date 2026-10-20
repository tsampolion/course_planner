package gr.edusuite.pacer.engine

import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.newId
import java.time.LocalDate

/*
 * Custom plans: the teacher's own lessons, built from the proposed plan (docs/CUSTOM_PLANS_BRIEF.md).
 * A custom plan stores the lessons still to teach, in order, and no dates: lesson k takes the group's k-th
 * teaching day. Nothing here changes the proposed plan, and a group without a custom plan never reaches this file.
 */

/** Rows for [c]: each teaching day takes the next lesson; a day the plan has no lesson for is a free lesson. */
internal fun GroupReport.customRows(c: CustomPlan): List<Row> {
    var k = 0
    return lessons.mapIndexed { i, date ->
        if (blockedByLesson[i]) {
            Row(i + 1, date, eventsByLesson[i], true, plan.books.map { emptyList() })
        } else {
            val l = c.lessons.getOrNull(k++)
            Row(i + 1, date, eventsByLesson[i], false, plan.books.map { l?.pages?.get(it.id).orEmpty() }, l?.note.orEmpty(), l?.id)
        }
    }
}

/** The proposed plan's lessons as a new custom plan, active, with no lesson touched. */
fun GroupReport.toCustomPlan(id: () -> String = ::newId): CustomPlan {
    val proposed = if (source == Source.PROPOSED) this else GroupReport(plan, group, Source.PROPOSED)
    return CustomPlan(true, proposed.rows.filter { !it.blocked }.map { PlannedLesson(id(), proposed.pagesByBook(it)) })
}

/**
 * What a custom plan gets wrong, book id to pages. [missing]: pages still to teach that no lesson has.
 * [doubled]: pages two or more lessons teach. [stale]: pages a lesson has that need no teaching any more
 * (taught already, skipped, set for homework, or outside the book). [overflow]: lessons with no teaching day left in the term.
 */
data class Coverage(
    val missing: Map<String, List<Int>>,
    val doubled: Map<String, List<Int>>,
    val stale: Map<String, List<Int>>,
    val overflow: Int,
) {
    val ok: Boolean get() = missing.isEmpty() && doubled.isEmpty() && stale.isEmpty() && overflow == 0
    val missingCount: Int get() = missing.values.sumOf { it.size }
    val doubledCount: Int get() = doubled.values.sumOf { it.size }
    val staleCount: Int get() = stale.values.sumOf { it.size }
}

/** How well the custom plan this report follows covers the pages left. Null for the proposed plan. */
fun GroupReport.coverage(): Coverage? {
    val c = custom ?: return null
    val missing = HashMap<String, List<Int>>()
    val doubled = HashMap<String, List<Int>>()
    val stale = HashMap<String, List<Int>>()
    // Core books only: supplementary pages are placed by hand and never left out, doubled or stale.
    for (b in plan.coreBooks) {
        val left = plan.remainingPages(group, b)
        val leftSet = left.toHashSet()
        val count = HashMap<Int, Int>()
        for (l in c.lessons) for (p in l.pages[b.id].orEmpty()) count[p] = (count[p] ?: 0) + 1
        left.filter { it !in count }.takeIf { it.isNotEmpty() }?.let { missing[b.id] = it }
        count.filterValues { it > 1 }.keys.sorted().takeIf { it.isNotEmpty() }?.let { doubled[b.id] = it }
        count.keys.filter { it !in leftSet }.sorted().takeIf { it.isNotEmpty() }?.let { stale[b.id] = it }
    }
    return Coverage(missing, doubled, stale, (c.lessons.size - teaching).coerceAtLeast(0))
}

/**
 * The group's custom plan without the lessons that fall on [dates], which are being marked done: the done lessons
 * take those days, so the lessons after them keep theirs. Done lessons are shared, whichever plan is active.
 */
internal fun Plan.takeSlots(g: Group, dates: Set<LocalDate>): CustomPlan? {
    val c = g.custom ?: return null
    val ids = GroupReport(this, g, Source.CUSTOM).rows.filter { it.date in dates }.mapNotNullTo(HashSet()) { it.lessonId }
    return if (ids.isEmpty()) c else c.copy(lessons = c.lessons.filter { it.id !in ids })
}

/** Returns the lesson on [date] to the schedule ([unmarkDone]); in a custom plan it comes back on its day, with the pages it was marked with. */
fun Plan.unmarkLesson(g: Group, date: LocalDate): Group = restoreSlots(g, g.unmarkDone(date))

/** Returns every done lesson on or after [date] to the schedule ([unmarkDoneFrom]), and to the custom plan as [unmarkLesson] does. */
fun Plan.unmarkLessonsFrom(g: Group, date: LocalDate): Group = restoreSlots(g, g.unmarkDoneFrom(date))

private fun Plan.restoreSlots(before: Group, after: Group, id: () -> String = ::newId): Group {
    val c = after.custom ?: return after
    val back = before.done.filter { d -> after.done.none { it.date == d.date } }.sortedBy { it.date }
    if (back.isEmpty()) return after
    val teaching = GroupReport(this, after, Source.PROPOSED).teachingDates
    val out = c.lessons.toMutableList()
    for (d in back) {
        val i = teaching.indexOf(d.date)
        if (i < 0) continue
        out.add(minOf(i, out.size), PlannedLesson(id(), d.pages, touched = true))
    }
    return after.copy(custom = c.copy(lessons = out))
}

/**
 * Re-spreads the pages from lesson [from] on, by the proposed plan's rule (allocatePages, finish-by dates included):
 * the pages still to teach that no earlier lesson and no touched lesson has go over the untouched lessons from
 * [from] that fall in the term. Touched lessons and lessons before [from] never change; untouched lessons past the
 * term end are dropped. Re-pacing a plan nobody touched from its first lesson gives the proposed plan back.
 * Only core pages move: supplementary pages stay in their lessons, and an untouched lesson past the term end that
 * holds some stays too, without its core pages.
 */
fun GroupReport.repaceFrom(from: Int): CustomPlan? {
    val c = group.custom ?: return null
    val days = teachingDates
    val k = from.coerceAtLeast(0)
    val kept = c.lessons.withIndex().mapNotNull { (i, l) ->
        when {
            i < days.size || i < k || l.touched -> l
            plan.hasSupplementary(l) -> l.copy(pages = plan.supplementaryOf(l.pages))
            else -> null
        }
    }
    val targets = kept.indices.filter { it >= k && it < days.size && !kept[it].touched }
    val out = kept.mapIndexed { i, l -> if (i in targets) l.copy(pages = plan.supplementaryOf(l.pages)) else l }.toMutableList()
    if (targets.isEmpty()) return c.copy(lessons = out)
    val pools = plan.books.associate { b ->
        val covered = out.flatMapTo(HashSet()) { it.pages[b.id].orEmpty() }
        b.id to plan.remainingPages(group, b).filter { it !in covered }
    }
    val alloc = plan.books.map { b ->
        val pool = pools.getValue(b.id)
        val due = b.due
        // Finish-by date before the first lesson re-paced: everything lands in it, as in the proposed plan.
        val m = (if (due == null) targets.size else targets.count { !days[it].isAfter(due) }).coerceAtLeast(1)
        allocatePages(pool, m).toMutableList().apply { while (size < targets.size) add(emptyList()) }
    }
    // With a lead book the others keep step with it here too, as in the proposed plan.
    followLead(alloc) { pools.getValue(it.id) }.forEachIndexed { bi, a ->
        val b = plan.books[bi]
        a.forEachIndexed { j, pages ->
            if (pages.isNotEmpty()) out[targets[j]] = out[targets[j]].let { it.copy(pages = it.pages + (b.id to pages)) }
        }
    }
    return c.copy(lessons = out)
}

/**
 * Takes out pages that need no teaching and pages an earlier lesson already teaches. A lesson the teacher never
 * touched and that is left with nothing to teach goes too. Supplementary pages are left as they are.
 */
fun GroupReport.tidy(): CustomPlan? {
    val c = group.custom ?: return null
    val left = plan.books.associate { b -> b.id to plan.remainingPages(group, b).toHashSet() }
    val seen = plan.books.associate { b -> b.id to HashSet<Int>() }
    val supp = plan.supplementaryIds
    val out = c.lessons.mapNotNull { l ->
        val pages = l.pages.mapNotNull { (book, ps) ->
            if (book in supp) return@mapNotNull book to ps
            val keep = ps.filter { p -> left[book]?.contains(p) == true && seen.getValue(book).add(p) }
            if (keep.isEmpty()) null else book to keep
        }.toMap()
        if (pages.isEmpty() && l.pages.isNotEmpty() && !l.touched && l.note.isBlank()) null else l.copy(pages = pages)
    }
    return c.copy(lessons = out)
}

/**
 * Brings lessons past the term end back into it. Their pages re-spread over the untouched lessons after the last one
 * the teacher touched; if every lesson in the term is touched, they join the last lesson. A lesson past the end that
 * holds supplementary pages stays where it is, with only those, for the teacher to place again.
 */
fun GroupReport.fitToTerm(): CustomPlan? {
    val c = group.custom ?: return null
    val days = teaching
    if (c.lessons.size <= days || days == 0) return c
    val inTerm = c.lessons.take(days)
    val beyond = c.lessons.drop(days)
    val keep = beyond.filter { plan.hasSupplementary(it) }.map { it.copy(pages = plan.supplementaryOf(it.pages)) }
    val k = inTerm.indexOfLast { it.touched } + 1
    if (k < days) {
        val fitted = GroupReport(plan, group.copy(custom = c.copy(lessons = inTerm)), Source.CUSTOM).repaceFrom(k) ?: return c
        return fitted.copy(lessons = fitted.lessons + keep)
    }
    val last = inTerm.last()
    val merged = beyond.fold(last.pages) { acc, l -> mergePages(acc, plan.coreOf(l.pages)) }
    return c.copy(lessons = inTerm.dropLast(1) + last.copy(pages = merged, touched = true) + keep)
}

/** The lesson [id] with [pages] and [note]. Changing its pages marks it touched, so re-pacing leaves it alone. */
fun CustomPlan.editLesson(id: String, pages: Map<String, List<Int>>, note: String): CustomPlan = copy(lessons = lessons.map { l ->
    if (l.id != id) l else {
        val clean = pages.mapValues { it.value.distinct().sorted() }.filterValues { it.isNotEmpty() }
        l.copy(pages = clean, note = note.trim(), touched = l.touched || clean != l.pages)
    }
})

/** Puts [lesson] at position [at] (clamped), marked touched: the teacher made it. */
fun CustomPlan.insertLesson(at: Int, lesson: PlannedLesson): CustomPlan =
    copy(lessons = lessons.toMutableList().apply { add(at.coerceIn(0, size), lesson.copy(touched = true)) })

/** Removes the lesson [id]. Its pages move into the next lesson (the previous one if it was the last), which counts as touched. */
fun CustomPlan.removeLesson(id: String): CustomPlan {
    val i = lessons.indexOfFirst { it.id == id }
    if (i < 0) return this
    val gone = lessons[i]
    val out = lessons.toMutableList().apply { removeAt(i) }
    if (gone.pages.isNotEmpty() && out.isNotEmpty()) {
        val j = minOf(i, out.lastIndex)
        val pages = mergePages(out[j].pages, gone.pages)
        // Two lessons' core pages no longer describe one lesson's, so there is no saved base to restore.
        val inLesson = (out[j].inLesson + gone.inLesson).filterTo(HashSet()) { it in pages }
        out[j] = out[j].copy(pages = pages, touched = true, inLesson = inLesson, replacementBase = null)
    }
    return copy(lessons = out)
}

/** Moves the lesson [id] to position [to] (clamped); the days stay where they are, so it takes that lesson's day. */
fun CustomPlan.moveLesson(id: String, to: Int): CustomPlan {
    val i = lessons.indexOfFirst { it.id == id }
    if (i < 0) return this
    val out = lessons.toMutableList()
    val l = out.removeAt(i)
    val j = to.coerceIn(0, out.size)
    if (j == i) return this
    out.add(j, l.copy(touched = true))
    return copy(lessons = out)
}

/**
 * [c] for another group of the class, [target]: the same lessons in the same order on the target's own days,
 * without the pages the target no longer has to teach (for a supplementary book, the pages it has already taught).
 * It replaces the target's own custom plan and becomes active.
 */
fun Plan.copyCustomPlan(c: CustomPlan, target: Group, id: () -> String = ::newId): CustomPlan {
    val left = books.associate { b ->
        b.id to if (b.supplementary) (b.includedPages() - target.taughtPages(b.id)).toHashSet() else remainingPages(target, b).toHashSet()
    }
    val keep = { m: Map<String, List<Int>> ->
        m.mapNotNull { (book, ps) -> ps.filter { left[book]?.contains(it) == true }.takeIf { it.isNotEmpty() }?.let { book to it } }.toMap()
    }
    return CustomPlan(true, c.lessons.map { l ->
        val pages = keep(l.pages)
        l.copy(id = id(), pages = pages, inLesson = l.inLesson.filterTo(HashSet()) { it in pages }, replacementBase = l.replacementBase?.let { coreOf(keep(it)) })
    })
}

private fun mergePages(a: Map<String, List<Int>>, b: Map<String, List<Int>>): Map<String, List<Int>> =
    (a.keys + b.keys).associateWith { (a[it].orEmpty() + b[it].orEmpty()).distinct().sorted() }.filterValues { it.isNotEmpty() }
