package gr.edusuite.pacer.data

import android.util.AtomicFile
import android.util.Log
import gr.edusuite.pacer.model.AppData
import java.io.File
import java.io.FileOutputStream

/** What [PlanStore.load] found. [setAside] is where an unreadable file was moved, so the app can say so once. */
data class Loaded(val data: AppData, val setAside: File? = null)

/**
 * Keeps every plan in one JSON file in app storage. Writes are atomic, so a crash never leaves half a file.
 * A fresh install, or a file that can't be read, starts with no school years.
 */
class PlanStore(dir: File) {
    private val file = AtomicFile(File(dir, "plans.json"))

    fun load(): Loaded = try {
        Loaded(JsonCodec.decode(file.readFully().decodeToString()))
    } catch (e: java.io.FileNotFoundException) {
        Loaded(EMPTY)
    } catch (e: Exception) {
        Log.w(TAG, "Unreadable plans file; set aside, starting with no school years", e)
        Loaded(EMPTY, setAside(file.baseFile))
    }

    /**
     * Synchronized: the debounced background save and the flush on stop may overlap.
     * Never throws: it runs in a background coroutine with no handler, where an exception would end the app.
     */
    @Synchronized
    fun save(data: AppData) {
        var out: FileOutputStream? = null
        try {
            val bytes = JsonCodec.encode(data).encodeToByteArray()
            out = file.startWrite()
            out.write(bytes)
            file.finishWrite(out)
        } catch (e: Exception) {
            out?.let(file::failWrite)
            Log.w(TAG, "Could not save plans", e)
        }
    }

    private companion object {
        const val TAG = "PlanStore"
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
