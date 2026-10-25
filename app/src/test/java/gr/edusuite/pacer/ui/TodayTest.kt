package gr.edusuite.pacer.ui

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayTest {
    @Test fun nextMidnightDelayIsTimeToMidnight() {
        assertEquals(30_000L, nextMidnightDelayMillis(LocalDateTime.of(2026, 10, 24, 23, 59, 30)))
    }

    @Test fun delayAtMidnightIsFullDay() {
        assertEquals(86_400_000L, nextMidnightDelayMillis(LocalDateTime.of(2026, 10, 25, 0, 0, 0)))
    }
}
