package com.example.domain.engine

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.domain.model.ColumnType
import com.example.domain.model.DataTable
import com.example.domain.model.SqlExecutionResult

object LocalSqlEngine {

    private val forbiddenMutationRegex = Regex(
        "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|REPLACE|ATTACH|DETACH|VACUUM|REINDEX)\\b",
        RegexOption.IGNORE_CASE,
    )

    fun sanitizeIdentifier(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val prefixed = if (cleaned.isEmpty() || cleaned.first().isDigit()) "col_$cleaned" else cleaned
        return prefixed
    }

    fun execute(
        table: DataTable,
        rawSql: String,
        readOnly: Boolean = true,
    ): SqlExecutionResult {
        val sql = rawSql.trim().trimEnd(';')
        if (sql.isBlank()) {
            return SqlExecutionResult(
                sql = rawSql,
                headers = emptyList(),
                rows = emptyList(),
                executionMs = 0L,
                rowCount = 0,
                isReadOnly = readOnly,
                errorMessage = "SQL query cannot be empty.",
            )
        }

        if (readOnly) {
            val upper = sql.uppercase()
            val allowedStart = upper.startsWith("SELECT") ||
                upper.startsWith("WITH") ||
                upper.startsWith("PRAGMA") ||
                upper.startsWith("EXPLAIN")
            if (!allowedStart || forbiddenMutationRegex.containsMatchIn(sql)) {
                return SqlExecutionResult(
                    sql = rawSql,
                    headers = emptyList(),
                    rows = emptyList(),
                    executionMs = 0L,
                    rowCount = 0,
                    isReadOnly = true,
                    errorMessage = "Read-only mode blocks write/DDL statements (INSERT, UPDATE, DELETE, DROP, ALTER). Use SELECT or WITH queries.",
                )
            }
        }

        val startNs = System.nanoTime()
        val db = SQLiteDatabase.create(null)
        return try {
            val colNames = table.headers.map { sanitizeIdentifier(it) }
            val colDefs = table.columnProfiles.mapIndexed { idx, profile ->
                val sqlType = when (profile.type) {
                    ColumnType.NUMERIC -> "REAL"
                    else -> "TEXT"
                }
                "\"${colNames[idx]}\" $sqlType"
            }
            db.execSQL("CREATE TABLE dataset (${colDefs.joinToString(", ")})")

            db.beginTransaction()
            try {
                for (row in table.rows) {
                    val cv = ContentValues(colNames.size)
                    for (idx in colNames.indices) {
                        val rawVal = row.getOrElse(idx) { "" }.trim()
                        if (rawVal.isEmpty()) {
                            cv.putNull(colNames[idx])
                        } else if (table.columnProfiles.getOrNull(idx)?.type == ColumnType.NUMERIC) {
                            val d = rawVal.replace(",", "").toDoubleOrNull()
                            if (d != null) cv.put(colNames[idx], d) else cv.putNull(colNames[idx])
                        } else {
                            cv.put(colNames[idx], rawVal)
                        }
                    }
                    db.insert("dataset", null, cv)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            if (readOnly || sql.uppercase().startsWith("SELECT") || sql.uppercase().startsWith("WITH") || sql.uppercase().startsWith("PRAGMA")) {
                db.rawQuery(sql, null).use { cursor ->
                    val headers = cursor.columnNames.toList()
                    val resultRows = mutableListOf<List<String>>()
                    while (cursor.moveToNext() && resultRows.size < 500) {
                        val rowList = ArrayList<String>(headers.size)
                        for (c in headers.indices) {
                            val cell = when (cursor.getType(c)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> ""
                                android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(c).toString()
                                android.database.Cursor.FIELD_TYPE_FLOAT -> {
                                    val d = cursor.getDouble(c)
                                    if (d % 1.0 == 0.0) d.toLong().toString() else String.format("%.2f", d)
                                }
                                else -> cursor.getString(c) ?: ""
                            }
                            rowList.add(cell)
                        }
                        resultRows.add(rowList)
                    }
                    val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                    SqlExecutionResult(
                        sql = rawSql,
                        headers = headers,
                        rows = resultRows,
                        executionMs = elapsedMs,
                        rowCount = resultRows.size,
                        isReadOnly = readOnly,
                    )
                }
            } else {
                db.execSQL(sql)
                val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                SqlExecutionResult(
                    sql = rawSql,
                    headers = listOf("Status"),
                    rows = listOf(listOf("Statement executed on isolated in-memory table")),
                    executionMs = elapsedMs,
                    rowCount = 1,
                    isReadOnly = false,
                )
            }
        } catch (e: Exception) {
            val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            SqlExecutionResult(
                sql = rawSql,
                headers = emptyList(),
                rows = emptyList(),
                executionMs = elapsedMs,
                rowCount = 0,
                isReadOnly = readOnly,
                errorMessage = e.localizedMessage ?: "SQL execution error",
            )
        } finally {
            db.close()
        }
    }

    fun defaultSuggestedQuery(table: DataTable): String {
        val cat = table.columnProfiles.firstOrNull { it.type == ColumnType.CATEGORICAL }?.name
        val num = table.columnProfiles.firstOrNull {
            it.type == ColumnType.NUMERIC && (it.name.contains("rev", true) || it.name.contains("sales", true) || it.name.contains("amount", true))
        } ?: table.columnProfiles.firstOrNull { it.type == ColumnType.NUMERIC }

        return if (cat != null && num != null) {
            val sCat = sanitizeIdentifier(cat)
            val sNum = sanitizeIdentifier(num.name)
            "SELECT $sCat, COUNT(*) AS Records, ROUND(SUM($sNum), 2) AS Total_$sNum, ROUND(AVG($sNum), 2) AS Avg_$sNum\nFROM dataset\nGROUP BY $sCat\nORDER BY Total_$sNum DESC\nLIMIT 15;"
        } else {
            "SELECT *\nFROM dataset\nLIMIT 25;"
        }
    }
}
