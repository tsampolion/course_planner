package gr.edusuite.pacer.ui

import org.junit.Assert.*
import org.junit.Test
import gr.edusuite.pacer.model.MAX_PAGE

class SetupChecksTest {
    @Test fun supplementaryPagesRejectReversedAndZeroRanges() {
        assertFalse(validSetupInclude("20-10"))
        assertFalse(validSetupInclude("0-10"))
        assertFalse(validSetupInclude(""))
        assertTrue(validSetupInclude("1-10, 20"))
        assertTrue(validSetupInclude("4—7; 9–12"))
    }
    @Test fun pageRangesRejectInvalidInputWithoutDefaulting() {
        assertFalse(validSetupRange("", "10"))
        assertFalse(validSetupRange("0", "10"))
        assertFalse(validSetupRange("20", "10"))
        assertFalse(validSetupRange("1", (MAX_PAGE + 1).toString()))
        assertTrue(validSetupRange("1", "1"))
        assertTrue(validSetupRange("1", MAX_PAGE.toString()))
    }
}
