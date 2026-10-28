package gr.edusuite.pacer.ui

/** How a sheet was asked to close: its filled action, or one of the four ways out. */
enum class SheetClose { ACTION, CLOSE_BUTTON, BACK, SCRIM, SWIPE }

enum class SheetCloseDecision { CLOSE, ASK }

/**
 * Streamlining S4 (F18): an untouched sheet closes at once; a changed draft asks Keep editing / Discard before any way
 * out drops it. The filled action saves, so it never asks.
 */
fun sheetCloseDecision(dirty: Boolean, via: SheetClose): SheetCloseDecision =
    if (dirty && via != SheetClose.ACTION) SheetCloseDecision.ASK else SheetCloseDecision.CLOSE

/** A saveable fingerprint of a sheet's fields, so a field edited and edited back reads as unchanged. */
fun draftSignature(values: List<Any?>): String = buildString {
    // Each field is written as its length and its text, so one field can never run into the next.
    values.forEach { v ->
        if (v == null) append('-') else v.toString().let { append(it.length); append('#'); append(it) }
    }
}
