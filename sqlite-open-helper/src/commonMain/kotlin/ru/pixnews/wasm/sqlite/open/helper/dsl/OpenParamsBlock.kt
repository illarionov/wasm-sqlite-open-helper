/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.dsl

import ru.pixnews.wasm.sqlite.open.helper.OpenFlags
import ru.pixnews.wasm.sqlite.open.helper.SQLiteDatabaseJournalMode
import ru.pixnews.wasm.sqlite.open.helper.SQLiteDatabaseSyncMode
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import ru.pixnews.wasm.sqlite.open.helper.common.api.Locale
import ru.pixnews.wasm.sqlite.open.helper.common.api.clear
import ru.pixnews.wasm.sqlite.open.helper.common.api.or

@WasmSqliteOpenHelperDsl
public class OpenParamsBlock {
    /**
     * Flags to control database access mode. Default value is 0.
     */
    public var openFlags: OpenFlags = OpenFlags.EMPTY

    /**
     * Default locale to open the database
     */
    public var locale: Locale = Locale.EN_US

    /**
     * Size in bytes of each lookaside slot or -1 if not set.
     */
    public var lookasideSlotSize: Int = -1
        private set

    /**
     * Total number of lookaside memory slots per database connection or -1 if not set
     */
    public var lookasideSlotCount: Int = -1
        private set

    /**
     * [journal mode](https://sqlite.org/pragma.html#pragma_journal_mode)
     */
    public var journalMode: SQLiteDatabaseJournalMode? = null

    /**
     * [synchronous mode](https://sqlite.org/pragma.html#pragma_synchronous).
     */
    public var syncMode: SQLiteDatabaseSyncMode? = null

    /**
     * Configures [lookaside memory allocator](https://sqlite.org/malloc.html#lookaside)
     *
     * SQLite default settings will be used, if this method isn't called.
     * Use `setLookasideConfig(0,0)` to disable lookaside
     *
     * **Note:** Provided slotSize/slotCount configuration is just a
     * recommendation. The system may choose different values depending on a device, e.g.
     * lookaside allocations can be disabled on low-RAM devices
     *
     * @param slotSize The size in bytes of each lookaside slot.
     * @param slotCount The total number of lookaside memory slots per database connection.
     */
    public fun setLookasideConfig(
        slotSize: Int,
        slotCount: Int,
    ): OpenParamsBlock = apply {
        check((slotSize > 0 && slotCount > 0) || (slotCount == 0 && slotSize == 0)) {
            "Invalid configuration: $slotSize, $slotCount"
        }
        lookasideSlotSize = slotSize
        lookasideSlotCount = slotCount
    }

    /**
     * Adds flags to control database access mode
     *
     * @param openFlags The new flags to add
     * @return same builder instance for chaining multiple calls into a single statement
     */
    public fun addOpenFlags(openFlags: OpenFlags): OpenParamsBlock = apply {
        this.openFlags = this.openFlags or openFlags
    }

    /**
     * Removes database access mode flags
     *
     * @param openFlags Flags to remove
     * @return same builder instance for chaining multiple calls into a single statement
     */
    public fun removeOpenFlags(openFlags: OpenFlags): OpenParamsBlock = apply {
        this.openFlags = this.openFlags.clear(openFlags)
    }
}
