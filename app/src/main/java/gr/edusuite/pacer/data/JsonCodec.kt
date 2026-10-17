package gr.edusuite.pacer.data

import gr.edusuite.pacer.engine.collapse
import gr.edusuite.pacer.engine.parseRanges
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.Book
import gr.edusuite.pacer.model.Closure
import gr.edusuite.pacer.model.DoneLesson
import gr.edusuite.pacer.model.Group
import gr.edusuite.pacer.model.Homework
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
 * (missing reads as not). Version 1 files and the web page's localStorage export store a flat
 * list of plans; those still read, through [migratePlans].
 */
object JsonCodec {
    const val VERSION = 4

    fun encode(data: AppData): String = JSONObject()
        .put("version", VERSION)
        .put("activeYear", data.activeYear)
        .put("activeClass", data.activeClass)
        .put("view", data.view)
        .put("years", JSONArray(data.years.map(::year)))
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
            return d.copy(years = sanitiseYears(d.years))
        }
        return AppData(
            years = sanitiseYears(years.rows(::year)),
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
    }

    private fun year(y: SchoolYear) = JSONObject()
        .put("id", y.id).put("name", y.name)
        .put("start", y.start?.toString() ?: "").put("end", y.end?.toString() ?: "")
        .put("closures", closures(y.closures))
        .put("classes", JSONArray(y.classes.map { c ->
            JSONObject().put("id", c.id).put("name", c.name).put("mins", c.mins).put("pace", c.pace)
                .put("groups", groups(c.groups)).put("books", books(c.books))
                .put("homework", homework(c.homework)).put("events", events(c.events))
        }))

    private fun year(o: JSONObject) = SchoolYear(
        id = o.id(),
        name = o.optString("name", "School year"),
        start = o.date("start"),
        end = o.date("end"),
        closures = closures(o.optJSONArray("closures")),
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
    )

    private fun groups(list: List<Group>) = JSONArray(list.map { g ->
        JSONObject().put("id", g.id).put("name", g.name)
            .put("days", JSONArray(g.days.map { it.value % 7 }))
            .put("from", g.from?.toString() ?: "")
            .put("pos", JSONObject(g.pos as Map<*, *>))
            .put("done", JSONArray(g.done.map { l ->
                JSONObject().put("date", l.date.toString())
                    .put("pages", JSONObject(l.pages.filterValues { it.isNotEmpty() }.mapValues { collapse(it.value) } as Map<*, *>))
                    .apply { if (l.edited) put("edited", true) }
            }))
    })

    private fun groups(a: JSONArray?): List<Group> = a.rows { g ->
        val pos = g.optJSONObject("pos")
        Group(
            id = g.id(),
            name = g.optString("name", "Group"),
            days = g.optJSONArray("days").ints().map { DayOfWeek.of(if (it == 0) 7 else it) }.toSet(),
            from = g.date("from"),
            pos = pos?.names().strings().associateWith { pos!!.optInt(it) }.filterValues { it > 0 },
            done = g.optJSONArray("done").rows { l ->
                val date = l.date("date") ?: return@rows null
                val pages = l.optJSONObject("pages")
                DoneLesson(
                    date,
                    pages?.names().strings().associateWith { parseRanges(pages!!.optString(it)).sorted() }.filterValues { it.isNotEmpty() },
                    l.optBoolean("edited", false),
                )
            }.sortedBy { it.date },
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
    })

    private fun books(a: JSONArray?): List<Book> = a.rows { b ->
        Book(b.id(), b.optString("name", "Book"), b.optInt("first", 1), b.optInt("last", 1),
            b.optString("skip", ""), b.date("due"))
    }

    private fun homework(list: List<Homework>) = JSONArray(list.map { h ->
        JSONObject().put("id", h.id).put("groupId", h.groupId).put("bookId", h.bookId)
            .put("from", h.from).put("to", h.to).put("when", h.setOn?.toString() ?: "")
    })

    /** Homework names its group and book; without either it means nothing, so it is left out. */
    private fun homework(a: JSONArray?): List<Homework> = a.rows { h ->
        val group = h.text("groupId") ?: return@rows null
        val book = h.text("bookId") ?: return@rows null
        Homework(h.id(), group, book, h.optInt("from"), h.optInt("to"), h.date("when"))
    }

    private fun events(list: List<TestEvent>) = JSONArray(list.map { e ->
        JSONObject().put("id", e.id).put("name", e.name).put("date", e.date.toString())
            .put("who", e.who).put("consumes", e.consumes)
    })

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
