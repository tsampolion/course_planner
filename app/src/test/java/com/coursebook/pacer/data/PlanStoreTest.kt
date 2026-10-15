package com.coursebook.pacer.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PlanStoreTest {
    @get:Rule val dir = TemporaryFolder()

    @Test fun anUnreadableFileIsMovedAsideWithoutOverwritingAnEarlierOne() {
        val plans = File(dir.root, "plans.json")
        plans.writeText("first")
        assertEquals(File(dir.root, "plans.json.bad"), setAside(plans))
        // Moved, so the next launch starts clean instead of finding it unreadable again.
        assertFalse(plans.exists())

        plans.writeText("second")
        assertEquals(File(dir.root, "plans.json.bad-2"), setAside(plans))
        assertEquals("first", File(dir.root, "plans.json.bad").readText())
        assertEquals("second", File(dir.root, "plans.json.bad-2").readText())
    }
}
