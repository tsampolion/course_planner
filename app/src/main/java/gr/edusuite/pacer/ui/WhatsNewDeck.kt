package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Hints
import gr.edusuite.pacer.model.Module

/** The cards of the What's new deck, in order: a greeting, one per tool, and where to choose (`docs/FEATURE_MODULES_BRIEF.md`). */
enum class DeckCard(val module: Module? = null) {
    HELLO, WORK(Module.WORK), BOOK_TOOLS(Module.BOOK_TOOLS), PLANNING(Module.PLANNING), OUTPUT(Module.OUTPUT), CHOOSE
}

fun deckCards(): List<DeckCard> = DeckCard.entries

/** The stored id that remembers a version's deck was shown, kept with the other show-once hints. */
fun deckId(version: String): String = Hints.whatsNew(shortVersion(version))

/** An install that already had data or saw Welcome gets the deck once; a fresh install sees Welcome instead. */
fun deckDue(seen: Set<String>, id: String, existingInstall: Boolean): Boolean = existingInstall && id !in seen

/** The teacher's name for the greeting, or null when none is set ("Hi." alone). */
fun deckName(name: String): String? = name.trim().takeIf { it.isNotEmpty() }

/** Back goes to the previous card and never closes the deck. */
fun deckBack(index: Int): Int = (index - 1).coerceAtLeast(0)

/** The card after [index], or null after the last one, where Continue finishes. */
fun deckNext(index: Int): Int? = (index + 1).takeIf { it < deckCards().size }
