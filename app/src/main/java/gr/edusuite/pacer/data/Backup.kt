package gr.edusuite.pacer.data

import gr.edusuite.pacer.model.Features
import gr.edusuite.pacer.model.Module
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.modulesInUse
import gr.edusuite.pacer.model.newId
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate

/** What reading a file chosen for import gave. Only [Years] changes anything, and only once the teacher picks Add or Replace all. */
sealed interface ImportResult {
    data class Years(val years: List<SchoolYear>) : ImportResult
    /** A readable file that holds no school year. */
    data object NoYears : ImportResult
    /** Written by a newer version of the app; re-saving it here would drop what this version doesn't know. */
    data object Newer : ImportResult
    /** Not JSON or not in a shape the app reads. */
    data object Unreadable : ImportResult
    /** Over [MAX_IMPORT_BYTES]. [bytes] is the file's size when the picker told us, else how much was read before stopping. */
    data class TooLarge(val bytes: Long, val limit: Long) : ImportResult
}

/**
 * The most a file chosen for import may hold. A backup this version exports is far smaller (several school years of
 * eight classes and fifteen groups each come to a few MiB), so the bound only stops a huge or junk file from filling memory.
 */
const val MAX_IMPORT_BYTES = 50L * 1024 * 1024

/** What reading a file's text within a size bound gave. */
sealed interface BoundedText {
    data class Text(val value: String) : BoundedText
    data class TooLarge(val bytes: Long, val limit: Long) : BoundedText
}

/**
 * Reads [input] as UTF-8 without a byte-order mark, stopping once it passes [limit]. [declaredSize] (from the picker,
 * when it knows) refuses an oversized file before any byte is read. Memory stays within about the limit.
 */
fun readBoundedText(input: InputStream, declaredSize: Long? = null, limit: Long = MAX_IMPORT_BYTES): BoundedText {
    if (declaredSize != null && declaredSize > limit) return BoundedText.TooLarge(declaredSize, limit)
    val out = ByteArrayOutputStream(minOf(declaredSize ?: 8192L, limit, 1L shl 20).toInt().coerceAtLeast(16))
    val buf = ByteArray(8192)
    var total = 0L
    while (true) {
        val n = input.read(buf)
        if (n < 0) break
        total += n
        if (total > limit) return BoundedText.TooLarge(total, limit)
        out.write(buf, 0, n)
    }
    return BoundedText.Text(out.toString(Charsets.UTF_8.name()).removePrefix("\uFEFF"))
}

/** Whole megabytes (MiB), rounded up, for the size shown in "too large" messages. */
fun megabytes(bytes: Long): Long = (bytes + 1024 * 1024 - 1) / (1024 * 1024)

/** The name the "Save to" picker suggests for an export. */
fun backupFileName(date: LocalDate): String = "course-pacer-$date.json"

/**
 * The years after Add: [incoming] goes after [current], which is left as it is.
 * A year whose id is taken gets a new one; ids only need to be unique among years, since classes,
 * groups, books and days off are looked up inside their own year. Collection ids are also remapped on clashes,
 * without changing their class references. A year whose name is taken gets
 * " (imported)", or " (imported 2)" and so on, so the two copies can be told apart on Home.
 * Pure Kotlin, so it is unit-tested without org.json.
 */
fun addYears(current: List<SchoolYear>, incoming: List<SchoolYear>): List<SchoolYear> {
    val ids = current.mapTo(HashSet()) { it.id }
    val names = current.mapTo(HashSet()) { it.name }
    val collectionIds = current.flatMapTo(HashSet()) { y -> y.collections.map { it.id } }
    val added = incoming.map { y ->
        var id = y.id
        while (id in ids) id = newId()
        var name = y.name
        if (name in names) {
            name = "${y.name} (imported)"
            var n = 2
            while (name in names) name = "${y.name} (imported ${n++})"
        }
        ids += id
        names += name
        val collections = y.collections.map { c ->
            var collectionId = c.id
            while (!collectionIds.add(collectionId)) collectionId = newId()
            c.copy(id = collectionId)
        }
        y.copy(id = id, name = name, collections = collections)
    }
    return current + added
}

/**
 * The modules [incoming] years use that are off here, in declaration order. Import asks to turn them on first, and
 * Cancel imports nothing (`docs/FEATURE_MODULES_BRIEF.md`, Imports). Only the teacher's modules count, not the data here.
 */
fun modulesNeeded(incoming: List<SchoolYear>, features: Features): Set<Module> =
    Module.entries.filterTo(linkedSetOf()) { it in modulesInUse(incoming) && it !in features.on }
