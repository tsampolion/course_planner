package gr.edusuite.pacer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionLabelTest {
    @Test fun aZeroLastNumberIsHidden() {
        assertEquals("0.4.14", shortVersion("0.4.14.0"))
        assertEquals("0.5.0", shortVersion("0.5.0.0"))
    }

    @Test fun aFixKeepsAllFourNumbers() {
        assertEquals("0.4.14.1", shortVersion("0.4.14.1"))
        assertEquals("0.4.14.10", shortVersion("0.4.14.10"))
    }

    @Test fun otherShapesAreLeftAlone() {
        assertEquals("4.1.0", shortVersion("4.1.0"))
        assertEquals("", shortVersion(""))
        assertEquals("0.4.14.0-debug", shortVersion("0.4.14.0-debug"))
    }

    @Test fun theNameFollowsTheVersion() {
        assertEquals("0.4.15 · Great Expectations", releaseLabel("0.4.15.0", "Great Expectations"))
        assertEquals("0.4.15", releaseLabel("0.4.15.0", null))
        assertEquals("0.4.15.1", releaseLabel("0.4.15.1", "  "))
    }
}
