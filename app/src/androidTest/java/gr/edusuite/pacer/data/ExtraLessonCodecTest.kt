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
            "homeworkLinksRoundTripAndSchemaTwelveDefaults" to ::testHomeworkLinksRoundTripAndSchemaTwelveDefaults,
            "groupOverridesAndCollectionsRoundTrip" to ::testGroupOverridesAndCollectionsRoundTrip,
            "versionTenDefaultsRemainUnchanged" to ::testVersionTenDefaultsRemainUnchanged,
            "extraRoundTripAndBackup" to ::testExtraRoundTripAndBackup,
            "assignmentRoundTripAndBackup" to ::testAssignmentRoundTripAndBackup,
            "legacyAssignmentsMigrateAndAreNotWrittenAgain" to ::testLegacyAssignmentsMigrateAndAreNotWrittenAgain,
            "schemaElevenKeepsOverridesAndMigratesWork" to ::testSchemaElevenKeepsOverridesAndMigratesWork,
            "malformedAssignmentIsSkippedIndividually" to ::testMalformedAssignmentIsSkippedIndividually,
            "paletteRoundTripsThroughStore" to ::testPaletteRoundTripsThroughStore,
            "planGroupingRoundTripsThroughStore" to ::testPlanGroupingRoundTripsThroughStore,
            "classOrderRoundTripsThroughStore" to ::testClassOrderRoundTripsThroughStore,
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
    fun testHomeworkLinksRoundTripAndSchemaTwelveDefaults() {
        val base = data()
        val y = base.years.single()
        val c = y.classes.single()
        val work = listOf(
            Assignment("h", AssignmentType.HOMEWORK, "Practice", date, date.plusDays(7),
                contentType = AssignmentType.WORKSHEET, linkedId = "w"),
            Assignment("w", AssignmentType.WORKSHEET, "Practice", due = date.plusDays(7), linkedId = "h"))
        val input = base.copy(years = listOf(y.copy(classes = listOf(c.copy(groups = listOf(c.groups.single().copy(assignments = work)))))))
        assertEquals(input, JsonCodec.decode(JsonCodec.encode(input)))
        val old = JSONObject(JsonCodec.encode(input)).put("version", 12)
        val rows = old.getJSONArray("years").getJSONObject(0).getJSONArray("classes").getJSONObject(0)
            .getJSONArray("groups").getJSONObject(0).getJSONArray("assignments")
        for (i in 0 until rows.length()) { rows.getJSONObject(i).remove("contentType"); rows.getJSONObject(i).remove("linkedId") }
        assertEquals(work.map { it.copy(contentType = null, linkedId = null) },
            JsonCodec.decode(old.toString()).years.single().classes.single().groups.single().assignments)
        rows.getJSONObject(0).put("contentType", "UNKNOWN").put("linkedId", "missing")
        assertEquals(null, JsonCodec.decode(old.toString()).years.single().classes.single().groups.single().assignments.first().contentType)
    }
    private fun data(): AppData {
        val g = Group("g", "G", setOf(MONDAY), date, extras = listOf(ExtraLesson("e", date, mapOf("b" to listOf(1, 2, 4)))))
        val c = SchoolClass("c", "C", 60, 2.0, listOf(g), listOf(Book("b", "Book", 1, 20)), emptyList(), emptyList())
        return AppData(listOf(SchoolYear("y", "Y", date, date.plusDays(30), emptyList(), listOf(c))))
    }
    fun testGroupOverridesAndCollectionsRoundTrip() {
        val original = data()
        val y = original.years.single()
        val c = y.classes.single()
        val optional = Book("optional", "Optional", 1, 10, groupOnly = true)
        val g = c.groups.single().copy(end = date.plusDays(14), skip = mapOf("b" to "2-3"), extraBooks = setOf(optional.id))
        val changedClass = c.copy(books = c.books + optional, groups = listOf(g))
        val changedYear = y.copy(classes = listOf(changedClass), collections = listOf(ClassCollection("co", "Collection", setOf(c.id))))
        val changed = original.copy(years = listOf(changedYear))
        assertEquals(changed, JsonCodec.decode(JsonCodec.encode(changed)))
    }

    fun testVersionTenDefaultsRemainUnchanged() {
        val original = data()
        val o = JSONObject(JsonCodec.encode(original)).put("version", 10)
        val y = o.getJSONArray("years").getJSONObject(0)
        y.remove("collections")
        val c = y.getJSONArray("classes").getJSONObject(0)
        val g = c.getJSONArray("groups").getJSONObject(0)
        g.remove("end"); g.remove("skip"); g.remove("extraBooks")
        assertEquals(original, JsonCodec.decode(o.toString()))
    }

    fun testExtraRoundTripAndBackup() {
        val data = data()
        val read = JsonCodec.decode(JsonCodec.encode(data))
        assertEquals(data, read)
        assertEquals(data.years.single().classes, addYears(data.years, read.years).last().classes)
    }
    fun testAssignmentRoundTripAndBackup() {
        val base = data()
        val c = base.years.single().classes.single()
        val work = AssignmentType.entries.map { type ->
            Assignment(type.name, type, "Read", date, date.plusDays(7),
                mapOf("b" to listOf(2, 3, 5)), "Bring notes", type != AssignmentType.HOMEWORK)
        }
        val incoming = base.copy(years = listOf(base.years.single().copy(classes = listOf(
            c.copy(groups = listOf(c.groups.single().copy(assignments = work)))
        ))))
        val read = JsonCodec.decode(JsonCodec.encode(incoming))
        assertEquals(incoming, read)
        assertEquals(incoming.years.single().classes, addYears(base.years, read.years).last().classes)
    }
    fun testLegacyAssignmentsMigrateAndAreNotWrittenAgain() {
        val o = JSONObject(JsonCodec.encode(data())).put("version", 10)
        val c = o.getJSONArray("years").getJSONObject(0).getJSONArray("classes").getJSONObject(0)
        c.getJSONArray("groups").getJSONObject(0).remove("assignments")
        c.put("homework", org.json.JSONArray().put(JSONObject().put("id", "h").put("groupId", "g")
            .put("bookId", "b").put("from", 3).put("to", 5).put("when", date.toString())))
        c.put("events", org.json.JSONArray().put(JSONObject().put("id", "t").put("name", "Test")
            .put("date", date.toString()).put("who", ALL_GROUPS).put("consumes", true)))
        val read = JsonCodec.decode(o.toString())
        val cls = read.years.single().classes.single()
        assertEquals(emptyList<Homework>(), cls.homework)
        assertEquals(emptyList<TestEvent>(), cls.events)
        assertEquals(2, cls.groups.single().assignments.size)
        assertEquals(date, cls.groups.single().assignments.first().setOn)
        val written = JSONObject(JsonCodec.encode(read)).getJSONArray("years").getJSONObject(0)
            .getJSONArray("classes").getJSONObject(0)
        assertEquals(false, written.has("homework"))
        assertEquals(false, written.has("events"))
        assertEquals(read, JsonCodec.decode(JsonCodec.encode(read)))
        val store = PlanStore(java.io.File(context.cacheDir, "assignment-codec-test").apply { mkdirs() })
        store.save(read)
        assertEquals(read, store.load().data)
    }
    fun testSchemaElevenKeepsOverridesAndMigratesWork() {
        val base = data()
        val y = base.years.single()
        val c = y.classes.single()
        val group = c.groups.single().copy(end = date.plusDays(10), skip = mapOf("b" to "2"), extraBooks = setOf("optional"))
        val original = base.copy(years = listOf(y.copy(
            collections = listOf(ClassCollection("collection", "Classes", setOf(c.id))),
            classes = listOf(c.copy(groups = listOf(group), books = c.books + Book("optional", "Optional", 1, 8, groupOnly = true)))
        )))
        val o = JSONObject(JsonCodec.encode(original)).put("version", 11)
        val cls = o.getJSONArray("years").getJSONObject(0).getJSONArray("classes").getJSONObject(0)
        cls.getJSONArray("groups").getJSONObject(0).remove("assignments")
        cls.put("homework", org.json.JSONArray().put(JSONObject().put("id", "h").put("groupId", "g")
            .put("bookId", "b").put("from", 1).put("to", 3).put("when", date.toString())))
        val read = JsonCodec.decode(o.toString())
        val out = read.years.single().classes.single()
        assertEquals(original.years.single().collections, read.years.single().collections)
        assertEquals(group.end, out.groups.single().end)
        assertEquals(group.skip, out.groups.single().skip)
        assertEquals(group.extraBooks, out.groups.single().extraBooks)
        assertEquals(true, out.books.last().groupOnly)
        assertEquals(mapOf("b" to listOf(1, 2, 3)), out.groups.single().assignments.single().pages)
        assertEquals(read, JsonCodec.decode(JsonCodec.encode(read)))
    }
    fun testMalformedAssignmentIsSkippedIndividually() {
        val o = JSONObject(JsonCodec.encode(data()))
        val group = o.getJSONArray("years").getJSONObject(0).getJSONArray("classes").getJSONObject(0)
            .getJSONArray("groups").getJSONObject(0)
        group.put("assignments", org.json.JSONArray()
            .put(JSONObject().put("id", "bad").put("type", "UNKNOWN"))
            .put(JSONObject().put("id", "ok").put("type", "WORKSHEET").put("title", "Read")
                .put("due", "broken")))
        val read = JsonCodec.decode(o.toString()).years.single().classes.single().groups.single()
        assertEquals(listOf(Assignment("ok", AssignmentType.WORKSHEET, "Read")), read.assignments)
        assertEquals(data().years.single().classes.single().groups.single().extras, read.extras)
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
        assertEquals(ImportResult.Newer, JsonCodec.readImport(JSONObject(JsonCodec.encode(data())).put("version", JsonCodec.VERSION + 1).toString()))
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

    fun testClassOrderRoundTripsThroughStore() {
        val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        assertEquals(ClassOrder.YOUR_ORDER, SettingsStore(context).load().classOrder)
        for (order in ClassOrder.entries) {
            SettingsStore(context).save(Settings(classOrder = order))
            assertEquals(order.name, prefs.getString("classOrder", null))
            assertEquals(order, SettingsStore(context).load().classOrder)
        }
        prefs.edit().putString("classOrder", "unknown").commit()
        assertEquals(ClassOrder.YOUR_ORDER, SettingsStore(context).load().classOrder)
        prefs.edit().putInt("classOrder", 7).commit()
        assertEquals(ClassOrder.YOUR_ORDER, SettingsStore(context).load().classOrder)
        prefs.edit().clear().commit()
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
