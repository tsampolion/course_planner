package gr.edusuite.pacer.ui.theme

import org.junit.Assert.*
import org.junit.Test

class MotionTest {
    @Test fun removingAnimationsSnapsEveryTransitionToItsFinishedState() {
        for (step in MotionSpec.Step.entries) {
            assertEquals(0, MotionSpec.duration(step, 0f))
            assertEquals(0, MotionSpec.duration(step, -1f))
        }
    }

    @Test fun normalTransitionsHaveBoundedDurationsAndRespectTheScale() {
        for (step in MotionSpec.Step.entries) {
            val normal = MotionSpec.duration(step, 1f)
            assertTrue(normal in 1..1200)
            assertEquals(normal * 2, MotionSpec.duration(step, 2f))
        }
    }
    @Test fun rowStaggerRunsOnceAndNeverDelaysOffscreenRowsForSeconds() {
        assertEquals(0L, MotionSpec.entranceDelay(3, true, 1f))
        assertEquals(0L, MotionSpec.entranceDelay(3, false, 0f))
        assertEquals(140L, MotionSpec.entranceDelay(2, false, 1f))
        assertTrue(MotionSpec.entranceDelay(10000, false, 1f) <= 420L)
    }
}
