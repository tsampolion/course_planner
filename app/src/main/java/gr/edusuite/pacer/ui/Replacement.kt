package gr.edusuite.pacer.ui

/*
 * The "Pages to teach this lesson" window that opens when a supplementary book counts in the lesson
 * (docs/SUPPLEMENTARY_BOOKS_BRIEF.md, decision 4). The teacher types what each core book will teach. A book typed as 0,
 * or left on the pages it had, asks whether to keep those pages or skip the book this lesson. Plain Kotlin, unit-tested.
 */

/** What 0 means in a core book's field of the window: leave the book out of this lesson. */
const val LEAVE_OUT = "0"

/**
 * Whether the core book whose field read [entry] when the window opened needs the keep-or-skip question for [typed]:
 * it had pages, and the teacher typed 0 or the same pages again ([parse] reads the field).
 */
fun needsKeepChoice(entry: List<Int>, typed: String, parse: (String) -> List<Int>?): Boolean {
    if (entry.isEmpty()) return false
    val t = typed.trim()
    return t == LEAVE_OUT || parse(t) == entry
}

/** The pages a core book teaches once its field is settled: [keep] answers the question, null when none was asked. */
fun settledPages(entry: List<Int>, typed: String, keep: Boolean?, parse: (String) -> List<Int>?): List<Int>? = when (keep) {
    true -> entry
    false -> emptyList()
    null -> if (typed.trim() == LEAVE_OUT) emptyList() else parse(typed.trim())
}
