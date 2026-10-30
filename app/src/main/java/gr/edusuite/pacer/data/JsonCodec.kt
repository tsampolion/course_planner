package gr.edusuite.pacer.data

import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.parseRanges
import gr.edusuite.pacer.model.UnitRevision
import gr.edusuite.pacer.model.RevisionKind
import gr.edusuite.pacer.model.Assignment
import gr.edusuite.pacer.model.AssignmentType
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.readBookType
import gr.edusuite.pacer.model.ClassCollection
import gr.edusuite.pacer.model.Closure
import gr.edusuite.pacer.model.CustomPlan
import gr.edusuite.pacer.model.DEFAULT_LAG_LESSONS
import gr.edusuite.pacer.model.DoneLesson
import gr.edusuite.pacer.model.ExtraLesson
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Homework
import gr.edusuite.pacer.model.PlannedLesson
import gr.edusuite.pacer.model.NO_TODAY
import gr.edusuite.pacer.model.Plan
import gr.edusuite.pacer.model.SchoolClass
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.model.TestEvent
import gr.edusuite.pacer.model.newId
import java.time.DayOfWeek
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hand-written JSON mapping on the framework's org.json, so no serialization library ships in the APK.
 * Dates are ISO strings and weekdays use the web page's numbering (0 = Sunday).
 *
 * Version 2 stores school years, each with its classes; version 3 adds each group's done lessons
 * (missing reads as none); version 4 flags done lessons whose pages the teacher recorded by hand
 * (missing reads as not); version 5 adds each group's custom plan (missing reads as none); version 6
 * adds each class's lead book and each book's unit start pages (missing reads as none); version 7 adds the
 * lessons a book may fall behind before a warning (missing reads as the default); version 8 adds supplementary books
 * (`supplementary`, `include`, `countInLesson`) and, on a custom plan's lessons, the books that count in the lesson
 * (`inLesson`) and the core pages to restore (`replacementBase`); version 9 adds the chosen catch-up boost
 * (`catchUpBoost`) on custom plans; version 10 adds date-pinned extra lessons (`extras`); version 11 adds group end/skip/optional books and class collections. Version 12 adds group assignments. Missing reads as none. Version 15 adds each book's `type` (missing reads from the name) and a group's `removedBooks` and `bookDue`. Version 16 adds an assignment's `completed` (homework marked done; missing reads as not done). Version 1
 * files and the web page's localStorage export store a flat list of plans; those still read, through [migratePlans].
 */
object JsonCodec {
    const val VERSION = 16

    fun encode(data: AppData): String = JSONObject()
        .put("version", VERSION)
        .put("activeYear", data.activeYear)
        .put("activeClass", data.activeClass)
        .put("view", data.view)
        .put("years", JSONArray(prepareYears(data.years).map(::year)))
        .toString()

    /** Every file read goes through [sanitiseYears], so a hand-edited or damaged one can't freeze or crash the app. */
    fun decode(text: String): AppData {
        val o = JSONObject(text)
        val view = o.optString("view", AppData.VIEW_SETUP)
        val years = o.optJSONArray("years")
        if (years == null) {
            val plans = o.getJSONArray("plans").rows(::plan)
            require(plans.isNotEmpty())
            val d = migratePlans(plans, o.optString("activePlan", plans.first().id), view)
            return d.copy(years = prepareYears(d.years))
        }
        return AppData(
            years = prepareYears(years.rows(::year)),
            activeYear = o.optString("activeYear", ""),
            activeClass = o.optString("activeClass", ""),
            view = view,
        )
    }

    /**
     * Reads a file chosen for import: an export, a `plans.json` of any version, or the web page's export.
     * Refuses a newer version, which re-saving here would cut down, and anything that doesn't decode.
     * org.json reads nested arrays by recursion, so a file of thousands of `[` overflows the stack;
     * that is an Error, not an Exception, and is caught here too rather than closing the app.
     */
    fun readImport(text: String): ImportResult = try {
        if (JSONObject(text).optInt("version", 1) > VERSION) {
            ImportResult.Newer
        } else {
            val years = decode(text).years
            if (years.isEmpty()) ImportResult.NoYears else ImportResult.Years(years)
        }
    } catch (e: Exception) {
        ImportResult.Unreadable
    } catch (e: StackOverflowError) {
        ImportResult.Unreadable
    } catch (e: OutOfMemoryError) {
        ImportResult.Unreadable
    }

