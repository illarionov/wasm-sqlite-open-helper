/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDatabaseJournalMode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDatabaseSyncMode

/**
 * Provides access to SQLite functions that affect all database connection,
 * such as memory management.
 *
 * The native code associated with SQLiteGlobal is also sets global configuration options
 * using sqlite3_config() then calls sqlite3_initialize() to ensure that the SQLite
 * library is properly initialized exactly once before any other framework or application
 * code has a chance to run.
 *
 * Verbose SQLite logging is enabled if the "log.tag.SQLiteLog" property is set to "V".
 * (per [SQLiteDebug.DEBUG_SQL_LOG]).
 *
 */
@Suppress("OBJECT_NAME_INCORRECT")
internal object SQLiteGlobal {
    /**
     * When opening a database, if the WAL file is larger than this size, we'll truncate it.
     *
     * (If it's 0, we do not truncate.)
     */
    const val WAL_TRUNCATE_SIZE: Long = 1048576

    // values derived from:
    // https://android.googlesource.com/platform/frameworks/base.git/+/master/core/res/res/values/config.xml
    const val DEFAULT_PAGE_SIZE: Int = 1024

    /**
     * Gets the journal size limit in bytes.
     */
    const val JOURNAL_SIZE_LIMIT: Int = 524288

    /**
     * Gets the WAL auto-checkpoint integer in database pages.
     */
    const val WAL_AUTO_CHECKPOINT: Int = 1000

    /**
     * Gets the connection pool size when in WAL mode.
     */
    const val WAL_CONNECTION_POOL_SIZE: Int = 10

    /**
     * Amount of heap memory that will be by all database connections within a single process.
     *
     * Set to 8MB in AOSP.
     *
     * https://www.sqlite.org/c3ref/hard_heap_limit64.html
     */
    const val SOFT_HEAP_LIMIT: Long = 8 * 1024 * 1024

    /**
     * Gets the default journal mode when WAL is not in use.
     */
    val DEFAULT_JOURNAL_MODE: SqliteDatabaseJournalMode = SqliteDatabaseJournalMode.TRUNCATE

    /**
     * Gets the default database synchronization mode when WAL is not in use.
     */
    val DEFAULT_SYNC_MODE: SqliteDatabaseSyncMode = SqliteDatabaseSyncMode.FULL

    /**
     * Gets the database synchronization mode when in WAL mode.
     */
    val WAL_SYNC_MODE: SqliteDatabaseSyncMode = SqliteDatabaseSyncMode.NORMAL
}
