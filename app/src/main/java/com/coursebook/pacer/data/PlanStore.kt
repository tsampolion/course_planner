package com.coursebook.pacer.data

import android.util.AtomicFile
import android.util.Log
import com.coursebook.pacer.model.AppData
import java.io.File
import java.io.FileOutputStream

/**
 * Keeps every plan in one JSON file in app storage. Writes are atomic, so a crash never leaves half a file.
 * A fresh install, or a file that can't be read, starts with no school years.
 */
class PlanStore(dir: File) {
    private val file = AtomicFile(File(dir, "plans.json"))

    fun load(): AppData = try {
        JsonCodec.decode(file.readFully().decodeToString())
    } catch (e: java.io.FileNotFoundException) {
        EMPTY
    } catch (e: Exception) {
        Log.w(TAG, "Unreadable plans file, kept as plans.json.bad; starting with no school years", e)
        runCatching { file.baseFile.copyTo(File(file.baseFile.path + ".bad"), overwrite = true) }
        EMPTY
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
