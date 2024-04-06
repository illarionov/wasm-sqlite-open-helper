/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import ru.pixnews.wasm.sqlite.open.helper.OpenFlags
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.ENABLE_WRITE_AHEAD_LOGGING
import ru.pixnews.wasm.sqlite.open.helper.SQLiteDatabaseJournalMode
import ru.pixnews.wasm.sqlite.open.helper.SQLiteDatabaseSyncMode
import ru.pixnews.wasm.sqlite.open.helper.base.DatabaseErrorHandler
import ru.pixnews.wasm.sqlite.open.helper.common.api.Locale
import ru.pixnews.wasm.sqlite.open.helper.common.api.clear
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.common.api.or
import ru.pixnews.wasm.sqlite.open.helper.dsl.OpenParamsBlock

/**
 * Wrapper for configuration parameters that are used for opening [SQLiteDatabase]
 *
 * @param openFlags flags to control database access mode. Default value is 0.
 * @param locale locale to open database
 * @param errorHandler handler for database corruption errors
 * @param lookasideSlotSize size in bytes of each lookaside slot or -1 if not set.
 * @param lookasideSlotCount total number of lookaside memory slots per database connection or -1 if not set
 * @param journalMode [journal mode](https://sqlite.org/pragma.html#pragma_journal_mode)
 * @param synchronousMode [synchronous mode](https://sqlite.org/pragma.html#pragma_synchronous).
 */
internal class SQLiteDatabaseOpenParams private constructor(
    val openFlags: OpenFlags,
    val locale: Locale,
    val errorHandler: DatabaseErrorHandler?,
    val lookasideSlotSize: Int,
    val lookasideSlotCount: Int,
    val journalMode: SQLiteDatabaseJournalMode?,
    val synchronousMode: SQLiteDatabaseSyncMode?,
) {
    /**
     * Returns maximum number of milliseconds that SQLite connection is allowed to be idle
     * before it is closed and removed from the pool.
     *
     * If the value isn't set, the timeout defaults to the system wide timeout
     *
     * @return timeout in milliseconds or -1 if the value wasn't set.
     */
    val idleConnectionTimeout: Long = -1

    /**
     * Creates a new instance of builder [initialized][Builder] with
     * `this` parameters.
     */
    fun toBuilder(): Builder {
        return Builder(this)
    }

    /**
     * Builder for [SQLiteOpenParams].
     */
    class Builder(
        params: SQLiteDatabaseOpenParams? = null,
    ) {
        var lookasideSlotSize = -1
            private set
        var lookasideSlotCount = -1
            private set
        var openFlags: OpenFlags = OpenFlags.EMPTY
        var locale: Locale = Locale.EN_US
        var errorHandler: DatabaseErrorHandler? = null
        var journalMode: SQLiteDatabaseJournalMode? = null
        var syncMode: SQLiteDatabaseSyncMode? = null

        @Suppress("NO_CORRESPONDING_PROPERTY")
        var isWriteAheadLoggingEnabled: Boolean
            /**
             * Returns true if [.ENABLE_WRITE_AHEAD_LOGGING] flag is set
             */
            get() = openFlags.contains(ENABLE_WRITE_AHEAD_LOGGING)

            /**
             * Sets [.ENABLE_WRITE_AHEAD_LOGGING] flag if `enabled` is `true`,
             * unsets otherwise
             */
            set(enabled) {
                if (enabled) {
                    addOpenFlags(ENABLE_WRITE_AHEAD_LOGGING)
                } else {
                    removeOpenFlags(ENABLE_WRITE_AHEAD_LOGGING)
                }
            }

        init {
            params?.let(::set)
        }

        internal fun set(params: SQLiteDatabaseOpenParams) {
            lookasideSlotSize = params.lookasideSlotSize
            lookasideSlotCount = params.lookasideSlotCount
            openFlags = params.openFlags
            locale = params.locale
            errorHandler = params.errorHandler
            journalMode = params.journalMode
            syncMode = params.synchronousMode
        }

        internal fun set(debugConfigBlock: OpenParamsBlock) {
            lookasideSlotSize = debugConfigBlock.lookasideSlotSize
            lookasideSlotCount = debugConfigBlock.lookasideSlotCount
            openFlags = debugConfigBlock.openFlags
            locale = debugConfigBlock.locale
            journalMode = debugConfigBlock.journalMode
            syncMode = debugConfigBlock.syncMode
        }

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
        fun setLookasideConfig(
            slotSize: Int,
            slotCount: Int,
        ): Builder = apply {
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
        fun addOpenFlags(openFlags: OpenFlags): Builder = apply {
            this.openFlags = this.openFlags or openFlags
        }

        /**
         * Removes database access mode flags
         *
         * @param openFlags Flags to remove
         * @return same builder instance for chaining multiple calls into a single statement
         */
        fun removeOpenFlags(openFlags: OpenFlags): Builder = apply {
            this.openFlags = this.openFlags.clear(openFlags)
        }

        /**
         * Creates an instance of [SQLiteDatabaseOpenParams] with the options that were previously set
         * on this builder
         */
        fun build(): SQLiteDatabaseOpenParams {
            return SQLiteDatabaseOpenParams(
                openFlags = openFlags,
                locale = locale,
                errorHandler = errorHandler,
                lookasideSlotSize = lookasideSlotSize,
                lookasideSlotCount = lookasideSlotCount,
                journalMode = journalMode,
                synchronousMode = syncMode,
            )
        }
    }
}
