/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.sqlite.common.api

import kotlin.jvm.JvmInline

/**
 * SQLite C Interface: Configuration Options
 *
 * https://www.sqlite.org/c3ref/c_config_covering_index_scan.html
 */
@JvmInline
public value class SqliteConfigParameter(
    public val id: Int,
) {
    public companion object {
        /**
         * Sets the threading mode to "Single-thread".
         *
         * Additional `sqlite3_db_config()` arguments: no
         */
        public val SQLITE_CONFIG_SINGLETHREAD: SqliteConfigParameter = SqliteConfigParameter(1)

        /**
         * This option sets the threading mode to "Multi-thread".
         *
         * Additional `sqlite3_db_config()` arguments: no
         */
        public val SQLITE_CONFIG_MULTITHREAD: SqliteConfigParameter = SqliteConfigParameter(2)

        /**
         * Sets the threading mode to "Serialized".
         *
         * Additional `sqlite3_db_config()` arguments: no
         */
        public val SQLITE_CONFIG_SERIALIZED: SqliteConfigParameter = SqliteConfigParameter(3)

        /**
         * Sets low-level memory allocation routines to be used in place of the memory allocation routines built
         * into SQLite.
         *
         * Additional `sqlite3_db_config()` arguments: sqlite3_mem_methods*
         */
        public val SQLITE_CONFIG_MALLOC: SqliteConfigParameter = SqliteConfigParameter(4)

        /**
         * Returns the currently defined memory allocation routines.
         *
         * Additional `sqlite3_db_config()` arguments: sqlite3_mem_methods*
         */
        public val SQLITE_CONFIG_GETMALLOC: SqliteConfigParameter = SqliteConfigParameter(5)

        /**
         * Specifies a memory pool that SQLite can use for the database page cache with the default page cache
         * implementation.
         *
         * Additional `sqlite3_db_config()` arguments: void*, int sz, int N
         */
        public val SQLITE_CONFIG_PAGECACHE: SqliteConfigParameter = SqliteConfigParameter(7)

        /**
         * Specifies a static memory buffer that SQLite will use for all of its dynamic memory allocation needs beyond
         * those provided for by SQLITE_CONFIG_PAGECACHE.
         *
         * Additional `sqlite3_db_config()` arguments: void*, int nByte, int min
         */
        public val SQLITE_CONFIG_HEAP: SqliteConfigParameter = SqliteConfigParameter(8)

        /**
         * Turns on the collection of memory allocation statistics.
         *
         * Additional `sqlite3_db_config()` arguments: boolean
         */
        public val SQLITE_CONFIG_MEMSTATUS: SqliteConfigParameter = SqliteConfigParameter(9)

        /**
         * Sets alternative low-level mutex routines to be used in place the mutex routines built into SQLite.
         *
         * Additional `sqlite3_db_config()` arguments: sqlite3_mutex_methods*
         */
        public val SQLITE_CONFIG_MUTEX: SqliteConfigParameter = SqliteConfigParameter(10)

        /**
         * Returns currently defined mutex routines.
         *
         * Additional `sqlite3_db_config()` arguments: sqlite3_mutex_methods*
         */
        public val SQLITE_CONFIG_GETMUTEX: SqliteConfigParameter = SqliteConfigParameter(11)

        /**
         * Sets the default lookaside size.
         *
         * Additional `sqlite3_db_config()` arguments: int int
         */
        public val SQLITE_CONFIG_LOOKASIDE: SqliteConfigParameter = SqliteConfigParameter(13)

        /**
         * Configures the SQLite global error log.
         *
         * Additional `sqlite3_db_config()` arguments: xFunc, void*
         */
        public val SQLITE_CONFIG_LOG: SqliteConfigParameter = SqliteConfigParameter(16)

        /**
         * Enables URI handling.
         *
         * Additional `sqlite3_db_config()` arguments: int
         */
        public val SQLITE_CONFIG_URI: SqliteConfigParameter = SqliteConfigParameter(17)

        /**
         * Specifies a custom page cache implementation.
         *
         * Additional `sqlite3_db_config()` arguments: sqlite3_pcache_methods2*
         */
        public val SQLITE_CONFIG_PCACHE2: SqliteConfigParameter = SqliteConfigParameter(18)

        /**
         * Returns current page cache implementation.
         *
         * Additional `sqlite3_db_config()` arguments: sqlite3_pcache_methods2*
         */
        public val SQLITE_CONFIG_GETPCACHE2: SqliteConfigParameter = SqliteConfigParameter(19)

        /**
         * Provides the ability to disable covering indexes for full table scans in the query optimizer.
         *
         * Additional `sqlite3_db_config()` arguments: int
         */
        public val SQLITE_CONFIG_COVERING_INDEX_SCAN: SqliteConfigParameter = SqliteConfigParameter(20)

        /**
         * Configures extra logs of all SQLite processing performed by an application.
         *
         * Additional `sqlite3_db_config()` arguments: xSqllog, void*
         */
        public val SQLITE_CONFIG_SQLLOG: SqliteConfigParameter = SqliteConfigParameter(21)

        /**
         * Sets  the default mmap size limit.
         *
         * Additional `sqlite3_db_config()` arguments: int64, int64
         */
        public val SQLITE_CONFIG_MMAP_SIZE: SqliteConfigParameter = SqliteConfigParameter(22)

        /**
         * Specifies the maximum size of the created heap on Windoes platform.
         *
         * Additional `sqlite3_db_config()` arguments: int nByte
         */
        public val SQLITE_CONFIG_WIN32_HEAPSIZE: SqliteConfigParameter = SqliteConfigParameter(23)

        /**
         * Returns the number of extra bytes per page required for each page in [SQLITE_CONFIG_PAGECACHE]
         *
         * Additional `sqlite3_db_config()` arguments: int *psz
         */
        public val SQLITE_CONFIG_PCACHE_HDRSZ: SqliteConfigParameter = SqliteConfigParameter(24)

        /**
         * Sets the "Minimum PMA Size" for the multithreaded sorter.
         *
         * Additional `sqlite3_db_config()` arguments: unsigned int szPma
         */
        public val SQLITE_CONFIG_PMASZ: SqliteConfigParameter = SqliteConfigParameter(25)

        /**
         * Sets the statement journal spill-to-disk threshold.
         *
         * Additional `sqlite3_db_config()` arguments: int nByte
         */
        public val SQLITE_CONFIG_STMTJRNL_SPILL: SqliteConfigParameter = SqliteConfigParameter(26)

        /**
         * Sets a hint to SQLite that it should avoid large memory allocations if possible
         *
         * Additional `sqlite3_db_config()` arguments: boolean
         */
        public val SQLITE_CONFIG_SMALL_MALLOC: SqliteConfigParameter = SqliteConfigParameter(27)

        /**
         * Sets sorter-reference size threshold.
         *
         * Additional `sqlite3_db_config()` arguments: int nByte
         */
        public val SQLITE_CONFIG_SORTERREF_SIZE: SqliteConfigParameter = SqliteConfigParameter(28)

        /**
         * Sets the maximum size for an in-memory database created using sqlite3_deserialize().
         *
         * Additional `sqlite3_db_config()` arguments: int64
         */
        public val SQLITE_CONFIG_MEMDB_MAXSIZE: SqliteConfigParameter = SqliteConfigParameter(29)
    }
}
