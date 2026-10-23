package gr.edusuite.pacer.data

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import gr.edusuite.pacer.model.*
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import org.json.JSONObject

/** Platform-only instrumentation: real org.json, no test runner or JSON dependencies in the APK. */
class ExtraLessonCodecTest : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val checks = listOf(
            "extraRoundTripAndBackup" to ::testExtraRoundTripAndBackup,
            "paletteRoundTripsThroughStore" to ::testPaletteRoundTripsThroughStore,
            "planGroupingRoundTripsThroughStore" to ::testPlanGroupingRoundTripsThroughStore,
            "versionEightWithoutExtrasReadsUnchanged" to ::testVersionEightWithoutExtrasReadsUnchanged,
            "futureVersionIsRefused" to ::testFutureVersionIsRefused,
            "versionNineCatchUpWithoutExtrasReadsUnchanged" to ::testVersionNineCatchUpWithoutExtrasReadsUnchanged,
            "malformedExtraDoesNotLoseItsGroupOrOtherExtras" to ::testMalformedExtraDoesNotLoseItsGroupOrOtherExtras,
        )
        var failures = 0
        checks.forEachIndexed { i, (name, run) ->
            val status = Bundle().apply {
                putString("id", ExtraLessonCodecTest::class.java.name)
                putString("class", ExtraLessonCodecTest::class.java.name)
                putString("test", name)
                putInt("numtests", checks.size)
                putInt("current", i + 1)
            }
            sendStatus(1, status)
            try {
                run()
                sendStatus(0, status)
            } catch (failure: Throwable) {
                failures++
                status.putString("stack", failure.stackTraceToString())
                sendStatus(-2, status)
            }
        }
        finish(Activity.RESULT_OK, Bundle().apply {
            putString("stream", "${checks.size} codec checks, $failures failures\n")
        })
    }

    private fun assertEquals(expected: Any?, actual: Any?) {
        check(expected == actual) { "Expected <$expected>, got <$actual>" }
    }

    private val date = LocalDate.of(2027, 1, 6)
    private fun data(): AppData {
        val g = Group("g", "G", setOf(MONDAY), date, extras = listOf(ExtraLesson("e", date, mapOf("b" to listOf(1, 2, 4)))))
        val c = SchoolClass("c", "C", 60, 2.0, listOf(g), listOf(Book("b", "Book", 1, 20)), emptyList(), emptyList())
        return AppData(listOf(SchoolYear("y", "Y", date, date.plusDays(30), emptyList(), listOf(c))))
    }
    fun testExtraRoundTripAndBackup() {
        val data = data()
        val read = JsonCodec.decode(JsonCodec.encode(data))
        assertEquals(data, read)
        assertEquals(data.years.single().classes, addYears(data.years, read.years).last().classes)
    }
    fun testVersionEightWithoutExtrasReadsUnchanged() {
        val data = data()
        val o = JSONObject(JsonCodec.encode(data)).put("version", 8)
        o.getJSONArray("years").getJSONObject(0).getJSONArray("classes").getJSONObject(0)
            .getJSONArray("groups").getJSONObject(0).remove("extras")
        val g = JsonCodec.decode(o.toString()).years.single().classes.single().groups.single()
        assertEquals(data.years.single().classes.single().groups.single().copy(extras = emptyList()), g)
    }
    fun testFutureVersionIsRefused() {
        assertEquals(ImportResult.Newer, JsonCodec.readImport(JSONObject(JsonCodec.encode(data())).put("version", 11).toString()))
    }
    fun testMalformedExtraDoesNotLoseItsGroupOrOtherExtras() {
        val o = JSONObject(JsonCodec.encode(data()))
        val extras = o.getJSONArray("years").getJSONObject(0).getJSONArray("classes").getJSONObject(0)
            .getJSONArray("groups").getJSONObject(0).getJSONArray("extras")
        extras.put(JSONObject().put("id", "bad").put("date", "not-a-date"))
        assertEquals(data(), JsonCodec.decode(o.toString()))
    }
    fun testVersionNineCatchUpWithoutExtrasReadsUnchanged() {
        val data = data()
        val o = JSONObject(JsonCodec.encode(data)).put("version", 9)
        val group = o.getJSONArray("years").getJSONObject(0).getJSONArray("classes").getJSONObject(0)
            .getJSONArray("groups").getJSONObject(0)
        group.remove("extras")
        group.put("custom", JSONObject().put("active", true).put("catchUpBoost", 2).put("lessons", org.json.JSONArray()))
        val read = JsonCodec.decode(o.toString()).years.single().classes.single().groups.single()
        assertEquals(emptyList<ExtraLesson>(), read.extras)
        assertEquals(CustomPlan(true, emptyList(), catchUpBoost = 2), read.custom)
    }

    fun testPlanGroupingRoundTripsThroughStore() {
        val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        assertEquals(PlanGrouping.WEEKS, SettingsStore(context).load().planGrouping)
        for (grouping in PlanGrouping.entries) {
            SettingsStore(context).save(Settings(planGrouping = grouping))
            assertEquals(grouping.name, prefs.getString("planGrouping", null))
            assertEquals(grouping, SettingsStore(context).load().planGrouping)
        }
        prefs.edit().putString("planGrouping", "unknown").commit()
        assertEquals(PlanGrouping.WEEKS, SettingsStore(context).load().planGrouping)
        prefs.edit().putInt("planGrouping", 7).commit()
        assertEquals(PlanGrouping.WEEKS, SettingsStore(context).load().planGrouping)
        prefs.edit().clear().commit()
    }

    fun testPaletteRoundTripsThroughStore() {
        val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        assertEquals(PaletteChoice.BONE, SettingsStore(context).load().palette)
        for (palette in PaletteChoice.entries) {
            SettingsStore(context).save(Settings(palette = palette))
            assertEquals(palette.name, prefs.getString("palette", null))
            assertEquals(palette, SettingsStore(context).load().palette)
        }
        prefs.edit().putString("palette", "neon").commit()
        assertEquals(PaletteChoice.BONE, SettingsStore(context).load().palette)
        prefs.edit().putInt("palette", 7).commit()
        assertEquals(PaletteChoice.BONE, SettingsStore(context).load().palette)
        prefs.edit().clear().commit()
    }

}
