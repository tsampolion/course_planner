package gr.edusuite.pacer.data

import gr.edusuite.pacer.model.AppData
import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private class FakeFile(override val base: File, var onRead: () -> ByteArray, val writes: MutableList<ByteArray> = mutableListOf()) : PlanFile {
        override fun read() = onRead()
        override fun write(bytes: ByteArray) { writes += bytes }
        val warnings = mutableListOf<String>()
        override fun warn(message: String, error: Throwable) { warnings += message + ": " + error }
    }

    // The JVM tests run against the android.jar stubs of org.json, so the codec is replaced by fakes here.
    private val empty = AppData(years = emptyList())

    private fun store(
        text: String?,
        decode: (String) -> AppData = { empty },
        base: File = File(dir.root, "plans.json"),
    ): Pair<PlanStore, FakeFile> {
        text?.let(base::writeText)
        val fake = FakeFile(base, { base.readBytes() })
        return PlanStore(fake, decode, { "{}" }) to fake
    }

    @Test fun ioErrorDoesNotMoveFile() {
        val (store, fake) = store("""{"years":[]}""")
        fake.onRead = { throw IOException("disk hiccup") }
        val loaded = store.load()
        assertTrue(loaded.readFailed)
        assertNull(loaded.setAside)
        assertTrue(fake.base.exists())
        assertFalse(File(dir.root, "plans.json.bad").exists())
    }

    @Test fun anUnopenableExistingFileIsNotSetAside() {
        val (store, fake) = store("""{"years":[]}""")
        fake.onRead = { throw java.io.FileNotFoundException("EACCES") }
        assertTrue(store.load().readFailed)
        assertTrue(fake.base.exists())
    }

    @Test fun aMissingFileStartsEmpty() {
        val (store, fake) = store(null)
        fake.onRead = { throw java.io.FileNotFoundException("missing") }
        val loaded = store.load()
        assertFalse(loaded.readFailed)
        assertNull(loaded.setAside)
        assertTrue(loaded.data.years.isEmpty())
    }

    @Test fun saveIsSkippedAfterReadFailure() {
        val (store, fake) = store("""{"years":[]}""")
        fake.onRead = { throw IOException("disk hiccup") }
        val failed = store.load()
        store.save(failed.data)
        assertTrue(fake.writes.isEmpty())

        fake.onRead = { fake.base.readBytes() }
        assertFalse(store.load().readFailed)
        store.save(failed.data)
        assertEquals(fake.warnings.toString(), 1, fake.writes.size)
    }

    @Test fun deepNestingIsSetAside() {
        val (store, fake) = store("deeply nested", decode = { throw StackOverflowError() })
        val loaded = store.load()
        assertNotNull(loaded.setAside)
        assertFalse(loaded.readFailed)
        assertFalse(fake.base.exists())
    }

    @Test fun garbageIsStillSetAside() {
        val (store, fake) = store("not json at all", decode = { throw IllegalArgumentException("bad") })
        val loaded = store.load()
        assertEquals(File(dir.root, "plans.json.bad"), loaded.setAside)
        assertFalse(fake.base.exists())
        assertTrue(loaded.data.years.isEmpty())
    }
}
