/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.internal

import at.released.wasm.sqlite.open.helper.Locale
import at.released.wasm.sqlite.open.helper.OpenFlags
import at.released.wasm.sqlite.open.helper.contains
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDatabaseJournalMode
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDatabaseSyncMode
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Describes how to configure a database.
 *
 * The purpose of this object is to keep track of all of the little
 * configuration settings that are applied to a database after it
 * is opened so that they can be applied to all connections in the
 * connection pool uniformly.
 *
 * Each connection maintains its own copy of this object so it can
 * keep track of which settings have already been applied.
 *
 * @property path The database path.
 * @property openFlags The flags used to open the database.
 * @property locale The database locale.
 * @property maxSqlCacheSize
 *   The maximum size of the prepared statement cache for each database connection.
 *   Must be non-negative.
 *   Default is 25.
 * @property foreignKeyConstraintsEnabled
 *   True if foreign key constraints are enabled.
 *
 *   Default is false.
 * @property perConnectionSql The statements to execute to initialize each connection.
 * @property lookasideSlotSize
 *   The size in bytes of each lookaside slot
 *   If negative, the default lookaside configuration will be used
 * @property lookasideSlotCount
 *   The total number of lookaside memory slots per database connection
 *   If negative, the default lookaside configuration will be used
 * @property journalMode
 *   Journal mode to use when [ENABLE_WRITE_AHEAD_LOGGING] is not set.
 *   Default is returned by [SQLiteGlobal.getDefaultJournalMode]
 * @property syncMode
 *   Synchronous mode to use.
 *   Default is returned by [SQLiteGlobal.getDefaultSyncMode]
 *   or [SQLiteGlobal.getWALSyncMode] depending on journal mode
 */
internal class SQLiteDatabaseConfiguration internal constructor(
    val path: String,
    var openFlags: OpenFlags,
    var locale: Locale = Locale.EN_US,
    var maxSqlCacheSize: Int = 25,
    var foreignKeyConstraintsEnabled: Boolean = false,
    val perConnectionSql: MutableList<Pair<String, List<Any?>>> = mutableListOf(),
    var lookasideSlotSize: Int = -1,
    var lookasideSlotCount: Int = -1,
    var journalMode: SqliteDatabaseJournalMode? = null,
    var syncMode: SqliteDatabaseSyncMode? = null,
    var shouldTruncateWalFile: Boolean = false,
) {
    /**
     * The label to use to describe the database when it appears in logs.
     * This is derived from the path but is stripped to remove PII.
     */
    val label: String by lazy(NONE) {
        stripPathForLogs(path)
    }

    /**
     * Creates a database configuration as a copy of another configuration.
     *
     * @param other The other configuration.
     */
    constructor(other: SQLiteDatabaseConfiguration) : this(
        path = other.path,
        openFlags = other.openFlags,
        locale = other.locale,
    ) {
        updateParametersFrom(other)
    }

    /**
     * Updates the non-immutable parameters of this configuration object
     * from the other configuration object.
     *
     * @param other The object from which to copy the parameters.
     */
    fun updateParametersFrom(other: SQLiteDatabaseConfiguration) {
        require(path == other.path) { "other configuration must refer to the same database." }

        openFlags = other.openFlags
        maxSqlCacheSize = other.maxSqlCacheSize
        locale = other.locale
        foreignKeyConstraintsEnabled = other.foreignKeyConstraintsEnabled
        perConnectionSql.clear()
        perConnectionSql.addAll(other.perConnectionSql)
        lookasideSlotSize = other.lookasideSlotSize
        lookasideSlotCount = other.lookasideSlotCount
        journalMode = other.journalMode
        syncMode = other.syncMode
    }

    internal companion object {
        /**
         * Special path used by in-memory databases.
         */
        const val MEMORY_DB_PATH: String = ":memory:"

        /**
         * Returns true if the database is in-memory.
         */
        val SQLiteDatabaseConfiguration.isInMemoryDb: Boolean
            get() = path.equals(MEMORY_DB_PATH, ignoreCase = true)

        val SQLiteDatabaseConfiguration.isReadOnlyDatabase: Boolean
            get() = openFlags.contains(OpenFlags.OPEN_READONLY)

        val SQLiteDatabaseConfiguration.isLookasideConfigSet: Boolean
            get() = lookasideSlotCount >= 0 && lookasideSlotSize >= 0

        // The pattern we use to strip email addresses from database paths
        // when constructing a label to use in log messages.
        private val EMAIL_IN_DB_PATTERN: Regex = Regex("""[\w.\-]+@[\w.\-]+""")

        private fun stripPathForLogs(path: String): String {
            if (path.indexOf('@') == -1) {
                return path
            }
            return EMAIL_IN_DB_PATTERN.replace(path, "XX@YY")
        }

        /**
         * Resolves the journal mode that should be used when opening a connection to the database.
         *
         * Note: assumes openFlags have already been set.
         *
         * @return Resolved journal mode that should be used for this database connection or null
         * if no journal mode should be set.
         */
        fun SQLiteDatabaseConfiguration.resolveJournalMode(): SqliteDatabaseJournalMode? {
            if (isReadOnlyDatabase) {
                // No need to specify a journal mode when only reading.
                return null
            }

            if (isInMemoryDb) {
                return if (journalMode == SqliteDatabaseJournalMode.OFF) {
                    SqliteDatabaseJournalMode.OFF
                } else {
                    SqliteDatabaseJournalMode.MEMORY
                }
            }

            shouldTruncateWalFile = false

            return if (isWalEnabledInternal()) {
                shouldTruncateWalFile = true
                SqliteDatabaseJournalMode.WAL
            } else {
                // WAL is not explicitly set so use requested journal mode or platform default
                this.journalMode ?: SQLiteGlobal.DEFAULT_JOURNAL_MODE
            }
        }

        /**
         * Resolves the sync mode that should be used when opening a connection to the database.
         *
         * Note: assumes openFlags have already been set.
         *
         * @return Resolved journal mode that should be used for this database connection or null
         * if no journal mode should be set.
         */
        @Suppress("ReturnCount")
        fun SQLiteDatabaseConfiguration.resolveSyncMode(): SqliteDatabaseSyncMode? {
            if (isReadOnlyDatabase) {
                // No sync mode will be used since database will be only used for reading.
                return null
            }

            if (isInMemoryDb) {
                // No sync mode will be used since database will be in volatile memory
                return null
            }

            syncMode?.let {
                return it
            }

            return if (isWalEnabledInternal()) {
                SQLiteGlobal.WAL_SYNC_MODE
            } else {
                SQLiteGlobal.DEFAULT_SYNC_MODE
            }
        }

        private fun SQLiteDatabaseConfiguration.isWalEnabledInternal(): Boolean {
            val walEnabled = openFlags.contains(OpenFlags.ENABLE_WRITE_AHEAD_LOGGING)
            return walEnabled || (journalMode == SqliteDatabaseJournalMode.WAL)
        }
    }
}
