/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.ENABLE_LEGACY_COMPATIBILITY_WAL
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.ENABLE_WRITE_AHEAD_LOGGING
import ru.pixnews.wasm.sqlite.open.helper.common.api.Locale
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteCompatibilityWalFlags
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteGlobal

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
 * @property label
 *   The label to use to describe the database when it appears in logs.
 *   This is derived from the path but is stripped to remove PII.
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
internal class SqliteDatabaseConfiguration internal constructor(
    val path: String,
    val label: String = stripPathForLogs(path),
    var openFlags: OpenFlags,
    var locale: Locale?,
    var maxSqlCacheSize: Int = 25,
    var foreignKeyConstraintsEnabled: Boolean = false,
    val perConnectionSql: MutableList<Pair<String, List<Any?>>> = mutableListOf(),
    var lookasideSlotSize: Int = -1,
    var lookasideSlotCount: Int = -1,
    var journalMode: SQLiteDatabaseJournalMode? = null,
    var syncMode: SQLiteDatabaseSyncMode? = null,

    var shouldTruncateWalFile: Boolean = false,
) {
    /**
     * The number of milliseconds that SQLite connection is allowed to be idle before it
     * is closed and removed from the pool.
     *
     * By default, idle connections are not closed
     */
    val idleConnectionTimeoutMs: Long = Long.MAX_VALUE

    /**
     * Creates a database configuration with the required parameters for opening a
     * database and default values for all other parameters.
     *
     * @param path The database path.
     * @param openFlags Open flags for the database, such as [SQLiteDatabase.OPEN_READWRITE].
     * @param defaultLocale Initial locale
     */
    constructor(
        openFlags: OpenFlags,
        defaultLocale: Locale,
    ) : this(
        path = MEMORY_DB_PATH,
        openFlags = openFlags,
        locale = defaultLocale,
    )

    /**
     * Creates a database configuration as a copy of another configuration.
     *
     * @param other The other configuration.
     */
    constructor(other: SqliteDatabaseConfiguration) : this(
        path = other.path,
        label = other.label,
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
    fun updateParametersFrom(other: SqliteDatabaseConfiguration?) {
        requireNotNull(other) { "other must not be null." }
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

    public companion object {
        /**
         * Special path used by in-memory databases.
         */
        public const val MEMORY_DB_PATH: String = ":memory:"

        /**
         * Returns true if the database is in-memory.
         */
        public val SqliteDatabaseConfiguration.isInMemoryDb: Boolean
            get() = path.equals(MEMORY_DB_PATH, ignoreCase = true)

        public val SqliteDatabaseConfiguration.isReadOnlyDatabase: Boolean
            get() = openFlags.contains(OpenFlags.OPEN_READONLY)

        public val SqliteDatabaseConfiguration.isLegacyCompatibilityWalEnabled: Boolean
            get() = journalMode == null && syncMode == null && openFlags.contains(ENABLE_LEGACY_COMPATIBILITY_WAL)

        public val SqliteDatabaseConfiguration.isLookasideConfigSet: Boolean
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
        public fun SqliteDatabaseConfiguration.resolveJournalMode(): SQLiteDatabaseJournalMode? {
            if (isReadOnlyDatabase) {
                // No need to specify a journal mode when only reading.
                return null
            }

            if (isInMemoryDb) {
                return if (journalMode == SQLiteDatabaseJournalMode.OFF) {
                    SQLiteDatabaseJournalMode.OFF
                } else {
                    SQLiteDatabaseJournalMode.MEMORY
                }
            }

            shouldTruncateWalFile = false

            return if (isWalEnabledInternal()) {
                shouldTruncateWalFile = true
                SQLiteDatabaseJournalMode.WAL
            } else {
                // WAL is not explicitly set so use requested journal mode or platform default
                this.journalMode ?: SQLiteGlobal.defaultJournalMode
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
        public fun SqliteDatabaseConfiguration.resolveSyncMode(): SQLiteDatabaseSyncMode? {
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
                if (isLegacyCompatibilityWalEnabled) {
                    SQLiteCompatibilityWalFlags.walSyncMode
                } else {
                    SQLiteGlobal.walSyncMode
                }
            } else {
                SQLiteGlobal.defaultSyncMode
            }
        }

        private fun SqliteDatabaseConfiguration.isWalEnabledInternal(): Boolean {
            val walEnabled = openFlags.contains(ENABLE_WRITE_AHEAD_LOGGING)
            // Use compatibility WAL unless an app explicitly set journal/synchronous mode
            // or DISABLE_COMPATIBILITY_WAL flag is set
            return walEnabled || isLegacyCompatibilityWalEnabled || (journalMode == SQLiteDatabaseJournalMode.WAL)
        }
    }
}
