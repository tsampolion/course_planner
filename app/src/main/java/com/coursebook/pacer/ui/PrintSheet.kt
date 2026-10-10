package com.coursebook.pacer.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.coursebook.pacer.engine.GroupReport
import com.coursebook.pacer.engine.fixed2
import com.coursebook.pacer.engine.fmt
import com.coursebook.pacer.engine.monthShort
import com.coursebook.pacer.engine.short
import com.coursebook.pacer.model.Plan

/**
 * Prints every group of every given plan (one per class), one group per page, through the system print dialog
 * (which also offers "Save as PDF"). Uses the platform WebView, so no PDF library ships in the APK.
 */
object PrintSheet {
    // The print job reads the WebView after this call returns; keep it alive until the next job.
    private var webView: WebView? = null

    fun print(context: Context, title: String, plans: List<Plan>) {
        val html = html(plans)
        val view = WebView(context)
        webView = view
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView, url: String?) {
                val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val name = "$title schedule"
                pm.print(name, v.createPrintDocumentAdapter(name), PrintAttributes.Builder().build())
            }
        }
        view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    fun html(plans: List<Plan>): String = buildString {
        append(
            """<!doctype html><html><head><meta charset="utf-8"><style>
            body{font:10pt sans-serif;color:#000;margin:0}
            h3{font:600 14pt Georgia,serif;margin:0 0 2px}
            .meta{font:9pt monospace;color:#333;margin-bottom:8px}
            table{border-collapse:collapse;width:100%;border:1px solid #aaa}
            th{text-align:left;font-size:8pt;letter-spacing:.06em;text-transform:uppercase;color:#333;padding:4px 8px;border-bottom:1px solid #aaa}
            td{padding:3px 8px;border-bottom:1px solid #ddd;white-space:nowrap}
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
            append("<table><thead><tr><th>#</th><th>Date</th>")
            plan.books.forEach { append("<th>").append(esc(it.name)).append("</th>") }
            append("</tr></thead><tbody>")
            var month = -1
            for (row in r.rows) {
                val key = row.date.year * 12 + row.date.monthValue
                if (key != month) {
                    month = key
                    append("<tr class=brk><td colspan=").append(2 + plan.books.size).append(">")
                        .append(row.date.monthShort()).append(' ').append(row.date.year).append("</td></tr>")
                }
                val names = esc(row.events.joinToString(" · ") { it.name })
                append(if (row.blocked) "<tr class=ev>" else "<tr>")
                append("<td class=n>").append(row.n).append("</td><td>").append(row.date.fmt()).append(' ')
                    .append(row.date.dayOfWeek.short())
                if (!row.blocked && names.isNotEmpty()) append(" <b>[").append(names).append("]</b>")
                append("</td>")
                if (row.blocked) {
                    append("<td colspan=").append(plan.books.size).append("><b>").append(names).append("</b></td>")
                } else {
                    row.cells.forEach { append("<td class=pg>").append(it).append("</td>") }
                }
                append("</tr>")
            }
            append("</tbody></table></section>")
        }
        append("</body></html>")
    }
}
