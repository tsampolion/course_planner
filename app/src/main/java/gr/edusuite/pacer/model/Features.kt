package gr.edusuite.pacer.model

/** The groups of tools a teacher turns on (`docs/FEATURE_MODULES_BRIEF.md`). Modules only ever turn on. */
enum class Module {
    WORK, BOOK_TOOLS, PLANNING, OUTPUT;

    companion object {
        fun parse(name: String?): Module? = entries.firstOrNull { it.name == name }
    }
}

/**
 * One feature, as in the brief's Bricks table: its [module] (null for the core, which is always on) and the bricks it
 * [needs]. Every control names its brick, and a screen shows only the controls whose brick [Features.has].
 */
enum class Brick(val module: Module?) {
    PACING(null),
    PLAN_BASE(null),
    CATCH_UP(null),
    HOMEWORK(Module.WORK),
    TESTS(Module.WORK),
    UNIT_REVISIONS(Module.WORK),
    LEAD_BOOK(Module.BOOK_TOOLS),
    UNITS(Module.BOOK_TOOLS),
    OPTIONAL_BOOKS(Module.BOOK_TOOLS),
    GROUP_BOOK_RULES(Module.BOOK_TOOLS),
    PLAN_EDITOR(Module.PLANNING),
    OWN_PACE(Module.PLANNING),
    EXTRA_LESSONS(Module.PLANNING),
    SUPPLEMENTARY(Module.PLANNING),
    PRINT(Module.OUTPUT),
    COPY_SHARE(Module.OUTPUT);

    /** A function, not a constructor argument, so a brick can need one declared after it. */
    val needs: Set<Brick> get() = when (this) {
        CATCH_UP, PLAN_EDITOR, OWN_PACE -> setOf(PLAN_BASE)
        UNIT_REVISIONS -> setOf(UNITS)
        SUPPLEMENTARY -> setOf(PLAN_EDITOR)
        else -> emptySet()
    }
}

/** A plan the teacher planned by hand somewhere. A plan written only by catch-up or another fix has no touched lesson. */
fun CustomPlan.handPlanned(): Boolean = lessons.any { it.touched }

/**
 * Whether saved data in [years] uses this brick. Class-wide skipped pages, finish-by dates and a group's end date are
 * core and never count. The core bricks, own pace and the Output bricks hold no data of their own.
 */
fun Brick.inUse(years: List<SchoolYear>): Boolean {
    val classes = years.flatMap { it.classes }
    val groups = classes.flatMap { it.groups }
    return when (this) {
        Brick.HOMEWORK -> classes.any { it.homework.isNotEmpty() } ||
            groups.any { g -> g.assignments.any { it.type == AssignmentType.HOMEWORK } }
        Brick.TESTS -> classes.any { it.events.isNotEmpty() } ||
            groups.any { g -> g.assignments.any { it.type != AssignmentType.HOMEWORK } }
        Brick.UNIT_REVISIONS -> groups.any { it.unitRevisions.isNotEmpty() }
        Brick.LEAD_BOOK -> classes.any { it.lead != null }
        Brick.UNITS -> classes.any { c -> c.unitsEnabled || c.bookLinks.isNotEmpty() || c.books.any { it.units.isNotBlank() } } ||
            groups.any { it.unitsOverride != null }
        Brick.OPTIONAL_BOOKS -> classes.any { c -> c.books.any { it.groupOnly } } || groups.any { it.extraBooks.isNotEmpty() }
        Brick.GROUP_BOOK_RULES -> groups.any { g -> g.removedBooks.isNotEmpty() || g.bookDue.isNotEmpty() || g.skip.values.any { it.isNotBlank() } }
        Brick.PLAN_EDITOR -> groups.any { it.custom?.handPlanned() == true }
        Brick.EXTRA_LESSONS -> groups.any { it.extras.isNotEmpty() }
        Brick.SUPPLEMENTARY -> classes.any { c -> c.books.any { it.supplementary } }
        Brick.PACING, Brick.PLAN_BASE, Brick.CATCH_UP, Brick.OWN_PACE, Brick.PRINT, Brick.COPY_SHARE -> false
    }
}

/** The modules whose bricks the data in [years] uses. */
fun modulesInUse(years: List<SchoolYear>): Set<Module> =
    Brick.entries.filter { it.inUse(years) }.mapNotNull { it.module }.toSet()

/**
 * The modules a teacher has [on], and the bricks the open data uses ([inUse]). A brick in use always shows, so data
 * the teacher has is never hidden; otherwise a brick is on when its module is (the core always is) and so is every
 * brick it needs.
 */
data class Features(val on: Set<Module>, val inUse: Set<Brick> = emptySet()) {
    fun has(b: Brick): Boolean = b in inUse || ((b.module == null || b.module in on) && b.needs.all { has(it) })

    companion object {
        val ALL = Features(Module.entries.toSet())
    }
}
