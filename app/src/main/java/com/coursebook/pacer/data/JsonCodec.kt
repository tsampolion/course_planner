package com.coursebook.pacer.data

import com.coursebook.pacer.model.AppData
import com.coursebook.pacer.model.Book
import com.coursebook.pacer.model.Closure
import com.coursebook.pacer.model.Group
import com.coursebook.pacer.model.Homework
import com.coursebook.pacer.model.Plan
import com.coursebook.pacer.model.TestEvent
import java.time.DayOfWeek
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hand-written JSON mapping on the framework's org.json, so no serialization library ships in the APK.
 * Dates are ISO strings and weekdays use the web page's numbering (0 = Sunday), so a plan exported
 * from the page's localStorage reads back unchanged.
 */
object JsonCodec {
    private const val VERSION = 1

    fun encode(data: AppData): String = JSONObject()
        .put("version", VERSION)
        .put("activePlan", data.activePlan)
        .put("view", data.view)
        .put("plans", JSONArray(data.plans.map(::plan)))
        .toString()

    fun decode(text: String): AppData {
        val o = JSONObject(text)
        val plans = o.getJSONArray("plans").objects().map(::plan)
        require(plans.isNotEmpty())
        return AppData(
            plans = plans,
            activePlan = o.optString("activePlan", plans.first().id),
            view = o.optString("view", AppData.VIEW_SETUP),
        )
    }

    private fun plan(p: Plan) = JSONObject()
        .put("id", p.id).put("name", p.name)
        .put("start", p.start?.toString() ?: "").put("end", p.end?.toString() ?: "")
        .put("mins", p.mins).put("pace", p.pace)
        .put("groups", JSONArray(p.groups.map { g ->
            JSONObject().put("id", g.id).put("name", g.name)
                .put("days", JSONArray(g.days.map { it.value % 7 }))
                .put("from", g.from?.toString() ?: "")
                .put("pos", JSONObject(g.pos as Map<*, *>))
        }))
        .put("closures", JSONArray(p.closures.map { c ->
            JSONObject().put("id", c.id).put("label", c.label)
                .put("from", c.from.toString()).put("to", c.to.toString()).put("who", c.who)
        }))
        .put("books", JSONArray(p.books.map { b ->
            JSONObject().put("id", b.id).put("name", b.name).put("first", b.first).put("last", b.last)
                .put("skip", b.skip).put("due", b.due?.toString() ?: "")
        }))
        .put("homework", JSONArray(p.homework.map { h ->
            JSONObject().put("id", h.id).put("groupId", h.groupId).put("bookId", h.bookId)
                .put("from", h.from).put("to", h.to).put("when", h.setOn?.toString() ?: "")
        }))
        .put("events", JSONArray(p.events.map { e ->
            JSONObject().put("id", e.id).put("name", e.name).put("date", e.date.toString())
                .put("who", e.who).put("consumes", e.consumes)
        }))

    private fun plan(o: JSONObject): Plan = Plan(
        id = o.getString("id"),
        name = o.optString("name", "Plan"),
        start = o.date("start"),
        end = o.date("end"),
        mins = o.optInt("mins", 60),
        pace = o.optDouble("pace", 0.0).takeUnless { it.isNaN() } ?: 0.0,
        groups = o.optJSONArray("groups").objects().map { g ->
            val pos = g.optJSONObject("pos")
            Group(
                id = g.getString("id"),
                name = g.optString("name", "Group"),
                days = g.optJSONArray("days").ints().map { DayOfWeek.of(if (it == 0) 7 else it) }.toSet(),
                from = g.date("from"),
                pos = pos?.names().strings().associateWith { pos!!.optInt(it) }.filterValues { it > 0 },
            )
        },
        closures = o.optJSONArray("closures").objects().mapNotNull { c ->
            val from = c.date("from") ?: return@mapNotNull null
            Closure(c.getString("id"), c.optString("label", "Day off"), from, c.date("to") ?: from, c.optString("who", "all"))
        },
        books = o.optJSONArray("books").objects().map { b ->
            Book(b.getString("id"), b.optString("name", "Book"), b.optInt("first", 1), b.optInt("last", 1),
                b.optString("skip", ""), b.date("due"))
        },
        homework = o.optJSONArray("homework").objects().map { h ->
            Homework(h.getString("id"), h.getString("groupId"), h.getString("bookId"),
                h.optInt("from"), h.optInt("to"), h.date("when"))
        },
        events = o.optJSONArray("events").objects().mapNotNull { e ->
            val date = e.date("date") ?: return@mapNotNull null
            TestEvent(e.getString("id"), e.optString("name", "Test"), date, e.optString("who", "all"), e.optBoolean("consumes", true))
        },
    )

    private fun JSONObject.date(key: String): LocalDate? =
        optString(key, "").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONArray?.ints(): List<Int> =
        if (this == null) emptyList() else (0 until length()).map { optInt(it) }.filter { it in 0..7 }
}
