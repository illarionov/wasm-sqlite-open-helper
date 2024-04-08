/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import androidx.annotation.VisibleForTesting
import ru.pixnews.wasm.sqlite.open.helper.SQLiteDatabaseSyncMode

/**
 * Helper class for accessing
 * [global compatibility WAL settings][Settings.Global.SQLITE_COMPATIBILITY_WAL_FLAGS].
 *
 * legacy_compatibility_wal_enabled: false
 * walSyncMode: SQLiteGlobal.getWALSyncMode()
 * truncate_size: -1
 */
@Suppress("OBJECT_NAME_INCORRECT")
public object SQLiteCompatibilityWalFlags {
    @get:VisibleForTesting
    public val isLegacyCompatibilityWalEnabled: Boolean = false

    // Make sense only if isLegacyCompatibilityWalEnabled = true
    // TODO: remove
    @Suppress("NULLABLE_PROPERTY_TYPE")
    internal val walSyncMode: SQLiteDatabaseSyncMode? = null

    /**
     * Override [com.android.internal.R.integer.db_wal_truncate_size].
     * When opening a database, if the WAL file is larger than this size, we'll truncate it.
     * (If it's 0, we do not truncate.)
     *
     * @return the value set in the global setting, or -1 if a value is not set.
     *
     * @hide
     */
    internal val truncateSize: Long = -1
}
