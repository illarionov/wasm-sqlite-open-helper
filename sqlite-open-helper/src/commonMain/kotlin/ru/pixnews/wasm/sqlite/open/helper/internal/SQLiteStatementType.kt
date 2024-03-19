/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

internal enum class SQLiteStatementType {
    STATEMENT_SELECT,
    STATEMENT_UPDATE,
    STATEMENT_ATTACH,
    STATEMENT_BEGIN,
    STATEMENT_COMMIT,
    STATEMENT_ABORT,
    STATEMENT_PRAGMA,
    STATEMENT_DDL,
    STATEMENT_UNPREPARED,
    STATEMENT_OTHER,
    ;

    companion object {
        /**
         * Returns one of the following which represent the type of the given SQL statement.
         *
         *  1. [.STATEMENT_SELECT]
         *  1. [.STATEMENT_UPDATE]
         *  1. [.STATEMENT_ATTACH]
         *  1. [.STATEMENT_BEGIN]
         *  1. [.STATEMENT_COMMIT]
         *  1. [.STATEMENT_ABORT]
         *  1. [.STATEMENT_OTHER]
         *
         * @param sql the SQL statement whose type is returned by this method
         * @return one of the values listed above
         */
        @Suppress("CyclomaticComplexMethod", "MagicNumber")
        fun getSqlStatementType(sql: String): SQLiteStatementType {
            if (sql.length < 3) {
                return STATEMENT_OTHER
            }
            // Skip leading comments to properly recognize the statement type
            val statementStart = statementStartIndex(sql)
            val prefixSql = sql.substring(statementStart, (statementStart + 3).coerceAtMost(sql.length))

            return when {
                prefixSql.equals("SEL", ignoreCase = true) -> STATEMENT_SELECT
                prefixSql.equals("WIT", ignoreCase = true) -> STATEMENT_SELECT
                prefixSql.equals("INS", ignoreCase = true) -> STATEMENT_UPDATE
                prefixSql.equals("UPD", ignoreCase = true) -> STATEMENT_UPDATE
                prefixSql.equals("REP", ignoreCase = true) -> STATEMENT_UPDATE
                prefixSql.equals("DEL", ignoreCase = true) -> STATEMENT_UPDATE
                prefixSql.equals("ATT", ignoreCase = true) -> STATEMENT_ATTACH
                prefixSql.equals("COM", ignoreCase = true) -> STATEMENT_COMMIT
                prefixSql.equals("END", ignoreCase = true) -> STATEMENT_COMMIT
                prefixSql.equals("ROL", ignoreCase = true) -> STATEMENT_ABORT
                prefixSql.equals("BEG", ignoreCase = true) -> STATEMENT_BEGIN
                prefixSql.equals("PRA", ignoreCase = true) -> STATEMENT_PRAGMA
                prefixSql.equals("CRE", ignoreCase = true) -> STATEMENT_DDL
                prefixSql.equals("DRO", ignoreCase = true) -> STATEMENT_DDL
                prefixSql.equals("ALT", ignoreCase = true) -> STATEMENT_DDL
                prefixSql.equals("ANA", ignoreCase = true) -> STATEMENT_UNPREPARED
                prefixSql.equals("DET", ignoreCase = true) -> STATEMENT_UNPREPARED
                else -> return STATEMENT_OTHER
            }
        }

        /**
         * @param sql sql statement to check
         * @return index of the SQL statement start, skipping leading comments
         */
        @Suppress("CyclomaticComplexMethod", "IDENTIFIER_LENGTH")
        internal fun statementStartIndex(sql: String): Int {
            var inSingleLineComment = false
            var inMultiLineComment = false
            var statementStartIndex = 0

            for (i in sql.indices) {
                val c = sql[i]
                when {
                    inSingleLineComment -> if (c == '\n') {
                        inSingleLineComment = false
                    }

                    inMultiLineComment -> if (c == '*' && i + 1 < sql.length && sql[i + 1] == '/') {
                        inMultiLineComment = false
                    }

                    c == '-' -> if (i + 1 < sql.length && sql[i + 1] == '-') {
                        inSingleLineComment = true
                    }

                    c == '/' -> if (i + 1 < sql.length && sql[i + 1] == '*') {
                        inMultiLineComment = true
                    }

                    c != '\n' && c != '\r' && c != ' ' && c != '\t' -> {
                        statementStartIndex = i
                        break
                    }
                }
            }
            return statementStartIndex
        }
    }
}
