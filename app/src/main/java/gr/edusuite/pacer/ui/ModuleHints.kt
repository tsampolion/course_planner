package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Brick
import gr.edusuite.pacer.model.Features
import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.model.Module

/** The two moments where a tool that is off is offered, once, in place (`docs/FEATURE_MODULES_BRIEF.md`, Turning a module on). */
enum class ModuleHint(val id: String, val module: Module) {
    /** Pages recorded into a lesson that are already ahead of the plan. */
    WORK(Hints.WORK_AHEAD, Module.WORK),

    /** A lesson's pages edited by hand twice. */
    PLANNING(Hints.PLAN_BY_HAND, Module.PLANNING),
}

/** How many of the [taught] pages lie beyond what the plan had for the lesson, over every book. */
fun pagesAhead(planned: Map<String, List<Int>>, taught: Map<String, List<Int>>): Int =
    taught.entries.sumOf { (book, pages) ->
        val limit = planned[book]?.maxOrNull() ?: 0
        pages.count { it > limit }
    }

/** True when the pages saved for a lesson are not the pages the plan proposed. */
fun isHandEdit(planned: Map<String, List<Int>>, taught: Map<String, List<Int>>): Boolean =
    (planned.keys + taught.keys).any { planned[it].orEmpty().toSet() != taught[it].orEmpty().toSet() }

/**
 * The hint to show now, if any: Work first. Nothing shows for a tool that is already on or whose data is saved, and
 * nothing shows twice ([seen] is `Settings.seenTips`).
 */
fun dueModuleHint(features: Features, seen: Set<String>, aheadPages: Int, handEdits: Int): ModuleHint? = when {
    aheadPages > 0 && ModuleHint.WORK.id !in seen && Module.WORK !in features.on && !features.has(Brick.HOMEWORK) -> ModuleHint.WORK
    handEdits >= 2 && ModuleHint.PLANNING.id !in seen && Module.PLANNING !in features.on && !features.has(Brick.PLAN_EDITOR) -> ModuleHint.PLANNING
    else -> null
}
