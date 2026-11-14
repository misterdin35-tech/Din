package com.example.domain.engine

import com.example.domain.model.CleaningOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AnalyticsEnginesTest {

    @Test
    fun `dataset parser and cleaning engine remove duplicates impute missing and improve quality`() {
        val sample = DatasetParser.sampleRetailDataset()
        assertTrue(sample.qualityReport.duplicateRows > 0)
        assertTrue(sample.qualityReport.missingCells > 0)
        assertTrue(sample.qualityReport.outlierCells > 0)

        val (cleaned, diff) = DataQualityEngine.cleanDataset(
            sample,
            CleaningOptions(removeDuplicates = true, imputeMissing = true, capOutliers = true, trimWhitespace = true),
        )
        assertEquals(0, cleaned.qualityReport.duplicateRows)
        assertEquals(0, cleaned.qualityReport.missingCells)
        assertTrue(diff.afterScore >= diff.beforeScore)
        assertEquals(100, cleaned.qualityReport.qualityScore)
    }

    @Test
    fun `local sql engine enforces read only and executes aggregations`() {
        val sample = DatasetParser.sampleRetailDataset()

        val blocked = LocalSqlEngine.execute(sample, "DROP TABLE dataset;", readOnly = true)
        assertNotNull(blocked.errorMessage)

        val res = LocalSqlEngine.execute(
            sample,
            "SELECT Region, SUM(Revenue) AS TotalRev FROM dataset GROUP BY Region ORDER BY TotalRev DESC;",
            readOnly = true,
        )
        assertNull(res.errorMessage)
        assertEquals(4, res.rowCount)
        assertEquals("North America", res.rows.first()[0])
    }

    @Test
    fun `bi insights python sandbox and forecasting compute verified results`() {
        val sample = DatasetParser.sampleRetailDataset()

        val bi = AnalyticsComputeEngine.computeBiInsights(sample)
        assertTrue(bi.totalRevenue > 0.0)
        assertNotNull(bi.totalProfit)
        assertTrue(bi.breakdowns.isNotEmpty())

        val pyOut = AnalyticsComputeEngine.executePythonStyleScript(
            sample,
            "df.info()\ndf.describe()\ndf.isnull().sum()\ndf.corr()",
        )
        assertNull(pyOut.error)
        assertTrue(pyOut.stdout.contains("Pearson Correlation"))

        val forecast = ForecastingEngine.forecast(sample, "Revenue", horizonPeriods = 3)
        assertNotNull(forecast)
        assertEquals(3, forecast!!.points.count { it.isForecast })
        assertTrue(forecast.points.all { it.upperBound >= it.predicted && it.predicted >= it.lowerBound })
    }

    @Test
    fun `parseSpreadsheetBytes supports csv bom xlsx zip excel xml and rejects ole2 binary xls`() {
        val csvWithBom = "\uFEFFRegion,Revenue\nNorth America,12500\nEurope,9800\n".toByteArray(Charsets.UTF_8)
        val csvTable = DatasetParser.parseSpreadsheetBytes("regional_sales.csv", csvWithBom, "text/csv")
        assertEquals("csv", csvTable.sourceType)
        assertEquals(listOf("Region", "Revenue"), csvTable.headers)
        assertEquals(2, csvTable.rows.size)

        val xlsxBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
                zip.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
                    <sst><si><t>Product</t></si><si><t>Sales</t></si><si><t>DataCore</t></si></sst>""".toByteArray()
                )
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
                zip.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
                    <worksheet><sheetData>
                        <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row>
                        <row r="2"><c r="A2" t="s"><v>2</v></c><c r="B2"><v>4200</v></c></row>
                    </sheetData></worksheet>""".toByteArray()
                )
                zip.closeEntry()
            }
            baos.toByteArray()
        }
        val xlsxTable = DatasetParser.parseSpreadsheetBytes(
            "metrics.xlsx",
            xlsxBytes,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        )
        assertEquals("xlsx", xlsxTable.sourceType)
        assertEquals(listOf("Product", "Sales"), xlsxTable.headers)
        assertEquals(listOf(listOf("DataCore", "4200")), xlsxTable.rows)

        val excelXml = """<?xml version="1.0"?>
            <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet">
              <Worksheet ss:Name="Sheet1" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
                <Table>
                  <Row><Cell><Data ss:Type="String">Quarter</Data></Cell><Cell><Data ss:Type="String">ARR</Data></Cell></Row>
                  <Row><Cell><Data ss:Type="String">Q1</Data></Cell><Cell><Data ss:Type="Number">50000</Data></Cell></Row>
                </Table>
              </Worksheet>
            </Workbook>""".trimIndent().toByteArray(Charsets.UTF_8)
        val xmlTable = DatasetParser.parseSpreadsheetBytes("legacy_xml.xls", excelXml, "application/vnd.ms-excel")
        assertEquals(listOf("Quarter", "ARR"), xmlTable.headers)
        assertEquals(listOf(listOf("Q1", "50000")), xmlTable.rows)

        val ole2Header = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(), 0x00, 0x00)
        var threw = false
        try {
            DatasetParser.parseSpreadsheetBytes("old_binary.xls", ole2Header, "application/vnd.ms-excel")
        } catch (e: IllegalArgumentException) {
            threw = true
            assertTrue(e.message.orEmpty().contains(".xlsx", ignoreCase = true))
        }
        assertTrue(threw)
    }
}
