package gr.edusuite.pacer.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.outputColumns
import gr.edusuite.pacer.engine.fixed2
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.monthShort
import gr.edusuite.pacer.engine.short
import gr.edusuite.pacer.model.Plan

/**
 * Prints every group of every given plan (one per class), one group per page, through the system print dialog
 * (which also offers "Save as PDF"). Uses the platform WebView, so no PDF library ships in the APK.
 */
/** Page margins for printing. */
enum class PrintMargin(val label: String, val mm: Int) {
    NARROW("Narrow", 8), NORMAL("Normal", 15), WIDE("Wide", 25),
}

object PrintSheet {
    // The print job reads the WebView after this call returns; keep it alive until the next job.
    private var webView: WebView? = null

    /** More books than this and the sheet turns landscape, so each book column keeps a readable width. */
    const val MAX_PORTRAIT_BOOKS = 4

    /** Whether any printed group has more book columns than a portrait page holds. One print job has one orientation. */
    fun landscape(plans: List<Plan>): Boolean = plans.any { plan ->
        plan.groups.any { g -> GroupReport(plan, g).let { it.rows.isNotEmpty() && it.outputColumns().size > MAX_PORTRAIT_BOOKS } }
    }

    fun print(context: Context, title: String, plans: List<Plan>, margin: PrintMargin = PrintMargin.NORMAL) {
        val html = html(plans, margin)
        val attributes = PrintAttributes.Builder().apply {
            if (landscape(plans)) setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
        }.build()
        val view = WebView(context)
        webView = view
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView, url: String?) {
                val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val name = "$title schedule"
                pm.print(name, v.createPrintDocumentAdapter(name), attributes)
            }
        }
        view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    fun html(plans: List<Plan>, margin: PrintMargin = PrintMargin.NORMAL): String = buildString {
        val wide = landscape(plans)
        append("<!doctype html><html><head><meta charset=\"utf-8\"><style>@page{margin:${margin.mm}mm")
        if (wide) append(";size:landscape")
        append("}")
        append(
            """
            body{font:10pt sans-serif;color:#000;margin:0}
            h3{font:600 14pt Georgia,serif;margin:0 0 2px}
            .meta{font:9pt monospace;color:#333;margin-bottom:4px}
            .note{font:italic 8pt sans-serif;color:#555;margin-bottom:8px}
            table{border-collapse:collapse;width:100%;table-layout:fixed;border:1px solid #aaa}
            th{text-align:left;font-size:8pt;letter-spacing:.06em;text-transform:uppercase;color:#333;padding:4px 8px;border-bottom:1px solid #aaa}
            td{padding:3px 8px;border-bottom:1px solid #ddd}
            th,td{overflow-wrap:anywhere}
            col.n{width:9mm} col.date{width:38mm} col.note{width:30%}
            td.pg,td.n{font-family:monospace}
            tr.brk td{background:#efefef;font-size:8pt;font-weight:600;letter-spacing:.06em;text-transform:uppercase}
            tr.ev td{background:#eee}
            thead{display:table-header-group} tr{break-inside:avoid}
            section{break-after:page} section:last-child{break-after:auto}
            </style></head><body>""",
        )
        for (plan in plans) for (g in plan.groups) {
            val r = GroupReport(plan, g)
            if (r.rows.isEmpty()) continue
            append("<section><h3>").append(esc("${plan.name} — ${g.name}")).append("</h3><div class=meta>")
            append(r.lessons.size).append(" lessons")
            if (r.blocked > 0) append(" (").append(r.blocked).append(" for tests)")
            append(" · ").append(r.pages).append(" pages · ").append(r.rate.fixed2()).append(" per teaching lesson · ")
            append(r.rows.first().date.fmt()).append(" to ").append(r.rows.last().date.fmt()).append("</div>")
            append("<div class=note>").append(esc(PRINT_NOTE)).append("</div>")
            val notes = r.rows.any { it.note.isNotEmpty() }
            // Every core book, and a supplementary book only when this group's table uses it.
            val cols = r.outputColumns()
            append("<table><colgroup><col class=n><col class=date>")
            cols.forEach { append("<col>") }
            if (notes) append("<col class=note>")
            append("</colgroup><thead><tr><th>#</th><th>Date</th>")
            cols.forEach { append("<th>").append(esc(r.plan.books[it].name)).append("</th>") }
            if (notes) append("<th>Note</th>")
            append("</tr></thead><tbody>")
            var month = -1
            for (row in r.rows) {
                val key = row.date.year * 12 + row.date.monthValue
                if (key != month) {
                    month = key
                    append("<tr class=brk><td colspan=").append(2 + cols.size + if (notes) 1 else 0).append(">")
                        .append(row.date.monthShort()).append(' ').append(row.date.year).append("</td></tr>")
                }
                val names = esc(row.events.joinToString(" · ") { it.name })
                val hasPages = row.pages.any { it.isNotEmpty() }
                append(if (row.blocked) "<tr class=ev>" else "<tr>")
                append("<td class=n>").append(row.n).append("</td><td>").append(row.date.fmt()).append(' ')
                    .append(row.date.dayOfWeek.short())
                if (row.extraId != null) append(" <b>[Extra]</b>")
                if ((!row.blocked || hasPages) && names.isNotEmpty()) append(" <b>[").append(names).append("]</b>")
                append("</td>")
                if (row.blocked && !hasPages) {
                    append("<td colspan=").append(cols.size).append("><b>").append(names).append("</b></td>")
                } else {
                    cols.forEach { append("<td class=pg>").append(row.cells[it]).append("</td>") }
                }
                if (notes) append("<td>").append(esc(row.note)).append("</td>")
                append("</tr>")
            }
            append("</tbody></table></section>")
        }
        append("</body></html>")
    }
}
