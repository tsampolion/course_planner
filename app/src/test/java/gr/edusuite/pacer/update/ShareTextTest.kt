package gr.edusuite.pacer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextTest {
    @Test fun sharedLinkIsTheLatestReleasePageOfThisRepository() {
        assertEquals("https://github.com/tsampolion/course_planner/releases/latest", RELEASE_LATEST_URL)
        assertTrue(RELEASE_LATEST_URL.startsWith(PAGE_URL_PREFIX))
    }

    @Test fun sharedTextIsTheMessageThenTheReleaseLink() {
        assertEquals("Hello\n$RELEASE_LATEST_URL", shareText("Hello"))
    }
}