    /** Sanitise legacy endpoints before expanding ranges, then clean the new page maps. */
    private fun prepareYears(years: List<SchoolYear>): List<SchoolYear> = sanitiseYears(
        sanitiseYears(years).map { y -> y.copy(classes = y.classes.map(::migrateAssignments)) }
    )

    private fun year(y: SchoolYear) = JSONObject()
        .put("id", y.id).put("name", y.name)
        .put("start", y.start?.toString() ?: "").put("end", y.end?.toString() ?: "")
        .put("closures", closures(y.closures))
        .put("collections", JSONArray(y.collections.map { c -> JSONObject().put("id", c.id).put("name", c.name).put("classIds", JSONArray(c.classIds.sorted())) }))
        .put("classes", JSONArray(y.classes.map { c ->
            JSONObject().put("id", c.id).put("name", c.name).put("mins", c.mins).put("pace", c.pace)
                .put("groups", groups(c.groups)).put("books", books(c.books))
                .put("unitsEnabled", c.unitsEnabled)
                .put("bookLinks", JSONArray(c.bookLinks.map { JSONArray(it.sorted()) }))
                .also { o -> c.lead?.let { o.put("lead", it) } }
                .also { o -> if (c.lagLessons != DEFAULT_LAG_LESSONS) o.put("lagLessons", c.lagLessons) }
        }))

    private fun year(o: JSONObject) = SchoolYear(
        id = o.id(),
        name = o.optString("name", "School year"),
        start = o.date("start"),
        end = o.date("end"),
        closures = closures(o.optJSONArray("closures")),
        collections = o.optJSONArray("collections").rows { c ->
            ClassCollection(c.id(), c.optString("name", "Collection"), c.optJSONArray("classIds").strings().toSet())
        },
        classes = o.optJSONArray("classes").rows { c ->
            SchoolClass(
                id = c.id(),
                name = c.optString("name", "Class"),
                mins = c.optInt("mins", 60),
                pace = c.pace(),
                groups = groups(c.optJSONArray("groups")),
                books = books(c.optJSONArray("books")),
                homework = homework(c.optJSONArray("homework")),
                events = events(c.optJSONArray("events")),
                lead = c.optString("lead", "").ifBlank { null },
                lagLessons = c.optInt("lagLessons", DEFAULT_LAG_LESSONS),
                unitsEnabled = c.optBoolean("unitsEnabled", false),
                bookLinks = c.optJSONArray("bookLinks").let { a -> if (a == null) emptyList() else (0 until a.length()).mapNotNull { i -> runCatching { a.optJSONArray(i).strings().toSet() }.getOrNull() } },
            )
        },
    )

    /** A version-1 plan. Only read: new files store years. */
    private fun plan(o: JSONObject): Plan = Plan(
        id = o.id(),
        name = o.optString("name", "Plan"),
        start = o.date("start"),
        end = o.date("end"),
        mins = o.optInt("mins", 60),
        pace = o.pace(),
        groups = groups(o.optJSONArray("groups")),
        closures = closures(o.optJSONArray("closures")),
        books = books(o.optJSONArray("books")),
        homework = homework(o.optJSONArray("homework")),
        events = events(o.optJSONArray("events")),
        today = NO_TODAY,
    )

    private fun groups(list: List<Group>) = JSONArray(list.map { g ->
        JSONObject().put("id", g.id).put("name", g.name)
            .put("days", JSONArray(g.days.map { it.value % 7 }))
            .put("from", g.from?.toString() ?: "")
            .put("end", g.end?.toString() ?: "")
            .put("skip", JSONObject(g.skip as Map<*, *>))
            .put("extraBooks", JSONArray(g.extraBooks.sorted()))
            .apply { if (g.removedBooks.isNotEmpty()) put("removedBooks", JSONArray(g.removedBooks.sorted())) }
            .apply { if (g.bookDue.isNotEmpty()) put("bookDue", JSONObject(g.bookDue.mapValues { it.value.toString() } as Map<*, *>)) }
            .put("pos", JSONObject(g.pos as Map<*, *>))
            .apply { g.unitsOverride?.let { put("unitsOverride", it) } }
            .put("unitRevisions", JSONArray(g.unitRevisions.map { r -> JSONObject()
                .put("unitIndex", r.unitIndex).put("kind", r.kind.name)
                .put("assignmentId", r.assignmentId ?: "").put("pages", pages(r.pages)) }))
            .put("done", JSONArray(g.done.map { l ->
                JSONObject().put("date", l.date.toString())
                    .put("pages", pages(l.pages))
                    .apply { if (l.edited) put("edited", true) }
            }))
            .put("extras", JSONArray(g.extras.map { e ->
                JSONObject().put("id", e.id).put("date", e.date.toString()).put("pages", pages(e.pages))
            }))
            .put("assignments", JSONArray(g.assignments.map { a ->
                JSONObject().put("id", a.id).put("type", a.type.name).put("title", a.title)
                    .put("setOn", a.setOn?.toString() ?: "").put("due", a.due?.toString() ?: "")
                    .put("pages", pages(a.pages)).put("note", a.note).put("takesLesson", a.takesLesson)
                    .put("contentType", a.contentType?.name ?: "").put("linkedId", a.linkedId ?: "").put("completed", a.completed)
            }))
            .apply { g.custom?.let { put("custom", custom(it)) } }
    })

