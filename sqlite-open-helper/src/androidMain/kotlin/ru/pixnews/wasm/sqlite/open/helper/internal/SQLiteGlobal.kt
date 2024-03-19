/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

/**
 * Provides access to SQLite functions that affect all database connection,
 * such as memory management.
 *
 *
 * The native code associated with SQLiteGlobal is also sets global configuration options
 * using sqlite3_config() then calls sqlite3_initialize() to ensure that the SQLite
 * library is properly initialized exactly once before any other framework or application
 * code has a chance to run.
 *
 *
 * Verbose SQLite logging is enabled if the "log.tag.SQLiteLog" property is set to "V".
 * (per [SQLiteDebug.DEBUG_SQL_LOG]).
 *
 * @hide
 */
@Suppress("OBJECT_NAME_INCORRECT")
internal object SQLiteGlobal {
    // values derived from:
    // https://android.googlesource.com/platform/frameworks/base.git/+/master/core/res/res/values/config.xml
    const val defaultPageSize: Int = 1024

    /**
     * Gets the default journal mode when WAL is not in use.
     */
    const val defaultJournalMode: String = "TRUNCATE"

    /**
     * Gets the journal size limit in bytes.
     */
    const val journalSizeLimit: Int = 524288

    /**
     * Gets the default database synchronization mode when WAL is not in use.
     */
    const val defaultSyncMode: String = "FULL"

    /**
     * Gets the database synchronization mode when in WAL mode.
     */
    const val wALSyncMode: String = "normal"

    /**
     * Gets the WAL auto-checkpoint integer in database pages.
     */
    const val wALAutoCheckpoint: Int = 1000

    /**
     * Gets the connection pool size when in WAL mode.
     */
    const val wALConnectionPoolSize: Int = 10

    // private external fun nativeReleaseMemory(): Int
}
