/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import ru.pixnews.wasm.sqlite.open.helper.SQLiteDatabaseJournalMode
import ru.pixnews.wasm.sqlite.open.helper.SQLiteDatabaseSyncMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    const val walTruncateSize: Long = 1048576
    const val checkDbWipe: Boolean = false

    // values derived from:
    // https://android.googlesource.com/platform/frameworks/base.git/+/master/core/res/res/values/config.xml
    const val defaultPageSize: Int = 1024
    internal const val WIPE_CHECK_FILE_SUFFIX = "-wipecheck"

    /**
     * Gets the journal size limit in bytes.
     */
    const val journalSizeLimit: Int = 524288

    /**
     * Gets the WAL auto-checkpoint integer in database pages.
     */
    const val walAutoCheckpoint: Int = 1000

    /**
     * Gets the connection pool size when in WAL mode.
     */
    const val walConnectionPoolSize: Int = 10

    /**
     * Gets the default journal mode when WAL is not in use.
     */
    val defaultJournalMode: SQLiteDatabaseJournalMode = SQLiteDatabaseJournalMode.TRUNCATE

    /**
     * Gets the default database synchronization mode when WAL is not in use.
     */
    val defaultSyncMode: SQLiteDatabaseSyncMode = SQLiteDatabaseSyncMode.FULL

    /**
     * Gets the database synchronization mode when in WAL mode.
     */
    val walSyncMode: SQLiteDatabaseSyncMode = SQLiteDatabaseSyncMode.NORMAL

    /**
     * The default number of milliseconds that SQLite connection is allowed to be idle before it
     * is closed and removed from the pool.
     */
    val idleConnectionTimeout: Duration = 30.seconds
}
