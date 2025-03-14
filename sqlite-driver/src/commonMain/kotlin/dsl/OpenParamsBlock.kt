/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.dsl

import at.released.wasm.sqlite.driver.dsl.OpenFlags.ANDROID_FUNCTIONS
import at.released.wasm.sqlite.driver.dsl.OpenFlags.LOCALIZED_COLLATORS
import at.released.wasm.sqlite.open.helper.Locale
import at.released.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl

/**
 * Parameters used when opening the database
 */
@WasmSqliteOpenHelperDsl
public class OpenParamsBlock {
    /**
     * Flags to control database access mode.
     *
     * Default: LOCALIZED_COLLATORS, ANDROID_FUNCTIONS
     */
    public var openFlags: Set<OpenFlags> = setOf(LOCALIZED_COLLATORS, ANDROID_FUNCTIONS)

    /**
     * Default locale to open the database.
     *
     * Default: en_us
     */
    public var locale: Locale = Locale.EN_US

    /**
     * Size in bytes of each lookaside slot or -1 if not set.
     *
     * Default: not set
     */
    public var lookasideSlotSize: Int = -1
        private set

    /**
     * Total number of lookaside memory slots per database connection or -1 if not set
     *
     * Default: not set
     */
    public var lookasideSlotCount: Int = -1
        private set

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
}
