package gr.edusuite.pacer.data

import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.newId
import java.time.LocalDate

/** What reading a file chosen for import gave. Only [Years] changes anything, and only once the teacher picks Add or Replace all. */
sealed interface ImportResult {
    data class Years(val years: List<SchoolYear>) : ImportResult
    /** A readable file that holds no school year. */
    data object NoYears : ImportResult
    /** Written by a newer version of the app; re-saving it here would drop what this version doesn't know. */
    data object Newer : ImportResult
    /** Not JSON, not in a shape the app reads, or too large to be an export. */
    data object Unreadable : ImportResult
}

/** Larger files can't be exports, so they aren't read into memory. */
const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

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
