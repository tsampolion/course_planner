package gr.edusuite.pacer.ui

enum class GroupMenuItem { CREATE_PLAN, MY_PLAN, REPACE, COPY_PLAN, DELETE_PLAN, OWN_PACE, CATCH_UP, COPY, SHARE, PRINT, EXTRA, TIPS }

/** Available actions independent of Compose; [catchUp] comes from the report (a custom plan that can be caught up). */
fun groupMenuItems(hasRows: Boolean, hasCustom: Boolean, otherGroups: Int, large: Boolean, catchUp: Boolean = false): List<GroupMenuItem> = buildList {
    if (large && hasRows && !hasCustom) add(GroupMenuItem.CREATE_PLAN)
    if (hasCustom) {
        add(GroupMenuItem.MY_PLAN)
        add(GroupMenuItem.REPACE)
        if (otherGroups > 0) add(GroupMenuItem.COPY_PLAN)
        add(GroupMenuItem.DELETE_PLAN)
    }
    if (large && hasRows) add(GroupMenuItem.OWN_PACE)
    if (catchUp) add(GroupMenuItem.CATCH_UP)
    add(GroupMenuItem.EXTRA)
    if (hasRows) { add(GroupMenuItem.COPY); add(GroupMenuItem.SHARE) }
    add(GroupMenuItem.PRINT)
    add(GroupMenuItem.TIPS)
}

/** The ⋮ menu's sections, in order. */
enum class GroupMenuSection { SHARE_PRINT, PLAN, LESSONS, HELP }

private fun GroupMenuItem.section(): GroupMenuSection = when (this) {
    GroupMenuItem.COPY, GroupMenuItem.SHARE, GroupMenuItem.PRINT -> GroupMenuSection.SHARE_PRINT
    GroupMenuItem.CREATE_PLAN, GroupMenuItem.MY_PLAN, GroupMenuItem.REPACE, GroupMenuItem.COPY_PLAN, GroupMenuItem.DELETE_PLAN,
    GroupMenuItem.OWN_PACE, GroupMenuItem.CATCH_UP -> GroupMenuSection.PLAN
    GroupMenuItem.EXTRA -> GroupMenuSection.LESSONS
    GroupMenuItem.TIPS -> GroupMenuSection.HELP
}

/**
 * The sections the ⋮ menu shows on [page], each with its items, so a heading never stands over nothing. Work lists
 * assignments, not lessons, so the plan's tools and Add extra lesson are left for the pages that have lessons.
 */
fun groupMenuSections(items: List<GroupMenuItem>, page: GroupPage): List<Pair<GroupMenuSection, List<GroupMenuItem>>> {
    val onWork = page == GroupPage.WORK
    return GroupMenuSection.entries
        .filter { !onWork || (it != GroupMenuSection.PLAN && it != GroupMenuSection.LESSONS) }
        .map { s -> s to items.filter { it.section() == s } }
        .filter { it.second.isNotEmpty() }
}
