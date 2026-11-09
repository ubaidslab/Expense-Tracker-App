package com.example.kharcha.report

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.kharcha.data.Entry
import java.io.ByteArrayOutputStream

/**
 * Monthly report ka PDF. Android ki built-in PdfDocument API use karta hai.
 * Note: default font Roman Urdu/English ke liye theek hai. Urdu script ke liye
 * custom font file ki zaroorat hogi (V2).
 */
object PdfReport {

    private const val PAGE_W = 595
    private const val PAGE_H = 842

    fun build(title: String, footer: String, entries: List<Entry>): ByteArray {
        val black = android.graphics.Color.BLACK

        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
            color = black
        }
        val boldPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
            color = black
        }
        val textPaint = Paint().apply {
            textSize = 11f
            isAntiAlias = true
            color = black
        }

        val lines = mutableListOf<Pair<String, Paint>>()
        val total = entries.sumOf { it.amount }

        lines += title to titlePaint
        lines += "Total: Rs ${fmt(total)}  (${entries.size} entries)" to boldPaint
        lines += "" to textPaint
        lines += "Category totals" to boldPaint
        entries.groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .forEach { lines += "   ${it.key}: Rs ${fmt(it.value)}" to textPaint }

        lines += "" to textPaint
        lines += "Entries" to boldPaint
        entries.sortedBy { it.entryDate }.forEach { e ->
            val tag = if (e.tag.isNotBlank()) "  #${e.tag}" else ""
            lines += "${e.entryDate}   Rs ${fmt(e.amount)}   ${e.item}   [${e.category}]$tag" to textPaint
            if (e.note.isNotBlank()) lines += "        ${e.note}" to textPaint
        }

        if (footer.isNotBlank()) {
            lines += "" to textPaint
            lines += footer to textPaint
        }

        val doc = PdfDocument()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var y = 50f

        lines.forEach { (text, paint) ->
            if (y > PAGE_H - 40) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
                y = 50f
            }
            page.canvas.drawText(text, 40f, y, paint)
            y += paint.textSize + 8f
        }
        doc.finishPage(page)

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun fmt(v: Double): String = "%,.0f".format(v)
}
