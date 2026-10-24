package gr.edusuite.pacer.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupFrameTest {
    @Test fun topInsetIsConsumedOnceAndBottomInsetReachesEveryGroupPage() {
        for (top in listOf(0.dp, 64.dp)) {
            val insets = groupInsets(PaddingValues(top = top, bottom = 24.dp))
            assertEquals(top, insets.top)
            assertEquals(0.dp, insets.body.calculateTopPadding())
            assertEquals(24.dp, insets.body.calculateBottomPadding())
        }
    }
}
