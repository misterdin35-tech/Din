package com.example.domain.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.domain.model.BiInsightsReport
import com.example.domain.model.DataTable
import com.example.domain.model.ForecastReport
import java.io.OutputStream

object PdfReportGenerator {

    fun writeExecutivePdf(
        outputStream: OutputStream,
        reportTitle: String,
        table: DataTable,
        biReport: BiInsightsReport,
        forecastReport: ForecastReport?,
        executiveSummary: String,
    ) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val bgPaint = Paint().apply { color = Color.rgb(10, 15, 32) }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        val accentPaint = Paint().apply {
            color = Color.rgb(34, 211, 238)
            strokeWidth = 3f
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(59, 130, 246)
            textSize = 12f
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = Color.rgb(34, 211, 238)
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            textSize = 11f
            isAntiAlias = true
        }

        var y = 48f
        canvas.drawText("DATAPILOT AI — EXECUTIVE ANALYTICS REPORT", 40f, y, subtitlePaint)
        y += 26f
        canvas.drawText(reportTitle.take(48), 40f, y, titlePaint)
        y += 14f
        canvas.drawLine(40f, y, 555f, y, accentPaint)
        y += 28f

        canvas.drawText("Dataset Overview", 40f, y, headingPaint)
        y += 18f
        canvas.drawText("Source: ${table.name} (${table.rowCountString()})", 40f, y, bodyPaint)
        y += 16f
        canvas.drawText(
            "Data Quality Score: ${table.qualityReport.qualityScore}/100  |  Missing: ${table.qualityReport.missingCells}  |  Duplicates: ${table.qualityReport.duplicateRows}  |  Outliers: ${table.qualityReport.outlierCells}",
            40f,
            y,
            bodyPaint,
        )
        y += 28f

        canvas.drawText("Key Performance Metrics", 40f, y, headingPaint)
        y += 18f
        canvas.drawText(
            "Total Revenue/Metric: ${AnalyticsComputeEngine.formatCompact(biReport.totalRevenue)}   |   Avg Record: ${AnalyticsComputeEngine.formatCompact(biReport.avgOrderValue)}",
            40f,
            y,
            bodyPaint,
        )
        y += 16f
        if (biReport.totalProfit != null) {
            canvas.drawText("Total Profit: ${AnalyticsComputeEngine.formatCompact(biReport.totalProfit)}", 40f, y, bodyPaint)
            y += 16f
        }
        y += 12f

        canvas.drawText("Executive Summary", 40f, y, headingPaint)
        y += 18f
        y = drawWrappedText(canvas, executiveSummary.ifBlank { biReport.rootCauseSummary }, 40f, y, 515f, bodyPaint)
        y += 18f

        if (forecastReport != null) {
            canvas.drawText("Predictive Forecast (${forecastReport.metricColumn})", 40f, y, headingPaint)
            y += 18f
            y = drawWrappedText(canvas, forecastReport.summary, 40f, y, 515f, bodyPaint)
            y += 10f
            canvas.drawText(
                "Model Accuracy: MAE=${AnalyticsComputeEngine.formatCompact(forecastReport.mae)} | RMSE=${AnalyticsComputeEngine.formatCompact(forecastReport.rmse)} | R²=${String.format("%.2f", forecastReport.rSquared)}",
                40f,
                y,
                bodyPaint,
            )
            y += 24f
        }

        biReport.breakdowns.firstOrNull()?.let { bd ->
            canvas.drawText("Top Breakdown by ${bd.dimensionName}", 40f, y, headingPaint)
            y += 18f
            bd.items.take(6).forEach { item ->
                val line = "• ${item.label}: ${AnalyticsComputeEngine.formatCompact(item.revenue)} (${String.format("%.1f", item.sharePercent)}%)"
                canvas.drawText(line, 44f, y, bodyPaint)
                y += 16f
            }
        }

        doc.finishPage(page)
        doc.writeTo(outputStream)
        doc.close()
    }

    private fun DataTable.rowCountString(): String = "${rows.size} rows × ${headers.size} cols"

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
    ): Float {
        var y = startY
        val words = text.split(Regex("\\s+"))
        val line = StringBuilder()
        for (w in words) {
            val candidate = if (line.isEmpty()) w else "$line $w"
            if (paint.measureText(candidate) > maxWidth) {
                canvas.drawText(line.toString(), x, y, paint)
                y += 15f
                line.clear()
                line.append(w)
            } else {
                line.clear()
                line.append(candidate)
            }
            if (y > 790f) break
        }
        if (line.isNotEmpty() && y <= 790f) {
            canvas.drawText(line.toString(), x, y, paint)
            y += 15f
        }
        return y
    }
}
