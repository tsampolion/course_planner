package gr.edusuite.pacer.data

import gr.edusuite.pacer.model.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate

/**
 * Platform checks (real org.json) that a backup this version exports is one it imports, at a scale above the old
 * 5 MiB limit. The data is generated here: invented classes and groups, never a teacher's records.
 */
object BackupRoundTripChecks {
    private const val OLD_LIMIT = 5L * 1024 * 1024
    private val date = LocalDate.of(2027, 1, 6)

    /** [years] years of 8 classes of 15 groups, each group with a custom plan of [lessons] noted lessons. */
    private fun synthetic(years: Int, lessons: Int): AppData = AppData(
        (1..years).map { y ->
            SchoolYear("y$y", "Year $y", date, date.plusDays(300), emptyList(), (1..8).map { c ->
                val books = listOf(Book("sb", "Student's Book", 1, 160), Book("wb", "Workbook", 1, 120))
                val groups = (1..15).map { g ->
                    val plan = CustomPlan(true, (1..lessons).map { l ->
                        PlannedLesson("y$y-c$c-g$g-l$l", mapOf("sb" to listOf(l, l + 1), "wb" to listOf(l)),
                            note = "Επανάληψη μονάδας $l, ασκήσεις για το σπίτι", touched = true)
                    })
                    Group("y$y-c$c-g$g", "Group $g", setOf(MONDAY), date, custom = plan)
                }
                SchoolClass("y$y-c$c", "Class $c", 60, 2.0, groups, books, emptyList(), emptyList())
            })
        },
    )

    private fun lessonCount(data: AppData) = data.years.sumOf { y -> y.classes.sumOf { c -> c.groups.sumOf { it.custom?.lessons?.size ?: 0 } } }

    private fun read(bytes: ByteArray, limit: Long = MAX_IMPORT_BYTES): BoundedText = readBoundedText(ByteArrayInputStream(bytes), bytes.size.toLong(), limit)

    fun smallBackupRoundTrips() {
        val input = synthetic(1, 2)
        val text = (read(JsonCodec.encode(input).encodeToByteArray()) as BoundedText.Text).value
        val result = JsonCodec.readImport(text)
        check(result is ImportResult.Years && result.years == input.years) { "small backup changed" }
    }

    fun aBackupAboveTheOldLimitRoundTripsWithNothingMissing() {
        val input = synthetic(3, 120)
        val bytes = JsonCodec.encode(input).encodeToByteArray()
        check(bytes.size > OLD_LIMIT) { "test data is only ${bytes.size} bytes" }
        check(bytes.size < MAX_IMPORT_BYTES) { "test data passes the new limit: ${bytes.size}" }
        val text = (read(bytes) as BoundedText.Text).value
        val result = JsonCodec.readImport(text)
        check(result is ImportResult.Years) { "refused: $result" }
        check(result.years == input.years) { "years changed" }
        check(lessonCount(AppData(result.years)) == lessonCount(input)) { "lessons missing" }
    }

    fun aBackupExactlyAtTheLimitReadsAndOneByteOverIsRefused() {
        val bytes = JsonCodec.encode(synthetic(1, 20)).encodeToByteArray()
        check(read(bytes, limit = bytes.size.toLong()) is BoundedText.Text) { "boundary refused" }
        val over = read(bytes, limit = bytes.size - 1L)
        check(over is BoundedText.TooLarge && over.bytes == bytes.size.toLong()) { "over the limit not refused: $over" }
    }

    fun junkAndEndlessInputStayBounded() {
        var reads = 0L
        val endless = object : InputStream() {
            override fun read(): Int = 'x'.code
            override fun read(b: ByteArray, off: Int, len: Int): Int { java.util.Arrays.fill(b, off, off + len, 'x'.code.toByte()); reads += len; return len }
        }
        val limit = 1L shl 20
        val result = readBoundedText(endless, null, limit)
        check(result is BoundedText.TooLarge) { "endless input not refused" }
        check(reads <= limit + 8192) { "read $reads bytes for a $limit limit" }
        check(JsonCodec.readImport("not json at all") == ImportResult.Unreadable) { "junk accepted" }
        check(JsonCodec.readImport("[".repeat(200_000)) == ImportResult.Unreadable) { "deep junk accepted" }
    }
}
