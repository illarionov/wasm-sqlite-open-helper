/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.sqlite.common.api

import kotlin.jvm.JvmInline

/**
 * SQLite C Interface: Status Parameters for database connections
 *
 * https://www.sqlite.org/c3ref/c_dbstatus_options.html
 */
@JvmInline
public value class SqliteDbStatusParameter(
    public val id: Int,
) {
    public companion object {
        /**
         * This parameter returns the number of lookaside memory slots currently checked out.
         */
        public val SQLITE_DBSTATUS_LOOKASIDE_USED: SqliteDbStatusParameter = SqliteDbStatusParameter(0)

        /**
         * This parameter returns the approximate number of bytes of heap memory used by all pager caches
         * associated with the database connection.
         * The highwater mark associated with SQLITE_DBSTATUS_CACHE_USED is always 0.
         */
        public val SQLITE_DBSTATUS_CACHE_USED: SqliteDbStatusParameter = SqliteDbStatusParameter(1)

        /**
         * This parameter returns the approximate number of bytes of heap memory used to store the schema for all
         * databases associated with the connection - main, temp, and any ATTACH-ed databases.
         * The full amount of memory used by the schemas is reported, even if the schema memory is shared with other
         * database connections due to shared cache mode being enabled.
         * The highwater mark associated with SQLITE_DBSTATUS_SCHEMA_USED is always 0.
         */
        public val SQLITE_DBSTATUS_SCHEMA_USED: SqliteDbStatusParameter = SqliteDbStatusParameter(2)

        /**
         * This parameter returns the approximate number of bytes of heap and lookaside memory used by all prepared
         * statements associated with the database connection.
         * The highwater mark associated with SQLITE_DBSTATUS_STMT_USED is always 0.
         */
        public val SQLITE_DBSTATUS_STMT_USED: SqliteDbStatusParameter = SqliteDbStatusParameter(3)

        /**
         * This parameter returns the number of malloc attempts that were satisfied using lookaside memory.
         * Only the high-water value is meaningful; the current value is always zero.
         */
        public val SQLITE_DBSTATUS_LOOKASIDE_HIT: SqliteDbStatusParameter = SqliteDbStatusParameter(4)

        /**
         * This parameter returns the number malloc attempts that might have been satisfied using lookaside memory but
         * failed due to the amount of memory requested being larger than the lookaside slot size. Only the high-water
         * value is meaningful; the current value is always zero.
         */
        public val SQLITE_DBSTATUS_LOOKASIDE_MISS_SIZE: SqliteDbStatusParameter = SqliteDbStatusParameter(5)

        /**
         * This parameter returns the number malloc attempts that might have been satisfied using lookaside memory but
         * failed due to all lookaside memory already being in use. Only the high-water value is meaningful;
         * the current value is always zero.
         */
        public val SQLITE_DBSTATUS_LOOKASIDE_MISS_FULL: SqliteDbStatusParameter = SqliteDbStatusParameter(6)

        /**
         * This parameter returns the number of pager cache hits that have occurred.
         * The highwater mark associated with SQLITE_DBSTATUS_CACHE_HIT is always 0.
         */
        public val SQLITE_DBSTATUS_CACHE_HIT: SqliteDbStatusParameter = SqliteDbStatusParameter(7)

        /**
         * This parameter returns the number of pager cache misses that have occurred.
         * The highwater mark associated with SQLITE_DBSTATUS_CACHE_MISS is always 0.
         */
        public val SQLITE_DBSTATUS_CACHE_MISS: SqliteDbStatusParameter = SqliteDbStatusParameter(8)

        /**
         * This parameter returns the number of dirty cache entries that have been written to disk. Specifically,
         * the number of pages written to the wal file in wal mode databases, or the number of pages written to the
         * database file in rollback mode databases. Any pages written as part of transaction rollback or database
         * recovery operations are not included. If an IO or other error occurs while writing a page to disk,
         * the effect on subsequent SQLITE_DBSTATUS_CACHE_WRITE requests is undefined.
         * The highwater mark associated with SQLITE_DBSTATUS_CACHE_WRITE is always 0.
         */
        public val SQLITE_DBSTATUS_CACHE_WRITE: SqliteDbStatusParameter = SqliteDbStatusParameter(9)

        /**
         * This parameter returns zero for the current value if and only if all foreign key constraints
         * (deferred or immediate) have been resolved. The highwater mark is always 0.
         */
        public val SQLITE_DBSTATUS_DEFERRED_FKS: SqliteDbStatusParameter = SqliteDbStatusParameter(10)

        /**
         * This parameter is similar to DBSTATUS_CACHE_USED, except that if a pager cache is shared between two or more
         * connections the bytes of heap memory used by that pager cache is divided evenly between the attached
         * connections. In other words, if none of the pager caches associated with the database connection are shared,
         * this request returns the same value as DBSTATUS_CACHE_USED.
         * Or, if one or more or the pager caches are shared, the value returned by this call will be smaller than
         * returned by DBSTATUS_CACHE_USED.
         * The highwater mark associated with SQLITE_DBSTATUS_CACHE_USED_SHARED is always 0.
         */
        public val SQLITE_DBSTATUS_CACHE_USED_SHARED: SqliteDbStatusParameter = SqliteDbStatusParameter(11)

        /**
         * This parameter returns the number of dirty cache entries that have been written to disk in the middle of
         * a transaction due to the page cache overflowing. Transactions are more efficient if they are written to disk
         * all at once. When pages spill mid-transaction, that introduces additional overhead.
         * This parameter can be used help identify inefficiencies that can be resolved by increasing the cache size.
         */
        public val SQLITE_DBSTATUS_CACHE_SPILL: SqliteDbStatusParameter = SqliteDbStatusParameter(12)

        /**
         * Largest defined DBSTATUS
         */
        public val SQLITE_DBSTATUS_MAX: SqliteDbStatusParameter = SQLITE_DBSTATUS_CACHE_SPILL
    }
}
