package gr.edusuite.pacer.ui

internal enum class MenuItemKind {
    ADD_SUPPLEMENTARY, PLAN_LESSON, ADD_BEFORE, MOVE_EARLIER, MOVE_LATER, REPACE, REMOVE,
    RECORD_TAUGHT, DONE_UP_TO, NOT_DONE_FROM,
}

/** [isPast] includes today. Both the long-press menu and TalkBack use this choice of actions. */
internal fun lessonMenuItems(lesson: Lesson, isPast: Boolean, hasCustomPlan: Boolean, canTick: Boolean): List<MenuItemKind> = buildList {
    if (!lesson.done && !lesson.blocked && lesson.slot >= 0) add(MenuItemKind.ADD_SUPPLEMENTARY)
    if (hasCustomPlan && !lesson.done && !lesson.blocked && (lesson.lessonId != null || lesson.free)) {
        if (!isPast) add(MenuItemKind.PLAN_LESSON)
        if (lesson.lessonId != null) {
            add(MenuItemKind.ADD_BEFORE)
            add(MenuItemKind.MOVE_EARLIER)
            add(MenuItemKind.MOVE_LATER)
            add(MenuItemKind.REPACE)
            add(MenuItemKind.REMOVE)
        }
    }
    if (canTick) {
        if (isPast) add(MenuItemKind.RECORD_TAUGHT)
        add(MenuItemKind.DONE_UP_TO)
        add(MenuItemKind.NOT_DONE_FROM)
    }
}
