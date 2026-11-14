package com.example.domain.engine

import android.util.Xml
import com.example.domain.model.DataTable
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

object DatasetParser {

    fun parseCsv(
        name: String,
        rawText: String,
        sourceType: String = "csv",
        existingId: String? = null,
        isCleaned: Boolean = false,
    ): DataTable {
        val cleanText = rawText.replace("\r\n", "\n").replace("\r", "\n").trim()
        require(cleanText.isNotEmpty()) { "Dataset file is empty." }

        val delimiter = detectDelimiter(cleanText.lineSequence().firstOrNull().orEmpty())
        val parsedRows = parseDelimitedRecords(cleanText, delimiter)
        require(parsedRows.isNotEmpty()) { "No tabular rows detected." }

        val rawHeaders = parsedRows.first().mapIndexed { idx, h ->
            h.trim().ifBlank { "Column_${idx + 1}" }
        }
        val headers = deduplicateHeaders(rawHeaders)
        val dataRows = parsedRows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .map { row ->
                if (row.size == headers.size) row
                else if (row.size < headers.size) row + List(headers.size - row.size) { "" }
                else row.take(headers.size)
            }

        val id = existingId ?: "ds_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        return DataQualityEngine.buildTable(
            id = id,
            name = name,
            sourceType = sourceType,
            headers = headers,
            rows = dataRows,
            isCleaned = isCleaned,
        )
    }

    fun parseXlsx(name: String, inputStream: InputStream): DataTable {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && (
                        entry.name == "xl/sharedStrings.xml" ||
                            entry.name.startsWith("xl/worksheets/sheet")
                        )
                ) {
                    entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: entries.entries.firstOrNull { it.key.startsWith("xl/worksheets/sheet") }?.value
            ?: throw IllegalArgumentException("No worksheet found inside XLSX workbook.")

        val rows = parseSheetXml(sheetBytes, sharedStrings)
        require(rows.isNotEmpty()) { "Worksheet contains no rows." }

        val headers = deduplicateHeaders(
            rows.first().mapIndexed { idx, v -> v.trim().ifBlank { "Column_${idx + 1}" } }
        )
        val body = rows.drop(1).map { r ->
            if (r.size == headers.size) r
            else if (r.size < headers.size) r + List(headers.size - r.size) { "" }
            else r.take(headers.size)
        }

        return DataQualityEngine.buildTable(
            id = "ds_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            name = name,
            sourceType = "xlsx",
            headers = headers,
            rows = body,
            isCleaned = false,
        )
    }

    fun parseSpreadsheetBytes(
        name: String,
        bytes: ByteArray,
        mimeType: String? = null,
    ): DataTable {
        require(bytes.isNotEmpty()) { "Selected file '$name' is empty." }

        val lowerName = name.lowercase()
        val lowerMime = mimeType?.lowercase().orEmpty()

        // ZIP magic bytes (PK\x03\x04) indicate .xlsx OOXML workbook regardless of filename
        val isZip = bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()

        if (isZip || lowerName.endsWith(".xlsx") || lowerMime.contains("spreadsheetml")) {
            return parseXlsx(name, bytes.inputStream())
        }

        // OLE2 Compound Document magic bytes (D0 CF 11 E0) indicate legacy binary .xls
        val isOle2BinaryXls = bytes.size >= 4 &&
            bytes[0] == 0xD0.toByte() &&
            bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() &&
            bytes[3] == 0xE0.toByte()

        if (isOle2BinaryXls) {
            throw IllegalArgumentException(
                "Legacy binary .xls format is not supported directly. Please export or save as .xlsx or .csv and re-upload."
            )
        }

        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val trimmedStart = text.trimStart()
        if (trimmedStart.startsWith("<?xml", ignoreCase = true) &&
            trimmedStart.contains("<Workbook", ignoreCase = true)
        ) {
            return parseExcel2003Xml(name, text)
        }

        val srcType = when {
            lowerName.endsWith(".tsv") || lowerMime.contains("tab-separated") -> "tsv"
            else -> "csv"
        }
        return parseCsv(name = name, rawText = text, sourceType = srcType)
    }

    private fun parseExcel2003Xml(name: String, xmlText: String): DataTable {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(xmlText.reader())
        var eventType = parser.eventType
        var currentRow = mutableListOf<String>()
        var inRow = false
        var inData = false
        val cellBuf = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val localName = parser.name.substringAfter(':')
                    when (localName) {
                        "Row" -> {
                            inRow = true
                            currentRow = mutableListOf()
                        }
                        "Data" -> if (inRow) {
                            inData = true
                            cellBuf.clear()
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inData) cellBuf.append(parser.text.orEmpty())
                }
                XmlPullParser.END_TAG -> {
                    val localName = parser.name.substringAfter(':')
                    when (localName) {
                        "Data" -> if (inData) {
                            currentRow.add(cellBuf.toString().trim())
                            inData = false
                        }
                        "Row" -> if (inRow) {
                            if (currentRow.any { it.isNotBlank() }) {
                                rows.add(currentRow.toList())
                            }
                            inRow = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        require(rows.isNotEmpty()) { "Excel workbook contains no rows." }
        val maxCols = rows.maxOf { it.size }.coerceAtLeast(1)
        val headers = deduplicateHeaders(
            rows.first().mapIndexed { idx, v -> v.trim().ifBlank { "Column_${idx + 1}" } } +
                List((maxCols - rows.first().size).coerceAtLeast(0)) { idx -> "Column_${rows.first().size + idx + 1}" }
        )
        val body = rows.drop(1).map { r ->
            if (r.size == headers.size) r
            else if (r.size < headers.size) r + List(headers.size - r.size) { "" }
            else r.take(headers.size)
        }
        return DataQualityEngine.buildTable(
            id = "ds_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            name = name,
            sourceType = "xlsx",
            headers = headers,
            rows = body,
            isCleaned = false,
        )
    }

    fun toCsvString(table: DataTable): String {
        val sb = StringBuilder()
        sb.appendLine(table.headers.joinToString(",") { escapeCsv(it) })
        for (row in table.rows) {
            sb.appendLine(row.joinToString(",") { escapeCsv(it) })
        }
        return sb.toString()
    }

    fun sampleRetailDataset(): DataTable {
        val csv = """
            Date,Region,Category,Product,CustomerSegment,Units,UnitPrice,Revenue,Cost,Profit
            2026-01-05,North America,Enterprise SaaS,DataCore Pro,Enterprise,18,1200,21600,6480,15120
            2026-01-12,Europe,Cloud Infra,ComputeMesh X,Mid-Market,25,650,16250,7312,8938
            2026-01-19,APAC,AI Add-ons,AutoPilot LLM,Enterprise,14,950,13300,3325,9975
            2026-01-24,LATAM,Support,Priority SLA,SMB,30,300,9000,3600,5400
            2026-02-04,North America,Enterprise SaaS,DataCore Pro,Enterprise,21,1200,25200,7560,17640
            2026-02-11,Europe,Cloud Infra,ComputeMesh X,Mid-Market,27,650,17550,7897,9653
            2026-02-16,APAC,AI Add-ons,AutoPilot LLM,Enterprise,16,950,15200,3800,11400
            2026-02-22,LATAM,Support,Priority SLA,SMB,28,300,8400,3360,5040
            2026-03-03,North America,Enterprise SaaS,DataCore Pro,Enterprise,24,1200,28800,8640,20160
            2026-03-09,Europe,AI Add-ons,AutoPilot LLM,Enterprise,19,950,18050,4512,13538
            2026-03-17,APAC,Cloud Infra,ComputeMesh X,Mid-Market,32,650,20800,9360,11440
            2026-03-25,LATAM,Enterprise SaaS,DataCore Lite,SMB,22,500,11000,3850,7150
            2026-04-06,North America,Enterprise SaaS,DataCore Pro,Enterprise,26,1200,31200,9360,21840
            2026-04-14,Europe,Cloud Infra,ComputeMesh X,Mid-Market,29,650,18850,8482,10368
            2026-04-20,APAC,AI Add-ons,AutoPilot LLM,Enterprise,21,950,19950,4987,14963
            2026-04-27,LATAM,Support,Priority SLA,SMB,31,300,9300,3720,5580
            2026-05-05,North America,AI Add-ons,AutoPilot LLM,Enterprise,28,950,26600,6650,19950
            2026-05-13,Europe,Enterprise SaaS,DataCore Pro,Enterprise,20,1200,24000,7200,16800
            2026-05-19,APAC,Cloud Infra,ComputeMesh X,Mid-Market,34,650,22100,9945,12155
            2026-05-26,LATAM,Enterprise SaaS,DataCore Lite,SMB,25,500,12500,4375,8125
            2026-06-04,North America,Enterprise SaaS,DataCore Pro,Enterprise,31,1200,37200,11160,26040
            2026-06-12,Europe,AI Add-ons,AutoPilot LLM,Enterprise,23,950,21850,5462,16388
            2026-06-18,APAC,Enterprise SaaS,DataCore Pro,Enterprise,22,1200,26400,7920,18480
            2026-06-25,LATAM,Support,Priority SLA,SMB,35,300,10500,4200,6300
            2026-07-07,North America,Enterprise SaaS,DataCore Pro,Enterprise,33,1200,39600,11880,27720
            2026-07-14,Europe,Cloud Infra,ComputeMesh X,Mid-Market,31,650,20150,9067,11083
            2026-07-21,APAC,AI Add-ons,AutoPilot LLM,Enterprise,25,950,23750,5937,17813
            2026-07-28,LATAM,Enterprise SaaS,DataCore Lite,SMB,27,500,13500,4725,8775
            2026-08-05,North America,AI Add-ons,AutoPilot LLM,Enterprise,35,950,33250,8312,24938
            2026-08-11,Europe,Enterprise SaaS,DataCore Pro,Enterprise,24,1200,28800,8640,20160
            2026-08-19,APAC,Cloud Infra,ComputeMesh X,Mid-Market,36,650,23400,10530,12870
            2026-08-26,LATAM,Support,Priority SLA,SMB,29,300,8700,3480,5220
            2026-09-04,North America,Enterprise SaaS,DataCore Pro,Enterprise,30,1200,36000,10800,25200
            2026-09-12,Europe,Cloud Infra,ComputeMesh X,Mid-Market,26,650,16900,7605,9295
            2026-09-18,APAC,AI Add-ons,AutoPilot LLM,Enterprise,20,950,19000,4750,14250
            2026-09-25,LATAM,Enterprise SaaS,DataCore Lite,SMB,21,500,10500,3675,6825
            2026-10-03,North America,Enterprise SaaS,DataCore Pro,Enterprise,22,1200,26400,7920,18480
            2026-10-10,Europe,Cloud Infra,ComputeMesh X,Mid-Market,18,650,11700,5265,6435
            2026-10-17,APAC,AI Add-ons,AutoPilot LLM,Enterprise,15,950,14250,3562,10688
            2026-10-24,LATAM,,Priority SLA,SMB,19,300,5700,2280,3420
            2026-10-24,LATAM,,Priority SLA,SMB,19,300,5700,2280,3420
            2026-11-02,North America,Enterprise SaaS,DataCore Pro,Enterprise,19,1200,22800,6840,15960
            2026-11-06,Europe,Cloud Infra,ComputeMesh X,Mid-Market,15,650,9750,4387,5363
            2026-11-09,APAC,AI Add-ons,AutoPilot LLM,Enterprise,,950,11400,2850,8550
            2026-11-11,North America,Enterprise SaaS,DataCore Pro,Enterprise,95,1200,114000,34200,79800
        """.trimIndent()
        return parseCsv(
            name = "Sample: Global SaaS Sales 2026",
            rawText = csv,
            sourceType = "sample",
            existingId = "sample_global_saas_2026",
        )
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun detectDelimiter(firstLine: String): Char {
        val commaCount = firstLine.count { it == ',' }
        val semiCount = firstLine.count { it == ';' }
        val tabCount = firstLine.count { it == '\t' }
        return when {
            tabCount > commaCount && tabCount > semiCount -> '\t'
            semiCount > commaCount -> ';'
            else -> ','
        }
    }

    private fun parseDelimitedRecords(text: String, delimiter: Char): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        records.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> currentField.append(ch)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            records.add(currentRow.toList())
        }
        return records
    }

    private fun deduplicateHeaders(raw: List<String>): List<String> {
        val seen = mutableMapOf<String, Int>()
        return raw.map { h ->
            val count = seen.getOrDefault(h, 0)
            seen[h] = count + 1
            if (count == 0) h else "${h}_${count + 1}"
        }
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var eventType = parser.eventType
        var insideSi = false
        val currentText = StringBuilder()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") {
                        insideSi = true
                        currentText.clear()
                    } else if (insideSi && parser.name == "t") {
                        currentText.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si") {
                        result.add(currentText.toString())
                        insideSi = false
                    }
                }
            }
            eventType = parser.next()
        }
        return result
    }

    private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var eventType = parser.eventType
        var currentRow = mutableListOf<String>()
        var currentCellColIndex = 0
        var currentCellType = ""
        var currentValue = ""
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableListOf()
                        }
                        "c" -> {
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            currentCellColIndex = columnIndexFromRef(ref)
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentValue = ""
                        }
                        "v", "t" -> {
                            currentValue = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            val resolved = when (currentCellType) {
                                "s" -> {
                                    val idx = currentValue.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                                }
                                "b" -> if (currentValue == "1") "true" else "false"
                                else -> currentValue
                            }
                            while (currentRow.size < currentCellColIndex) {
                                currentRow.add("")
                            }
                            if (currentRow.size == currentCellColIndex) {
                                currentRow.add(resolved)
                            } else {
                                currentRow[currentCellColIndex] = resolved
                            }
                        }
                        "row" -> {
                            if (currentRow.any { it.isNotBlank() }) {
                                rows.add(currentRow.toList())
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        val maxCols = rows.maxOfOrNull { it.size } ?: 0
        return rows.map { r -> if (r.size < maxCols) r + List(maxCols - r.size) { "" } else r }
    }

    private fun columnIndexFromRef(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }.uppercase()
        if (letters.isEmpty()) return 0
        var col = 0
        for (ch in letters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return (col - 1).coerceAtLeast(0)
    }
}
