package gr.edusuite.pacer.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoGateTest {
    @Test fun staleCloseDoesNotClearNewerToast() {
        val gate = UndoGate()
        val first = gate.open()
        val second = gate.open()
        assertFalse(gate.close(first))
        assertTrue(gate.isCurrent(second))
    }

    @Test fun closeClearsCurrentToken() {
        val gate = UndoGate()
        val token = gate.open()
        assertTrue(gate.close(token))
        assertFalse(gate.isCurrent(token))
        assertFalse(gate.close(token))
    }

    @Test fun openInvalidatesPreviousToken() {
        val gate = UndoGate()
        val first = gate.open()
        val second = gate.open()
        assertTrue(second > first)
        assertFalse(gate.isCurrent(first))
        assertTrue(gate.isCurrent(second))
    }
}
