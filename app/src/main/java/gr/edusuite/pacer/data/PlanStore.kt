package gr.edusuite.pacer.data

import android.util.AtomicFile
import android.util.Log
import gr.edusuite.pacer.model.AppData
import java.io.File
import java.io.FileOutputStream

/**
 * What [PlanStore.load] found. [setAside] is where an unparseable file was moved, so the app can say so once.
 * [readFailed] means the file exists but could not be read right now (an I/O error): it was left where it is
 * and [PlanStore.save] does nothing until a later load succeeds, so the unread data is never overwritten.
 */
data class Loaded(val data: AppData, val setAside: File? = null, val readFailed: Boolean = false)

/** The bytes of `plans.json`, read and written as the platform allows. Tests supply their own. */
internal interface PlanFile {
    val base: File
    fun read(): ByteArray
    fun write(bytes: ByteArray)
    fun warn(message: String, error: Throwable)
}

private class AtomicPlanFile(override val base: File) : PlanFile {
    private val file = AtomicFile(base)

    override fun read(): ByteArray = file.readFully()

    override fun write(bytes: ByteArray) {
        var out: FileOutputStream? = null
        try {
            out = file.startWrite()
            out.write(bytes)
            file.finishWrite(out)
        } catch (e: Exception) {
            out?.let(file::failWrite)
            throw e
        }
    }

    override fun warn(message: String, error: Throwable) {
        Log.w("PlanStore", message, error)
    }
}

/**
 * Keeps every plan in one JSON file in app storage. Writes are atomic, so a crash never leaves half a file.
 * A fresh install, or a file whose content can't be parsed, starts with no school years; only unparseable
 * content is set aside. A read error leaves the file alone and blocks saving (see [Loaded.readFailed]).
 */
class PlanStore internal constructor(private val plans: PlanFile) {
    constructor(dir: File) : this(AtomicPlanFile(File(dir, "plans.json")))

    @Volatile private var locked = false

    fun load(): Loaded {
        val result = try {
            Loaded(JsonCodec.decode(plans.read().decodeToString()))
        } catch (e: java.io.FileNotFoundException) {
            if (plans.base.exists()) {
                plans.warn("Could not open the plans file; leaving it alone", e)
                Loaded(EMPTY, readFailed = true)
            } else {
                Loaded(EMPTY)
            }
        } catch (e: java.io.IOException) {
            plans.warn("Could not read the plans file; leaving it alone", e)
            Loaded(EMPTY, readFailed = true)
        } catch (e: Exception) {
            plans.warn("Unreadable plans file; set aside, starting with no school years", e)
            Loaded(EMPTY, setAside(plans.base))
        } catch (e: StackOverflowError) {
            plans.warn("Plans file nested too deeply; set aside, starting with no school years", e)
            Loaded(EMPTY, setAside(plans.base))
        }
        locked = result.readFailed
        return result
    }

    /**
     * Synchronized: the debounced background save and the flush on stop may overlap.
     * Never throws: it runs in a background coroutine with no handler, where an exception would end the app.
     * Does nothing after a read failure, until the next successful [load].
     */
    @Synchronized
    fun save(data: AppData) {
        if (locked) return
        try {
            plans.write(JsonCodec.encode(data).encodeToByteArray())
        } catch (e: Exception) {
            plans.warn("Could not save plans", e)
        }
    }

    private companion object {
        val EMPTY = AppData(years = emptyList())
    }
}

/**
 * Moves the unreadable [file] to the first free name of `plans.json.bad`, `plans.json.bad-2` and so on, so an
 * earlier one is never overwritten. Moved, not copied, so the next launch doesn't find it unreadable again.
 * Null when it couldn't be moved or copied.
 */
internal fun setAside(file: File): File? {
    val target = generateSequence(1) { it + 1 }
        .map { n -> File(file.path + ".bad" + if (n == 1) "" else "-$n") }
        .first { !it.exists() }
    if (file.renameTo(target)) return target
    return runCatching { file.copyTo(target); file.delete(); target }.getOrNull()
}