    private fun pages(p: Map<String, List<Int>>) = JSONObject(p.filterValues { it.isNotEmpty() }.mapValues { collapse(it.value) } as Map<*, *>)

    private fun pages(o: JSONObject?): Map<String, List<Int>> =
        o?.names().strings().associateWith { parseRanges(o!!.optString(it)).sorted() }.filterValues { it.isNotEmpty() }

    private fun custom(c: CustomPlan) = JSONObject().put("active", c.active).put("lessons", JSONArray(c.lessons.map { l ->
        JSONObject().put("id", l.id).put("pages", pages(l.pages))
            .apply { if (l.note.isNotEmpty()) put("note", l.note) }
            .apply { if (l.touched) put("touched", true) }
            .apply { if (l.inLesson.isNotEmpty()) put("inLesson", JSONArray(l.inLesson.sorted())) }
            .apply { l.replacementBase?.let { put("replacementBase", pages(it)) } }
    })).apply { c.catchUpBoost?.let { put("catchUpBoost", it) } }

    private fun custom(o: JSONObject?): CustomPlan? = o?.let {
        CustomPlan(it.optBoolean("active", true), it.optJSONArray("lessons").rows { l ->
            PlannedLesson(
                l.id(), pages(l.optJSONObject("pages")), l.optString("note", ""), l.optBoolean("touched", false),
                inLesson = l.optJSONArray("inLesson").strings().toSet(),
                // Present but empty means the lesson had no core pages; missing means nothing to restore.
                replacementBase = l.optJSONObject("replacementBase")?.let { pages(it) },
            )
        }, catchUpBoost = it.optInt("catchUpBoost", 0).takeIf { boost -> boost > 0 })
    }

    private fun groups(a: JSONArray?): List<Group> = a.rows { g ->
        val pos = g.optJSONObject("pos")
        Group(
            id = g.id(),
            unitsOverride = if (g.has("unitsOverride") && !g.isNull("unitsOverride")) g.opt("unitsOverride") as? Boolean else null,
            unitRevisions = g.optJSONArray("unitRevisions").rows { r ->
                val kind = runCatching { RevisionKind.valueOf(r.optString("kind", "")) }.getOrNull() ?: return@rows null
                val index = r.optInt("unitIndex", -1).takeIf { it >= 0 } ?: return@rows null
                UnitRevision(index, kind, r.text("assignmentId"), pages(r.optJSONObject("pages")))
            },
            name = g.optString("name", "Group"),
            days = g.optJSONArray("days").ints().map { DayOfWeek.of(if (it == 0) 7 else it) }.toSet(),
            from = g.date("from"),
            end = g.date("end"),
            skip = g.optJSONObject("skip")?.let { skip -> skip.names().strings().associateWith { skip.optString(it, "") } }.orEmpty(),
            extraBooks = g.optJSONArray("extraBooks").strings().toSet(),
            removedBooks = g.optJSONArray("removedBooks").strings().toSet(),
            bookDue = g.optJSONObject("bookDue")?.let { o ->
                o.names().strings().mapNotNull { id -> o.date(id)?.let { id to it } }.toMap()
            }.orEmpty(),
            pos = pos?.names().strings().associateWith { pos!!.optInt(it) }.filterValues { it > 0 },
            done = g.optJSONArray("done").rows { l ->
                val date = l.date("date") ?: return@rows null
                DoneLesson(date, pages(l.optJSONObject("pages")), l.optBoolean("edited", false))
            }.sortedBy { it.date },
            extras = g.optJSONArray("extras").rows { e ->
                val date = e.date("date") ?: return@rows null
                ExtraLesson(e.id(), date, pages(e.optJSONObject("pages")))
            }.sortedBy { it.date },
            custom = runCatching { custom(g.optJSONObject("custom")) }.getOrNull(),
            assignments = g.optJSONArray("assignments").rows { a ->
                val type = runCatching { AssignmentType.valueOf(a.optString("type", "")) }.getOrNull()
                    ?: return@rows null
                Assignment(a.id(), type, a.optString("title", ""), a.date("setOn"), a.date("due"),
                    pages(a.optJSONObject("pages")), a.optString("note", ""), a.optBoolean("takesLesson", false),
                    runCatching { AssignmentType.valueOf(a.optString("contentType", "")) }.getOrNull(),
                    a.optString("linkedId", "").takeIf { it.isNotBlank() }, a.optBoolean("completed", false))
            },
        )
    }

