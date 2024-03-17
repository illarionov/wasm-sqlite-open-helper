/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDatabase
import java.util.Locale
import java.util.regex.Pattern

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
 */
public class SqliteDatabaseConfiguration {
    /**
     * The database path.
     */
    public val path: String

    /**
     * The label to use to describe the database when it appears in logs.
     * This is derived from the path but is stripped to remove PII.
     */
    public val label: String

    /**
     * The flags used to open the database.
     */
    public var openFlags: OpenFlags = OpenFlags.EMPTY

    /**
     * The maximum size of the prepared statement cache for each database connection.
     * Must be non-negative.
     *
     * Default is 25.
     */
    public var maxSqlCacheSize: Int = 0

    /**
     * The database locale.
     *
     * Default is the value returned by [Locale.getDefault].
     */
    public var locale: Locale? = null

    /**
     * True if foreign key constraints are enabled.
     *
     * Default is false.
     */
    public var foreignKeyConstraintsEnabled: Boolean = false

    public val isInMemoryDb: Boolean
        /**
         * Returns true if the database is in-memory.
         *
         * @return True if the database is in-memory.
         */
        get() = path.equals(MEMORY_DB_PATH, ignoreCase = true)

    /**
     * Creates a database configuration with the required parameters for opening a
     * database and default values for all other parameters.
     *
     * @param path The database path.
     * @param openFlags Open flags for the database, such as [SQLiteDatabase.OPEN_READWRITE].
     */
    public constructor(
        path: String = MEMORY_DB_PATH,
        openFlags: OpenFlags,
    ) {
        this.path = path
        this.openFlags = openFlags
        label = stripPathForLogs(path)

        // Set default values for optional parameters.
        maxSqlCacheSize = @Suppress("MagicNumber") 25
        locale = Locale.getDefault()
    }

    /**
     * Creates a database configuration as a copy of another configuration.
     *
     * @param other The other configuration.
     */
    internal constructor(other: SqliteDatabaseConfiguration) {
        this.path = other.path
        this.label = other.label
        updateParametersFrom(other)
    }

    /**
     * Updates the non-immutable parameters of this configuration object
     * from the other configuration object.
     *
     * @param other The object from which to copy the parameters.
     */
    public fun updateParametersFrom(other: SqliteDatabaseConfiguration?) {
        requireNotNull(other) { "other must not be null." }
        require(path == other.path) { "other configuration must refer to the same database." }

        openFlags = other.openFlags
        maxSqlCacheSize = other.maxSqlCacheSize
        locale = other.locale
        foreignKeyConstraintsEnabled = other.foreignKeyConstraintsEnabled
    }

    public companion object {
        /**
         * Special path used by in-memory databases.
         */
        public const val MEMORY_DB_PATH: String = ":memory:"

        // The pattern we use to strip email addresses from database paths
        // when constructing a label to use in log messages.
        private val EMAIL_IN_DB_PATTERN: Pattern = Pattern.compile("[\\w\\.\\-]+@[\\w\\.\\-]+")

        private fun stripPathForLogs(path: String): String {
            if (path.indexOf('@') == -1) {
                return path
            }
            return EMAIL_IN_DB_PATTERN.matcher(path).replaceAll("XX@YY")
        }
    }
}
