package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewDeckTest {
    @Test fun theDeckSaysHelloThenOneCardPerToolThenWhereToChoose() {
        val cards = deckCards()
        assertEquals(DeckCard.HELLO, cards.first())
        assertEquals(DeckCard.CHOOSE, cards.last())
        assertEquals(Module.entries.toList(), cards.mapNotNull { it.module })
    }

    @Test fun anUpdateSeesTheDeckOnce() {
        assertTrue(deckDue(seen = emptySet(), id = "whatsnew.0.4.15", existingInstall = true))
        assertFalse(deckDue(seen = setOf("whatsnew.0.4.15"), id = "whatsnew.0.4.15", existingInstall = true))
    }

    @Test fun aFreshInstallSeesWelcomeInsteadOfTheDeck() {
        assertFalse(deckDue(seen = emptySet(), id = "whatsnew.0.4.15", existingInstall = false))
    }

    @Test fun theDeckIdFollowsTheShortVersion() {
        assertEquals("whatsnew.0.4.15", deckId("0.4.15.0"))
        assertEquals("whatsnew.0.4.15.1", deckId("0.4.15.1"))
    }

    @Test fun greetingUsesTheNameWhenThereIsOne() {
        assertEquals("Dimitrios", deckName("  Dimitrios "))
        assertEquals(null, deckName(""))
        assertEquals(null, deckName("   "))
    }

    @Test fun backStepsBackAndNeverClosesTheDeck() {
        assertEquals(0, deckBack(0))
        assertEquals(1, deckBack(2))
    }

    @Test fun theLastContinueFinishes() {
        val last = deckCards().lastIndex
        assertEquals(null, deckNext(last))
        assertEquals(1, deckNext(0))
    }
}
