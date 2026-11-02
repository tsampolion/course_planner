package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.model.Module

/** The cards of the What's new deck, in order: a greeting, one per tool, and where to choose (`docs/FEATURE_MODULES_BRIEF.md`). */
enum class DeckCard(val module: Module? = null) {
    HELLO, WORK(Module.WORK), BOOK_TOOLS(Module.BOOK_TOOLS), PLANNING(Module.PLANNING), OUTPUT(Module.OUTPUT), CHOOSE
}

fun deckCards(): List<DeckCard> = DeckCard.entries

/** The release whose deck this is. The deck's cards describe the tools launched in 0.4.15, so later updates don't repeat it. */
const val DECK_RELEASE = "0.4.15"

/** The stored id that remembers the deck was shown, kept with the other show-once hints. */
fun deckId(): String = Hints.whatsNew(DECK_RELEASE)

/**
 * An install that already had data or saw Welcome gets the deck once; a fresh install sees Welcome instead. 0.4.15.1
 * stored the deck under its own version (`whatsnew.0.4.15.1`), so any id of this release counts as seen.
 */
fun deckDue(seen: Set<String>, existingInstall: Boolean): Boolean =
    existingInstall && seen.none { it == deckId() || it.startsWith(deckId() + ".") }

/** What the deck offers: a choice of tools, or, when every tool is already on, a tour of them. */
enum class DeckVariant { CHOOSE, TOUR }

fun deckVariant(on: Set<Module>): DeckVariant = if (on.containsAll(Module.entries)) DeckVariant.TOUR else DeckVariant.CHOOSE

/** The teacher's name for the greeting, or null when none is set ("Hi." alone). */
fun deckName(name: String): String? = name.trim().takeIf { it.isNotEmpty() }

/** Back goes to the previous card and never closes the deck. */
fun deckBack(index: Int): Int = (index - 1).coerceAtLeast(0)

/** The card after [index], or null after the last one, where Continue finishes. */
fun deckNext(index: Int): Int? = (index + 1).takeIf { it < deckCards().size }

/** The deck's content enters in reading order, this far apart, and the fourth item on waits no longer. */
const val DECK_STEP_MILLIS = 60L
private const val DECK_STEPS = 3

/** When item [index] of a card starts to enter; at once when the system's animations are off. */
fun deckEntranceDelay(index: Int, enabled: Boolean): Long = if (!enabled) 0L else index.coerceIn(0, DECK_STEPS) * DECK_STEP_MILLIS
