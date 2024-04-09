/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteStatementType.Companion.ExtendedStatementType.PublicType

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
        private const val PREFIX_GROUP_NUM: Int = 2

        /**
         * A regular expression that matches the first three characters in a SQL statement, after
         * skipping past comments and whitespace.  PREFIX_GROUP_NUM is the regex group that contains
         * the matching prefix string.  If PREFIX_REGEX is changed, PREFIX_GROUP_NUM may require an
         * update too.
         */
        private val PREFIX_REGEX = Regex(
            "(" + // Zero-or more...
                    """\s+""" + //   Leading space
                    """|""" +
                    "--.*?\n" + //   Line comment
                    """|""" +
                    """/\*[\w\W]*?\*/""" + //   Block comment
                    """)*""" +
                    """(\w\w\w)""", // Three word-characters
        )

        internal val SQLiteStatementType.isCacheable: Boolean
            get() = when (this) {
                STATEMENT_SELECT, STATEMENT_UPDATE -> true
                else -> false
            }

        /**
         * Returns one of the following which represent the type of the given SQL statement.
         *
         * @param sql the SQL statement whose type is returned by this method
         */
        fun getSqlStatementType(sql: String): SQLiteStatementType = getSqlStatementTypeExtended(sql).publicType

        /**
         * Return the extended statement type for the SQL statement.  This is not a public API and it
         * can return values that are not publicly visible.
         */
        private fun getSqlStatementTypeExtended(sql: String): ExtendedStatementType {
            var type = categorizeStatement(getSqlStatementPrefixSimple(sql), sql)
            if (type == ExtendedStatementType.StatementComment) {
                type = categorizeStatement(getSqlStatementPrefixExtended(sql), sql)
            }
            return type
        }

        /**
         * The legacy prefix matcher.
         */
        private fun getSqlStatementPrefixSimple(sql: String): String? {
            return sql.trim { it <= ' ' }.let {
                if (it.length >= 3) {
                    it.substring(0, 3).uppercase()
                } else {
                    null
                }
            }
        }

        /**
         * Return the three-letter prefix of a SQL statement, skipping past whitespace and comments.
         * Comments either start with "--" and run to the end of the line or are C-style block
         * comments.  The function returns null if a prefix could not be found.
         */
        private fun getSqlStatementPrefixExtended(sql: String): String? {
            return PREFIX_REGEX.matchAt(sql, 0)?.let { match ->
                checkNotNull(match.groups[PREFIX_GROUP_NUM]).value.uppercase()
            }
        }

        /**
         * Return the extended statement type for the SQL statement.  This is not a public API and it
         * can return values that are not publicly visible.
         */
        @Suppress("CyclomaticComplexMethod")
        private fun categorizeStatement(prefix: String?, sql: String): ExtendedStatementType = when (prefix) {
            null -> PublicType(STATEMENT_OTHER)
            "SEL" -> PublicType(STATEMENT_SELECT)
            "INS", "UPD", "REP", "DEL" -> PublicType(STATEMENT_UPDATE)
            "ATT" -> PublicType(STATEMENT_ATTACH)
            "COM", "END" -> PublicType(STATEMENT_COMMIT)
            "ROL" -> if (sql.uppercase().contains(" TO ")) {
                // Rollback to savepoint.
                PublicType(STATEMENT_OTHER)
            } else {
                PublicType(STATEMENT_ABORT)
            }

            "BEG" -> PublicType(STATEMENT_BEGIN)
            "PRA" -> PublicType(STATEMENT_PRAGMA)
            "CRE" -> ExtendedStatementType.StatementCreate
            "DRO", "ALT" -> PublicType(STATEMENT_DDL)
            "ANA", "DET" -> PublicType(STATEMENT_UNPREPARED)
            "WIT" -> ExtendedStatementType.StatementWith
            else -> if (prefix.startsWith("--") || prefix.startsWith("/*")) {
                ExtendedStatementType.StatementComment
            } else {
                PublicType(STATEMENT_OTHER)
            }
        }

        @Suppress("ConvertObjectToDataObject")
        private sealed class ExtendedStatementType(val publicType: SQLiteStatementType) {
            object StatementWith : ExtendedStatementType(STATEMENT_OTHER)
            object StatementCreate : ExtendedStatementType(STATEMENT_DDL)

            /**
             * An internal statement type denoting a comment.
             */
            object StatementComment : ExtendedStatementType(STATEMENT_OTHER)
            class PublicType(type: SQLiteStatementType) : ExtendedStatementType(type)
        }
    }
}