    private fun closures(list: List<Closure>) = JSONArray(list.map { c ->
        JSONObject().put("id", c.id).put("label", c.label)
            .put("from", c.from.toString()).put("to", c.to.toString()).put("who", c.who)
    })

    private fun closures(a: JSONArray?): List<Closure> = a.rows { c ->
        val from = c.date("from") ?: return@rows null
        Closure(c.id(), c.optString("label", "Day off"), from, c.date("to") ?: from, c.optString("who", "all"))
    }

    private fun books(list: List<Book>) = JSONArray(list.map { b ->
        JSONObject().put("id", b.id).put("name", b.name).put("first", b.first).put("last", b.last)
            .put("skip", b.skip).put("due", b.due?.toString() ?: "")
            .put("type", b.type.name)
            .also { o -> if (b.groupOnly) o.put("groupOnly", true) }
            .also { o -> if (b.units.isNotBlank()) o.put("units", b.units) }
            .also { o -> if (b.supplementary) o.put("supplementary", true).put("include", b.include) }
            .also { o -> if (b.countInLesson) o.put("countInLesson", true) }
    })

    private fun books(a: JSONArray?): List<Book> = a.rows { b ->
        val name = b.optString("name", "Book")
        val supplementary = b.optBoolean("supplementary", false)
        Book(b.id(), name, b.optInt("first", 1), b.optInt("last", 1),
            b.optString("skip", ""), b.date("due"), b.optString("units", ""),
            supplementary = supplementary, include = b.optString("include", ""),
            countInLesson = b.optBoolean("countInLesson", false), groupOnly = b.optBoolean("groupOnly", false),
            // Files before schema 15 have no type: a clear name gives one, anything else stays unclassified.
            type = readBookType(b.text("type"), name, supplementary))
    }

    private fun homework(a: JSONArray?): List<Homework> = a.rows { h ->
        val group = h.text("groupId") ?: return@rows null
        val book = h.text("bookId") ?: return@rows null
        Homework(h.id(), group, book, h.optInt("from"), h.optInt("to"), h.date("when"))
    }

    private fun events(a: JSONArray?): List<TestEvent> = a.rows { e ->
        val date = e.date("date") ?: return@rows null
        TestEvent(e.id(), e.optString("name", "Test"), date, e.optString("who", "all"), e.optBoolean("consumes", true))
    }

    private fun JSONObject.pace(): Double = optDouble("pace", 0.0).takeUnless { it.isNaN() } ?: 0.0

    private fun JSONObject.date(key: String): LocalDate? =
        optString(key, "").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** A non-blank string, or null. org.json reads a JSON null as the text "null", so that counts as missing too. */
    private fun JSONObject.text(key: String): String? = optString(key, "").takeIf { it.isNotBlank() && it != "null" }

    /** The item's id, or a new one when it's missing: the item is kept, though anything that pointed at it is lost. */
    private fun JSONObject.id(): String = text("id") ?: newId()

    /**
     * Each object in the array through [read], leaving out those it returns null for or can't read,
     * so one bad record costs only itself, not the whole file.
     */
    private fun <T : Any> JSONArray?.rows(read: (JSONObject) -> T?): List<T> =
        objects().mapNotNull { runCatching { read(it) }.getOrNull() }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONArray?.ints(): List<Int> =
        if (this == null) emptyList() else (0 until length()).map { optInt(it) }.filter { it in 0..7 }
}
