package gr.edusuite.pacer.ui

enum class GroupMenuItem { CREATE_PLAN, MY_PLAN, REPACE, COPY_PLAN, DELETE_PLAN, OWN_PACE, COPY, SHARE, PRINT, EXTRA, TERM_WEEKS, TIPS }

/** Available actions independent of Compose; catch-up availability comes from the report. */
fun groupMenuItems(hasRows: Boolean, hasCustom: Boolean, otherGroups: Int, large: Boolean): List<GroupMenuItem> = buildList {
    if (large && hasRows && !hasCustom) add(GroupMenuItem.CREATE_PLAN)
    if (hasCustom) {
        add(GroupMenuItem.MY_PLAN)
        add(GroupMenuItem.REPACE)
        if (otherGroups > 0) add(GroupMenuItem.COPY_PLAN)
        add(GroupMenuItem.DELETE_PLAN)
    }
    if (large && hasRows) add(GroupMenuItem.OWN_PACE)
    add(GroupMenuItem.EXTRA)
    if (hasRows) { add(GroupMenuItem.COPY); add(GroupMenuItem.SHARE) }
    add(GroupMenuItem.PRINT)
    add(GroupMenuItem.TERM_WEEKS)
    add(GroupMenuItem.TIPS)
}
