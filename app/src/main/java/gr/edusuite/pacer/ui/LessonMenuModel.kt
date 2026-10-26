package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.fmt

internal enum class MenuItemKind {
    ADD_SUPPLEMENTARY, PLAN_LESSON, ADD_BEFORE, MOVE_EARLIER, MOVE_LATER, REPACE, REMOVE,
    RECORD_TAUGHT, DONE_UP_TO, NOT_DONE_FROM, EDIT_EXTRA, DELETE_EXTRA,
    ADD_HOMEWORK, EDIT_HOMEWORK, DELETE_HOMEWORK,
}

internal fun homeworkForLesson(l: Lesson): List<Assignment> = l.assignments.filter {
    it.type == AssignmentType.HOMEWORK && (it.setOn == l.date || it.due == l.date)
}

internal fun homeworkPageCells(a: Assignment, bookIds: List<String>, names: List<String>): List<String> =
    bookIds.mapIndexedNotNull { i, id -> a.pages[id]?.takeIf { it.isNotEmpty() }?.let {
        "${names.getOrElse(i) { (i + 1).toString() }} ${collapse(it)}"
    } }

/** Shared data portion of visible and spoken homework summaries. */
internal fun homeworkTextParts(a: Assignment, bookIds: List<String>, names: List<String>): List<String> =
    listOfNotNull(a.title.takeIf { it.isNotBlank() }) + homeworkPageCells(a, bookIds, names) +
        listOfNotNull(a.note.takeIf { it.isNotBlank() && it != a.title })

internal fun homeworkActionNames(work: List<Assignment>, bookIds: List<String>, names: List<String>): List<String> =
    work.mapIndexed { i, a ->
        val detail = (homeworkTextParts(a, bookIds, names) + listOfNotNull(a.due?.fmt())).joinToString(" · ")
        if (detail.isBlank()) "" else "${i + 1}. $detail"
    }

/** [isPast] includes today. Both the long-press menu and TalkBack use this choice of actions. */
internal fun lessonMenuItems(lesson: Lesson, isPast: Boolean, hasCustomPlan: Boolean, canTick: Boolean): List<MenuItemKind> = buildList {
    add(MenuItemKind.ADD_HOMEWORK)
    if (homeworkForLesson(lesson).isNotEmpty()) {
        add(MenuItemKind.EDIT_HOMEWORK)
        add(MenuItemKind.DELETE_HOMEWORK)
    }
    if (lesson.extraId != null) {
        if (!lesson.done) add(MenuItemKind.EDIT_EXTRA)
        add(MenuItemKind.DELETE_EXTRA)
    }
    if (lesson.extraId == null && !lesson.done && !lesson.blocked && lesson.slot >= 0) add(MenuItemKind.ADD_SUPPLEMENTARY)
    if (lesson.extraId == null && hasCustomPlan && !lesson.done && !lesson.blocked && (lesson.lessonId != null || lesson.free)) {
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

/** The custom plan's actions sit in "My plan", behind one item; everything else is about the lesson itself. */
internal fun MenuItemKind.section(): MenuSection = when (this) {
    MenuItemKind.PLAN_LESSON, MenuItemKind.ADD_BEFORE, MenuItemKind.MOVE_EARLIER,
    MenuItemKind.MOVE_LATER, MenuItemKind.REPACE, MenuItemKind.REMOVE -> MenuSection.MY_PLAN
    else -> MenuSection.LESSON
}
