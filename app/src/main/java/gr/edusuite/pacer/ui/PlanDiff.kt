package gr.edusuite.pacer.ui

/**
 * The books whose pages in a custom plan's [custom] lesson differ from what the proposed plan teaches that day
 * ([proposed]), by their place in the plan's book list. Empty when there is no proposed lesson, or the lesson is
 * done, a test, an extra lesson or a free day. Supplementary books are never compared: they are placed by hand.
 */
internal fun differingBooks(custom: Lesson, proposed: Lesson?, supplementary: List<Boolean> = emptyList()): Set<Int> {
    if (proposed == null || custom.done || custom.blocked || custom.lessonId == null || custom.extraId != null) return emptySet()
    return custom.cells.indices.filter { i ->
        !supplementary.getOrElse(i) { false } && custom.cells[i] != proposed.cells.getOrNull(i)
    }.toSet()
}
